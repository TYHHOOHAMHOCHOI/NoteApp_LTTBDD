package com.example.noteapp_lttbdd

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class BackupManager(private val context: Context, private val dbHelper: DatabaseHelper) {

    private val secretKey = "MySecretKeyNoteA" // 16 bytes for AES-128

    // ==================== ENCRYPTION ====================
    private fun encrypt(data: String): String {
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    private fun decrypt(encryptedData: String): String {
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val decodedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes)
    }

    // ==================== BACKUP ====================
    suspend fun createBackup(customUri: Uri? = null): Uri? = withContext(Dispatchers.IO) {
        return@withContext try {
            val notes = dbHelper.getAllNotes()
            val backupData = BackupData(notes = notes)
            val json = Gson().toJson(backupData)
            val encryptedJson = encrypt(json)

            val fileName = "NoteApp_Backup_${SimpleDateFormat("ddMMyy_HHmm", Locale.getDefault()).format(Date())}.backup"

            // 1. Always save a copy to app-specific storage for the "History List"
            val privateDir = context.getExternalFilesDir("Backups")
            if (privateDir != null) {
                if (!privateDir.exists()) privateDir.mkdirs()
                val localFile = File(privateDir, fileName)
                localFile.writeBytes(encryptedJson.toByteArray())
            }

            // 2. Write to the target URI (System Picker or MediaStore)
            val resolver = context.contentResolver
            val uri = if (customUri != null) {
                customUri
            } else {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NoteAppBackup")
                    }
                }
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
                resolver.insert(collection, contentValues)
            }

            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    output.write(encryptedJson.toByteArray())
                }
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==================== RESTORE ====================
    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val encryptedJson = inputStream.bufferedReader().use { it.readText() }

            val json = try {
                decrypt(encryptedJson)
            } catch (e: Exception) {
                encryptedJson
            }

            val backupData = Gson().fromJson(json, BackupData::class.java)

            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.delete("notes", null, null)
                backupData.notes.forEach { note: Note ->
                    val values = ContentValues().apply {
                        put("title", note.title)
                        put("content", note.content)
                        put("is_locked", if (note.isLocked) 1 else 0)
                        put("is_pinned", if (note.isPinned) 1 else 0)
                        put("reminder_time", note.reminderTime)
                        put("is_reminder_enabled", if (note.isReminderEnabled) 1 else 0)
                        put("repeat_type", note.repeatType)
                        put("tag", note.tag)
                    }
                    db.insert("notes", null, values)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLocalBackups(): List<File> {
        val backups = mutableListOf<File>()

        // Check app-specific dir (Most reliable for listing)
        val privateDir = context.getExternalFilesDir("Backups")
        if (privateDir?.exists() == true) {
            privateDir.listFiles { _, name -> name.endsWith(".backup") }?.let { backups.addAll(it) }
        }

        // Also check the default Downloads folder if it exists (legacy/specific folder)
        val publicDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NoteAppBackup")
        } else {
            File(Environment.getExternalStorageDirectory(), "NoteAppBackup")
        }

        if (publicDir.exists()) {
            publicDir.listFiles { _, name -> name.endsWith(".backup") }?.let {
                it.forEach { file ->
                    if (!backups.any { backup -> backup.name == file.name }) {
                        backups.add(file)
                    }
                }
            }
        }

        return backups.sortedByDescending { it.lastModified() }
    }
}
