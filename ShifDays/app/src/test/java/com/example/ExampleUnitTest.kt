package com.example

import com.example.data.parser.WorkdayDateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testDDMMDateParserSingle() {
    val result = WorkdayDateParser.parseInput("24.08.")
    assertEquals(1, result.items.size)
    assertEquals(24, result.items[0].day)
    assertEquals(8, result.items[0].month)
  }

  @Test
  fun testDDMMDateParserMultipleCommaSeparated() {
    val result = WorkdayDateParser.parseInput("24.08, 25.08, 26.08")
    assertEquals(3, result.items.size)
    assertEquals(24, result.items[0].day)
    assertEquals(25, result.items[1].day)
    assertEquals(26, result.items[2].day)
  }

  @Test
  fun testDDMMDateParserRange() {
    val result = WorkdayDateParser.parseInput("20.08 - 24.08")
    assertEquals(5, result.items.size)
    assertEquals(20, result.items[0].day)
    assertEquals(24, result.items[4].day)
  }

  @Test
  fun testDDMMWithCustomTimes() {
    val result = WorkdayDateParser.parseInput("24.08. 07:00-15:30")
    assertEquals(1, result.items.size)
    assertEquals("07:00", result.items[0].startTime)
    assertEquals("15:30", result.items[0].endTime)
  }
}
