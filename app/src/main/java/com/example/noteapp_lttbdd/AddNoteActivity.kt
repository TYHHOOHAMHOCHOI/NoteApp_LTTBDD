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
    
    // Biến để lưu ID, mặc định -1 nghĩa là Đang Thêm Mới
    private var currentNoteId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        databaseHelper = DatabaseHelper(this)

        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteContent = findViewById(R.id.etNoteContent)
        btnSaveNote = findViewById(R.id.btnSaveNote)

        // KIỂM TRA: Có phải đang mở để CHỈNH SỬA không?
        if (intent.hasExtra("EXTRA_NOTE_ID")) {
            currentNoteId = intent.getLongExtra("EXTRA_NOTE_ID", -1L)
            etNoteTitle.setText(intent.getStringExtra("EXTRA_NOTE_TITLE"))
            etNoteContent.setText(intent.getStringExtra("EXTRA_NOTE_CONTENT"))
            btnSaveNote.text = "CẬP NHẬT GHI CHÚ"
        }

        btnSaveNote.setOnClickListener {
            val title = etNoteTitle.text.toString().trim()
            val content = etNoteContent.text.toString().trim()

            // Bắt buộc nhập cả hai
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentNoteId == -1L) {
                // ĐANG LƯU MỚI
                val id = databaseHelper.insertNote(title, content)
                if (id > -1) {
                    Toast.makeText(this, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Lỗi khi lưu!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // ĐANG CẬP NHẬT (SỬA)
                val rows = databaseHelper.updateNote(currentNoteId, title, content)
                if (rows > 0) {
                    Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Lỗi khi cập nhật!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}