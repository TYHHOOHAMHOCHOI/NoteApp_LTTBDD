package com.example.noteapp_lttbdd

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes_db"
        private const val DATABASE_VERSION = 8
        private const val TABLE_NOTES = "notes"

        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_IS_LOCKED = "is_locked"
        private const val COLUMN_IS_PINNED = "is_pinned"
        private const val COLUMN_REMINDER_TIME = "reminder_time"
        private const val COLUMN_IS_REMINDER_ENABLED = "is_reminder_enabled"
        private const val COLUMN_REPEAT_TYPE = "repeat_type"
        private const val COLUMN_TAG = "tag"               // Tên cột lưu thẻ (tag)
        private const val COLUMN_IS_DELETED = "is_deleted"
        private const val COLUMN_CREATED_AT = "created_at"
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
                + "$COLUMN_REPEAT_TYPE TEXT DEFAULT 'once',"
                + "$COLUMN_TAG TEXT DEFAULT '',"
                + "$COLUMN_IS_DELETED INTEGER DEFAULT 0,"
                + "$COLUMN_CREATED_AT INTEGER DEFAULT 0)")
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
        // Version 6: thêm cột tag
        if (oldVersion < 6) {
            ensureColumnExists(db, COLUMN_TAG, "$COLUMN_TAG TEXT DEFAULT ''")
        }
        // Version 7: thêm cột is_deleted
        if (oldVersion < 7) {
            ensureColumnExists(db, COLUMN_IS_DELETED, "$COLUMN_IS_DELETED INTEGER DEFAULT 0")
        }
        // Version 8 : cập nhật các mốc thời gian cho ghi chú hiện có (in calendar)
        if (oldVersion < 8) {
            ensureColumnExists(db, COLUMN_CREATED_AT, "$COLUMN_CREATED_AT INTEGER DEFAULT 0")
            db.execSQL("UPDATE $TABLE_NOTES SET $COLUMN_CREATED_AT = ${System.currentTimeMillis()} WHERE $COLUMN_CREATED_AT = 0")
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
            ensureColumnExists(db, COLUMN_TAG, "$COLUMN_TAG TEXT DEFAULT ''")
            ensureColumnExists(db, COLUMN_IS_DELETED, "$COLUMN_IS_DELETED INTEGER DEFAULT 0")
            ensureColumnExists(db, COLUMN_CREATED_AT, "$COLUMN_CREATED_AT INTEGER DEFAULT 0")
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

    // Thêm ghi chú mới vào database
    fun insertNote(title: String, content: String, tag: String = ""): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_IS_LOCKED, 0)
            put(COLUMN_IS_PINNED, 0)      // Khi tạo mới mặc định không ghim
            put(COLUMN_REMINDER_TIME, 0L)
            put(COLUMN_IS_REMINDER_ENABLED, 0)
            put(COLUMN_REPEAT_TYPE, "once")
            put(COLUMN_TAG, tag)          // Lưu tag vào database
            put(COLUMN_IS_DELETED, 0)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        val id = db.insert(TABLE_NOTES, null, contentValues)
        db.close()
        return id
    }

    // Cập nhật tiêu đề và nội dung ghi chú
    fun updateNote(id: Long, title: String, content: String, tag: String = ""): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_TAG, tag)          // Cập nhật tag khi lưu ghi chú
        }
        val success = db.update(TABLE_NOTES, contentValues, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    // Cập nhật tag riêng lẻ cho ghi chú (dùng khi chỉ muốn đổi tag)
    fun updateTag(id: Long, tag: String): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TAG, tag)
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

    fun softDeleteNote(id: Long): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_IS_DELETED, 1)
        }
        val success = db.update(TABLE_NOTES, contentValues, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    fun restoreNote(id: Long): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_IS_DELETED, 0)
        }
        val success = db.update(TABLE_NOTES, contentValues, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    fun permanentlyDeleteNote(id: Long): Int {
        val db = this.writableDatabase
        val success = db.delete(TABLE_NOTES, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    fun getAllNotes(): List<Note> {
        val noteList = mutableListOf<Note>()
        // Sắp xếp ưu tiên hiển thị mục ghim (is_pinned DESC) trước rồi mới theo ID giảm dần
        val selectQuery = "SELECT * FROM $TABLE_NOTES WHERE $COLUMN_IS_DELETED = 0 " +
                "ORDER BY $COLUMN_IS_PINNED DESC, $COLUMN_ID DESC"
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
                // Lấy giá trị tag, nếu cột chưa có thì mặc định là chuỗi rỗng
                val tagIndex = cursor.getColumnIndex(COLUMN_TAG)
                val tag = if (tagIndex != -1) cursor.getString(tagIndex) ?: "" else ""
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1
                val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))

                noteList.add(
                    Note(
                        id = id,
                        title = title,
                        content = content,
                        isLocked = isLocked,
                        isPinned = isPinned,
                        reminderTime = reminderTime,
                        isReminderEnabled = isReminderEnabled,
                        repeatType = repeatType,
                        tag = tag,  // Gán tag cho ghi chú
                        isDeleted = isDeleted,
                        createdAt = createdAt
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return noteList
    }

    fun getDeletedNotes(): List<Note> {
        val noteList = mutableListOf<Note>()
        val selectQuery = "SELECT * FROM $TABLE_NOTES WHERE $COLUMN_IS_DELETED = 1 " +
                "ORDER BY $COLUMN_ID DESC"

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
                val tagIndex = cursor.getColumnIndex(COLUMN_TAG)
                val tag = if (tagIndex != -1) cursor.getString(tagIndex) ?: "" else ""
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1
                val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))

                noteList.add(
                    Note(
                        id = id,
                        title = title,
                        content = content,
                        isLocked = isLocked,
                        isPinned = isPinned,
                        reminderTime = reminderTime,
                        isReminderEnabled = isReminderEnabled,
                        repeatType = repeatType,
                        tag = tag,
                        isDeleted = isDeleted,
                        createdAt = createdAt
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
            // Lấy tag, nếu cột chưa tồn tại thì trả về chuỗi rỗng
            val tagIndex = cursor.getColumnIndex(COLUMN_TAG)
            val tag = if (tagIndex != -1) cursor.getString(tagIndex) ?: "" else ""
            val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1
            val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))

            note = Note(
                id = noteId,
                title = title,
                content = content,
                isLocked = isLocked,
                isPinned = isPinned,
                reminderTime = reminderTime,
                isReminderEnabled = isReminderEnabled,
                repeatType = repeatType,
                tag = tag,
                isDeleted = isDeleted,
                createdAt = createdAt
            )
        }

        cursor.close()
        db.close()

        return note
    }
    fun getNotesByDate(startTime: Long, endTime: Long): List<Note> {
        val noteList = mutableListOf<Note>()
        val db = this.readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NOTES WHERE $COLUMN_IS_DELETED = 0 AND $COLUMN_CREATED_AT >= ? AND $COLUMN_CREATED_AT <= ?",
            arrayOf(startTime.toString(), endTime.toString())
        )

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
                val tag = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG)) ?: ""
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1
                val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))

                noteList.add(
                    Note(
                        id = id,
                        title = title,
                        content = content,
                        isLocked = isLocked,
                        isPinned = isPinned,
                        reminderTime = reminderTime,
                        isReminderEnabled = isReminderEnabled,
                        repeatType = repeatType,
                        tag = tag,
                        isDeleted = isDeleted,
                        createdAt = createdAt
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return noteList
    }

    fun getEnabledReminderNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase

        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$COLUMN_IS_REMINDER_ENABLED=? AND $COLUMN_REMINDER_TIME>?",
            arrayOf("1", "0"),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                val content = it.getString(it.getColumnIndexOrThrow(COLUMN_CONTENT))
                val isLocked = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_LOCKED)) == 1
                val isPinned = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_PINNED)) == 1
                val reminderTime = it.getLong(it.getColumnIndexOrThrow(COLUMN_REMINDER_TIME))
                val isReminderEnabled = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_REMINDER_ENABLED)) == 1
                val repeatType = it.getString(it.getColumnIndexOrThrow(COLUMN_REPEAT_TYPE)) ?: "once"
                val tag = it.getString(it.getColumnIndexOrThrow(COLUMN_TAG)) ?: ""
                val isDeleted = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1
                val createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))

                notes.add(
                    Note(
                        id = id,
                        title = title,
                        content = content,
                        isLocked = isLocked,
                        isPinned = isPinned,
                        reminderTime = reminderTime,
                        isReminderEnabled = isReminderEnabled,
                        repeatType = repeatType,
                        tag = tag,
                        isDeleted = isDeleted,
                        createdAt = createdAt
                    )
                )
            }
        }

        db.close()
        return notes
    }
}