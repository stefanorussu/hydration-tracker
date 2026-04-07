package com.stefanorussu.hydrationtracker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.stefanorussu.hydrationtracker.data.local.AppDatabase
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Determina la quantità in base al bottone premuto
        val amount = when (intent.action) {
            "ADD_WATER_100" -> 100
            "ADD_WATER_250" -> 250
            "ADD_WATER_500" -> 500
            else -> return // Azione sconosciuta, si ferma
        }

        val db = AppDatabase.getDatabase(context)

        // Salva l'acqua nel database in background
        CoroutineScope(Dispatchers.IO).launch {
            db.waterDao().insert(
                WaterRecord(
                    amountMl = amount,
                    timestamp = System.currentTimeMillis(),
                    drinkName = "Acqua"
                )
            )
        }

        // Chiude la notifica dopo il click sul bottone rapido
        NotificationManagerCompat.from(context).cancel(1001)
    }
}