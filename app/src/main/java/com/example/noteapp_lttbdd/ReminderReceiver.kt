package com.example.noteapp_lttbdd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_NOTE_REMINDER) {
            return
        }

        val noteId = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)

        if (noteId == -1L) {
            return
        }

        Log.d("ReminderReceiver", "Reminder triggered for noteId = $noteId")

        val databaseHelper = DatabaseHelper(context)
        val note = databaseHelper.getNoteById(noteId)

        if (note == null) {
            Log.d("ReminderReceiver", "Note not found for noteId = $noteId")
            return
        }

        if (!note.isReminderEnabled) {
            Log.d("ReminderReceiver", "Reminder is disabled for noteId = $noteId")
            return
        }

        NotificationHelper.showReminderNotification(context, note)
    }
}