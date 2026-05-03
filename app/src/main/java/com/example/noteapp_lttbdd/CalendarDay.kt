package com.example.noteapp_lttbdd

data class CalendarDay(
    val day: Int,
    val noteCount: Int,
    val hasReminder: Boolean = false
)
