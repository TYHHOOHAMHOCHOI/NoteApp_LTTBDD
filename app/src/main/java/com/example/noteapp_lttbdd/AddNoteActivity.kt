package com.example.noteapp_lttbdd

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class AddNoteActivity : AppCompatActivity() {

    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var databaseHelper: DatabaseHelper

    private var currentNoteId: Long = -1L
    private var originalTitle: String = ""
    private var originalContent: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        databaseHelper = DatabaseHelper(this)

        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteContent = findViewById(R.id.etNoteContent)
        findViewById<android.view.View>(R.id.btnAddInNote).setOnClickListener {
            showBottomMenu()
        }
        findViewById<android.view.View>(R.id.btnAlign).setOnClickListener {
            showBottomMenu()
        }
        findViewById<android.view.View>(R.id.btnTextSetting).setOnClickListener {
            showBottomMenu()
        }

        if (intent.hasExtra("EXTRA_NOTE_ID")) {
            currentNoteId = intent.getLongExtra("EXTRA_NOTE_ID", -1L)
            originalTitle = intent.getStringExtra("EXTRA_NOTE_TITLE").orEmpty()
            originalContent = intent.getStringExtra("EXTRA_NOTE_CONTENT").orEmpty()

            etNoteTitle.setText(originalTitle)
            etNoteContent.setText(originalContent)
        }
    }

    override fun onPause() {
        persistNoteIfNeeded()
        super.onPause()
    }

    private fun persistNoteIfNeeded() {
        val title = etNoteTitle.text.toString().trim()
        val content = etNoteContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            return
        }

        if (currentNoteId == -1L) {
            val insertedId = databaseHelper.insertNote(title, content)
            if (insertedId > -1L) {
                currentNoteId = insertedId
                originalTitle = title
                originalContent = content
            }
            return
        }

        if (title == originalTitle && content == originalContent) {
            return
        }

        val updatedRows = databaseHelper.updateNote(currentNoteId, title, content)
        if (updatedRows > 0) {
            originalTitle = title
            originalContent = content
        }
    }

    private fun showBottomMenu() {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_note_action)
        bottomSheetDialog.setOnShowListener { dialog ->
            val sheetDialog = dialog as BottomSheetDialog
            val bottomSheet =
                sheetDialog.findViewById<android.view.View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener

            val targetHeight = (resources.displayMetrics.heightPixels * 0.4f).toInt()
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = targetHeight
            }

            BottomSheetBehavior.from(bottomSheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
                isFitToContents = false
                expandedOffset = resources.displayMetrics.heightPixels - targetHeight
            }
        }
        bottomSheetDialog.show()
    }
}
