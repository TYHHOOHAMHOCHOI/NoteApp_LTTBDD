package com.example.noteapp_lttbdd

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NoteListFragment : Fragment() {

    private lateinit var rvNotes: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var databaseHelper: DatabaseHelper
    
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var isReminderType: Boolean = false

    companion object {
        fun newInstance(startTime: Long, endTime: Long, isReminderType: Boolean): NoteListFragment {
            val fragment = NoteListFragment()
            val args = Bundle()
            args.putLong("START_TIME", startTime)
            args.putLong("END_TIME", endTime)
            args.putBoolean("IS_REMINDER", isReminderType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)
        
        startTime = arguments?.getLong("START_TIME") ?: 0
        endTime = arguments?.getLong("END_TIME") ?: 0
        isReminderType = arguments?.getBoolean("IS_REMINDER") ?: false
        
        databaseHelper = DatabaseHelper(requireContext())
        
        rvNotes = view.findViewById(R.id.rvNotes)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        
        setupRecyclerView()
        loadNotes()
        
        return view
    }

    private fun setupRecyclerView() {
        rvNotes.layoutManager = LinearLayoutManager(requireContext())
        noteAdapter = NoteAdapter(
            emptyList(),
            onItemClick = { note -> handleNoteClick(note) },
            onItemLongClick = { note, _ -> showNoteOptions(note) }
        )
        rvNotes.adapter = noteAdapter
    }

    fun loadNotes() {
        val notes = if (isReminderType) {
            databaseHelper.getNotesByReminderRange(startTime, endTime)
        } else {
            databaseHelper.getNotesByDate(startTime, endTime)
        }
        
        noteAdapter.updateData(notes)
        tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun handleNoteClick(note: Note) {
        if (note.isLocked) {
            showUnlockToViewDialog(note)
        } else {
            openNote(note)
        }
    }

    private fun openNote(note: Note) {
        val intent = Intent(requireContext(), AddNoteActivity::class.java)
        intent.putExtra("EXTRA_NOTE_ID", note.id)
        intent.putExtra("EXTRA_NOTE_TITLE", note.title)
        intent.putExtra("EXTRA_NOTE_CONTENT", note.content)
        intent.putExtra("EXTRA_NOTE_TAG", note.tag)
        intent.putExtra("EXTRA_REMINDER_TIME", note.reminderTime)
        intent.putExtra("EXTRA_IS_REMINDER_ENABLED", note.isReminderEnabled)
        intent.putExtra("EXTRA_REPEAT_TYPE", note.repeatType)
        startActivity(intent)
    }

    private fun showUnlockToViewDialog(note: Note) {
        val editText = EditText(requireContext())
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        
        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        AlertDialog.Builder(requireContext())
            .setTitle("Nhập mật khẩu để xem ghi chú")
            .setView(editText)
            .setPositiveButton("Mở") { _, _ ->
                val input = editText.text.toString()
                val savedPassword = sharedPref.getString("note_password", "")
                if (input == savedPassword) {
                    openNote(note)
                } else {
                    Toast.makeText(requireContext(), "Mật khẩu không chính xác", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showNoteOptions(note: Note) {
        val optionsList = mutableListOf("Xóa")
        if (note.isPinned) optionsList.add("Bỏ ghim") else optionsList.add("Ghim ghi chú")
        if (note.isLocked) optionsList.add("Mở khóa ghi chú") else optionsList.add("Khóa ghi chú")

        val options = optionsList.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Xóa" -> {
                        databaseHelper.softDeleteNote(note.id)
                        loadNotes()
                        Toast.makeText(requireContext(), "Đã chuyển vào thùng rác", Toast.LENGTH_SHORT).show()
                    }
                    "Ghim ghi chú" -> {
                        databaseHelper.updatePinStatus(note.id, true)
                        loadNotes()
                    }
                    "Bỏ ghim" -> {
                        databaseHelper.updatePinStatus(note.id, false)
                        loadNotes()
                    }
                    "Khóa ghi chú" -> {
                        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                        val savedPassword = sharedPref.getString("note_password", "")
                        if (savedPassword.isNullOrEmpty()) {
                            Toast.makeText(requireContext(), "Bạn chưa thiết lập mật khẩu trong Cài đặt", Toast.LENGTH_SHORT).show()
                        } else {
                            databaseHelper.updateLockStatus(note.id, true)
                            loadNotes()
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
        val editText = EditText(requireContext())
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        editText.hint = "Nhập mật khẩu"
        
        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        AlertDialog.Builder(requireContext())
            .setTitle("Xác thực")
            .setMessage("Vui lòng nhập mật khẩu để mở khóa ghi chú này")
            .setView(editText)
            .setPositiveButton("Xác nhận") { _, _ ->
                val input = editText.text.toString()
                val savedPassword = sharedPref.getString("note_password", "")
                if (input == savedPassword) {
                    databaseHelper.updateLockStatus(note.id, false)
                    loadNotes()
                    Toast.makeText(requireContext(), "Đã mở khóa ghi chú", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Mật khẩu không chính xác", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }
}