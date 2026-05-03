package com.example.noteapp_lttbdd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.d("BootReceiver", "Device boot completed, restoring reminders")

        val databaseHelper = DatabaseHelper(context)
        val reminderNotes = databaseHelper.getEnabledReminderNotes()
        val now = System.currentTimeMillis()

        reminderNotes.forEach { note ->
            when {
                note.reminderTime > now -> {
                    ReminderScheduler.scheduleReminder(
                        context = context,
                        noteId = note.id,
                        reminderTime = note.reminderTime
                    )

                    Log.d("BootReceiver", "Restored reminder for noteId=${note.id}")
                }

                note.repeatType == "daily" || note.repeatType == "weekly" -> {
                    val nextTime = ReminderScheduler.getNextFutureRepeatTime(
                        currentTime = note.reminderTime,
                        repeatType = note.repeatType,
                        now = now
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

                        Log.d("BootReceiver", "Restored repeat reminder for noteId=${note.id}")
                    }
                }

                else -> {
                    databaseHelper.clearReminder(note.id)

                    Log.d("BootReceiver", "Cleared expired once reminder for noteId=${note.id}")
                }
            }
        }
    }
}