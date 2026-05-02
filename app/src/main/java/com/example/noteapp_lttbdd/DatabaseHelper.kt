package com.example.noteapp_lttbdd

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes_db"
        private const val DATABASE_VERSION = 5
        private const val TABLE_NOTES = "notes"

        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_IS_LOCKED = "is_locked"
        private const val COLUMN_IS_PINNED = "is_pinned"
        private const val COLUMN_REMINDER_TIME = "reminder_time"
        private const val COLUMN_IS_REMINDER_ENABLED = "is_reminder_enabled"
        private const val COLUMN_REPEAT_TYPE = "repeat_type"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = ("CREATE TABLE $TABLE_NOTES ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_TITLE TEXT,"
                + "$COLUMN_CONTENT TEXT,"
                + "$COLUMN_IS_LOCKED INTEGER DEFAULT 0,"
                + "$COLUMN_IS_PINNED INTEGER DEFAULT 0,"
                + "$COLUMN_REMINDER_TIME INTEGER DEFAULT 0,"
                + "$COLUMN_IS_REMINDER_ENABLED INTEGER DEFAULT 0,"
                + "$COLUMN_REPEAT_TYPE TEXT DEFAULT 'once')")
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        ensureColumnExists(db, COLUMN_IS_LOCKED, "$COLUMN_IS_LOCKED INTEGER DEFAULT 0")
        ensureColumnExists(db, COLUMN_IS_PINNED, "$COLUMN_IS_PINNED INTEGER DEFAULT 0")

        if (oldVersion < 5) {
            ensureColumnExists(db, COLUMN_REMINDER_TIME, "$COLUMN_REMINDER_TIME INTEGER DEFAULT 0")
            ensureColumnExists(db, COLUMN_IS_REMINDER_ENABLED, "$COLUMN_IS_REMINDER_ENABLED INTEGER DEFAULT 0")
            ensureColumnExists(db, COLUMN_REPEAT_TYPE, "$COLUMN_REPEAT_TYPE TEXT DEFAULT 'once'")
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)

        if (!db.isReadOnly) {
            ensureColumnExists(db, COLUMN_IS_LOCKED, "$COLUMN_IS_LOCKED INTEGER DEFAULT 0")
            ensureColumnExists(db, COLUMN_IS_PINNED, "$COLUMN_IS_PINNED INTEGER DEFAULT 0")
            ensureColumnExists(db, COLUMN_REMINDER_TIME, "$COLUMN_REMINDER_TIME INTEGER DEFAULT 0")
            ensureColumnExists(db, COLUMN_IS_REMINDER_ENABLED, "$COLUMN_IS_REMINDER_ENABLED INTEGER DEFAULT 0")
            ensureColumnExists(db, COLUMN_REPEAT_TYPE, "$COLUMN_REPEAT_TYPE TEXT DEFAULT 'once'")
        }
    }

    private fun ensureColumnExists(db: SQLiteDatabase, columnName: String, columnDefinition: String) {
        if (!columnExists(db, columnName)) {
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $columnDefinition")
        }
    }

    private fun columnExists(db: SQLiteDatabase, columnName: String): Boolean {
        val cursor = db.rawQuery("PRAGMA table_info($TABLE_NOTES)", null)
        cursor.use {
            val nameIndex = it.getColumnIndex("name")
            while (it.moveToNext()) {
                if (it.getString(nameIndex) == columnName) {
                    return true
                }
            }
        }
        return false
    }

    fun insertNote(title: String, content: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_IS_LOCKED, 0)
            put(COLUMN_IS_PINNED, 0) // Khi tạo mới mặc định không ghim
            put(COLUMN_REMINDER_TIME, 0L)
            put(COLUMN_IS_REMINDER_ENABLED, 0)
            put(COLUMN_REPEAT_TYPE, "once")
        }
        val id = db.insert(TABLE_NOTES, null, contentValues)
        db.close()
        return id
    }

    fun updateNote(id: Long, title: String, content: String): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
        }
        val success = db.update(TABLE_NOTES, contentValues, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    fun updateLockStatus(id: Long, isLocked: Boolean): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_IS_LOCKED, if (isLocked) 1 else 0)
        }
        val success = db.update(TABLE_NOTES, contentValues, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    // Cập nhật trạng thái ghim ghi chú
    fun updatePinStatus(id: Long, isPinned: Boolean): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_IS_PINNED, if (isPinned) 1 else 0)
        }
        val success = db.update(TABLE_NOTES, contentValues, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    fun updateReminder(id: Long, reminderTime: Long, repeatType: String): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_REMINDER_TIME, reminderTime)
            put(COLUMN_IS_REMINDER_ENABLED, 1)
            put(COLUMN_REPEAT_TYPE, repeatType)
        }

        val success = db.update(
            TABLE_NOTES,
            contentValues,
            "$COLUMN_ID=?",
            arrayOf(id.toString())
        )

        db.close()
        return success
    }

    fun clearReminder(id: Long): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_REMINDER_TIME, 0L)
            put(COLUMN_IS_REMINDER_ENABLED, 0)
            put(COLUMN_REPEAT_TYPE, "once")
        }

        val success = db.update(
            TABLE_NOTES,
            contentValues,
            "$COLUMN_ID=?",
            arrayOf(id.toString())
        )

        db.close()
        return success
    }

    fun deleteNote(id: Long): Int {
        val db = this.writableDatabase
        val success = db.delete(TABLE_NOTES, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    fun getAllNotes(): List<Note> {
        val noteList = mutableListOf<Note>()
        // Sắp xếp ưu tiên hiển thị mục ghim (is_pinned DESC) trước rồi mới theo ID giảm dần
        val selectQuery = "SELECT * FROM $TABLE_NOTES ORDER BY $COLUMN_IS_PINNED DESC, $COLUMN_ID DESC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)) 
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
                val isLocked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_LOCKED)) == 1
                val isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PINNED)) == 1
                val reminderTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME))
                val isReminderEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_REMINDER_ENABLED)) == 1
                val repeatType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPEAT_TYPE)) ?: "once"

                noteList.add(
                    Note(
                        id = id,
                        title = title,
                        content = content,
                        isLocked = isLocked,
                        isPinned = isPinned,
                        reminderTime = reminderTime,
                        isReminderEnabled = isReminderEnabled,
                        repeatType = repeatType
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return noteList
    }

    fun getNoteById(id: Long): Note? {
        val db = this.readableDatabase

        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$COLUMN_ID=?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        var note: Note? = null

        if (cursor.moveToFirst()) {
            val noteId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
            val isLocked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_LOCKED)) == 1
            val isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PINNED)) == 1
            val reminderTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME))
            val isReminderEnabled =
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_REMINDER_ENABLED)) == 1
            val repeatType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPEAT_TYPE)) ?: "once"

            note = Note(
                id = noteId,
                title = title,
                content = content,
                isLocked = isLocked,
                isPinned = isPinned,
                reminderTime = reminderTime,
                isReminderEnabled = isReminderEnabled,
                repeatType = repeatType
            )
        }

        cursor.close()
        db.close()

        return note
    }
}
