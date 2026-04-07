package com.stefanorussu.hydrationtracker.data.backup

import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File as DriveFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stefanorussu.hydrationtracker.data.local.WaterDao
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupRepository(
    private val context: Context,
    private val waterDao: WaterDao,
    private val cryptoManager: CryptoManager,
    private val driveManager: GoogleDriveManager // <-- Aggiunto il manager di Google Drive
) {
    private val gson = Gson()
    private val BACKUP_FILE_NAME = "hydration_backup.enc"

    // --- BACKUP LOCALE (FILE INTERNO) ---
    suspend fun createLocalBackup(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allRecords = waterDao.getAllRecords()
            val jsonStr = gson.toJson(allRecords)
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            FileOutputStream(file).use { outputStream ->
                cryptoManager.encrypt(jsonStr, outputStream)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreLocalBackup(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            if (!file.exists()) return@withContext Result.failure(Exception("Nessun backup trovato"))
            val jsonStr = FileInputStream(file).use { inputStream ->
                cryptoManager.decrypt(inputStream)
            }
            restoreFromJson(jsonStr)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- BACKUP ESTERNO (STORAGE ACCESS FRAMEWORK) ---
    suspend fun exportToExternalUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allRecords = waterDao.getAllRecords()
            val jsonStr = gson.toJson(allRecords)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                cryptoManager.encrypt(jsonStr, outputStream)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromExternalUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                cryptoManager.decrypt(inputStream)
            } ?: return@withContext Result.failure(Exception("Impossibile aprire il file"))
            restoreFromJson(jsonStr)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- GOOGLE DRIVE ---
    suspend fun backupToDrive(account: GoogleSignInAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driveService = driveManager.getDriveService(account)
            val allRecords = waterDao.getAllRecords()
            val jsonStr = gson.toJson(allRecords)

            // 1. Salva i dati cifrati in un file temporaneo
            val tempFile = File(context.cacheDir, BACKUP_FILE_NAME)
            FileOutputStream(tempFile).use { outputStream ->
                cryptoManager.encrypt(jsonStr, outputStream)
            }
            val fileContent = FileContent("application/octet-stream", tempFile)

            // 2. Cerca se esiste già un backup nella cartella nascosta "appDataFolder"
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$BACKUP_FILE_NAME'")
                .execute()

            if (fileList.files.isEmpty()) {
                // 3a. Se non esiste, crea un nuovo file
                val fileMetadata = DriveFile().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, fileContent).execute()
            } else {
                // 3b. Se esiste, aggiornalo sovrascrivendolo
                val existingFileId = fileList.files[0].id
                driveService.files().update(existingFileId, DriveFile(), fileContent).execute()
            }

            tempFile.delete() // Pulisce il file temporaneo
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromDrive(account: GoogleSignInAccount): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val driveService = driveManager.getDriveService(account)

            // 1. Cerca il file di backup su Drive
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$BACKUP_FILE_NAME'")
                .execute()

            if (fileList.files.isEmpty()) {
                return@withContext Result.failure(Exception("Nessun backup trovato su Google Drive"))
            }

            // 2. Scarica il file in una cartella temporanea
            val fileId = fileList.files[0].id
            val tempFile = File(context.cacheDir, "downloaded_backup.enc")
            FileOutputStream(tempFile).use { outputStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }

            // 3. Decifra i dati
            val jsonStr = FileInputStream(tempFile).use { inputStream ->
                cryptoManager.decrypt(inputStream)
            }
            tempFile.delete() // Pulisce il file temporaneo

            // 4. Ripristina nel database
            restoreFromJson(jsonStr)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FUNZIONE DI SUPPORTO PER IL RIPRISTINO (ANTI-DUPLICATI) ---
    private suspend fun restoreFromJson(jsonStr: String): Result<Int> {
        val type = object : TypeToken<List<WaterRecord>>() {}.type
        val restoredRecords: List<WaterRecord> = gson.fromJson(jsonStr, type)
        val currentRecords = waterDao.getAllRecords().map { it.timestamp }.toSet()
        var restoredCount = 0

        restoredRecords.forEach { record ->
            if (!currentRecords.contains(record.timestamp)) {
                waterDao.insert(record.copy(id = 0))
                restoredCount++
            }
        }
        return Result.success(restoredCount)
    }
}