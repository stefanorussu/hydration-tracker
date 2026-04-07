package com.stefanorussu.hydrationtracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefanorussu.hydrationtracker.data.backup.BackupRepository
import com.stefanorussu.hydrationtracker.data.backup.CryptoManager
import com.stefanorussu.hydrationtracker.data.backup.GoogleDriveManager
import com.stefanorussu.hydrationtracker.data.local.AppDatabase
import com.stefanorussu.hydrationtracker.data.local.BackupPreferencesManager
import kotlinx.coroutines.flow.first

class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Inizializza il manager delle preferenze
        val backupPrefs = BackupPreferencesManager(applicationContext)

        // 2. Legge lo stato dell'interruttore (ON/OFF)
        val isEnabled = backupPrefs.isLocalBackupEnabled.first()

        // 3. Se è spento, si ferma subito con successo
        if (!isEnabled) {
            return Result.success()
        }

        // 4. Se è acceso, prepara gli strumenti per il backup
        val database = AppDatabase.getDatabase(applicationContext)
        val cryptoManager = CryptoManager()
        val driveManager = GoogleDriveManager(applicationContext) // <-- Creato il manager di Drive

        val backupRepository = BackupRepository(
            context = applicationContext,
            waterDao = database.waterDao(),
            cryptoManager = cryptoManager,
            driveManager = driveManager // <-- Passato al motore dei backup
        )

        // 5. Esegue il salvataggio nel file interno cifrato
        val result = backupRepository.createLocalBackup()

        // 6. Comunica al sistema se ha finito o se deve riprovare
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}