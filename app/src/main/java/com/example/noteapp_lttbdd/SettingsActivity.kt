package com.example.noteapp_lttbdd

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val cbDarkMode = findViewById<CheckBox>(R.id.cbDarkMode)
        val spinnerLanguage = findViewById<Spinner>(R.id.spinnerLanguage)

        // 🔙 Back
        btnBack.setOnClickListener { finish() }

        // 💾 SharedPreferences
        val pref = getSharedPreferences("Settings", MODE_PRIVATE)

        // =========================
        // 🌗 DARK MODE (FIX CHUẨN)
        // =========================
        val isDark = pref.getBoolean("DARK_MODE", false)
        cbDarkMode.isChecked = isDark

        cbDarkMode.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("DARK_MODE", isChecked).apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // =========================
        // 🌍 LANGUAGE (UI DEMO)
        // =========================
        val languages = arrayOf("Tiếng Việt", "English", "日本語")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerLanguage.adapter = adapter

        val savedLang = pref.getInt("LANG_POS", 0)
        spinnerLanguage.setSelection(savedLang)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                pref.edit().putInt("LANG_POS", position).apply()
                Toast.makeText(this@SettingsActivity, "Đã chọn: ${languages[position]}", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}