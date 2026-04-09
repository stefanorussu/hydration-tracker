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

        if (now - lastNotifTime < (2.5 * 60 * 60 * 1000L).toLong()) return Result.success()

        val database = AppDatabase.getDatabase(context)
        val repository = WaterRepository(database.waterDao())

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        val todayTotal = repository.getTodayTotal(startOfDay, endOfDay).firstOrNull() ?: 0
        val dailyGoal = prefs.getInt("daily_goal_ml", 2000)
        val progressPercentage = if (dailyGoal > 0) (todayTotal.toFloat() / dailyGoal) else 0f

        if (progressPercentage >= 1.0f) return Result.success()

        val records = repository.getRecordsBetweenDates(startOfDay, endOfDay).firstOrNull() ?: emptyList()
        val lastRecord = records.firstOrNull()
        val lastDrinkTime = lastRecord?.timestamp ?: startOfDay
        val minutesSinceLastDrink = (now - lastDrinkTime) / (1000 * 60)

        if (minutesSinceLastDrink < 90) return Result.success()

        // --- GEMELLO DELLA HOME ---
        val frequentWaterAmount = repository.getMostFrequentAmount("Acqua") ?: 250
        val frequentCoffeeAmount = repository.getMostFrequentAmount("Caffè") ?: 50

        val dehydratingDrinks = listOf("Caffè", "Birra", "Vino", "Cocktail", "Digestivo")

        var title = "Hydration Tracker"
        var message = ""
        var suggestedDrink = "Acqua"
        var primaryAmount = frequentWaterAmount

        when {
            lastRecord != null && lastRecord.drinkName in dehydratingDrinks -> {
                message = "Il ${lastRecord.drinkName.lowercase()} disidrata. Bilanciamo? ✨"
                suggestedDrink = "Acqua"
                primaryAmount = frequentWaterAmount
            }
            currentHour in 8..9 && records.none { it.drinkName == "Caffè" } -> {
                message = "Un buon caffè per iniziare la giornata? ☕"
                suggestedDrink = "Caffè"
                primaryAmount = frequentCoffeeAmount
            }
            currentHour in 13..14 && records.none { it.drinkName == "Caffè" && it.timestamp > (now - 4 * 3600 * 1000) } -> {
                message = "Pausa caffè post-pranzo? ☕"
                suggestedDrink = "Caffè"
                primaryAmount = frequentCoffeeAmount
            }
            currentHour in 21..22 -> {
                message = "Una tisana rilassante per la notte? 🌿"
                suggestedDrink = "Tisana"
                primaryAmount = 250
            }
            else -> {
                message = "Mantieni il tuo ritmo! È ora di un sorso. ✨"
                suggestedDrink = "Acqua"
                primaryAmount = frequentWaterAmount
            }
        }

        // Determiniamo i 2 bottoni alternativi
        val standardAmounts = listOf(200, 250, 500)
        val alternatives = standardAmounts.filter { it != primaryAmount }.take(2)

        // Se si tratta di caffè, riduciamo le alternative (es. 30, 50, 100)
        val alt1 = if (suggestedDrink == "Caffè") (if (primaryAmount == 50) 30 else 50) else alternatives.getOrElse(0) { 200 }
        val alt2 = if (suggestedDrink == "Caffè") (if (primaryAmount == 100) 50 else 100) else alternatives.getOrElse(1) { 500 }

        sendDynamicNotification(title, message, suggestedDrink, primaryAmount, alt1, alt2)
        prefs.edit().putLong("last_notif_time", now).apply()

        return Result.success()
    }

    private fun sendDynamicNotification(title: String, message: String, drinkName: String, amount1: Int, amount2: Int, amount3: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "hydration_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Promemoria Intelligenti", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // 1. Click sul CORPO per aprire l'app (es. se hai una bottiglia da 1000ml)
        val intent = Intent(context, Class.forName("com.stefanorussu.hydrationtracker.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Funzione per creare i bottoni di azione rapida
        fun createActionIntent(amount: Int, requestCode: Int): PendingIntent {
            val actionIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ADD_SMART_DRINK"
                putExtra("EXTRA_AMOUNT", amount)
                putExtra("EXTRA_DRINK_NAME", drinkName)
            }
            // Il requestCode (1, 2, 3) DEVE essere diverso altrimenti Android sovrascrive i bottoni
            return PendingIntent.getBroadcast(context, requestCode, actionIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // COSTRUZIONE NOTIFICA
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Apre l'app se clicchi il testo
            .addAction(0, "+${amount1}ml", createActionIntent(amount1, 101)) // Suggerimento Principale (Abitudine)
            .addAction(0, "+${amount2}ml", createActionIntent(amount2, 102)) // Alternativa 1
            .addAction(0, "+${amount3}ml", createActionIntent(amount3, 103)) // Alternativa 2
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}