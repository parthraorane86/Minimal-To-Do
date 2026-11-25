# Minimal To-Do

A calm, personal-use Android to-do list app built with **Kotlin** and **Jetpack Compose**, designed around zero clutter, spring-physics animations, and tactile haptic feedback.

No tags, no priority labels, no cloud accounts, and no bloat—just today's tasks, flexible scheduling, and a clean progress report.

## Features

* **Home (Today-Only View)**

  * Displays only tasks scheduled for today (everyday, matching weekly days, or matching specific dates).

  * Animated spring checkmarks with strikethrough and fade transitions.

  * **Drag-and-drop priority ordering**: long-press and drag any task to reorder your day; order persists locally.

  * Personalized greeting (`Hi, [Name]`), today's compact date (`Wed, Nov 25`) aligned to the top-right across from the profile icon, and a clean empty state.

* **Tasks (Create & Manage)**

  * Quick task creation (defaults to **Every day** if Advanced Settings is skipped).

  * **Repeat Weekly**: multi-select Mon–Sun toggle chips.

  * **One Time / Multi-Date**: pick one or multiple specific dates with **+ Add another date**.

  * **Quarter-second hold to edit**: hold any task for 250ms (with rapid ramping micro-tick haptics) to edit its name or schedule.

  * **Safe delete**: trash icon opens a confirmation popup to prevent misclicks.

  * Completed date-specific tasks automatically sink to the bottom with a dimmed strikethrough style while recurring tasks remain active at the top.

* **Report (Progress & History)**

  * Custom `Canvas`-drawn animated pie ring showing today's completed vs. pending ratio (counting only tasks scheduled for today).

  * Monthly calendar heatmap with completion-rate shading, a **Jump to Today** pill, and a daily task breakdown when tapping any date.

* **Profile & Settings** (accessible from Home)

  * Custom name for the Home greeting.

  * **Dark Mode** and **Follow System** toggles (with synchronous theme caching for zero dark/light startup flash).

  * **Haptic Feedback** toggle + **Haptics Power** slider (**Low / Medium / High**), visible but disabled when haptics are off.

  * **Task Reminders** toggle for local daily reminders via WorkManager.

  * App-wide **Font Size** selector (**Small / Medium / Large**).

## Color Palette

Warm off-tones with a muted sage accent—no pure `#000000` or `#FFFFFF` primary backgrounds.

| Token | Light Mode | Dark Mode | 
| ----- | ----- | ----- | 
| App Background | `#F5F5F0` | `#171816` | 
| Card / Surface | `#E5E5DF` | `#222420` | 
| Elevated Card | `#FFFFFF` | `#2D302B` | 
| Main Text | `#333333` | `#EAECE6` | 
| Secondary Text | `#77776F` | `#A6A69B` | 
| Accent | `#6C8A72` | `#8BA18D` | 
| Border / Divider | `#D5D5CD` | `#383A35` | 

## UI & Motion Components

* **Spring Physics (`MotionSpecs`)**: `spring()` curves drive tab transitions, checkbox draw-ins, card reordering, expandable panels, and `0.96x` tactile button presses.

* **Custom Compose Controls (`UiverseComponents.kt`)**:

  * `MinimalInputField` — rounded focus-animated task input inspired by [ErzenXz on Uiverse.io](https://uiverse.io/ErzenXz/horrible-rattlesnake-14).

  * `SegmentedPillControl` — recessed pill navigation bar and mode selector with a sliding elevated indicator inspired by [Yaya12085 on Uiverse.io](https://uiverse.io/Yaya12085/rude-mouse-79).

  * `AnimatedCheckbox` & `MinimalToggleSwitch` — lightweight vector/spring controls re-skinned to the sage palette.

* **Strength-Aware Haptics (`HapticEngine.kt`)**: uses Android's `VibrationEffect` amplitude API across **Low**, **Medium**, and **High** levels for checkboxes, drag pickup/swap/drop, rapid hold ramping micro-ticks, tab switches, and deletes.

## Tech Stack & Architecture

* **Language**: Kotlin `2.0.21`

* **UI**: Jetpack Compose (BOM `2024.10.01`, Material 3)

* **Architecture**: MVVM (`TodoViewModel` + reactive `StateFlow`)

* **Database**: Room `2.6.1` (KSP) with schema migrations (`v1 → v2 → v3`)

* **Background Reminders**: WorkManager `2.9.1`

* **Minimum SDK**: Android 8.0 (API 26) · **Target SDK**: Android 15 (API 35)

### Project Structure

```
MinimalTodo/
├── app/src/main/java/com/personal/minimaltodo/
│   ├── MainActivity.kt                  # Edge-to-edge host, tab transitions, settings overlay
│   ├── MinimalTodoApp.kt                # App entry point, Room & notification channel init
│   ├── data/
│   │   ├── Models.kt                    # TaskEntity, TaskCompletionEntity, UserSettingsEntity
│   │   ├── TaskDao.kt                   # Reactive Room DAOs
│   │   ├── AppDatabase.kt               # Room database (v3) with migrations 1→2 and 2→3
│   │   ├── TodoRepository.kt            # Repository bridging DAOs & ViewModel
│   │   └── ThemePrefs.kt                # Synchronous SharedPreferences cache for zero-flash startup
│   ├── viewmodel/
│   │   └── TodoViewModel.kt             # Reactive state for Home, Tasks, Report, and Settings
│   ├── notifications/
│   │   ├── TaskReminderWorker.kt        # Daily pending-task reminder notification
│   │   └── ReminderScheduler.kt         # WorkManager periodic scheduler
│   └── ui/
│       ├── theme/Theme.kt               # Light/Dark palettes, MotionSpecs, font-size scaling
│       ├── haptics/HapticEngine.kt      # Amplitude-scaled haptic feedback engine
│       ├── components/UiverseComponents.kt
│       └── screens/
│           ├── HomeScreen.kt            # Today's tasks + drag-to-reorder priority
│           ├── TaskScreen.kt            # Add/edit/delete tasks + multi-date & weekly schedules
│           ├── ReportScreen.kt          # Animated pie chart + calendar history + Jump to Today
│           └── SettingsScreen.kt        # Name, theme, haptics, reminders, font size

```

## How to Run

### Prerequisites

* [Android Studio](https://developer.android.com/studio) (2024.2 Ladybug or newer)

* Android 8.0+ (API 26+) physical device or emulator

### Run on Your Phone

1. Clone the repository:

   ```
   git clone https://github.com/parthraorane86/Minimal-To-Do.git
   
   ```

2. Open the `MinimalTodo` folder in **Android Studio** and wait for **Gradle Sync** to finish.

3. Enable **Developer options → USB debugging** (and **Install via USB** on Xiaomi/MIUI/HyperOS devices) on your phone.

4. Select your device in the top toolbar and click **Run (`▶`)**.

### Build a Standalone APK

* **Debug APK**: Open **Menu (`≡`) → Build → Generate App Bundles or APKs → Generate APKs**, then click **locate** to find `app-debug.apk`.

* **Release APK (recommended for daily use)**: Open **Menu (`≡`) → Build → Generate Signed App Bundle or APK… → APK**, create or select a keystore, and build the **release** variant (R8-shrunk for smaller size and smoother Compose performance).

## Changelog

### v1.1 (Current)

* Added **drag-and-drop task reordering** on the Home screen to set daily priorities (`sortOrder` persisted in Room).

* Moved today's compact date (`Wed, Nov 25`) to the top-right on Home and removed the profile icon from Report.

* Added **multi-date scheduling** (**+ Add another date**) for One Time tasks in both creation and edit flows.

* Added **quarter-second (250ms) hold to edit** on Tasks with a Yes/No prompt and full schedule editor.

* Added a **delete confirmation dialog** on the trash icon to prevent accidental deletes.

* Completed date-specific tasks now move to the bottom of the Task list with dimmed strikethrough styling (everyday/weekly tasks unaffected).

* Added a **Jump to Today** pill in the Report calendar.

* Added a **Haptics** engine with an on/off toggle and **Low / Medium / High** power slider in Settings (ramping micro-ticks on hold, crisp ticks on checks/drags/taps).

* Replaced the heavy custom Dark Mode switch with a lightweight spring toggle for smoother performance.

* Fixed the brief dark-mode startup flash when reopening in Light mode on a dark-mode device via synchronous `ThemePrefs` caching.

* Added safe Room database migrations (`v1 → v2 → v3`) so existing tasks and history are preserved when upgrading from v1.0.

### v1.0

* Initial release with **Task**, **Home**, **Report**, and **Profile & Settings** screens.

* Room persistence, WorkManager daily reminders, custom Canvas pie chart, calendar history, Light/Dark sage palette, and dynamic font sizing.

## License

Personal project — feel free to fork or adapt for your own use.