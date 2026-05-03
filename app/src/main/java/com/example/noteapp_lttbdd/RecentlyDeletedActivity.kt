package com.example.noteapp_lttbdd

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecentlyDeletedActivity : AppCompatActivity() {

    private lateinit var rvDeletedNotes: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var deletedAdapter: DeletedNoteAdapter
    private lateinit var databaseHelper: DatabaseHelper

    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = LocaleHelper.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recently_deleted)

        databaseHelper = DatabaseHelper(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        rvDeletedNotes = findViewById(R.id.rvDeletedNotes)
        tvEmpty = findViewById(R.id.tvEmpty)

        btnBack?.setOnClickListener { finish() }

        setupRecyclerView()
        loadDeletedNotes()
    }

    private fun setupRecyclerView() {
        deletedAdapter = DeletedNoteAdapter { note ->
            showNoteOptions(note)
        }
        rvDeletedNotes.layoutManager = LinearLayoutManager(this)
        rvDeletedNotes.adapter = deletedAdapter
    }

    private fun loadDeletedNotes() {
        val deletedList = databaseHelper.getDeletedNotes()

        if (deletedList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvDeletedNotes.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvDeletedNotes.visibility = View.VISIBLE
            deletedAdapter.submitList(deletedList)
        }
    }

    private fun showNoteOptions(note: Note) {
        val options = arrayOf("Khôi phục", "Xóa vĩnh viễn")
        AlertDialog.Builder(this)
            .setTitle(note.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        databaseHelper.restoreNote(note.id)
                        loadDeletedNotes()
                        Toast.makeText(this, "Đã khôi phục ghi chú", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        showConfirmDeleteDialog(note)
                    }
                }
            }
            .show()
    }

    private fun showConfirmDeleteDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Xóa vĩnh viễn")
            .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn ghi chú này? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                databaseHelper.permanentlyDeleteNote(note.id)
                loadDeletedNotes()
                Toast.makeText(this, "Đã xóa vĩnh viễn", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
