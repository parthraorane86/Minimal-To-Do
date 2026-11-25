package com.personal.minimaltodo.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class ScheduleType {
    DAILY,
    WEEKLY,
    ONE_TIME
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class FontSizeOption(val scale: Float, val label: String) {
    SMALL(0.88f, "Small"),
    MEDIUM(1.0f, "Medium"),
    LARGE(1.16f, "Large")
}

enum class HapticStrength(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

@Immutable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    /** Bitmask for DayOfWeek: bit (dayOfWeek.value - 1), where Mon=1, Tue=2, ..., Sun=64 */
    val daysOfWeekMask: Int = 0,
    val oneTimeDateEpochDay: Long? = null,
    val createdAtEpochDay: Long = LocalDate.now().toEpochDay(),
    /** Comma-separated epochDay longs supporting multiple specific dates for ONE_TIME tasks */
    val specificDatesCsv: String = "",
    /** User-defined priority ordering on the Home screen */
    val sortOrder: Int = 0
) {
    fun specificEpochDays(): List<Long> {
        val parsed = specificDatesCsv
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .distinct()
            .sorted()
        return if (parsed.isNotEmpty()) {
            parsed
        } else {
            listOfNotNull(oneTimeDateEpochDay)
        }
    }

    fun specificDates(): List<LocalDate> = specificEpochDays().map(LocalDate::ofEpochDay)

    fun isScheduledFor(date: LocalDate, respectCreationDate: Boolean = false): Boolean {
        if (respectCreationDate && date.toEpochDay() < createdAtEpochDay) {
            return false
        }
        return when (scheduleType) {
            ScheduleType.DAILY -> true
            ScheduleType.WEEKLY -> {
                val bit = 1 shl (date.dayOfWeek.value - 1)
                (daysOfWeekMask and bit) != 0
            }
            ScheduleType.ONE_TIME -> date.toEpochDay() in specificEpochDays()
        }
    }

    fun isOneTimeFullyCompleted(completedEpochDays: Set<Long>): Boolean {
        if (scheduleType != ScheduleType.ONE_TIME) return false
        val scheduled = specificEpochDays()
        return scheduled.isNotEmpty() && scheduled.all { it in completedEpochDays }
    }

    fun selectedDays(): Set<DayOfWeek> {
        if (scheduleType != ScheduleType.WEEKLY) return emptySet()
        return DayOfWeek.entries.filter { day ->
            (daysOfWeekMask and (1 shl (day.value - 1))) != 0
        }.toSet()
    }

    fun scheduleSummary(): String {
        return when (scheduleType) {
            ScheduleType.DAILY -> "Every day"
            ScheduleType.WEEKLY -> {
                val days = DayOfWeek.entries.filter { (daysOfWeekMask and (1 shl (it.value - 1))) != 0 }
                when {
                    days.size == 7 -> "Every day"
                    days == listOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    ) -> "Weekdays"
                    days == listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
                    else -> days.joinToString(", ") {
                        it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    }
                }
            }
            ScheduleType.ONE_TIME -> {
                val dates = specificDates()
                when {
                    dates.isEmpty() -> "One time"
                    dates.size == 1 -> dates.first().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    dates.size <= 3 -> dates.joinToString(", ") {
                        it.format(DateTimeFormatter.ofPattern("MMM d"))
                    }
                    else -> {
                        val preview = dates.take(2).joinToString(", ") {
                            it.format(DateTimeFormatter.ofPattern("MMM d"))
                        }
                        "$preview +${dates.size - 2} more"
                    }
                }
            }
        }
    }

    companion object {
        fun maskFromDays(days: Set<DayOfWeek>): Int {
            var mask = 0
            days.forEach { day ->
                mask = mask or (1 shl (day.value - 1))
            }
            return mask
        }

        fun csvFromDates(dates: List<LocalDate>): String {
            return dates
                .map { it.toEpochDay() }
                .distinct()
                .sorted()
                .joinToString(",")
        }
    }
}

@Immutable
@Entity(
    tableName = "task_completions",
    primaryKeys = ["taskId", "dateEpochDay"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dateEpochDay"])]
)
data class TaskCompletionEntity(
    val taskId: Long,
    val dateEpochDay: Long
)

@Immutable
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = false,
    val fontSizeOption: FontSizeOption = FontSizeOption.MEDIUM,
    val hapticsEnabled: Boolean = true,
    val hapticsStrength: HapticStrength = HapticStrength.MEDIUM
)

class Converters {
    @TypeConverter
    fun fromScheduleType(value: ScheduleType): String = value.name

    @TypeConverter
    fun toScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)

    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): ThemeMode = ThemeMode.valueOf(value)

    @TypeConverter
    fun fromFontSizeOption(value: FontSizeOption): String = value.name

    @TypeConverter
    fun toFontSizeOption(value: String): FontSizeOption = FontSizeOption.valueOf(value)

    @TypeConverter
    fun fromHapticStrength(value: HapticStrength): String = value.name

    @TypeConverter
    fun toHapticStrength(value: String): HapticStrength = HapticStrength.valueOf(value)
}
