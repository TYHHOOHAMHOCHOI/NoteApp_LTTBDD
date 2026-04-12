package com.example.noteapp_lttbdd

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var rvNotes: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private var noteList: List<Note> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Xử lý edge-to-edge: Chỉ áp dụng padding cho top, left, right. 
        // BottomNavigationView sẽ tự xử lý bottom inset.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        databaseHelper = DatabaseHelper(this)

        rvNotes = findViewById(R.id.rvNotes)
        tvEmpty = findViewById(R.id.tvEmpty)
        fabAddNote = findViewById(R.id.fabAddNote)
        bottomNavigation = findViewById(R.id.bottom_navigation)

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
                    // Loại bỏ hiệu ứng chuyển cảnh để mượt mà hơn như các tab
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
        // Đảm bảo item Home được chọn khi quay lại từ các màn hình khác
        bottomNavigation.selectedItemId = R.id.navigation_home
    }

    private fun loadNotes() {
        noteList = databaseHelper.getAllNotes()
        noteAdapter.updateData(noteList)

        if (noteList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvNotes.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvNotes.visibility = View.VISIBLE
        }
    }
}