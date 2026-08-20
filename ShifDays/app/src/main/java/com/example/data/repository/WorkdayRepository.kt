package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.WorkdayDao
import com.example.data.model.GoogleCalendarAccount
import com.example.data.model.ParsedWorkdayItem
import com.example.data.model.ShiftPreset
import com.example.data.model.WorkdayEntity
import com.example.data.parser.ParseResult
import com.example.data.parser.WorkdayDateParser
import com.example.data.sync.GoogleCalendarSyncManager
import com.example.data.sync.SyncReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class UserPreferences(
    val defaultStartTime: String = "08:00",
    val defaultEndTime: String = "16:00",
    val defaultTitle: String = "Work Shift",
    val reminderMinutes: Int = 30,
    val selectedCalendarId: Long = -1L,
    val autoSyncToGoogle: Boolean = true
)

class WorkdayRepository(
    private val workdayDao: WorkdayDao,
    private val syncManager: GoogleCalendarSyncManager,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("workday_prefs", Context.MODE_PRIVATE)

    private val _userPreferences = MutableStateFlow(loadPreferences())
    val userPreferences = _userPreferences.asStateFlow()

    val allWorkdays: Flow<List<WorkdayEntity>> = workdayDao.getAllWorkdays()

    val defaultPresets = listOf(
        ShiftPreset("day_8_16", "08:00 - 16:00", "08:00", "16:00", "wb_sunny"),
        ShiftPreset("full_8_21", "08:00 - 21:00", "08:00", "21:00", "schedule"),
        ShiftPreset("mid_12_20", "12:00 - 20:00", "12:00", "20:00", "timelapse"),
        ShiftPreset("eve_13_21", "13:00 - 21:00", "13:00", "21:00", "nights_stay"),
        ShiftPreset("morn_8_14", "08:00 - 14:00", "08:00", "14:00", "light_mode"),
        ShiftPreset("std_9_17", "09:00 - 17:00", "09:00", "17:00", "work")
    )

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            defaultStartTime = prefs.getString("default_start_time", "08:00") ?: "08:00",
            defaultEndTime = prefs.getString("default_end_time", "16:00") ?: "16:00",
            defaultTitle = prefs.getString("default_title", "Work Shift") ?: "Work Shift",
            reminderMinutes = prefs.getInt("reminder_minutes", 30),
            selectedCalendarId = prefs.getLong("selected_calendar_id", -1L),
            autoSyncToGoogle = prefs.getBoolean("auto_sync", true)
        )
    }

    fun updatePreferences(newPrefs: UserPreferences) {
        prefs.edit().apply {
            putString("default_start_time", newPrefs.defaultStartTime)
            putString("default_end_time", newPrefs.defaultEndTime)
            putString("default_title", newPrefs.defaultTitle)
            putInt("reminder_minutes", newPrefs.reminderMinutes)
            putLong("selected_calendar_id", newPrefs.selectedCalendarId)
            putBoolean("auto_sync", newPrefs.autoSyncToGoogle)
            apply()
        }
        _userPreferences.value = newPrefs
    }

    fun parseDateInput(input: String, customStart: String? = null, customEnd: String? = null, customTitle: String? = null): ParseResult {
        val currentPrefs = _userPreferences.value
        return WorkdayDateParser.parseInput(
            input = input,
            defaultStartTime = customStart ?: currentPrefs.defaultStartTime,
            defaultEndTime = customEnd ?: currentPrefs.defaultEndTime,
            defaultTitle = customTitle ?: currentPrefs.defaultTitle
        )
    }

    suspend fun addWorkdaysFromParsed(
        items: List<ParsedWorkdayItem>,
        syncImmediately: Boolean = true
    ): Pair<Int, SyncReport?> {
        val currentPrefs = _userPreferences.value
        val toInsert = items.map { item ->
            WorkdayEntity(
                day = item.day,
                month = item.month,
                year = item.year,
                startTime = item.startTime,
                endTime = item.endTime,
                title = item.title,
                shiftType = determineShiftType(item.startTime, item.endTime),
                isSyncedToGoogle = false
            )
        }

        // Save locally in Room
        val insertedEntities = mutableListOf<WorkdayEntity>()
        for (entity in toInsert) {
            val existing = workdayDao.getWorkdayForDate(entity.day, entity.month, entity.year)
            if (existing != null) {
                val updated = existing.copy(
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    title = entity.title,
                    shiftType = entity.shiftType,
                    isSyncedToGoogle = false
                )
                workdayDao.updateWorkday(updated)
                insertedEntities.add(updated)
            } else {
                val newId = workdayDao.insertWorkday(entity)
                insertedEntities.add(entity.copy(id = newId))
            }
        }

        // Sync with Google Calendar if enabled
        var syncReport: SyncReport? = null
        if (syncImmediately && insertedEntities.isNotEmpty()) {
            val targetCalId = if (currentPrefs.selectedCalendarId > 0) currentPrefs.selectedCalendarId else null
            val (syncedEntities, report) = syncManager.syncWorkdays(
                workdays = insertedEntities,
                targetCalendarId = targetCalId,
                reminderMinutes = currentPrefs.reminderMinutes
            )
            for (synced in syncedEntities) {
                workdayDao.updateWorkday(synced)
            }
            syncReport = report
        }

        return Pair(insertedEntities.size, syncReport)
    }

    suspend fun syncSingleWorkday(workday: WorkdayEntity): WorkdayEntity {
        val currentPrefs = _userPreferences.value
        val targetCalId = if (currentPrefs.selectedCalendarId > 0) currentPrefs.selectedCalendarId else null
        val synced = syncManager.syncWorkday(
            workday = workday,
            targetCalendarId = targetCalId,
            reminderMinutes = currentPrefs.reminderMinutes
        )
        workdayDao.updateWorkday(synced)
        return synced
    }

    suspend fun syncAllWorkdays(workdays: List<WorkdayEntity>): SyncReport {
        val currentPrefs = _userPreferences.value
        val targetCalId = if (currentPrefs.selectedCalendarId > 0) currentPrefs.selectedCalendarId else null
        val (synced, report) = syncManager.syncWorkdays(
            workdays = workdays,
            targetCalendarId = targetCalId,
            reminderMinutes = currentPrefs.reminderMinutes
        )
        for (item in synced) {
            workdayDao.updateWorkday(item)
        }
        return report
    }

    suspend fun deleteWorkday(workday: WorkdayEntity) {
        if (workday.calendarEventId != null) {
            syncManager.deleteEventFromCalendar(workday.calendarEventId)
        }
        workdayDao.deleteWorkday(workday)
    }

    suspend fun deleteWorkdayForDate(day: Int, month: Int, year: Int): Boolean {
        val existing = workdayDao.getWorkdayForDate(day, month, year)
        return if (existing != null) {
            deleteWorkday(existing)
            true
        } else {
            false
        }
    }

    suspend fun updateWorkday(workday: WorkdayEntity, resync: Boolean = true) {
        if (resync) {
            val synced = syncSingleWorkday(workday)
            workdayDao.updateWorkday(synced)
        } else {
            workdayDao.updateWorkday(workday)
        }
    }

    suspend fun clearAllWorkdays() {
        workdayDao.clearAll()
    }

    suspend fun getAvailableCalendars(): List<GoogleCalendarAccount> {
        return syncManager.getAvailableCalendars()
    }

    fun openCalendarAtDate(year: Int, month: Int, day: Int) {
        syncManager.openCalendarAppAtDate(year, month, day)
    }

    private fun determineShiftType(start: String, end: String): String {
        return when {
            start.startsWith("06") || start.startsWith("07") -> "Morning"
            start.startsWith("14") || start.startsWith("15") || start.startsWith("16") -> "Evening"
            start.startsWith("21") || start.startsWith("22") || start.startsWith("23") -> "Night"
            else -> "Day"
        }
    }
}
