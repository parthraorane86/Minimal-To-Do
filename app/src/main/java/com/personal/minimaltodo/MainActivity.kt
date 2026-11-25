package com.personal.minimaltodo

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.minimaltodo.data.ThemeMode
import com.personal.minimaltodo.data.ThemePrefs
import com.personal.minimaltodo.ui.components.SegmentedPillControl
import com.personal.minimaltodo.ui.haptics.HapticEngine
import com.personal.minimaltodo.ui.haptics.LocalHapticEngine
import com.personal.minimaltodo.ui.screens.HomeScreen
import com.personal.minimaltodo.ui.screens.ReportScreen
import com.personal.minimaltodo.ui.screens.SettingsScreen
import com.personal.minimaltodo.ui.screens.TaskScreen
import com.personal.minimaltodo.ui.theme.DarkMinimalPalette
import com.personal.minimaltodo.ui.theme.LightMinimalPalette
import com.personal.minimaltodo.ui.theme.LocalMinimalPalette
import com.personal.minimaltodo.ui.theme.MinimalTodoTheme
import com.personal.minimaltodo.ui.theme.MotionSpecs
import com.personal.minimaltodo.viewmodel.TodoViewModel

enum class AppTab(val label: String) {
    TASK("Task"),
    HOME("Home"),
    REPORT("Report")
}

class MainActivity : ComponentActivity() {

    private val viewModel: TodoViewModel by viewModels {
        TodoViewModel.Factory(applicationContext, (application as MinimalTodoApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val savedTheme = ThemePrefs.getThemeMode(this)
        val systemIsDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val initialDark = when (savedTheme) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemIsDark
        }
        val initialBg = if (initialDark) DarkMinimalPalette.background else LightMinimalPalette.background
        window.decorView.setBackgroundColor(initialBg.toArgb())

        val initialBarStyle = if (initialDark) {
            SystemBarStyle.dark(Color.Transparent.toArgb())
        } else {
            SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
        }
        enableEdgeToEdge(statusBarStyle = initialBarStyle, navigationBarStyle = initialBarStyle)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val hapticEngine = remember(settings.hapticsEnabled, settings.hapticsStrength) {
                HapticEngine(
                    context = applicationContext,
                    enabled = settings.hapticsEnabled,
                    strength = settings.hapticsStrength
                )
            }

            CompositionLocalProvider(LocalHapticEngine provides hapticEngine) {
                MinimalTodoTheme(
                    themeMode = settings.themeMode,
                    fontSizeOption = settings.fontSizeOption
                ) {
                    val palette = LocalMinimalPalette.current
                    SideEffect {
                        window.decorView.setBackgroundColor(palette.background.toArgb())
                        val barStyle = if (palette.isDark) {
                            SystemBarStyle.dark(Color.Transparent.toArgb())
                        } else {
                            SystemBarStyle.light(
                                Color.Transparent.toArgb(),
                                Color.Transparent.toArgb()
                            )
                        }
                        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                    }

                    MinimalTodoRoot(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshToday()
    }
}

@Composable
private fun MinimalTodoRoot(viewModel: TodoViewModel) {
    val palette = LocalMinimalPalette.current
    val haptic = LocalHapticEngine.current
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val manageTaskItems by viewModel.manageTaskItems.collectAsStateWithLifecycle()
    val todayTasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    val todayProgress by viewModel.todayProgress.collectAsStateWithLifecycle()
    val displayedMonth by viewModel.displayedMonth.collectAsStateWithLifecycle()
    val selectedReportDate by viewModel.selectedReportDate.collectAsStateWithLifecycle()
    val monthProgressMap by viewModel.monthProgressMap.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(
                        animationSpec = MotionSpecs.snappySpring(),
                        initialOffsetX = { fullWidth -> (fullWidth * 0.14f * direction).toInt() }
                    ) + fadeIn(tween(220))) togetherWith (slideOutHorizontally(
                        animationSpec = MotionSpecs.snappySpring(),
                        targetOffsetX = { fullWidth -> (-fullWidth * 0.14f * direction).toInt() }
                    ) + fadeOut(tween(180)))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "mainTabTransition"
            ) { tab ->
                when (tab) {
                    AppTab.TASK -> TaskScreen(
                        taskItems = manageTaskItems,
                        onAddTask = viewModel::addTask,
                        onUpdateTask = viewModel::updateTask,
                        onDeleteTask = viewModel::deleteTask
                    )

                    AppTab.HOME -> HomeScreen(
                        userName = settings.userName,
                        today = today,
                        tasks = todayTasks,
                        onToggleTask = { id, isCompleted ->
                            viewModel.toggleTaskCompletion(id, isCompleted)
                        },
                        onReorderTasks = viewModel::reorderTodayTasks,
                        onOpenSettings = { showSettings = true }
                    )

                    AppTab.REPORT -> ReportScreen(
                        today = today,
                        todayProgress = todayProgress,
                        displayedMonth = displayedMonth,
                        selectedDate = selectedReportDate,
                        monthProgressMap = monthProgressMap,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        onJumpToToday = viewModel::jumpToToday,
                        onSelectDate = viewModel::selectReportDate
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                SegmentedPillControl(
                    items = AppTab.entries,
                    selectedItem = currentTab,
                    onSelect = { tab ->
                        if (tab != currentTab) {
                            haptic.tick()
                            currentTab = tab
                        }
                    },
                    labelProvider = { it.label },
                    height = 50.dp,
                    cornerRadius = 16.dp,
                    showAccentDot = true
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(
                animationSpec = MotionSpecs.snappySpring(),
                initialOffsetX = { -it / 3 }
            ) + fadeIn(tween(220)),
            exit = slideOutHorizontally(
                animationSpec = MotionSpecs.snappySpring(),
                targetOffsetX = { -it / 3 }
            ) + fadeOut(tween(180))
        ) {
            SettingsScreen(
                settings = settings,
                onUpdateName = viewModel::updateUserName,
                onUpdateThemeMode = viewModel::updateThemeMode,
                onUpdateNotifications = { enabled ->
                    viewModel.updateNotificationsEnabled(context, enabled)
                },
                onUpdateFontSize = viewModel::updateFontSize,
                onUpdateHapticsEnabled = viewModel::updateHapticsEnabled,
                onUpdateHapticsStrength = viewModel::updateHapticsStrength,
                onBack = { showSettings = false }
            )
        }
    }
}
