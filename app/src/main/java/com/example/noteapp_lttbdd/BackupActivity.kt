package com.example.noteapp_lttbdd

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BackupActivity : AppCompatActivity() {

    private lateinit var backupManager: BackupManager
    private lateinit var btnBackup: Button
    private lateinit var btnRestore: Button
    private lateinit var rvBackupList: RecyclerView
    private lateinit var backupAdapter: BackupAdapter

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { restoreBackup(it) }
    }

    private val customBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        uri?.let { createBackup(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_backup)

        val rootView = findViewById<View>(R.id.backup_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        backupManager = BackupManager(this, DatabaseHelper(this))

        btnBackup = findViewById(R.id.btnCreateBackup)
        btnRestore = findViewById(R.id.btnRestore)
        rvBackupList = findViewById(R.id.rvBackupList)
        
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadBackupList()
    }

    private fun setupRecyclerView() {
        backupAdapter = BackupAdapter { backupUri ->
            showRestoreConfirmDialog(backupUri)
        }
        rvBackupList.layoutManager = LinearLayoutManager(this)
        rvBackupList.adapter = backupAdapter
    }

    private fun setupListeners() {
        btnBackup.setOnClickListener {
            val fileName = "NoteApp_Backup_${SimpleDateFormat("ddMMyy_HHmm", Locale.getDefault()).format(Date())}.backup"
            customBackupLauncher.launch(fileName)
        }

        btnRestore.setOnClickListener {
            restoreLauncher.launch("*/*")
        }
    }

    private fun createBackup(customUri: Uri? = null) {
        lifecycleScope.launch {
            val uri = backupManager.createBackup(customUri)
            if (uri != null) {
                Toast.makeText(this@BackupActivity, "Sao lưu thành công!", Toast.LENGTH_SHORT).show()
                loadBackupList()
            } else {
                Toast.makeText(this@BackupActivity, "Sao lưu thất bại", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreBackup(uri: Uri) {
        lifecycleScope.launch {
            val success = backupManager.restoreBackup(uri)
            if (success) {
                Toast.makeText(this@BackupActivity, "Khôi phục thành công!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@BackupActivity, "Khôi phục thất bại", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRestoreConfirmDialog(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Khôi phục")
            .setMessage("Bạn có chắc chắn muốn khôi phục từ bản sao lưu này? Dữ liệu hiện tại sẽ bị thay thế.")
            .setPositiveButton("Khôi phục") { _, _ ->
                restoreBackup(uri)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun loadBackupList() {
        lifecycleScope.launch {
            val backups = withContext(Dispatchers.IO) {
                backupManager.getLocalBackups()
            }
            backupAdapter.updateData(backups)
        }
    }
}
