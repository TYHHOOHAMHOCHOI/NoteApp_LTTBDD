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
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.core.text.HtmlCompat
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import org.json.JSONObject
import java.io.File
import java.io.IOException

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
    private var noteToExport: Note? = null

    private val createTxtLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { saveNoteToTxt(it) }
    }

    private val createPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { saveNoteToPdf(it) }
    }

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

    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = LocaleHelper.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    private fun openNote(note: Note) {
        val intent = Intent(this, AddNoteActivity::class.java)
        intent.putExtra("EXTRA_NOTE_ID", note.id)
        intent.putExtra("EXTRA_NOTE_TITLE", note.title)
        intent.putExtra("EXTRA_NOTE_CONTENT", note.content)
        intent.putExtra("EXTRA_NOTE_TAG", note.tag)         // Truyền tag sang màn hình chỉnh sửa

        intent.putExtra("EXTRA_REMINDER_TIME", note.reminderTime)
        intent.putExtra("EXTRA_IS_REMINDER_ENABLED", note.isReminderEnabled)
        intent.putExtra("EXTRA_REPEAT_TYPE", note.repeatType)

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
        val optionsList = mutableListOf("Xóa", "Xuất file")

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
                        databaseHelper.softDeleteNote(note.id)
                        loadNotes()
                        Toast.makeText(this, "Đã chuyển vào thùng rác", Toast.LENGTH_SHORT).show()
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
                    "Xuất file" -> {
                        showExportDialog(note)
                    }
                }
            }
            .show()
    }

    private fun showExportDialog(note: Note) {
        val options = arrayOf("File PDF (.pdf)", "File văn bản (.txt)")
        AlertDialog.Builder(this)
            .setTitle("Chọn định dạng xuất file")
            .setItems(options) { _, which ->
                noteToExport = note
                val fileName = note.title.ifBlank { "Ghi chú_${note.id}" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                when (which) {
                    0 -> createPdfLauncher.launch("$fileName.pdf")
                    1 -> createTxtLauncher.launch("$fileName.txt")
                }
            }
            .show()
    }

    private fun saveNoteToTxt(uri: Uri) {
        val note = noteToExport ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val plainContent = HtmlCompat.fromHtml(note.content, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                val textToSave = "Tiêu đề: ${note.title}\n\n$plainContent"
                outputStream.write(textToSave.toByteArray())
                Toast.makeText(this, "Đã xuất file TXT thành công", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Lỗi khi xuất file TXT", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveNoteToPdf(uri: Uri) {
        val note = noteToExport ?: return
        val pdfDocument = PdfDocument()
        
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50f
        val contentWidth = (pageWidth - 2 * margin).toInt()

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = margin
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isFakeBoldText = true
        }
        canvas.drawText("Tiêu đề: ${note.title}", margin, y + 20f, titlePaint)
        y += 60f

        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 14f
        }

        val imageGetter = Html.ImageGetter { source ->
            try {
                if (source != null) {
                    if (source.contains("table_")) {
                        return@ImageGetter createTableDrawableForPdf(source, contentWidth)
                    }
                    val path = source.substringBefore("?mode=")
                    val isSmallMode = source.endsWith("?mode=small")
                    val d = Drawable.createFromPath(path)
                    if (d != null) {
                        var width = contentWidth
                        if (isSmallMode) {
                            width = if (d.intrinsicHeight > d.intrinsicWidth) contentWidth / 4 else (contentWidth * 0.5).toInt()
                        }
                        val height = (d.intrinsicHeight * (width.toFloat() / d.intrinsicWidth)).toInt()
                        d.setBounds(0, 0, width, height)
                        return@ImageGetter d
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            null
        }

        val spannedContent = HtmlCompat.fromHtml(note.content, HtmlCompat.FROM_HTML_MODE_LEGACY, imageGetter, null)
        
        val staticLayout = StaticLayout.Builder.obtain(spannedContent, 0, spannedContent.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(margin, y)
        staticLayout.draw(canvas)
        canvas.restore()

        pdfDocument.finishPage(page)

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                Toast.makeText(this, "Đã xuất file PDF thành công", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Lỗi khi xuất file PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun createTableDrawableForPdf(source: String, tableWidth: Int): Drawable? {
        try {
            val path = source.substringBefore("?mode=")
            val file = File(path)
            if (!file.exists()) return null
            val jsonObject = JSONObject(file.readText())
            val rows = jsonObject.getInt("rows")
            val cols = jsonObject.getInt("cols")
            val dataArray = jsonObject.getJSONArray("data")
            
            val paint = Paint().apply { color = Color.BLACK; strokeWidth = 1f; style = Paint.Style.STROKE }
            val textPaint = Paint().apply { color = Color.BLACK; textSize = 12f; isAntiAlias = true }
            
            val cellHeight = 30
            val height = rows * cellHeight
            val cellWidth = tableWidth / cols
            
            val bitmap = Bitmap.createBitmap(tableWidth, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            
            for (r in 0 until rows) {
                val rowArray = dataArray.getJSONArray(r)
                for (c in 0 until cols) {
                    val text = rowArray.getString(c)
                    val left = c * cellWidth.toFloat()
                    val top = r * cellHeight.toFloat()
                    val right = left + cellWidth
                    val bottom = top + cellHeight
                    canvas.drawRect(left, top, right, bottom, paint)
                    val textY = top + (cellHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
                    canvas.drawText(text, left + 5f, textY, textPaint)
                }
            }
            val d = BitmapDrawable(resources, bitmap)
            d.setBounds(0, 0, tableWidth, height)
            return d
        } catch (e: Exception) { return null }
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
                R.id.navigation_calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
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