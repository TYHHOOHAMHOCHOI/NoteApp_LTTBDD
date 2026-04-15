package com.example.noteapp_lttbdd

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
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

    // Theo dõi chế độ danh sách hiện tại: "none", "dot", "ordered", "alpha"
    private var activeListMode: String = "none"
    private var isAutoInserting = false

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
            showAlignMenu()
        }
        findViewById<android.view.View>(R.id.btnTextSetting).setOnClickListener {
            showBottomMenu()
        }

        setupListAutoComplete()

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

    /**
     * TextWatcher tự động chèn đầu dòng tiếp theo khi người dùng nhấn Enter
     * trong chế độ danh sách đang hoạt động.
     * Nếu dòng trước chỉ có prefix mà không có nội dung → xóa prefix đó và tắt chế độ list.
     */
    private fun setupListAutoComplete() {
        etNoteContent.addTextChangedListener(object : TextWatcher {
            private var beforeLength: Int = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isAutoInserting || s == null || activeListMode == "none") return

                val text = s.toString()
                val cursorPos = etNoteContent.selectionStart
                if (cursorPos <= 0 || cursorPos > text.length) return

                // Chỉ xử lý khi có ký tự mới được thêm vào (không phải xóa)
                if (text.length <= beforeLength) return

                // Chỉ xử lý khi ký tự vừa gõ là Enter
                if (text[cursorPos - 1] != '\n') return

                // Tìm dòng phía trước (dòng mà người dùng vừa Enter từ đó)
                val prevLineEnd = cursorPos - 1
                var prevLineStart = prevLineEnd
                while (prevLineStart > 0 && text[prevLineStart - 1] != '\n') {
                    prevLineStart--
                }
                val prevLine = text.substring(prevLineStart, prevLineEnd)

                // Nếu dòng trước chỉ là prefix trống (không có nội dung) → xóa prefix và tắt list
                val emptyPrefixRegex = Regex("^(\u2022 |\\d+\\. |[A-Z]\\. )$")
                if (emptyPrefixRegex.matches(prevLine)) {
                    isAutoInserting = true
                    s.delete(prevLineStart, cursorPos)
                    activeListMode = "none"
                    isAutoInserting = false
                    return
                }

                // Tính prefix tiếp theo dựa trên dòng trước
                val nextPrefix: String? = when (activeListMode) {
                    "dot" -> {
                        if (prevLine.startsWith("\u2022 ")) "\u2022 " else null
                    }
                    "ordered" -> {
                        val match = Regex("^(\\d+)\\. ").find(prevLine)
                        if (match != null) {
                            val num = match.groupValues[1].toInt()
                            "${num + 1}. "
                        } else null
                    }
                    "alpha" -> {
                        val match = Regex("^([A-Z])\\. ").find(prevLine)
                        if (match != null) {
                            val letter = match.groupValues[1][0]
                            if (letter < 'Z') "${letter + 1}. " else "A. "
                        } else null
                    }
                    else -> null
                }

                if (nextPrefix != null) {
                    isAutoInserting = true
                    s.insert(cursorPos, nextPrefix)
                    isAutoInserting = false
                }
            }
        })
    }

    private fun showAlignMenu() {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_align)

        val btnAlignLeft = bottomSheetDialog.findViewById<android.view.View>(R.id.btnAlignLeft)
        val btnAlignCenter = bottomSheetDialog.findViewById<android.view.View>(R.id.btnAlignCenter)
        val btnAlignRight = bottomSheetDialog.findViewById<android.view.View>(R.id.btnAlignRight)
        val btnListDot = bottomSheetDialog.findViewById<android.view.View>(R.id.btnListDot)
        val btnListOrdered = bottomSheetDialog.findViewById<android.view.View>(R.id.btnListOrdered)
        val btnListAlpha = bottomSheetDialog.findViewById<android.view.View>(R.id.btnListAlpha)

        // Căn lề: dùng gravity thay vì textAlignment để EditText phản hồi đúng
        btnAlignLeft?.setOnClickListener {
            etNoteContent.gravity = Gravity.TOP or Gravity.START
            bottomSheetDialog.dismiss()
        }
        btnAlignCenter?.setOnClickListener {
            etNoteContent.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            bottomSheetDialog.dismiss()
        }
        btnAlignRight?.setOnClickListener {
            etNoteContent.gravity = Gravity.TOP or Gravity.END
            bottomSheetDialog.dismiss()
        }

        // Danh sách: bật chế độ list + format dòng hiện tại
        btnListDot?.setOnClickListener {
            activeListMode = "dot"
            applyListFormat("dot")
            bottomSheetDialog.dismiss()
        }
        btnListOrdered?.setOnClickListener {
            activeListMode = "ordered"
            applyListFormat("ordered")
            bottomSheetDialog.dismiss()
        }
        btnListAlpha?.setOnClickListener {
            activeListMode = "alpha"
            applyListFormat("alpha")
            bottomSheetDialog.dismiss()
        }

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

    /**
     * Format các dòng được chọn (hoặc dòng tại con trỏ) với prefix danh sách.
     * Tự động đếm số dòng list liên tiếp phía trên để tiếp tục đánh số đúng.
     */
    private fun applyListFormat(type: String) {
        val editable = etNoteContent.text
        val text = editable.toString()
        val startSelection = etNoteContent.selectionStart.coerceIn(0, text.length)
        val endSelection = etNoteContent.selectionEnd.coerceIn(0, text.length)

        // Tìm đầu dòng đầu tiên trong vùng chọn
        var regionStart = startSelection
        while (regionStart > 0 && text[regionStart - 1] != '\n') {
            regionStart--
        }

        // Tìm cuối dòng cuối cùng trong vùng chọn
        var regionEnd = endSelection
        while (regionEnd < text.length && text[regionEnd] != '\n') {
            regionEnd++
        }

        val selectedText = text.substring(regionStart, regionEnd)
        val lines = selectedText.split("\n")
        val sb = StringBuilder()

        // Regex để loại bỏ prefix cũ (nếu có)
        val stripRegex = Regex("^(\u2022 |\\d+\\. |[A-Z]\\. )")

        // Đếm số dòng list liên tiếp phía trên vùng chọn để tiếp tục đánh số
        val startIndex = countExistingListItemsAbove(text, regionStart, type)

        for (i in lines.indices) {
            var line = lines[i]
            // Xóa prefix cũ
            line = line.replace(stripRegex, "")

            val prefix = when (type) {
                "dot" -> "\u2022 "
                "ordered" -> "${startIndex + i + 1}. "
                "alpha" -> "${((startIndex + i) % 26 + 65).toChar()}. "
                else -> ""
            }
            sb.append(prefix).append(line)
            if (i < lines.size - 1) {
                sb.append("\n")
            }
        }

        isAutoInserting = true
        editable.replace(regionStart, regionEnd, sb.toString())
        isAutoInserting = false
    }

    /**
     * Đếm số dòng list liên tiếp (cùng loại) ngay phía trên vùng chọn.
     * Dùng để xác định số thứ tự bắt đầu khi format dòng mới.
     */
    private fun countExistingListItemsAbove(text: String, regionStart: Int, type: String): Int {
        if (regionStart <= 0) return 0

        val textAbove = text.substring(0, regionStart)
        val linesAbove = textAbove.split("\n")

        val prefixRegex = when (type) {
            "dot" -> Regex("^\u2022 ")
            "ordered" -> Regex("^\\d+\\. ")
            "alpha" -> Regex("^[A-Z]\\. ")
            else -> return 0
        }

        // Đếm ngược từ dòng cuối cùng phía trên, dừng khi gặp dòng không phải list
        var count = 0
        for (i in linesAbove.indices.reversed()) {
            val line = linesAbove[i]
            if (prefixRegex.containsMatchIn(line)) {
                count++
            } else {
                break
            }
        }
        return count
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
