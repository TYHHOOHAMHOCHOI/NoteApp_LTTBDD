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
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog

import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.ImageButton
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.style.ImageSpan
import androidx.activity.result.contract.ActivityResultContracts
import java.io.FileOutputStream
import java.io.File
import org.json.JSONObject
import org.json.JSONArray
import android.text.InputType
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.LinearLayout
import android.graphics.drawable.BitmapDrawable
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Canvas
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.os.Build

class AddNoteActivity : AppCompatActivity() {

    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var tvReminderInfo: TextView

    private var selectedReminderTime: Long = 0L
    private var selectedRepeatType: String = "once"
    private var isReminderEnabled: Boolean = false


    private var currentNoteId: Long = -1L
    private var currentEditingImageSpan: ImageSpan? = null
    private var originalTitle: String = ""
    private var originalContent: String = ""


    private var activeListMode: String = "none"
    private var isAutoInserting = false


    private var activePanel: View? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecordingVoice = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            insertImageToNote(uri)
        }
    }

    companion object {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        databaseHelper = DatabaseHelper(this)
        requestNotificationPermissionIfNeeded()

        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteContent = findViewById(R.id.etNoteContent)
        tvReminderInfo = findViewById(R.id.tvReminderInfo)
        
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
        setupImageClickListeners()
        setupDrawingPanel()


        if (intent.hasExtra("EXTRA_NOTE_ID")) {
            currentNoteId = intent.getLongExtra("EXTRA_NOTE_ID", -1L)
            originalTitle = intent.getStringExtra("EXTRA_NOTE_TITLE").orEmpty()
            originalContent = intent.getStringExtra("EXTRA_NOTE_CONTENT").orEmpty()

            etNoteTitle.setText(originalTitle)

            val imageGetter = Html.ImageGetter { source ->
                try {
                    if (source != null) {
                        if (source.contains("table_")) {
                            return@ImageGetter createTableDrawable(source, resources.displayMetrics.widthPixels)
                        }
                        val path = source.substringBefore("?mode=")
                        val isSmallMode = source.endsWith("?mode=small")
                        val d = Drawable.createFromPath(path)
                        if (d != null) {
                            val displayMetrics = resources.displayMetrics
                            val screenWidth = displayMetrics.widthPixels
                            var width = screenWidth
                            if (isSmallMode) {
                                if (d.intrinsicHeight > d.intrinsicWidth) {
                                    width = screenWidth / 5
                                } else {
                                    width = (screenWidth * 0.5).toInt()
                                }
                            }
                            val height = (d.intrinsicHeight * (width.toFloat() / d.intrinsicWidth)).toInt()
                            d.setBounds(0, 0, width, height)
                            return@ImageGetter d
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                null
            }

            val spannedText = HtmlCompat.fromHtml(originalContent, HtmlCompat.FROM_HTML_MODE_LEGACY, imageGetter, null)
            etNoteContent.setText(spannedText)
            selectedReminderTime = intent.getLongExtra("EXTRA_REMINDER_TIME", 0L)
            isReminderEnabled = intent.getBooleanExtra("EXTRA_IS_REMINDER_ENABLED", false)
            selectedRepeatType = intent.getStringExtra("EXTRA_REPEAT_TYPE") ?: "once"
        }
        updateReminderUi()
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
                val length = s?.length ?: 0
                this@AddNoteActivity.findViewById<TextView>(R.id.tvCharCount)?.text = "$length ký tự"

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

        panelNoteAction.findViewById<View>(R.id.btnActionImage)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
            hidePanels()
        }

        panelNoteAction.findViewById<View>(R.id.btnActionPen)?.setOnClickListener {
            val drawingView = findViewById<DrawingView>(R.id.drawingView)
            drawingView.setBlankCanvas()
            currentEditingImageSpan = null
            findViewById<View>(R.id.rlDrawingOverlay).visibility = View.VISIBLE
            hidePanels()
        }

        panelNoteAction.findViewById<View>(R.id.btnActionTable)?.setOnClickListener {
            insertNewTable(3, 4)
            hidePanels()
        }

        panelNoteAction.findViewById<View>(R.id.btnActionReminder)?.setOnClickListener {
            hidePanels()

            if (isReminderEnabled && selectedReminderTime > 0L) {
                showReminderOptionsDialog()
            } else {
                showDatePicker()
            }
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

    private fun insertImageToNote(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val directory = getDir("note_images", Context.MODE_PRIVATE)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val fileName = "img_${System.currentTimeMillis()}.png"
                val file = File(directory, fileName)
                val outStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
                outStream.close()

                val absolutePath = file.absolutePath
                val source = "$absolutePath?mode=large"
                val d = Drawable.createFromPath(absolutePath)
                
                if (d != null) {
                    val displayMetrics = resources.displayMetrics
                    val width = displayMetrics.widthPixels
                    val height = (d.intrinsicHeight * (width.toFloat() / d.intrinsicWidth)).toInt()
                    d.setBounds(0, 0, width, height)

                    val imageSpan = ImageSpan(d, source)
                    val editable = etNoteContent.text
                    val cursorPosition = etNoteContent.selectionStart.coerceAtLeast(0)

                    val imageToken = "\n\uFFFC\n"
                    editable.insert(cursorPosition, imageToken)
                    
                    val start = cursorPosition + 1
                    val end = start + 1
                    editable.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    
                    etNoteContent.setSelection(end + 1)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể tải ảnh", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun setupImageClickListeners() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                val x = e.x.toInt()
                val y = e.y.toInt()

                var xOffset = x - etNoteContent.totalPaddingLeft
                var yOffset = y - etNoteContent.totalPaddingTop

                xOffset += etNoteContent.scrollX
                yOffset += etNoteContent.scrollY

                val layout = etNoteContent.layout ?: return
                val line = layout.getLineForVertical(yOffset)
                val off = layout.getOffsetForHorizontal(line, xOffset.toFloat())

                val spans = etNoteContent.text.getSpans(off, off, ImageSpan::class.java)
                if (spans.isNotEmpty()) {
                    showImageModePopup(spans[0])
                }
            }
        })

        etNoteContent.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun showImageModePopup(imageSpan: ImageSpan) {
        val source = imageSpan.source ?: return
        if (source.contains("table_")) {
            val options = arrayOf("Chỉnh sửa nội dung bảng", "Thay đổi kích thước (Hàng/Cột)")
            AlertDialog.Builder(this)
                .setItems(options) { _, which ->
                    if (which == 0) {
                        showTableEditorDialog(imageSpan)
                    } else {
                        showTableResizeDialog(imageSpan)
                    }
                }
                .show()
            return
        }

        val isSmallMode = source.endsWith("?mode=small")
        
        val options = if (isSmallMode) {
            arrayOf("Chế độ ảnh lớn", "Vẽ lên ảnh")
        } else {
            arrayOf("Chế độ ảnh nhỏ", "Vẽ lên ảnh")
        }

        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                if (which == 0) {
                    val newSource = if (isSmallMode) {
                        source.replace("?mode=small", "?mode=large")
                    } else {
                        if (source.contains("?mode=")) source.replace("?mode=large", "?mode=small")
                        else "$source?mode=small"
                    }
                    updateImageSpan(imageSpan, newSource)
                } else if (which == 1) {
                    val path = source.substringBefore("?mode=")
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        currentEditingImageSpan = imageSpan
                        val drawingView = findViewById<DrawingView>(R.id.drawingView)
                        drawingView.loadBitmap(bitmap)
                        findViewById<View>(R.id.rlDrawingOverlay).visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this, "Không thể tải ảnh để vẽ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun updateImageSpan(oldSpan: ImageSpan, newSource: String) {
        val editable = etNoteContent.text
        val start = editable.getSpanStart(oldSpan)
        val end = editable.getSpanEnd(oldSpan)
        if (start == -1 || end == -1) return
        
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        val d: Drawable? = if (newSource.contains("table_")) {
            createTableDrawable(newSource, screenWidth)
        } else {
            val path = newSource.substringBefore("?mode=")
            Drawable.createFromPath(path)
        }
        
        if (d == null) return
        
        val isSmallMode = newSource.endsWith("?mode=small")
        var width = screenWidth
        if (isSmallMode && !newSource.contains("table_")) {
            if (d.intrinsicHeight > d.intrinsicWidth) {
                width = screenWidth / 5
            } else {
                width = (screenWidth * 0.5).toInt()
            }
        } else if (newSource.contains("table_")) {
            width = d.bounds.width()
        }
        
        val height = if (newSource.contains("table_")) {
            d.bounds.height()
        } else {
            (d.intrinsicHeight * (width.toFloat() / d.intrinsicWidth)).toInt()
        }
        
        if (!newSource.contains("table_")) {
            d.setBounds(0, 0, width, height)
        }
        
        val newSpan = ImageSpan(d, newSource)
        editable.removeSpan(oldSpan)
        editable.replace(start, end, "\uFFFC")
        editable.setSpan(newSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun setupDrawingPanel() {
        val rlDrawingOverlay = findViewById<View>(R.id.rlDrawingOverlay)
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        
        findViewById<Button>(R.id.btnDrawingClose).setOnClickListener {
            currentEditingImageSpan = null
            drawingView.clear()
            rlDrawingOverlay.visibility = View.GONE
        }
        
        findViewById<Button>(R.id.btnDrawingClear).setOnClickListener {
            drawingView.clear()
        }
        
        findViewById<Button>(R.id.btnDrawingDone).setOnClickListener {
            val bitmap = drawingView.getBitmap()
            if (bitmap != null) {
                if (currentEditingImageSpan != null) {
                    val source = currentEditingImageSpan!!.source
                    val path = source?.substringBefore("?mode=")
                    if (path != null) {
                        try {
                            val file = File(path)
                            val outStream = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                            outStream.flush()
                            outStream.close()
                            
                            updateImageSpan(currentEditingImageSpan!!, source)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show()
                        }
                    }
                    currentEditingImageSpan = null
                } else {
                    insertBitmapToNote(bitmap)
                }
            }
            drawingView.clear()
            rlDrawingOverlay.visibility = View.GONE
        }
    }

    private fun insertBitmapToNote(bitmap: Bitmap) {
        try {
            val directory = getDir("note_images", Context.MODE_PRIVATE)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "draw_${System.currentTimeMillis()}.png"
            val file = File(directory, fileName)
            val outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()

            val absolutePath = file.absolutePath
            val source = "$absolutePath?mode=large"
            val d = Drawable.createFromPath(absolutePath)
            
            if (d != null) {
                val displayMetrics = resources.displayMetrics
                val width = displayMetrics.widthPixels
                val height = (d.intrinsicHeight * (width.toFloat() / d.intrinsicWidth)).toInt()
                d.setBounds(0, 0, width, height)

                val imageSpan = ImageSpan(d, source)
                val editable = etNoteContent.text
                val cursorPosition = etNoteContent.selectionStart.coerceAtLeast(0)

                val imageToken = "\n\uFFFC\n"
                editable.insert(cursorPosition, imageToken)
                
                val start = cursorPosition + 1
                val end = start + 1
                editable.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                
                etNoteContent.setSelection(end + 1)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể lưu hình vẽ", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun createTableDrawable(source: String, screenWidth: Int): Drawable? {
        val path = source.substringBefore("?mode=")
        val file = File(path)
        if (!file.exists()) return null
        val json = file.readText()
        val jsonObject = JSONObject(json)
        val rows = jsonObject.getInt("rows")
        val cols = jsonObject.getInt("cols")
        val dataArray = jsonObject.getJSONArray("data")
        
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 30f
            isAntiAlias = true
        }
        
        val cellHeight = 80
        val tableWidth = (screenWidth * 0.9).toInt()
        val height = rows * cellHeight
        val cellWidth = tableWidth / cols
        
        val bitmap = Bitmap.createBitmap(screenWidth, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        val startX = (screenWidth - tableWidth) / 2f
        
        for (r in 0 until rows) {
            val rowArray = dataArray.getJSONArray(r)
            for (c in 0 until cols) {
                val text = rowArray.getString(c)
                val left = startX + c * cellWidth.toFloat()
                val top = r * cellHeight.toFloat()
                val right = left + cellWidth
                val bottom = top + cellHeight
                
                canvas.drawRect(left, top, right, bottom, paint)
                
                val textY = top + (cellHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
                val textX = left + 10f
                canvas.drawText(text, textX, textY, textPaint)
            }
        }
        
        val d = BitmapDrawable(resources, bitmap)
        d.setBounds(0, 0, screenWidth, height)
        return d
    }

    private fun showTableEditorDialog(imageSpan: ImageSpan) {
        val source = imageSpan.source ?: return
        val path = source.substringBefore("?mode=")
        val file = File(path)
        if (!file.exists()) return
        val jsonObject = JSONObject(file.readText())
        val rows = jsonObject.getInt("rows")
        val cols = jsonObject.getInt("cols")
        val dataArray = jsonObject.getJSONArray("data")
        
        val scrollView = ScrollView(this)
        val horizontalScrollView = HorizontalScrollView(this)
        val tableLayout = TableLayout(this)
        
        val editTexts = Array(rows) { Array(cols) { EditText(this) } }
        
        for (r in 0 until rows) {
            val tableRow = TableRow(this)
            val rowData = dataArray.getJSONArray(r)
            for (c in 0 until cols) {
                val et = EditText(this).apply {
                    setText(rowData.getString(c))
                    minEms = 4
                    setBackgroundResource(android.R.drawable.edit_text)
                }
                editTexts[r][c] = et
                tableRow.addView(et)
            }
            tableLayout.addView(tableRow)
        }
        
        horizontalScrollView.addView(tableLayout)
        scrollView.addView(horizontalScrollView)
        
        AlertDialog.Builder(this)
            .setTitle("Chỉnh sửa bảng")
            .setView(scrollView)
            .setPositiveButton("Lưu") { _, _ ->
                val newDataArray = JSONArray()
                for (r in 0 until rows) {
                    val rowArray = JSONArray()
                    for (c in 0 until cols) {
                        rowArray.put(editTexts[r][c].text.toString())
                    }
                    newDataArray.put(rowArray)
                }
                jsonObject.put("data", newDataArray)
                file.writeText(jsonObject.toString())
                
                updateImageSpan(imageSpan, source)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showTableResizeDialog(imageSpan: ImageSpan) {
        val source = imageSpan.source ?: return
        val path = source.substringBefore("?mode=")
        val file = File(path)
        if (!file.exists()) return
        val jsonObject = JSONObject(file.readText())
        val oldRows = jsonObject.getInt("rows")
        val oldCols = jsonObject.getInt("cols")
        val dataArray = jsonObject.getJSONArray("data")
        
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)
        
        val etRows = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Số hàng (hiện tại $oldRows)"
        }
        layout.addView(etRows)
        
        val etCols = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Số cột (hiện tại $oldCols)"
        }
        layout.addView(etCols)
        
        AlertDialog.Builder(this)
            .setTitle("Thay đổi kích thước bảng")
            .setView(layout)
            .setPositiveButton("Lưu") { _, _ ->
                val newRowsStr = etRows.text.toString()
                val newColsStr = etCols.text.toString()
                if (newRowsStr.isNotEmpty() && newColsStr.isNotEmpty()) {
                    val newRows = newRowsStr.toInt()
                    val newCols = newColsStr.toInt()
                    
                    val newDataArray = JSONArray()
                    for (r in 0 until newRows) {
                        val rowArray = JSONArray()
                        for (c in 0 until newCols) {
                            if (r < oldRows && c < oldCols) {
                                rowArray.put(dataArray.getJSONArray(r).getString(c))
                            } else {
                                rowArray.put("")
                            }
                        }
                        newDataArray.put(rowArray)
                    }
                    
                    jsonObject.put("rows", newRows)
                    jsonObject.put("cols", newCols)
                    jsonObject.put("data", newDataArray)
                    file.writeText(jsonObject.toString())
                    
                    updateImageSpan(imageSpan, source)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun insertNewTable(rows: Int, cols: Int) {
        val directory = getDir("note_tables", Context.MODE_PRIVATE)
        if (!directory.exists()) directory.mkdirs()
        
        val file = File(directory, "table_${System.currentTimeMillis()}.json")
        val jsonObject = JSONObject()
        jsonObject.put("rows", rows)
        jsonObject.put("cols", cols)
        
        val dataArray = JSONArray()
        for (r in 0 until rows) {
            val rowArray = JSONArray()
            for (c in 0 until cols) {
                rowArray.put("")
            }
            dataArray.put(rowArray)
        }
        jsonObject.put("data", dataArray)
        file.writeText(jsonObject.toString())
        
        val source = file.absolutePath + "?mode=large"
        val d = createTableDrawable(source, resources.displayMetrics.widthPixels)
        if (d != null) {
            val imageSpan = ImageSpan(d, source)
            val editable = etNoteContent.text
            val cursorPosition = etNoteContent.selectionStart.coerceAtLeast(0)

            val imageToken = "\n\uFFFC\n"
            editable.insert(cursorPosition, imageToken)
            
            val start = cursorPosition + 1
            val end = start + 1
            editable.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            etNoteContent.setSelection(end + 1)
        }
    }

    private fun updateReminderUi() {
        if (isReminderEnabled && selectedReminderTime > 0L) {
            tvReminderInfo.visibility = View.VISIBLE
            tvReminderInfo.text = "⏰ Nhắc lúc: ${formatReminderTime(selectedReminderTime)}"
        } else {
            tvReminderInfo.visibility = View.GONE
        }

        val panelNoteAction = findViewById<View>(R.id.panelNoteAction)
        val btnActionReminder = panelNoteAction.findViewById<ImageButton>(R.id.btnActionReminder)
        val tvActionReminder = panelNoteAction.findViewById<TextView>(R.id.tvActionReminder)

        if (isReminderEnabled && selectedReminderTime > 0L) {
            btnActionReminder.setImageResource(R.drawable.outline_alarm_24)
            tvActionReminder.text = "Sửa nhắc"
        } else {
            btnActionReminder.setImageResource(R.drawable.baseline_alarm_add_24)
            tvActionReminder.text = "Nhắc hẹn"
        }
    }

    private fun showReminderOptionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sửa nhắc hẹn")
            .setMessage("Đang nhắc lúc: ${formatReminderTime(selectedReminderTime)}")
            .setPositiveButton("Sửa thời gian") { _, _ ->
                showDatePicker()
            }
            .setNeutralButton("Hủy nhắc hẹn") { _, _ ->
                clearReminderForCurrentNote()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        if (selectedReminderTime > 0L) {
            calendar.timeInMillis = selectedReminderTime
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                showTimePicker(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(calendar: Calendar) {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val reminderTime = calendar.timeInMillis

                if (reminderTime <= System.currentTimeMillis()) {
                    Toast.makeText(
                        this,
                        "Vui lòng chọn thời gian trong tương lai",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@TimePickerDialog
                }

                selectedReminderTime = reminderTime
                isReminderEnabled = true
                selectedRepeatType = "once"

                saveReminderForCurrentNote()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun saveReminderForCurrentNote() {
        persistNoteIfNeeded()

        if (currentNoteId == -1L) {
            Toast.makeText(
                this,
                "Vui lòng nhập tiêu đề hoặc nội dung trước khi đặt nhắc hẹn",
                Toast.LENGTH_SHORT
            ).show()

            selectedReminderTime = 0L
            isReminderEnabled = false
            selectedRepeatType = "once"
            updateReminderUi()
            return
        }

        val updatedRows = databaseHelper.updateReminder(
            currentNoteId,
            selectedReminderTime,
            selectedRepeatType
        )

        if (updatedRows > 0) {
            ReminderScheduler.scheduleReminder(
                context = this,
                noteId = currentNoteId,
                reminderTime = selectedReminderTime
            )

            updateReminderUi()
            Toast.makeText(this, "Đã đặt nhắc hẹn", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Không thể đặt nhắc hẹn", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearReminderForCurrentNote() {
        if (currentNoteId == -1L) {
            selectedReminderTime = 0L
            isReminderEnabled = false
            selectedRepeatType = "once"
            updateReminderUi()
            return
        }

        val updatedRows = databaseHelper.clearReminder(currentNoteId)

        if (updatedRows > 0) {
            ReminderScheduler.cancelReminder(
                context = this,
                noteId = currentNoteId
            )

            selectedReminderTime = 0L
            isReminderEnabled = false
            selectedRepeatType = "once"

            updateReminderUi()
            Toast.makeText(this, "Đã hủy nhắc hẹn", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Không thể hủy nhắc hẹn", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatReminderTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(timeMillis)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2001
                )
            }
        }
    }
}
