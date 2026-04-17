package com.example.noteapp_lttbdd

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
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
    private lateinit var etSearch: EditText // Khai báo biến cho ô tìm kiếm
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var sharedPref: SharedPreferences

    private var noteList: List<Note> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {

        // 🔥 ÁP DỤNG DARK MODE TRƯỚC KHI LOAD UI
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

        // Edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        databaseHelper = DatabaseHelper(this)

        rvNotes = findViewById(R.id.rvNotes)
        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch) // Ánh xạ UI
        fabAddNote = findViewById(R.id.fabAddNote)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Cài đặt trình lắng nghe khi người dùng nhập Text để tìm kiếm
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Gọi hàm lọc danh sách dựa trên từ khóa người dùng nhập vào
                filterNotes(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        rvNotes.layoutManager = LinearLayoutManager(this)

        noteAdapter = NoteAdapter(noteList) { selectedNote ->
            val intent = Intent(this, AddNoteActivity::class.java)
            intent.putExtra("EXTRA_NOTE_ID", selectedNote.id)
            intent.putExtra("EXTRA_NOTE_TITLE", selectedNote.title)
            intent.putExtra("EXTRA_NOTE_CONTENT", selectedNote.content)
            startActivity(intent)
        }

        rvNotes.adapter = noteAdapter

        fabAddNote.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            startActivity(intent)
        }

        setupBottomNavigation()
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
        
        // Cập nhật lại danh sách tùy vào việc có đang tìm kiếm hay không
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

    // Hàm lọc các ghi chú dựa theo tiêu đề hoặc nội dung
    private fun filterNotes(query: String) {
        val filteredList = noteList.filter { note ->
            note.title.contains(query, ignoreCase = true) || 
            note.content.contains(query, ignoreCase = true)
        }
        
        // Cập nhật lại RecyclerView với danh sách đã lọc
        noteAdapter.updateData(filteredList)

        // Kiểm tra nếu không có kết quả hợp lệ thì hiển thị thông báo
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