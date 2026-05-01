package com.example.noteapp_lttbdd

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat

import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.ImageButton
import java.io.File
class AddNoteActivity : AppCompatActivity() {

    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var databaseHelper: DatabaseHelper


    private var currentNoteId: Long = -1L
    private var originalTitle: String = ""
    private var originalContent: String = ""


    private var activeListMode: String = "none"
    private var isAutoInserting = false


    private var activePanel: View? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecordingVoice = false

    companion object {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        databaseHelper = DatabaseHelper(this)


        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteContent = findViewById(R.id.etNoteContent)
        
        val panelAlign = findViewById<View>(R.id.panelAlign)
        val panelTextSetting = findViewById<View>(R.id.panelTextSetting)
        val panelNoteAction = findViewById<View>(R.id.panelNoteAction)

        findViewById<View>(R.id.btnAddInNote).setOnClickListener {
            togglePanel(panelNoteAction)
        }





        findViewById<View>(R.id.btnAlign).setOnClickListener {
            togglePanel(panelAlign)
        }
        findViewById<View>(R.id.btnTextSetting).setOnClickListener {
            togglePanel(panelTextSetting)
        }

        etNoteContent.setOnClickListener {
            hidePanels()
        }
        etNoteContent.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) hidePanels()
        }

        setupListAutoComplete()
        setupAlignPanel()
        setupTextSettingPanel()
        setupNoteActionPanel()
        setupVoiceRecording()


        if (intent.hasExtra("EXTRA_NOTE_ID")) {
            currentNoteId = intent.getLongExtra("EXTRA_NOTE_ID", -1L)
            originalTitle = intent.getStringExtra("EXTRA_NOTE_TITLE").orEmpty()
            originalContent = intent.getStringExtra("EXTRA_NOTE_CONTENT").orEmpty()

            etNoteTitle.setText(originalTitle)
            val spannedText = HtmlCompat.fromHtml(originalContent, HtmlCompat.FROM_HTML_MODE_LEGACY)
            etNoteContent.setText(spannedText)
        }
    }

    override fun onPause() {
        persistNoteIfNeeded()
        super.onPause()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }



    private fun persistNoteIfNeeded() {
        val title = etNoteTitle.text.toString().trim()
        val plainContent = etNoteContent.text.toString().trim()

        if (title.isEmpty() && plainContent.isEmpty()) {
            return
        }

        val contentHtml = HtmlCompat.toHtml(etNoteContent.text, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

        if (currentNoteId == -1L) {
            val insertedId = databaseHelper.insertNote(title, contentHtml)
            if (insertedId > -1L) {
                currentNoteId = insertedId
                originalTitle = title
                originalContent = contentHtml
            }
            return
        }

        if (title == originalTitle && contentHtml == originalContent) {
            return
        }

        val updatedRows = databaseHelper.updateNote(currentNoteId, title, contentHtml)
        if (updatedRows > 0) {
            originalTitle = title
            originalContent = contentHtml
        }
    }

    /**
     * Logic Toggle Panel & Keyboard
     */
    private fun hidePanels() {
        val container = findViewById<View>(R.id.flPanelContainer)
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            activePanel?.visibility = View.GONE
            activePanel = null
        }
    }

    private fun togglePanel(panelView: View) {
        val container = findViewById<View>(R.id.flPanelContainer)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        if (activePanel == panelView && container.visibility == View.VISIBLE) {
            hidePanels()
            etNoteContent.requestFocus()
            imm.showSoftInput(etNoteContent, InputMethodManager.SHOW_IMPLICIT)
        } else {
            imm.hideSoftInputFromWindow(etNoteContent.windowToken, 0)
            
            // Show new panel
            container.visibility = View.VISIBLE
            activePanel?.visibility = View.GONE
            panelView.visibility = View.VISIBLE
            activePanel = panelView

            if (panelView.id == R.id.panelTextSetting) {
                updateUIState()
            }
        }
    }

    /**
     * Khởi tạo các sự kiện cho bảng Căn lề / Danh sách
     */
    private fun setupAlignPanel() {
        val panelAlign = findViewById<View>(R.id.panelAlign)
        
        panelAlign.findViewById<View>(R.id.btnAlignLeft)?.setOnClickListener {
            etNoteContent.gravity = Gravity.TOP or Gravity.START
        }
        panelAlign.findViewById<View>(R.id.btnAlignCenter)?.setOnClickListener {
            etNoteContent.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        panelAlign.findViewById<View>(R.id.btnAlignRight)?.setOnClickListener {
            etNoteContent.gravity = Gravity.TOP or Gravity.END
        }

        panelAlign.findViewById<View>(R.id.btnListDot)?.setOnClickListener {
            activeListMode = "dot"
            applyListFormat("dot")
        }
        panelAlign.findViewById<View>(R.id.btnListOrdered)?.setOnClickListener {
            activeListMode = "ordered"
            applyListFormat("ordered")
        }
        panelAlign.findViewById<View>(R.id.btnListAlpha)?.setOnClickListener {
            activeListMode = "alpha"
            applyListFormat("alpha")
        }
    }



    /**
     * Khởi tạo các sự kiện cho bảng Định dạng
     */
    private fun setupTextSettingPanel() {
        val panelTextSetting = findViewById<View>(R.id.panelTextSetting)
        
        panelTextSetting.findViewById<View>(R.id.btnFormatBold)?.setOnClickListener {
            applyFormatToCurrentLine("bold")
            updateUIState()
        }
        panelTextSetting.findViewById<View>(R.id.btnFormatItalic)?.setOnClickListener {
            applyFormatToCurrentLine("italic")
            updateUIState()
        }
        panelTextSetting.findViewById<View>(R.id.btnFormatUnderline)?.setOnClickListener {
            applyFormatToCurrentLine("underline")
            updateUIState()
        }

        val sizeContainer = panelTextSetting.findViewById<ViewGroup>(R.id.llTextSizeContainer)
        if (sizeContainer != null) {
            for (i in 0 until sizeContainer.childCount) {
                val child = sizeContainer.getChildAt(i)
                if (child is TextView && child.tag != null) {
                    child.setOnClickListener {
                        val sizeStr = child.tag.toString()
                        val size = sizeStr.toIntOrNull() ?: 14
                        applyFormatToCurrentLine("size", size)
                        updateUIState()
                    }
                }
            }
        }
    }

    /**
     * Cập nhật trạng thái đổi màu xám nút Format
     */
    private fun updateUIState() {
        if (activePanel?.id != R.id.panelTextSetting) return

        val editable = etNoteContent.text
        val text = editable.toString()
        val cursor = etNoteContent.selectionStart.coerceIn(0, text.length)
        
        var lineStart = cursor
        while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--
        var lineEnd = cursor
        while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++

        val bolds = editable.getSpans(lineStart, lineEnd, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
        val isBold = bolds.any { editable.getSpanStart(it) <= lineEnd && editable.getSpanEnd(it) >= lineStart }

        val italics = editable.getSpans(lineStart, lineEnd, StyleSpan::class.java).filter { it.style == Typeface.ITALIC }
        val isItalic = italics.any { editable.getSpanStart(it) <= lineEnd && editable.getSpanEnd(it) >= lineStart }

        val underlines = editable.getSpans(lineStart, lineEnd, UnderlineSpan::class.java)
        val isUnderline = underlines.any { editable.getSpanStart(it) <= lineEnd && editable.getSpanEnd(it) >= lineStart }

        val sizes = editable.getSpans(lineStart, lineEnd, AbsoluteSizeSpan::class.java)
        val activeSizeSpans = sizes.filter { editable.getSpanStart(it) <= lineEnd && editable.getSpanEnd(it) >= lineStart }
        val currentSize = if (activeSizeSpans.isNotEmpty()) activeSizeSpans.last().size else 14

        val panel = findViewById<View>(R.id.panelTextSetting)
        panel.findViewById<View>(R.id.btnFormatBold)?.setBackgroundColor(if (isBold) android.graphics.Color.parseColor("#E0E0E0") else android.graphics.Color.TRANSPARENT)
        panel.findViewById<View>(R.id.btnFormatItalic)?.setBackgroundColor(if (isItalic) android.graphics.Color.parseColor("#E0E0E0") else android.graphics.Color.TRANSPARENT)
        panel.findViewById<View>(R.id.btnFormatUnderline)?.setBackgroundColor(if (isUnderline) android.graphics.Color.parseColor("#E0E0E0") else android.graphics.Color.TRANSPARENT)

        val sizeContainer = panel.findViewById<ViewGroup>(R.id.llTextSizeContainer)
        if (sizeContainer != null) {
            for (i in 0 until sizeContainer.childCount) {
                val child = sizeContainer.getChildAt(i)
                if (child is TextView && child.tag != null) {
                    val sizeStr = child.tag?.toString()
                    val size = sizeStr?.toIntOrNull() ?: 14
                    if (size == currentSize) {
                        child.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                    } else {
                        child.setBackgroundColor(android.graphics.Color.parseColor("#F0F0F0"))
                    }
                }
            }
        }
    }

    private fun applyFormatToCurrentLine(formatType: String, sizeValue: Int = -1) {
        val editable = etNoteContent.text
        val text = editable.toString()
        val cursor = etNoteContent.selectionStart.coerceIn(0, text.length)

        var lineStart = cursor
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        var lineEnd = cursor
        while (lineEnd < text.length && text[lineEnd] != '\n') {
            lineEnd++
        }

        val flag = Spannable.SPAN_INCLUSIVE_INCLUSIVE

        if (formatType == "size") {
            val existingSpans = editable.getSpans(lineStart, lineEnd, AbsoluteSizeSpan::class.java)
            for (span in existingSpans) {
                val s = editable.getSpanStart(span)
                val e = editable.getSpanEnd(span)
                val oldSize = span.size

                if (s <= lineEnd && e >= lineStart) {
                    editable.removeSpan(span)
                    if (s < lineStart) {
                        editable.setSpan(AbsoluteSizeSpan(oldSize, true), s, lineStart, flag)
                    }
                    if (e > lineEnd) {
                        editable.setSpan(AbsoluteSizeSpan(oldSize, true), lineEnd, e, flag)
                    }
                }
            }
            // Always set size span even if it replaces an old one (no toggle)
            editable.setSpan(AbsoluteSizeSpan(sizeValue, true), lineStart, lineEnd, flag)
        } else {
            toggleStyleSpan(editable, formatType, lineStart, lineEnd)
        }
    }

    private fun toggleStyleSpan(editable: Editable, formatType: String, lineStart: Int, lineEnd: Int) {
        val flag = Spannable.SPAN_INCLUSIVE_INCLUSIVE
        var hasSpan = false

        when (formatType) {
            "bold" -> {
                val spans = editable.getSpans(lineStart, lineEnd, StyleSpan::class.java)
                    .filter { it.style == Typeface.BOLD }
                for (span in spans) {
                    val s = editable.getSpanStart(span)
                    val e = editable.getSpanEnd(span)
                    if (s <= lineEnd && e >= lineStart) {
                        hasSpan = true
                        editable.removeSpan(span)
                        if (s < lineStart) editable.setSpan(StyleSpan(Typeface.BOLD), s, lineStart, flag)
                        if (e > lineEnd) editable.setSpan(StyleSpan(Typeface.BOLD), lineEnd, e, flag)
                    }
                }
                if (!hasSpan) editable.setSpan(StyleSpan(Typeface.BOLD), lineStart, lineEnd, flag)
            }
            "italic" -> {
                val spans = editable.getSpans(lineStart, lineEnd, StyleSpan::class.java)
                    .filter { it.style == Typeface.ITALIC }
                for (span in spans) {
                    val s = editable.getSpanStart(span)
                    val e = editable.getSpanEnd(span)
                    if (s <= lineEnd && e >= lineStart) {
                        hasSpan = true
                        editable.removeSpan(span)
                        if (s < lineStart) editable.setSpan(StyleSpan(Typeface.ITALIC), s, lineStart, flag)
                        if (e > lineEnd) editable.setSpan(StyleSpan(Typeface.ITALIC), lineEnd, e, flag)
                    }
                }
                if (!hasSpan) editable.setSpan(StyleSpan(Typeface.ITALIC), lineStart, lineEnd, flag)
            }
            "underline" -> {
                val spans = editable.getSpans(lineStart, lineEnd, UnderlineSpan::class.java)
                for (span in spans) {
                    val s = editable.getSpanStart(span)
                    val e = editable.getSpanEnd(span)
                    if (s <= lineEnd && e >= lineStart) {
                        hasSpan = true
                        editable.removeSpan(span)
                        if (s < lineStart) editable.setSpan(UnderlineSpan(), s, lineStart, flag)
                        if (e > lineEnd) editable.setSpan(UnderlineSpan(), lineEnd, e, flag)
                    }
                }
                if (!hasSpan) editable.setSpan(UnderlineSpan(), lineStart, lineEnd, flag)
            }
        }
    }

    private fun setupListAutoComplete() {
        // (Bảo toàn nguyên vẹn hàm này)
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

                if (text.length <= beforeLength) return

                if (text[cursorPos - 1] != '\n') return

                val prevLineEnd = cursorPos - 1
                var prevLineStart = prevLineEnd
                while (prevLineStart > 0 && text[prevLineStart - 1] != '\n') {
                    prevLineStart--
                }
                val prevLine = text.substring(prevLineStart, prevLineEnd)

                val emptyPrefixRegex = Regex("^(\u2022 |\\d+\\. |[A-Z]\\. )$")
                if (emptyPrefixRegex.matches(prevLine)) {
                    isAutoInserting = true
                    s.delete(prevLineStart, cursorPos)
                    activeListMode = "none"
                    isAutoInserting = false
                    return
                }

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

    private fun applyListFormat(type: String) {
        val editable = etNoteContent.text
        val text = editable.toString()
        val startSelection = etNoteContent.selectionStart.coerceIn(0, text.length)
        val endSelection = etNoteContent.selectionEnd.coerceIn(0, text.length)

        var regionStart = startSelection
        while (regionStart > 0 && text[regionStart - 1] != '\n') {
            regionStart--
        }

        var regionEnd = endSelection
        while (regionEnd < text.length && text[regionEnd] != '\n') {
            regionEnd++
        }

        val selectedText = text.substring(regionStart, regionEnd)
        val lines = selectedText.split("\n")
        val sb = StringBuilder()

        val stripRegex = Regex("^(\u2022 |\\d+\\. |[A-Z]\\. )")

        val startIndex = countExistingListItemsAbove(text, regionStart, type)

        for (i in lines.indices) {
            var line = lines[i]
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

    private fun setupNoteActionPanel() {
        val panelNoteAction = findViewById<View>(R.id.panelNoteAction)
        val llRecordBar = findViewById<View>(R.id.llRecordBar)

        panelNoteAction.findViewById<View>(R.id.btnActionVoice)?.setOnClickListener {
            llRecordBar.visibility = View.VISIBLE
            hidePanels()
        }
    }

    private fun setupVoiceRecording() {
        val llRecordBar = findViewById<View>(R.id.llRecordBar)
        val btnStartRecord = findViewById<ImageButton>(R.id.btnStartRecord)
        val btnStopRecord = findViewById<ImageButton>(R.id.btnStopRecord)
        val btnCloseRecord = findViewById<ImageButton>(R.id.btnCloseRecord)
        val tvRecordStatus = findViewById<TextView>(R.id.tvRecordStatus)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvRecordStatus.text = "Đang nghe..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                tvRecordStatus.text = "Đang xử lý..."
            }
            override fun onError(error: Int) {
                tvRecordStatus.text = "Lỗi ghi âm ($error). Nhấn Play để thử lại."
                isRecordingVoice = false
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val cursorPosition = etNoteContent.selectionStart.coerceAtLeast(0)
                    etNoteContent.text.insert(cursorPosition, text + " ")
                    tvRecordStatus.text = "Nhấn Play để ghi âm tiếp..."
                }
                isRecordingVoice = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        btnStartRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
                return@setOnClickListener
            }
            if (!isRecordingVoice) {
                speechRecognizer?.startListening(speechRecognizerIntent)
                isRecordingVoice = true
                tvRecordStatus.text = "Đang chuẩn bị..."
            }
        }

        btnStopRecord.setOnClickListener {
            if (isRecordingVoice) {
                speechRecognizer?.stopListening()
                isRecordingVoice = false
                tvRecordStatus.text = "Đã dừng. Nhấn Play để ghi tiếp."
            }
        }

        btnCloseRecord.setOnClickListener {
            if (isRecordingVoice) {
                speechRecognizer?.stopListening()
                isRecordingVoice = false
            }
            llRecordBar.visibility = View.GONE
        }
    }
}
