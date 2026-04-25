package com.jetcemetery.twotwothree

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ScheduleUtils {
    // Default anchor date: a Monday that starts "Week A"
    val defaultAnchorDate: LocalDate = LocalDate.of(2024, 12, 30)

    fun isWorkDay(date: LocalDate, anchorDate: LocalDate = defaultAnchorDate): Boolean {
        val daysBetween = ChronoUnit.DAYS.between(anchorDate, date)
        // Adjust for negative differences if date is before anchorDate
        val normalizedDays = if (daysBetween >= 0) {
            daysBetween % 14
        } else {
            (14 + (daysBetween % 14)) % 14
        }

        return when (normalizedDays.toInt()) {
            0, 1 -> true   // Mon, Tue (Week A)
            2, 3 -> false  // Wed, Thu (Week A)
            4, 5, 6 -> true // Fri, Sat, Sun (Week A)
            7, 8 -> false  // Mon, Tue (Week B)
            9, 10 -> true  // Wed, Thu (Week B)
            11, 12, 13 -> false // Fri, Sat, Sun (Week B)
            else -> false
        }
    }

    fun isWorkDaySwitched(date: LocalDate, switchDates: Set<LocalDate>): Boolean {
        val original = isWorkDay(date)
        val switchesBefore = switchDates.count { !it.isAfter(date) }
        return if (switchesBefore % 2 == 0) original else !original
    }
}
