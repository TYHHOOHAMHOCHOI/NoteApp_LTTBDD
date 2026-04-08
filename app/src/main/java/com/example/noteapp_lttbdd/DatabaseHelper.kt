package com.example.noteapp_lttbdd

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes_db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NOTES = "notes"

        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = ("CREATE TABLE $TABLE_NOTES ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_TITLE TEXT,"
                + "$COLUMN_CONTENT TEXT)")
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }

    fun insertNote(title: String, content: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
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

    fun getAllNotes(): List<Note> {
        val noteList = mutableListOf<Note>()
        val selectQuery = "SELECT * FROM $TABLE_NOTES ORDER BY $COLUMN_ID DESC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
                noteList.add(Note(id, title, content))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return noteList
    }
}
