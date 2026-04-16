package com.example.noteapp_lttbdd

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: Switch
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        pref = getSharedPreferences("settings", MODE_PRIVATE)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val spinnerLanguage = findViewById<Spinner>(R.id.spinnerLanguage)
        switchDarkMode = findViewById(R.id.switchDarkMode)

        // 🔙 Back
        btnBack.setOnClickListener { finish() }

        // =========================
        // 🌗 DARK MODE
        // =========================

        val isDarkMode = pref.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkMode

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->

            // Lưu trạng thái
            pref.edit().putBoolean("dark_mode", isChecked).apply()

            // Áp dụng theme (KHÔNG cần recreate)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // =========================
        // 🌍 LANGUAGE (UI DEMO)
        // =========================

        val languages = arrayOf("Tiếng Việt", "English", "日本語")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerLanguage.adapter = adapter

        val savedLang = pref.getInt("LANG_POS", 0)
        spinnerLanguage.setSelection(savedLang)

        var isFirstSelection = true

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (isFirstSelection) {
                    isFirstSelection = false
                    return
                }

                pref.edit().putInt("LANG_POS", position).apply()

                Toast.makeText(
                    this@SettingsActivity,
                    "Đã chọn: ${languages[position]}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}