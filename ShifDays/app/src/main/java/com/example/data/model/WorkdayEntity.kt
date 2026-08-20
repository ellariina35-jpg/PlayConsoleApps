package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "workdays")
data class WorkdayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val day: Int,
    val month: Int,
    val year: Int,
    val startTime: String = "08:00", // HH:mm
    val endTime: String = "16:00",   // HH:mm
    val title: String = "Work Shift",
    val notes: String = "",
    val shiftType: String = "Day",   // Morning, Day, Evening, Night, Custom
    val isSyncedToGoogle: Boolean = false,
    val googleEventId: String? = null,
    val calendarEventId: Long? = null,
    val syncedCalendarName: String? = null,
    val syncedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val localDate: LocalDate
        get() = LocalDate.of(year, month, day)

    val formattedDate: String
        get() = String.format("%02d.%02d.%04d", day, month, year)

    val formattedShortDate: String
        get() = String.format("%02d.%02d.", day, month)

    val weekdayName: String
        get() = localDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

    val fullDisplayDate: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
            return localDate.format(formatter)
        }

    val timeRangeDisplay: String
        get() = "$startTime - $endTime"
}

data class ShiftPreset(
    val id: String,
    val name: String,
    val startTime: String,
    val endTime: String,
    val iconName: String = "schedule"
)

data class GoogleCalendarAccount(
    val id: Long,
    val name: String,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isPrimary: Boolean
)

data class ParsedWorkdayItem(
    val day: Int,
    val month: Int,
    val year: Int,
    val startTime: String,
    val endTime: String,
    val title: String,
    val originalText: String,
    val isValid: Boolean = true,
    val validationMessage: String = ""
) {
    val formattedShortDate: String
        get() = String.format("%02d.%02d.", day, month)

    val formattedFullDate: String
        get() {
            return try {
                val date = LocalDate.of(year, month, day)
                val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
                date.format(formatter)
            } catch (e: Exception) {
                "$day.$month.$year"
            }
        }
}
