package com.example.noteapp_lttbdd

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var tvCurrentLanguage: TextView
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
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage)

        updateLanguageText()

        // 🔙 Back
        btnBack.setOnClickListener { finish() }

        // =========================
        // 🌗 DARK MODE
        // =========================

        val isDarkMode = pref.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkMode

        findViewById<View>(R.id.itemDarkMode).setOnClickListener {
            switchDarkMode.isChecked = !switchDarkMode.isChecked
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        
        // 🔥 Navigate to LockNotesActivity
        findViewById<View>(R.id.itemNoteLock).setOnClickListener {
            val savedPassword = pref.getString("note_password", "")
            if (savedPassword.isNullOrEmpty()) {
                // If no password set, go directly to set one
                startActivity(Intent(this, LockNotesActivity::class.java))
            } else {
                showVerifyPasswordDialog()
            }
        }

        findViewById<View>(R.id.itemLanguage).setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun updateLanguageText() {
        val lang = LocaleHelper.getLanguage(this)
        tvCurrentLanguage.text = if (lang == "vi") getString(R.string.vietnamese) else getString(R.string.english)
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.vietnamese), getString(R.string.english))
        val languageCodes = arrayOf("vi", "en")
        
        val currentLang = LocaleHelper.getLanguage(this)
        val checkedItem = languageCodes.indexOf(currentLang)

        AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedLang = languageCodes[which]
                if (selectedLang != currentLang) {
                    LocaleHelper.setLocale(this, selectedLang)
                    
                    // Restart app to apply language globally
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = LocaleHelper.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    private fun showVerifyPasswordDialog() {
        val editText = EditText(this)
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        editText.hint = "Nhập mật khẩu hiện tại"

        AlertDialog.Builder(this)
            .setTitle("Xác thực")
            .setMessage("Vui lòng nhập mật khẩu để truy cập cài đặt khóa ghi chú")
            .setView(editText)
            .setPositiveButton("Xác nhận") { _, _ ->
                val input = editText.text.toString()
                val savedPassword = pref.getString("note_password", "")
                if (input == savedPassword) {
                    startActivity(Intent(this, LockNotesActivity::class.java))
                } else {
                    Toast.makeText(this, "Mật khẩu không chính xác", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
