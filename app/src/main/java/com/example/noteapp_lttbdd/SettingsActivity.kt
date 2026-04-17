package com.example.noteapp_lttbdd

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pref = getSharedPreferences("settings", MODE_PRIVATE)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        switchDarkMode = findViewById(R.id.switchDarkMode)

        // 🔙 Back
        btnBack.setOnClickListener { finish() }

        // =========================
        // 🌗 DARK MODE
        // =========================

        val isDarkMode = pref.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkMode

        // Tapping the whole item also toggles the switch
        findViewById<View>(R.id.itemDarkMode).setOnClickListener {
            switchDarkMode.isChecked = !switchDarkMode.isChecked
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Lưu trạng thái
            pref.edit().putBoolean("dark_mode", isChecked).apply()

            // Áp dụng theme
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Setup other items with placeholders/toasts
        findViewById<View>(R.id.itemTheme).setOnClickListener {
            Toast.makeText(this, "Chủ đề - Sắp ra mắt", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.itemAutoSave).setOnClickListener {
            Toast.makeText(this, "Tự động lưu - Đã bật", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.itemAppLock).setOnClickListener {
            Toast.makeText(this, "Khóa ứng dụng - Sắp ra mắt", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.itemNoteLock).setOnClickListener {
            Toast.makeText(this, "Khóa ghi chú - Sắp ra mắt", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.itemLanguage).setOnClickListener {
            Toast.makeText(this, "Ngôn ngữ - Tiếng Việt", Toast.LENGTH_SHORT).show()
        }
    }
}