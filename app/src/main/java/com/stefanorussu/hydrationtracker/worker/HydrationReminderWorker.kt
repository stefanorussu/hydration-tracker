package com.stefanorussu.hydrationtracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefanorussu.hydrationtracker.data.local.AppDatabase
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository
import com.stefanorussu.hydrationtracker.model.UserProfile
import kotlinx.coroutines.flow.first

class HydrationReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // 1. Inizializzazione manuale (come abbiamo concordato)
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.waterDao()
            val repository = WaterRepository(dao)

            // 2. Estraiamo il valore reale.
            // Usiamo .first() e aggiungiamo ?: 0 in caso il DB sia vuoto
            val totalWaterValue = repository.getTodayTotal().first() ?: 0

            // 3. Calcoliamo l'obiettivo
            val userProfile = UserProfile(weightKg = 75.0, age = 25, isMale = true)
            val dailyGoalValue = userProfile.calculateDailyGoalMl()

            // 4. Confronto e invio
            if (totalWaterValue < dailyGoalValue) {
                sendNotification(totalWaterValue, dailyGoalValue)
            }

            return Result.success()
        } catch (e: Exception) {
            // Se succede qualcosa (es. errore DB), riprova più tardi
            return Result.retry()
        }
    }

    private fun sendNotification(total: Int, goal: Int) {
        val channelId = "hydration_reminder"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Promemoria Idratati",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Bevi un sorso d'acqua! 💧")
            // USA LE VARIABILI PASSATE (total e goal)
            .setContentText("Sei a $total ml. Il tuo obiettivo è $goal ml!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}