# Minimal Todo (Android — Kotlin + Jetpack Compose)

A personal-use Android to-do list app built with **Kotlin**, **Jetpack Compose**, **Room**, **MVVM**, and **WorkManager**, following your exact palette, screen structure, Uiverse-inspired controls, and spring-physics animation spec.

## Opening & Running in Android Studio

1. Unzip `MinimalTodoAndroid.zip` to a local folder.
2. Open **Android Studio** (Ladybug / 2024.2 or newer recommended) → **Open** → select the `MinimalTodo/` folder.
3. Let Gradle sync download dependencies (AGP `8.7.2`, Kotlin `2.0.21`, Compose BOM `2024.10.01`, Room `2.6.1`, WorkManager `2.9.1`).
4. Connect a physical Android 8.0+ (API 26+) device (recommended in your spec to verify 60fps+ spring animations) and click **Run**.

## Project Structure

- `app/src/main/java/com/personal/minimaltodo/`
  - `MainActivity.kt` — Edge-to-edge host, directional spring tab transitions (`Task | Home | Report`), and Settings overlay.
  - `MinimalTodoApp.kt` — Application class initializing Room database and notification channel.
  - `data/Models.kt` — `TaskEntity`, `TaskCompletionEntity`, `UserSettingsEntity`, schedule logic, and Room `Converters`.
  - `data/TaskDao.kt` & `data/AppDatabase.kt` & `data/TodoRepository.kt` — Reactive Room persistence with `Flow`.
  - `viewmodel/TodoViewModel.kt` — MVVM state holder for Home, Task, Report, and Settings.
  - `notifications/TaskReminderWorker.kt` & `ReminderScheduler.kt` — Local daily task reminders via WorkManager.
  - `ui/theme/Theme.kt` — Exact Light & Dark sage/neutral palettes, animated theme colors, spring `MotionSpecs`, and dynamic `FontSizeOption` scaling.
  - `ui/components/UiverseComponents.kt` — Compose adaptations of the 3 Uiverse references (`DarkModeSunMoonSwitch`, `MinimalInputField`, `SegmentedPillControl`), plus `AnimatedCheckbox`, `MinimalToggleSwitch`, and `TactileButton`.
  - `ui/screens/HomeScreen.kt` — Today-only task list, animated tickboxes, greeting, and top-left profile icon.
  - `ui/screens/TaskScreen.kt` — Task input, expandable Advanced Settings (Repeat Weekly chips / One-Time date picker), and task management list.
  - `ui/screens/ReportScreen.kt` — Custom Canvas animated pie chart for today + historical progress calendar with day breakdown.
  - `ui/screens/SettingsScreen.kt` — Name, Dark Mode sun/moon switch + Follow System, Notifications toggle (with Android 13+ permission handling), and Font Size selector.
