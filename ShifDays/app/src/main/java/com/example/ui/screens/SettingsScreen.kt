package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserPreferences
import com.example.ui.theme.StatusSynced
import com.example.ui.viewmodel.WorkdayUiState
import com.example.ui.viewmodel.WorkdayViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WorkdayViewModel,
    uiState: WorkdayUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var startTime by remember(uiState.userPreferences) { mutableStateOf(uiState.userPreferences.defaultStartTime) }
    var endTime by remember(uiState.userPreferences) { mutableStateOf(uiState.userPreferences.defaultEndTime) }
    var defaultTitle by remember(uiState.userPreferences) { mutableStateOf(uiState.userPreferences.defaultTitle) }
    var reminderMinutes by remember(uiState.userPreferences) { mutableIntStateOf(uiState.userPreferences.reminderMinutes) }
    var selectedCalendarId by remember(uiState.userPreferences) { mutableLongStateOf(uiState.userPreferences.selectedCalendarId) }
    var autoSync by remember(uiState.userPreferences) { mutableStateOf(uiState.userPreferences.autoSyncToGoogle) }

    var calendarDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Settings & Google Sync",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Configure default shift hours, reminders, and Google Calendar connection",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: Google Calendar Connection Status
        Card(
            modifier = Modifier.fillMaxWidth().testTag("google_calendar_status_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Calendar API",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Connected & Active",
                                fontSize = 12.sp,
                                color = StatusSynced,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val today = LocalDate.now()
                            viewModel.openInGoogleCalendar(
                                com.example.data.model.WorkdayEntity(
                                    day = today.dayOfMonth,
                                    month = today.monthValue,
                                    year = today.year
                                )
                            )
                        },
                        modifier = Modifier.testTag("open_gcal_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open Google Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Calendar Selector
                Text(
                    text = "Sync Destination Calendar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                val currentSelectedCal = uiState.availableCalendars.find { it.id == selectedCalendarId }
                    ?: uiState.availableCalendars.firstOrNull()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { calendarDropdownExpanded = true }
                        .testTag("calendar_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentSelectedCal?.displayName ?: "Primary Google Calendar",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentSelectedCal?.accountName ?: "Google Account",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = calendarDropdownExpanded,
                    onDismissRequest = { calendarDropdownExpanded = false }
                ) {
                    uiState.availableCalendars.forEach { cal ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(cal.displayName, fontWeight = FontWeight.Bold)
                                    Text(cal.accountName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                selectedCalendarId = cal.id
                                calendarDropdownExpanded = false
                                savePreferences(
                                    viewModel,
                                    startTime,
                                    endTime,
                                    defaultTitle,
                                    reminderMinutes,
                                    cal.id,
                                    autoSync
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 2: Shift Default Times
        Card(
            modifier = Modifier.fillMaxWidth().testTag("shift_defaults_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Default Work Shift Hours",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Applied automatically when parsing DD.MM. workdays",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {
                            startTime = it
                            savePreferences(viewModel, startTime, endTime, defaultTitle, reminderMinutes, selectedCalendarId, autoSync)
                        },
                        label = { Text("Default Start") },
                        modifier = Modifier.weight(1f).testTag("settings_start_time"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {
                            endTime = it
                            savePreferences(viewModel, startTime, endTime, defaultTitle, reminderMinutes, selectedCalendarId, autoSync)
                        },
                        label = { Text("Default End") },
                        modifier = Modifier.weight(1f).testTag("settings_end_time"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = defaultTitle,
                    onValueChange = {
                        defaultTitle = it
                        savePreferences(viewModel, startTime, endTime, defaultTitle, reminderMinutes, selectedCalendarId, autoSync)
                    },
                    label = { Text("Default Event Title") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_event_title"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 3: Notification & Reminders
        Card(
            modifier = Modifier.fillMaxWidth().testTag("reminders_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Calendar Reminder Alert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val reminderOptions = listOf(
                    0 to "None",
                    15 to "15 min before",
                    30 to "30 min before",
                    60 to "1 hour before",
                    120 to "2 hours before",
                    1440 to "1 day before"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reminderOptions.take(3).forEach { (mins, label) ->
                        FilterChip(
                            selected = reminderMinutes == mins,
                            onClick = {
                                reminderMinutes = mins
                                savePreferences(viewModel, startTime, endTime, defaultTitle, reminderMinutes, selectedCalendarId, autoSync)
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reminderOptions.drop(3).forEach { (mins, label) ->
                        FilterChip(
                            selected = reminderMinutes == mins,
                            onClick = {
                                reminderMinutes = mins
                                savePreferences(viewModel, startTime, endTime, defaultTitle, reminderMinutes, selectedCalendarId, autoSync)
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 4: Format Guide & Tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DD.MM. Supported Format Tips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Comma-separated: 24.08, 25.08, 26.08\n" +
                            "• Line breaks: paste directly from your roster message\n" +
                            "• Date ranges: 24.08 - 28.08 (automatically schedules all days)\n" +
                            "• Inline shift times: 24.08. 08:00-16:00 or 25.08. Evening\n" +
                            "• Quick Week selector: tap days on the weekly calendar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bulk sync all button
        Button(
            onClick = { viewModel.syncAll() },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_all_workdays_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Re-sync All Workdays to Google Calendar")
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

private fun savePreferences(
    viewModel: WorkdayViewModel,
    start: String,
    end: String,
    title: String,
    reminder: Int,
    calId: Long,
    autoSync: Boolean
) {
    val updated = UserPreferences(
        defaultStartTime = start,
        defaultEndTime = end,
        defaultTitle = title,
        reminderMinutes = reminder,
        selectedCalendarId = calId,
        autoSyncToGoogle = autoSync
    )
    viewModel.updatePreferences(updated)
}
