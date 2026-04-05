package com.example.noteapp_lttbdd

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddNoteActivity : AppCompatActivity() {

    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var btnSaveNote: Button
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        databaseHelper = DatabaseHelper(this)

        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteContent = findViewById(R.id.etNoteContent)
        btnSaveNote = findViewById(R.id.btnSaveNote)

        btnSaveNote.setOnClickListener {
            val title = etNoteTitle.text.toString().trim()
            val content = etNoteContent.text.toString().trim()

            if (title.isEmpty() && content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = databaseHelper.insertNote(title, content)
            if (id > -1) {
                Toast.makeText(this, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show()
                finish() // Đóng activity và trở về màn hình chính
            } else {
                Toast.makeText(this, "Lỗi khi lưu!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
