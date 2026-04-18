package com.example.noteapp_lttbdd

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var rvNotes: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var sharedPref: SharedPreferences

    private var noteList: List<Note> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        databaseHelper = DatabaseHelper(this)

        rvNotes = findViewById(R.id.rvNotes)
        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)
        fabAddNote = findViewById(R.id.fabAddNote)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        rvNotes.layoutManager = LinearLayoutManager(this)

        noteAdapter = NoteAdapter(
            noteList,
            onItemClick = { selectedNote ->
                if (selectedNote.isLocked) {
                    showUnlockToViewDialog(selectedNote)
                } else {
                    openNote(selectedNote)
                }
            },
            onItemLongClick = { selectedNote, view ->
                showNoteOptions(selectedNote, view)
            }
        )

        rvNotes.adapter = noteAdapter

        fabAddNote.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            startActivity(intent)
        }

        setupBottomNavigation()
    }

    private fun openNote(note: Note) {
        val intent = Intent(this, AddNoteActivity::class.java)
        intent.putExtra("EXTRA_NOTE_ID", note.id)
        intent.putExtra("EXTRA_NOTE_TITLE", note.title)
        intent.putExtra("EXTRA_NOTE_CONTENT", note.content)
        startActivity(intent)
    }

    private fun showUnlockToViewDialog(note: Note) {
        val editText = EditText(this)
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        
        AlertDialog.Builder(this)
            .setTitle("Nhập mật khẩu để xem ghi chú")
            .setView(editText)
            .setPositiveButton("Mở") { _, _ ->
                val input = editText.text.toString()
                val savedPassword = sharedPref.getString("note_password", "")
                if (input == savedPassword) {
                    openNote(note)
                } else {
                    Toast.makeText(this, "Mật khẩu không chính xác", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showNoteOptions(note: Note, view: View) {
        val optionsList = mutableListOf("Xóa")

        // Thêm mục Ghim/Bỏ ghim tùy thuộc vào trạng thái
        if (note.isPinned) {
            optionsList.add("Bỏ ghim")
        } else {
            optionsList.add("Ghim ghi chú")
        }

        if (note.isLocked) {
            optionsList.add("Mở khóa ghi chú")
        } else {
            optionsList.add("Khóa ghi chú")
        }

        val options = optionsList.toTypedArray()

        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Xóa" -> {
                        databaseHelper.deleteNote(note.id)
                        loadNotes()
                        Toast.makeText(this, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show()
                    }
                    "Ghim ghi chú" -> {
                        databaseHelper.updatePinStatus(note.id, true) // Update trạng thái ghim xuống DB
                        loadNotes() // Tải lại list
                        Toast.makeText(this, "Đã ghim lên phía trên", Toast.LENGTH_SHORT).show()
                    }
                    "Bỏ ghim" -> {
                        databaseHelper.updatePinStatus(note.id, false) // Hủy trạng thái ghim từ DB
                        loadNotes()
                        Toast.makeText(this, "Đã gỡ ghim", Toast.LENGTH_SHORT).show()
                    }
                    "Khóa ghi chú" -> { 
                        val savedPassword = sharedPref.getString("note_password", "")
                        if (savedPassword.isNullOrEmpty()) {
                            Toast.makeText(this, "Bạn chưa thiết lập mật khẩu trong Cài đặt", Toast.LENGTH_SHORT).show()
                        } else {
                            databaseHelper.updateLockStatus(note.id, true)
                            loadNotes()
                            Toast.makeText(this, "Ghi chú đã bị khóa", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Mở khóa ghi chú" -> {
                        showVerifyPasswordToUnlock(note)
                    }
                }
            }
            .show()
    }

    private fun showVerifyPasswordToUnlock(note: Note) {
        val editText = EditText(this)
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        editText.hint = "Nhập mật khẩu"

        AlertDialog.Builder(this)
            .setTitle("Xác thực")
            .setMessage("Vui lòng nhập mật khẩu để mở khóa ghi chú này")
            .setView(editText)
            .setPositiveButton("Xác nhận") { _, _ ->
                val input = editText.text.toString()
                val savedPassword = sharedPref.getString("note_password", "")
                if (input == savedPassword) {
                    databaseHelper.updateLockStatus(note.id, false)
                    loadNotes()
                    Toast.makeText(this, "Đã mở khóa ghi chú", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Mật khẩu không chính xác", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.navigation_home
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
        bottomNavigation.selectedItemId = R.id.navigation_home
    }

    private fun loadNotes() {
        noteList = databaseHelper.getAllNotes()
        val currentQuery = etSearch.text.toString()
        if (currentQuery.isNotEmpty()) {
            filterNotes(currentQuery)
        } else {
            noteAdapter.updateData(noteList)
            if (noteList.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Chưa có ghi chú nào"
                rvNotes.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvNotes.visibility = View.VISIBLE
            }
        }
    }

    private fun filterNotes(query: String) {
        val filteredList = noteList.filter { note ->
            note.title.contains(query, ignoreCase = true) || 
            note.content.contains(query, ignoreCase = true)
        }
        noteAdapter.updateData(filteredList)
        if (filteredList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Không tìm thấy kết quả"
            rvNotes.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvNotes.visibility = View.VISIBLE
        }
    }
}