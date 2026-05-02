package com.example.noteapp_lttbdd

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val isLocked: Boolean = false,
    val isPinned: Boolean = false, // Trạng thái kiểm tra ghi chú có được ghim hay không
    val reminderTime: Long = 0L,
    val isReminderEnabled: Boolean = false,
    val repeatType: String = "once",
    val tag: String = ""            // Thẻ (tag) gắn cho ghi chú, mặc định rỗng
)
