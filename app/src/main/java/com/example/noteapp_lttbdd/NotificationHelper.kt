package com.example.noteapp_lttbdd

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.app.TaskStackBuilder
object NotificationHelper {

    private const val CHANNEL_ID = "note_reminder_channel"
    private const val CHANNEL_NAME = "Nhắc hẹn ghi chú"
    private const val CHANNEL_DESCRIPTION = "Thông báo nhắc hẹn cho ghi chú"

    fun showReminderNotification(context: Context, note: Note) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                return
            }
        }

        val clickPendingIntent = createClickPendingIntent(context, note)

        val title = if (note.isLocked) {
            "Ghi chú được bảo vệ"
        } else {
            note.title.ifBlank { "Nhắc hẹn ghi chú" }
        }

        val content = if (note.isLocked) {
            "Nhấn để mở ghi chú"
        } else {
            getPlainTextPreview(note.content)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(clickPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(note.id.toInt(), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createClickPendingIntent(context: Context, note: Note): PendingIntent {
        if (note.isLocked) {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_FROM_REMINDER", true)
                putExtra("EXTRA_NOTE_ID", note.id)
            }

            return PendingIntent.getActivity(
                context,
                note.id.toInt(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val mainIntent = Intent(context, MainActivity::class.java)

        val noteIntent = Intent(context, AddNoteActivity::class.java).apply {
            putExtra("EXTRA_NOTE_ID", note.id)
            putExtra("EXTRA_NOTE_TITLE", note.title)
            putExtra("EXTRA_NOTE_CONTENT", note.content)

            putExtra("EXTRA_REMINDER_TIME", note.reminderTime)
            putExtra("EXTRA_IS_REMINDER_ENABLED", note.isReminderEnabled)
            putExtra("EXTRA_REPEAT_TYPE", note.repeatType)
        }

        return TaskStackBuilder.create(context).run {
            addNextIntent(mainIntent)
            addNextIntent(noteIntent)

            getPendingIntent(
                note.id.toInt(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }

    private fun getPlainTextPreview(htmlContent: String): String {
        val plainText = HtmlCompat.fromHtml(
            htmlContent,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()

        return plainText.ifBlank {
            "Bạn có một ghi chú cần xem lại"
        }
    }
}