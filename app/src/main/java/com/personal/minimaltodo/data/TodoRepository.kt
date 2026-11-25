package com.personal.minimaltodo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class TodoRepository(
    private val taskDao: TaskDao,
    private val settingsDao: SettingsDao
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.observeAllTasks()

    val settings: Flow<UserSettingsEntity> = settingsDao.observeSettings().map {
        it ?: UserSettingsEntity()
    }

    val completedDatesByTaskId: Flow<Map<Long, Set<Long>>> = taskDao.observeAllCompletions().map { rows ->
        rows.groupBy(
            keySelector = { it.taskId },
            valueTransform = { it.dateEpochDay }
        ).mapValues { it.value.toSet() }
    }

    fun observeCompletionsForDate(date: LocalDate): Flow<Set<Long>> {
        return taskDao.observeCompletionsForDate(date.toEpochDay()).map { list ->
            list.map { it.taskId }.toSet()
        }
    }

    fun observeCompletionsForMonth(yearMonth: YearMonth): Flow<Map<Long, Set<Long>>> {
        val start = yearMonth.atDay(1).toEpochDay()
        val end = yearMonth.atEndOfMonth().toEpochDay()
        return taskDao.observeCompletionsInRange(start, end).map { rows ->
            rows.groupBy(
                keySelector = { it.dateEpochDay },
                valueTransform = { it.taskId }
            ).mapValues { it.value.toSet() }
        }
    }

    suspend fun addTask(
        name: String,
        scheduleType: ScheduleType,
        selectedDays: Set<DayOfWeek>,
        oneTimeDates: List<LocalDate>
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        val distinctDates = oneTimeDates.distinct().sorted()
        val resolvedType = when (scheduleType) {
            ScheduleType.WEEKLY -> if (selectedDays.isEmpty()) ScheduleType.DAILY else ScheduleType.WEEKLY
            ScheduleType.ONE_TIME -> if (distinctDates.isEmpty()) ScheduleType.DAILY else ScheduleType.ONE_TIME
            ScheduleType.DAILY -> ScheduleType.DAILY
        }

        val minOrder = taskDao.getMinSortOrder()
        val entity = TaskEntity(
            name = trimmed,
            scheduleType = resolvedType,
            daysOfWeekMask = if (resolvedType == ScheduleType.WEEKLY) {
                TaskEntity.maskFromDays(selectedDays)
            } else {
                0
            },
            oneTimeDateEpochDay = if (resolvedType == ScheduleType.ONE_TIME) {
                distinctDates.firstOrNull()?.toEpochDay()
            } else {
                null
            },
            createdAtEpochDay = LocalDate.now().toEpochDay(),
            specificDatesCsv = if (resolvedType == ScheduleType.ONE_TIME) {
                TaskEntity.csvFromDates(distinctDates)
            } else {
                ""
            },
            sortOrder = minOrder - 1
        )
        taskDao.insertTask(entity)
    }

    suspend fun updateTask(
        existing: TaskEntity,
        name: String,
        scheduleType: ScheduleType,
        selectedDays: Set<DayOfWeek>,
        oneTimeDates: List<LocalDate>
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        val distinctDates = oneTimeDates.distinct().sorted()
        val resolvedType = when (scheduleType) {
            ScheduleType.WEEKLY -> if (selectedDays.isEmpty()) ScheduleType.DAILY else ScheduleType.WEEKLY
            ScheduleType.ONE_TIME -> if (distinctDates.isEmpty()) ScheduleType.DAILY else ScheduleType.ONE_TIME
            ScheduleType.DAILY -> ScheduleType.DAILY
        }

        val updated = existing.copy(
            name = trimmed,
            scheduleType = resolvedType,
            daysOfWeekMask = if (resolvedType == ScheduleType.WEEKLY) {
                TaskEntity.maskFromDays(selectedDays)
            } else {
                0
            },
            oneTimeDateEpochDay = if (resolvedType == ScheduleType.ONE_TIME) {
                distinctDates.firstOrNull()?.toEpochDay()
            } else {
                null
            },
            specificDatesCsv = if (resolvedType == ScheduleType.ONE_TIME) {
                TaskEntity.csvFromDates(distinctDates)
            } else {
                ""
            }
        )
        taskDao.updateTask(updated)
    }

    suspend fun updateTaskOrder(orderedTasks: List<TaskEntity>) {
        val updated = orderedTasks.mapIndexed { index, task ->
            task.copy(sortOrder = index)
        }
        taskDao.updateTasks(updated)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun setTaskCompleted(taskId: Long, date: LocalDate, completed: Boolean) {
        val epochDay = date.toEpochDay()
        if (completed) {
            taskDao.insertCompletion(TaskCompletionEntity(taskId = taskId, dateEpochDay = epochDay))
        } else {
            taskDao.deleteCompletion(taskId = taskId, epochDay = epochDay)
        }
    }

    suspend fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        val current = settingsDao.getSettingsOnce() ?: UserSettingsEntity()
        settingsDao.upsertSettings(transform(current))
    }

    suspend fun getTodayPendingTaskNames(today: LocalDate = LocalDate.now()): List<String> {
        val tasks = taskDao.getAllTasksOnce().filter { it.isScheduledFor(today) }
        val completedIds = taskDao.getCompletionsForDateOnce(today.toEpochDay())
            .map { it.taskId }
            .toSet()
        return tasks.filter { it.id !in completedIds }.map { it.name }
    }
}
