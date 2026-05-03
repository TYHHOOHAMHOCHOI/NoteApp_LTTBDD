package com.example.noteapp_lttbdd

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var layoutUsername: View

    private val sharedPreferences by lazy {
        getSharedPreferences("user_profile", Context.MODE_PRIVATE)
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let {
                saveAvatarUri(it.toString())
                displayAvatar(it.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        val rootView = findViewById<View>(R.id.profile_main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        ivAvatar = findViewById(R.id.ivAvatar)
        tvUsername = findViewById(R.id.tvUsername)
        layoutUsername = findViewById(R.id.layoutUsername)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Load saved data
        tvUsername.text = sharedPreferences.getString("username", "User Name")
        displayAvatar(sharedPreferences.getString("avatar_uri", null))

        // Change Avatar
        ivAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        // Change Username
        layoutUsername.setOnClickListener {
            showEditUsernameDialog()
        }

        val itemSettings = findViewById<View>(R.id.itemSettings)
        itemSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val itemBackup = findViewById<View>(R.id.itemBackup)
        itemBackup.setOnClickListener {
            startActivity(Intent(this, BackupActivity::class.java))
        }

        val itemRecentlyDeleted = findViewById<View>(R.id.itemTrash)
        itemRecentlyDeleted.setOnClickListener {
            startActivity(Intent(this, RecentlyDeletedActivity::class.java))
        }

        setupBottomNavigation()
    }

    private fun showEditUsernameDialog() {
        val editText = EditText(this)
        editText.setText(tvUsername.text)

        AlertDialog.Builder(this)
            .setTitle("Đổi tên người dùng")
            .setView(editText)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    tvUsername.text = newName
                    sharedPreferences.edit().putString("username", newName).apply()
                } else {
                    Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun saveAvatarUri(uri: String) {
        sharedPreferences.edit().putString("avatar_uri", uri).apply()
    }

    private fun displayAvatar(uriString: String?) {
        if (uriString != null) {
            try {
                ivAvatar.setImageURI(Uri.parse(uriString))
            } catch (e: Exception) {
                ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.navigation_profile
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_profile -> true
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.navigation_profile
    }
}
