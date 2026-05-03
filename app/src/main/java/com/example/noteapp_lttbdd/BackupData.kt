package com.example.noteapp_lttbdd

data class BackupData(
    val version: Int = 1,
    val appVersion: String = "1.0",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: List<Note>
)
