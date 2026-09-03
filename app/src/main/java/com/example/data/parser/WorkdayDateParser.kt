package com.example.data.parser

import com.example.data.model.ParsedWorkdayItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

data class ParseResult(
    val items: List<ParsedWorkdayItem>,
    val errors: List<String>,
    val summaryText: String
)

object WorkdayDateParser {

    /**
     * Parses freeform text containing DD.MM. format dates (single, comma/newline-separated,
     * ranges like 12.08-16.08, or with optional inline hours like 24.08 08:00-16:00).
     */
    fun parseInput(
        input: String,
        defaultStartTime: String = "08:00",
        defaultEndTime: String = "16:00",
        defaultTitle: String = "Work Shift"
    ): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParseResult(emptyList(), emptyList(), "Enter dates in DD.MM. format")
        }

        val items = mutableListOf<ParsedWorkdayItem>()
        val errors = mutableListOf<String>()
        val today = LocalDate.now()
        val currentYear = today.year

        // First, split into candidate segments by commas, semicolons, or newlines
        val rawSegments = trimmed.split(Regex("[,;\n\r]+"))

        for (rawSegment in rawSegments) {
            val segment = rawSegment.trim()
            if (segment.isEmpty()) continue

            // Check if segment is a date range: e.g. "20.08 - 24.08" or "20.08.-24.08."
            val rangeMatch = Regex("""(\d{1,2}\.\d{1,2}(?:\.\d{2,4})?)\s*[-–—to]+\s*(\d{1,2}\.\d{1,2}(?:\.\d{2,4})?)""").find(segment)
            if (rangeMatch != null) {
                val startStr = rangeMatch.groupValues[1]
                val endStr = rangeMatch.groupValues[2]
                val startDate = parseSingleDateOnly(startStr, currentYear, today)
                val endDate = parseSingleDateOnly(endStr, currentYear, today)

                if (startDate != null && endDate != null) {
                    if (!startDate.isAfter(endDate)) {
                        var cur: LocalDate = startDate
                        while (!cur.isAfter(endDate)) {
                            items.add(
                                ParsedWorkdayItem(
                                    day = cur.dayOfMonth,
                                    month = cur.monthValue,
                                    year = cur.year,
                                    startTime = defaultStartTime,
                                    endTime = defaultEndTime,
                                    title = defaultTitle,
                                    originalText = String.format("%02d.%02d.", cur.dayOfMonth, cur.monthValue)
                                )
                            )
                            cur = cur.plusDays(1)
                        }
                        continue
                    } else {
                        errors.add("Range start $startStr is after end $endStr")
                    }
                }
            }

            // Extract possible inline time if present (e.g. 08:00-16:00, 8.00-21.00, 8-16, 08:00 - 16:30, 8.00 - 21.00)
            var startTime = defaultStartTime
            var endTime = defaultEndTime
            var title = defaultTitle

            // Matches patterns like "8.00-21.00", "08:00-16:00", "8:00 - 21:00", "8 - 21", "8.00 - 16.00"
            val timeMatch = Regex("""(\d{1,2}(?:[:.]\d{2})?)\s*[-–—to]+\s*(\d{1,2}(?:[:.]\d{2})?)""").find(segment)
            var textWithoutTime = segment
            if (timeMatch != null) {
                val tStart = normalizeTimeString(timeMatch.groupValues[1])
                val tEnd = normalizeTimeString(timeMatch.groupValues[2])
                if (tStart != null && tEnd != null) {
                    startTime = tStart
                    endTime = tEnd
                    textWithoutTime = segment.replace(timeMatch.value, "").trim()
                }
            }

            // Extract shift keyword if present (morning, evening, night, day)
            val lower = textWithoutTime.lowercase()
            if (lower.contains("morning") || lower.contains("aamu")) {
                startTime = "06:00"
                endTime = "14:00"
                title = "Morning Shift"
                textWithoutTime = textWithoutTime.replace(Regex("""(?i)(morning|aamu)"""), "").trim()
            } else if (lower.contains("evening") || lower.contains("ilta")) {
                startTime = "14:00"
                endTime = "22:00"
                title = "Evening Shift"
                textWithoutTime = textWithoutTime.replace(Regex("""(?i)(evening|ilta)"""), "").trim()
            } else if (lower.contains("night") || lower.contains("yö")) {
                startTime = "22:00"
                endTime = "06:00"
                title = "Night Shift"
                textWithoutTime = textWithoutTime.replace(Regex("""(?i)(night|yö)"""), "").trim()
            }

            // Match all DD.MM or DD.MM. or DD.MM.YYYY dates in this segment
            val dateMatches = Regex("""\b(\d{1,2})\.(\d{1,2})(?:\.(\d{2,4}))?\.?\b""").findAll(textWithoutTime).toList()

            if (dateMatches.isNotEmpty()) {
                for (match in dateMatches) {
                    val dayStr = match.groupValues[1]
                    val monthStr = match.groupValues[2]
                    val yearStr = match.groupValues[3]

                    val day = dayStr.toIntOrNull()
                    val month = monthStr.toIntOrNull()

                    if (day == null || month == null || month !in 1..12 || day !in 1..31) {
                        errors.add("Invalid date numbers in: ${match.value}")
                        continue
                    }

                    var year = if (yearStr.isNotEmpty()) {
                        val y = yearStr.toIntOrNull() ?: currentYear
                        if (y < 100) 2000 + y else y
                    } else {
                        // Smart year calculation: if month/day is in the past by > 90 days, might be next year
                        // otherwise current year
                        val targetDateThisYear = try {
                            LocalDate.of(currentYear, month, day)
                        } catch (e: Exception) { null }

                        if (targetDateThisYear != null && targetDateThisYear.isBefore(today.minusDays(180))) {
                            currentYear + 1
                        } else {
                            currentYear
                        }
                    }

                    // Validate actual calendar date (e.g. leap year, 30 vs 31 days)
                    try {
                        val validDate = LocalDate.of(year, month, day)
                        items.add(
                            ParsedWorkdayItem(
                                day = validDate.dayOfMonth,
                                month = validDate.monthValue,
                                year = validDate.year,
                                startTime = startTime,
                                endTime = endTime,
                                title = title,
                                originalText = match.value
                            )
                        )
                    } catch (e: Exception) {
                        errors.add("Invalid date ${match.value}: $day is not valid for month $month")
                    }
                }
            } else {
                if (segment.isNotEmpty() && !segment.matches(Regex("""[\s.,\-_]+"""))) {
                    errors.add("Could not parse DD.MM. date from '$segment'")
                }
            }
        }

        // Deduplicate items with the same day, month, year
        val uniqueItems = items.distinctBy { "${it.year}-${it.month}-${it.day}" }
            .sortedWith(compareBy({ it.year }, { it.month }, { it.day }))

        val summary = if (uniqueItems.isNotEmpty()) {
            "Found ${uniqueItems.size} workday${if (uniqueItems.size > 1) "s" else ""}"
        } else if (errors.isNotEmpty()) {
            "No valid workdays found"
        } else {
            "Enter dates like 24.08, 25.08, 26.08"
        }

        return ParseResult(
            items = uniqueItems,
            errors = errors,
            summaryText = summary
        )
    }

    private fun parseSingleDateOnly(str: String, currentYear: Int, today: LocalDate): LocalDate? {
        val match = Regex("""(\d{1,2})\.(\d{1,2})(?:\.(\d{2,4}))?\.?""").find(str.trim()) ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val yearStr = match.groupValues[3]

        val year = if (yearStr.isNotEmpty()) {
            val y = yearStr.toIntOrNull() ?: currentYear
            if (y < 100) 2000 + y else y
        } else {
            currentYear
        }

        return try {
            LocalDate.of(year, month, day)
        } catch (e: Exception) {
            null
        }
    }

    private fun normalizeTimeString(raw: String): String? {
        val trimmed = raw.trim()
        val separator = if (trimmed.contains(":")) ":" else if (trimmed.contains(".")) "." else null
        return if (separator != null) {
            val parts = trimmed.split(separator)
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            String.format("%02d:%02d", h.coerceIn(0, 23), m.coerceIn(0, 59))
        } else {
            val h = trimmed.toIntOrNull() ?: return null
            String.format("%02d:00", h.coerceIn(0, 23))
        }
    }
}
