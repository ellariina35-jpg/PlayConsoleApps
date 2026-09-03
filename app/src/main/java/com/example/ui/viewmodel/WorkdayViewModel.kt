package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GoogleCalendarAccount
import com.example.data.model.ParsedWorkdayItem
import com.example.data.model.ShiftPreset
import com.example.data.model.WorkdayEntity
import com.example.data.parser.ParseResult
import com.example.data.repository.UserPreferences
import com.example.data.repository.WorkdayRepository
import com.example.data.sync.SyncReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class WorkdayFilter {
    ALL, UPCOMING, SYNCED, LOCAL
}

data class WorkdayUiState(
    val workdays: List<WorkdayEntity> = emptyList(),
    val filteredWorkdays: List<WorkdayEntity> = emptyList(),
    val upcomingCount: Int = 0,
    val syncedCount: Int = 0,
    val unsyncedCount: Int = 0,
    val inputText: String = "",
    val parseResult: ParseResult = ParseResult(emptyList(), emptyList(), ""),
    val selectedWeekOffset: Int = 0, // 0 for this week, 1 for next week
    val currentWeekDays: List<LocalDate> = emptyList(),
    val selectedDatesInCurrentWeek: Set<LocalDate> = emptySet(),
    val selectedPresetId: String = "day",
    val customStartTime: String = "08:00",
    val customEndTime: String = "16:00",
    val customTitle: String = "Work Shift",
    val isSyncing: Boolean = false,
    val syncReport: SyncReport? = null,
    val availableCalendars: List<GoogleCalendarAccount> = emptyList(),
    val userPreferences: UserPreferences = UserPreferences(),
    val filter: WorkdayFilter = WorkdayFilter.ALL,
    val userMessage: String? = null,
    val nextUpcomingShift: WorkdayEntity? = null
)

class WorkdayViewModel(private val repository: WorkdayRepository) : ViewModel() {

    private val _state = MutableStateFlow(WorkdayUiState())
    val state: StateFlow<WorkdayUiState> = _state.asStateFlow()

    init {
        loadInitialData()
        observeWorkdays()
        calculateWeekDays(0)
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val calendars = repository.getAvailableCalendars()
            val prefs = repository.userPreferences.value
            _state.update {
                it.copy(
                    availableCalendars = calendars,
                    userPreferences = prefs,
                    customStartTime = prefs.defaultStartTime,
                    customEndTime = prefs.defaultEndTime,
                    customTitle = prefs.defaultTitle
                )
            }
        }
    }

    private fun observeWorkdays() {
        viewModelScope.launch {
            combine(
                repository.allWorkdays,
                repository.userPreferences,
                _state
            ) { workdays, prefs, currentState ->
                val today = LocalDate.now()
                val upcoming = workdays.filter { !it.localDate.isBefore(today) }
                val synced = workdays.filter { it.isSyncedToGoogle }
                val unsynced = workdays.filter { !it.isSyncedToGoogle }
                val nextShift = upcoming.firstOrNull()

                val filtered = when (currentState.filter) {
                    WorkdayFilter.ALL -> workdays
                    WorkdayFilter.UPCOMING -> upcoming
                    WorkdayFilter.SYNCED -> synced
                    WorkdayFilter.LOCAL -> unsynced
                }

                // Check which dates in the selected week are already scheduled or typed
                val scheduledDates = workdays.map { it.localDate }.toSet()
                val parsedDates = currentState.parseResult.items.map {
                    LocalDate.of(it.year, it.month, it.day)
                }.toSet()

                currentState.copy(
                    workdays = workdays,
                    filteredWorkdays = filtered,
                    upcomingCount = upcoming.size,
                    syncedCount = synced.size,
                    unsyncedCount = unsynced.size,
                    nextUpcomingShift = nextShift,
                    userPreferences = prefs,
                    selectedDatesInCurrentWeek = scheduledDates + parsedDates
                )
            }.collect { updatedState ->
                _state.value = updatedState
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        val parseRes = repository.parseDateInput(
            input = newText,
            customStart = _state.value.customStartTime,
            customEnd = _state.value.customEndTime,
            customTitle = _state.value.customTitle
        )
        _state.update {
            it.copy(
                inputText = newText,
                parseResult = parseRes
            )
        }
    }

    fun selectWeekOffset(offset: Int) {
        _state.update { it.copy(selectedWeekOffset = offset) }
        calculateWeekDays(offset)
    }

    private fun calculateWeekDays(offsetWeeks: Int) {
        val today = LocalDate.now().plusWeeks(offsetWeeks.toLong())
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val days = (0..6).map { monday.plusDays(it.toLong()) }
        _state.update { it.copy(currentWeekDays = days) }
    }

    fun toggleWeekDay(date: LocalDate) {
        val dateFormatted = String.format("%02d.%02d.", date.dayOfMonth, date.monthValue)
        val currentInput = _state.value.inputText.trim()

        val parseRes = _state.value.parseResult
        val alreadyParsed = parseRes.items.any { it.day == date.dayOfMonth && it.month == date.monthValue }

        val newInput = if (alreadyParsed) {
            // Remove from input text
            val regex = Regex("""\b0?${date.dayOfMonth}\.0?${date.monthValue}\.?\b""")
            currentInput.replace(regex, "").replace(Regex("""[,;\s]+(?=[,;\s])"""), " ")
                .trim(',', ' ', ';', '\n')
        } else {
            // Add to input text
            if (currentInput.isEmpty()) {
                dateFormatted
            } else {
                "$currentInput, $dateFormatted"
            }
        }

        onInputTextChanged(newInput)
    }

    fun selectShiftPreset(preset: ShiftPreset) {
        _state.update {
            it.copy(
                selectedPresetId = preset.id,
                customStartTime = preset.startTime,
                customEndTime = preset.endTime,
                customTitle = if (preset.name == "Day") "Work Shift" else "${preset.name} Shift"
            )
        }
        // Re-parse with updated preset times
        if (_state.value.inputText.isNotEmpty()) {
            onInputTextChanged(_state.value.inputText)
        }
    }

    fun setCustomTimes(start: String, end: String) {
        _state.update {
            it.copy(
                selectedPresetId = "custom",
                customStartTime = start,
                customEndTime = end
            )
        }
        if (_state.value.inputText.isNotEmpty()) {
            onInputTextChanged(_state.value.inputText)
        }
    }

    fun setCustomTitle(title: String) {
        _state.update { it.copy(customTitle = title) }
        if (_state.value.inputText.isNotEmpty()) {
            onInputTextChanged(_state.value.inputText)
        }
    }

    fun addAndSyncWorkdays(syncToGoogle: Boolean = true) {
        val items = _state.value.parseResult.items
        if (items.isEmpty()) {
            _state.update { it.copy(userMessage = "No valid dates to add. Enter dates like 24.08, 25.08") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                val (count, report) = repository.addWorkdaysFromParsed(items, syncImmediately = syncToGoogle)
                _state.update {
                    it.copy(
                        isSyncing = false,
                        inputText = "",
                        parseResult = ParseResult(emptyList(), emptyList(), ""),
                        syncReport = report,
                        userMessage = if (syncToGoogle) "Added $count workday(s) and synced with Google Calendar!" else "Saved $count workday(s) locally"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSyncing = false,
                        userMessage = "Error adding workdays: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun syncSingleWorkday(workday: WorkdayEntity) {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                val synced = repository.syncSingleWorkday(workday)
                _state.update {
                    it.copy(
                        isSyncing = false,
                        userMessage = "Synced ${synced.formattedDate} to Google Calendar"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSyncing = false,
                        userMessage = "Sync failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun syncAllUnsynced() {
        val unsynced = _state.value.workdays.filter { !it.isSyncedToGoogle }
        if (unsynced.isEmpty()) {
            _state.update { it.copy(userMessage = "All workdays are already synced with Google Calendar!") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            val report = repository.syncAllWorkdays(unsynced)
            _state.update {
                it.copy(
                    isSyncing = false,
                    syncReport = report,
                    userMessage = "Synced ${report.successCount} shifts to Google Calendar"
                )
            }
        }
    }

    fun syncAll() {
        if (_state.value.workdays.isEmpty()) {
            _state.update { it.copy(userMessage = "No workdays to sync") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            val report = repository.syncAllWorkdays(_state.value.workdays)
            _state.update {
                it.copy(
                    isSyncing = false,
                    syncReport = report,
                    userMessage = "Synced ${report.successCount} shifts to Google Calendar"
                )
            }
        }
    }

    fun deleteWorkday(workday: WorkdayEntity) {
        viewModelScope.launch {
            repository.deleteWorkday(workday)
            _state.update { it.copy(userMessage = "Removed shift for ${workday.formattedDate}") }
        }
    }

    fun deleteWorkdayForDate(date: LocalDate) {
        viewModelScope.launch {
            val removed = repository.deleteWorkdayForDate(date.dayOfMonth, date.monthValue, date.year)
            if (removed) {
                _state.update { it.copy(userMessage = "Removed shift for ${date.dayOfMonth}.${date.monthValue}.") }
            } else {
                // If it was only in input, remove from input
                removeDateFromInput(date.dayOfMonth, date.monthValue)
            }
        }
    }

    fun removeDateFromInput(day: Int, month: Int) {
        val currentInput = _state.value.inputText.trim()
        val regex = Regex("""\b0?$day\.0?$month\.?\b""")
        val newInput = currentInput.replace(regex, "")
            .replace(Regex("""[,;\s]+(?=[,;\s])"""), " ")
            .trim(',', ' ', ';', '\n')
        onInputTextChanged(newInput)
    }

    fun clearAllWorkdays() {
        viewModelScope.launch {
            repository.clearAllWorkdays()
            _state.update { it.copy(userMessage = "Cleared all scheduled workdays") }
        }
    }

    fun setFilter(filter: WorkdayFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun updatePreferences(newPrefs: UserPreferences) {
        repository.updatePreferences(newPrefs)
        _state.update {
            it.copy(
                userPreferences = newPrefs,
                customStartTime = newPrefs.defaultStartTime,
                customEndTime = newPrefs.defaultEndTime,
                customTitle = newPrefs.defaultTitle,
                userMessage = "Settings saved"
            )
        }
    }

    fun openInGoogleCalendar(workday: WorkdayEntity) {
        repository.openCalendarAtDate(workday.year, workday.month, workday.day)
    }

    fun quickAddCommonWeekdays() {
        val week = _state.value.currentWeekDays.take(5) // Mon-Fri
        val text = week.joinToString(", ") { String.format("%02d.%02d.", it.dayOfMonth, it.monthValue) }
        onInputTextChanged(text)
    }

    fun clearInput() {
        onInputTextChanged("")
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }

    fun dismissSyncReport() {
        _state.update { it.copy(syncReport = null) }
    }
}

class WorkdayViewModelFactory(private val repository: WorkdayRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkdayViewModel::class.java)) {
            return WorkdayViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
