package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WorkdayDatabase
import com.example.data.model.WorkdayEntity
import com.example.data.parser.WorkdayDateParser
import com.example.data.repository.WorkdayRepository
import com.example.data.sync.GoogleCalendarSyncManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: WorkdayDatabase
    private lateinit var repository: WorkdayRepository
    private lateinit var syncManager: GoogleCalendarSyncManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkdayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        syncManager = GoogleCalendarSyncManager(context)
        repository = WorkdayRepository(database.workdayDao(), syncManager, context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Workdays", appName)
    }

    @Test
    fun testDateParser_StandardDDMMList() {
        val input = "24.08, 25.08, 26.08."
        val result = WorkdayDateParser.parseInput(input, defaultStartTime = "08:00", defaultEndTime = "16:00")
        
        assertEquals(3, result.items.size)
        assertEquals(24, result.items[0].day)
        assertEquals(8, result.items[0].month)
        assertEquals("08:00", result.items[0].startTime)
        assertEquals("16:00", result.items[0].endTime)
        
        assertEquals(25, result.items[1].day)
        assertEquals(26, result.items[2].day)
    }

    @Test
    fun testDateParser_TimeInput8to21DotNotation() {
        // Testing 8.00-21.00 input format
        val input = "24.08 8.00-21.00, 25.08 8.00-16.00"
        val result = WorkdayDateParser.parseInput(input)
        
        assertEquals(2, result.items.size)
        assertEquals(24, result.items[0].day)
        assertEquals("08:00", result.items[0].startTime)
        assertEquals("21:00", result.items[0].endTime)
        
        assertEquals(25, result.items[1].day)
        assertEquals("08:00", result.items[1].startTime)
        assertEquals("16:00", result.items[1].endTime)
    }

    @Test
    fun testDateParser_DateRange() {
        val input = "10.09 - 13.09"
        val result = WorkdayDateParser.parseInput(input, defaultStartTime = "08:00", defaultEndTime = "21:00")
        
        assertEquals(4, result.items.size)
        assertEquals(10, result.items[0].day)
        assertEquals(11, result.items[1].day)
        assertEquals(12, result.items[2].day)
        assertEquals(13, result.items[3].day)
        assertEquals("08:00", result.items[0].startTime)
        assertEquals("21:00", result.items[0].endTime)
    }

    @Test
    fun testDatabase_InsertAndRemoveShift() = runBlocking {
        val shift = WorkdayEntity(
            day = 24,
            month = 8,
            year = 2026,
            startTime = "08:00",
            endTime = "21:00",
            title = "Work Shift",
            shiftType = "Day"
        )

        database.workdayDao().insertWorkday(shift)
        val allShifts = repository.allWorkdays.first()
        assertEquals(1, allShifts.size)
        assertEquals(24, allShifts[0].day)
        assertEquals("08:00", allShifts[0].startTime)
        assertEquals("21:00", allShifts[0].endTime)

        // Test remove shift feature
        val removed = repository.deleteWorkdayForDate(24, 8, 2026)
        assertTrue(removed)

        val afterRemove = repository.allWorkdays.first()
        assertEquals(0, afterRemove.size)
    }

    @Test
    fun testMondayToSundayWeekCalculation() {
        val testDate = LocalDate.of(2026, 8, 20) // Thursday
        val monday = testDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDays = (0L..6L).map { monday.plusDays(it) }
        
        assertEquals(7, weekDays.size)
        assertEquals("MONDAY", weekDays.first().dayOfWeek.name)
        assertEquals("SUNDAY", weekDays.last().dayOfWeek.name)
        assertEquals(17, weekDays.first().dayOfMonth) // Monday Aug 17
        assertEquals(23, weekDays.last().dayOfMonth) // Sunday Aug 23
    }
}
