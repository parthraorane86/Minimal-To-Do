package com.personal.minimaltodo.viewmodel

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.minimaltodo.data.FontSizeOption
import com.personal.minimaltodo.data.HapticStrength
import com.personal.minimaltodo.data.ScheduleType
import com.personal.minimaltodo.data.TaskEntity
import com.personal.minimaltodo.data.ThemeMode
import com.personal.minimaltodo.data.ThemePrefs
import com.personal.minimaltodo.data.TodoRepository
import com.personal.minimaltodo.data.UserSettingsEntity
import com.personal.minimaltodo.notifications.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class HomeTaskItem(
    val task: TaskEntity,
    val isCompleted: Boolean
)

@Immutable
data class ManageTaskItem(
    val task: TaskEntity,
    val isOneTimeCompleted: Boolean
)

@Immutable
data class DayProgress(
    val date: LocalDate,
    val scheduledTasks: List<HomeTaskItem>
) {
    val totalCount: Int get() = scheduledTasks.size
    val completedCount: Int get() = scheduledTasks.count { it.isCompleted }
    val pendingCount: Int get() = totalCount - completedCount
    val completionRatio: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModel(
    private val appContext: Context,
    private val repository: TodoRepository
) : ViewModel() {

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    val displayedMonth: StateFlow<YearMonth> = _displayedMonth.asStateFlow()

    private val _selectedReportDate = MutableStateFlow(LocalDate.now())
    val selectedReportDate: StateFlow<LocalDate> = _selectedReportDate.asStateFlow()

    private val initialSettings = UserSettingsEntity(
        themeMode = ThemePrefs.getThemeMode(appContext),
        fontSizeOption = ThemePrefs.getFontSize(appContext)
    )

    val settings: StateFlow<UserSettingsEntity> = repository.settings
        .onEach { loaded ->
            if (ThemePrefs.getThemeMode(appContext) != loaded.themeMode) {
                ThemePrefs.setThemeMode(appContext, loaded.themeMode)
            }
            if (ThemePrefs.getFontSize(appContext) != loaded.fontSizeOption) {
                ThemePrefs.setFontSize(appContext, loaded.fontSizeOption)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialSettings
        )

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val manageTaskItems: StateFlow<List<ManageTaskItem>> = combine(
        allTasks,
        repository.completedDatesByTaskId
    ) { tasks, completedMap ->
        tasks
            .map { task ->
                val doneDates = completedMap[task.id].orEmpty()
                ManageTaskItem(
                    task = task,
                    isOneTimeCompleted = task.isOneTimeFullyCompleted(doneDates)
                )
            }
            .sortedBy { it.isOneTimeCompleted }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val todayCompletions: StateFlow<Set<Long>> = _today
        .flatMapLatest { date -> repository.observeCompletionsForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    val todayTasks: StateFlow<List<HomeTaskItem>> = combine(
        allTasks,
        todayCompletions,
        _today
    ) { tasks, completedIds, date ->
        tasks
            .filter { it.isScheduledFor(date, respectCreationDate = false) }
            .map { HomeTaskItem(task = it, isCompleted = it.id in completedIds) }
            .sortedBy { it.isCompleted }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val todayProgress: StateFlow<DayProgress> = combine(
        todayTasks,
        _today
    ) { items, date ->
        DayProgress(date = date, scheduledTasks = items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DayProgress(LocalDate.now(), emptyList())
    )

    private val monthCompletions: StateFlow<Map<Long, Set<Long>>> = _displayedMonth
        .flatMapLatest { month -> repository.observeCompletionsForMonth(month) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    val monthProgressMap: StateFlow<Map<LocalDate, DayProgress>> = combine(
        allTasks,
        monthCompletions,
        _displayedMonth
    ) { tasks, completionsByEpochDay, month ->
        buildMap {
            for (day in 1..month.lengthOfMonth()) {
                val date = month.atDay(day)
                val completedIds = completionsByEpochDay[date.toEpochDay()].orEmpty()
                val scheduled = tasks
                    .filter { task ->
                        task.isScheduledFor(date, respectCreationDate = true) ||
                            task.id in completedIds
                    }
                    .map { HomeTaskItem(task = it, isCompleted = it.id in completedIds) }
                put(date, DayProgress(date = date, scheduledTasks = scheduled))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    fun refreshToday() {
        val current = LocalDate.now()
        if (_today.value != current) {
            _today.value = current
        }
    }

    fun addTask(
        name: String,
        scheduleType: ScheduleType,
        selectedDays: Set<DayOfWeek>,
        oneTimeDates: List<LocalDate>
    ) {
        viewModelScope.launch {
            repository.addTask(
                name = name,
                scheduleType = scheduleType,
                selectedDays = selectedDays,
                oneTimeDates = oneTimeDates
            )
        }
    }

    fun updateTask(
        existing: TaskEntity,
        name: String,
        scheduleType: ScheduleType,
        selectedDays: Set<DayOfWeek>,
        oneTimeDates: List<LocalDate>
    ) {
        viewModelScope.launch {
            repository.updateTask(
                existing = existing,
                name = name,
                scheduleType = scheduleType,
                selectedDays = selectedDays,
                oneTimeDates = oneTimeDates
            )
        }
    }

    fun reorderTodayTasks(reorderedIds: List<Long>) {
        val currentAll = allTasks.value
        if (currentAll.isEmpty() || reorderedIds.isEmpty()) return

        val reorderedSet = reorderedIds.toSet()
        val taskById = currentAll.associateBy { it.id }
        val reorderedQueue = ArrayDeque(reorderedIds.mapNotNull { taskById[it] })

        val merged = currentAll.map { existing ->
            if (existing.id in reorderedSet && reorderedQueue.isNotEmpty()) {
                reorderedQueue.removeFirst()
            } else {
                existing
            }
        }

        viewModelScope.launch {
            repository.updateTaskOrder(merged)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(taskId: Long, currentlyCompleted: Boolean, date: LocalDate = _today.value) {
        viewModelScope.launch {
            repository.setTaskCompleted(
                taskId = taskId,
                date = date,
                completed = !currentlyCompleted
            )
        }
    }

    fun previousMonth() {
        _displayedMonth.value = _displayedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _displayedMonth.value = _displayedMonth.value.plusMonths(1)
    }

    fun jumpToToday() {
        val now = LocalDate.now()
        _today.value = now
        _selectedReportDate.value = now
        _displayedMonth.value = YearMonth.from(now)
    }

    fun selectReportDate(date: LocalDate) {
        _selectedReportDate.value = date
        val targetMonth = YearMonth.from(date)
        if (_displayedMonth.value != targetMonth) {
            _displayedMonth.value = targetMonth
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(userName = name) }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        ThemePrefs.setThemeMode(appContext, mode)
        viewModelScope.launch {
            repository.updateSettings { it.copy(themeMode = mode) }
        }
    }

    fun updateFontSize(option: FontSizeOption) {
        ThemePrefs.setFontSize(appContext, option)
        viewModelScope.launch {
            repository.updateSettings { it.copy(fontSizeOption = option) }
        }
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(hapticsEnabled = enabled) }
        }
    }

    fun updateHapticsStrength(strength: HapticStrength) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(hapticsStrength = strength) }
        }
    }

    fun updateNotificationsEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(notificationsEnabled = enabled) }
            ReminderScheduler.sync(context, enabled)
        }
    }

    class Factory(
        private val appContext: Context,
        private val repository: TodoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
                return TodoViewModel(appContext.applicationContext, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
