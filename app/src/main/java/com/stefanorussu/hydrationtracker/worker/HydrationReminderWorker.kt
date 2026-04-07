package com.stefanorussu.hydrationtracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefanorussu.hydrationtracker.data.local.AppDatabase
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class HydrationReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour in 23..23 || currentHour in 0..7) return Result.success()

        val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
        val lastNotifTime = prefs.getLong("last_notif_time", 0L)
        val now = System.currentTimeMillis()

        if (now - lastNotifTime < 3 * 60 * 60 * 1000L) return Result.success()

        val database = AppDatabase.getDatabase(context)
        val repository = WaterRepository(database.waterDao())

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        val todayTotal = repository.getTodayTotal(startOfDay, endOfDay).firstOrNull() ?: 0
        val records = repository.getRecordsBetweenDates(startOfDay, endOfDay).firstOrNull() ?: emptyList()
        val lastDrinkTime = records.lastOrNull()?.timestamp ?: startOfDay
        val hoursSinceLastDrink = (now - lastDrinkTime) / (1000 * 60 * 60).toFloat()

        val dailyGoal = prefs.getInt("daily_goal_ml", 2000)
        val progressPercentage = if (dailyGoal > 0) (todayTotal.toFloat() / dailyGoal) else 0f

        if (progressPercentage >= 1.0f) return Result.success()

        var shouldSend = false
        var message = ""

        when {
            progressPercentage < 0.30f -> {
                shouldSend = true
                message = "Sei indietro con l’idratazione 💧 Bevi qualcosa!"
            }
            progressPercentage in 0.30f..0.80f -> {
                if (hoursSinceLastDrink >= 2.5f) {
                    shouldSend = true
                    message = "Continua così 👍 Un altro bicchiere ti avvicina all’obiettivo"
                }
            }
            progressPercentage > 0.80f -> {
                if (hoursSinceLastDrink >= 3.0f) {
                    shouldSend = true
                    message = "Ci sei quasi! 💪 Dai un ultimo sorso per raggiungere l'obiettivo!"
                }
            }
        }

        if (shouldSend) {
            sendNotification("Hydration Tracker", message)
            prefs.edit().putLong("last_notif_time", now).apply()
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "hydration_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Promemoria Acqua", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // QUESTA PARTE RENDE LA NOTIFICA CLICCABILE PER APRIRE L'APP
        val intent = Intent(context, Class.forName("com.stefanorussu.hydrationtracker.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // AZIONI RAPIDE (I 3 bottoni sotto il testo)
        val add100Intent = Intent(context, NotificationReceiver::class.java).apply { action = "ADD_WATER_100" }
        val pIntent100 = PendingIntent.getBroadcast(context, 1, add100Intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val add250Intent = Intent(context, NotificationReceiver::class.java).apply { action = "ADD_WATER_250" }
        val pIntent250 = PendingIntent.getBroadcast(context, 2, add250Intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val add500Intent = Intent(context, NotificationReceiver::class.java).apply { action = "ADD_WATER_500" }
        val pIntent500 = PendingIntent.getBroadcast(context, 3, add500Intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // COSTRUZIONE DELLA NOTIFICA
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Il click sul corpo della notifica
            .addAction(0, "+100ml", pIntent100) // Non uso icone sui tasti per mantenere il design pulito
            .addAction(0, "+250ml", pIntent250)
            .addAction(0, "+500ml", pIntent500)
            .setAutoCancel(true) // Scompare automaticamente se ci clicchi
            .build()

        notificationManager.notify(1001, notification)
    }
}