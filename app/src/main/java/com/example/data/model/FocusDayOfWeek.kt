package com.example.data.model

enum class FocusDayOfWeek(val shortName: String, val fullName: String, val calendarDay: Int) {
    MONDAY("Mon", "Monday", java.util.Calendar.MONDAY),
    TUESDAY("Tue", "Tuesday", java.util.Calendar.TUESDAY),
    WEDNESDAY("Wed", "Wednesday", java.util.Calendar.WEDNESDAY),
    THURSDAY("Thu", "Thursday", java.util.Calendar.THURSDAY),
    FRIDAY("Fri", "Friday", java.util.Calendar.FRIDAY),
    SATURDAY("Sat", "Saturday", java.util.Calendar.SATURDAY),
    SUNDAY("Sun", "Sunday", java.util.Calendar.SUNDAY);

    companion object {
        val ALL_DAYS: Set<FocusDayOfWeek> = entries.toSet()
        val WEEKDAYS: Set<FocusDayOfWeek> = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
        val WEEKENDS: Set<FocusDayOfWeek> = setOf(SATURDAY, SUNDAY)

        fun fromCalendarDay(day: Int): FocusDayOfWeek {
            return entries.firstOrNull { it.calendarDay == day } ?: MONDAY
        }
    }
}
