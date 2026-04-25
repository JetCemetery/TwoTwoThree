package com.jetcemetery.twotwothree

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ScheduleUtils {
    // Default anchor date: a Monday that starts "Week A"
    val defaultAnchorDate: LocalDate = LocalDate.of(2024, 12, 30)

    /**
     * Core logic to determine if a date is a work day based on the base schedule.
     */
    fun isWorkDay(date: LocalDate, anchorDate: LocalDate = defaultAnchorDate, scheduleType: String = "2-2-3"): Boolean {
        val daysBetween = ChronoUnit.DAYS.between(anchorDate, date)
        val normalizedDays = if (daysBetween >= 0) {
            daysBetween % 14
        } else {
            (14 + (daysBetween % 14)) % 14
        }

        return when (scheduleType) {
            "5-2" -> {
                when (normalizedDays.toInt()) {
                    0, 1, 2, 3, 4 -> true   // Mon-Fri (Week A)
                    5, 6 -> false           // Sat-Sun (Week A)
                    7, 8, 9, 10, 11 -> false // Mon-Fri (Week B)
                    12, 13 -> true          // Sat-Sun (Week B)
                    else -> false
                }
            }
            else -> { // Default 2-2-3
                when (normalizedDays.toInt()) {
                    0, 1 -> true   // Mon, Tue (Week A)
                    2, 3 -> false  // Wed, Thu (Week A)
                    4, 5, 6 -> true // Fri, Sat, Sun (Week A)
                    7, 8 -> false  // Mon, Tue (Week B)
                    9, 10 -> true  // Wed, Thu (Week B)
                    11, 12, 13 -> false // Fri, Sat, Sun (Week B)
                    else -> false
                }
            }
        }
    }

    /**
     * Optimized switch logic.
     * @param sortedSwitches Must be pre-sorted for performance.
     */
    fun isWorkDaySwitchedOptimized(date: LocalDate, sortedSwitches: List<LocalDate>, scheduleType: String = "2-2-3"): Boolean {
        val original = isWorkDay(date, scheduleType = scheduleType)
        
        // Use binary search to find how many switches occur before or on this date
        var low = 0
        var high = sortedSwitches.size - 1
        var count = 0
        
        while (low <= high) {
            val mid = (low + high) / 2
            if (!sortedSwitches[mid].isAfter(date)) {
                count = mid + 1
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        
        return if (count % 2 == 0) original else !original
    }

    // Deprecated for performance, keeping for compatibility if needed elsewhere
    fun isWorkDaySwitched(date: LocalDate, switchDates: Set<LocalDate>, scheduleType: String = "2-2-3"): Boolean {
        val sorted = switchDates.toList().sorted()
        return isWorkDaySwitchedOptimized(date, sorted, scheduleType)
    }
}
