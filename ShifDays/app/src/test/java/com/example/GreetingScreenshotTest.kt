package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.WorkdayEntity
import com.example.ui.components.WorkdayCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleWorkday = WorkdayEntity(
        id = 1L,
        day = 24,
        month = 8,
        year = 2026,
        startTime = "08:00",
        endTime = "16:00",
        title = "Work Shift",
        shiftType = "Day",
        isSyncedToGoogle = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        WorkdayCard(
            workday = sampleWorkday,
            onSyncClick = {},
            onOpenInCalendar = {},
            onDeleteClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
