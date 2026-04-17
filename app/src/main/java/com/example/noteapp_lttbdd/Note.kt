package com.example.noteapp_lttbdd

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val isLocked: Boolean = false
)
