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

        handleReminderTrigger(context, databaseHelper, note)
    }

    private fun handleReminderTrigger(
        context: Context,
        databaseHelper: DatabaseHelper,
        note: Note
    ) {
        when (note.repeatType) {
            "daily", "weekly" -> {
                val nextTime = ReminderScheduler.getNextRepeatTime(
                    currentTime = note.reminderTime,
                    repeatType = note.repeatType
                )

                if (nextTime > 0L) {
                    databaseHelper.updateReminder(
                        note.id,
                        nextTime,
                        note.repeatType
                    )

                    ReminderScheduler.scheduleReminder(
                        context = context,
                        noteId = note.id,
                        reminderTime = nextTime
                    )

                    val updatedNoteForClick = note.copy(
                        reminderTime = nextTime,
                        isReminderEnabled = true
                    )

                    NotificationHelper.showReminderNotification(
                        context,
                        updatedNoteForClick
                    )
                }
            }

            else -> {
                databaseHelper.clearReminder(note.id)

                val updatedNoteForClick = note.copy(
                    reminderTime = 0L,
                    isReminderEnabled = false,
                    repeatType = "once"
                )

                NotificationHelper.showReminderNotification(
                    context,
                    updatedNoteForClick
                )
            }
        }
    }
}