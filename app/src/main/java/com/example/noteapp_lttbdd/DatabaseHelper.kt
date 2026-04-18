package com.example.noteapp_lttbdd

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes_db"
        private const val DATABASE_VERSION = 3 // Tăng version từ 2 lên 3 để hỗ trợ ghim
        private const val TABLE_NOTES = "notes"

        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_IS_LOCKED = "is_locked"
        private const val COLUMN_IS_PINNED = "is_pinned" // Cột lưu trạng thái ghim
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = ("CREATE TABLE $TABLE_NOTES ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_TITLE TEXT,"
                + "$COLUMN_CONTENT TEXT,"
                + "$COLUMN_IS_LOCKED INTEGER DEFAULT 0,"
                + "$COLUMN_IS_PINNED INTEGER DEFAULT 0)") // Mặc định 0 là chưa ghim
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_IS_LOCKED INTEGER DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_IS_PINNED INTEGER DEFAULT 0")
        }
    }

    fun insertNote(title: String, content: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_IS_LOCKED, 0)
            put(COLUMN_IS_PINNED, 0) // Khi tạo mới mặc định không ghim
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
                
                noteList.add(Note(id, title, content, isLocked, isPinned))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return noteList
    }
}
