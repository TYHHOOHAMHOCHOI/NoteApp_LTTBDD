package com.example.noteapp_lttbdd

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText

class LockNotesActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = LocaleHelper.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lock_notes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnSavePassword = findViewById<Button>(R.id.btnSavePassword)

        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        // Load existing password if any
        val savedPassword = sharedPreferences.getString("note_password", "")
        etPassword.setText(savedPassword)
        etConfirmPassword.setText(savedPassword)

        btnBack.setOnClickListener { finish() }

        btnSavePassword.setOnClickListener {
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (password.isEmpty()) {
                Toast.makeText(this, "Mật khẩu không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password == confirmPassword) {
                sharedPreferences.edit().putString("note_password", password).apply()
                Toast.makeText(this, "Đã lưu mật khẩu", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
            }
        }
    }
}