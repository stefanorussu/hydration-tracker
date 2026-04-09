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
        val action = intent.action ?: return

        // Ascoltiamo solo l'azione "Smart"
        if (action == "ADD_SMART_DRINK") {
            // Estraiamo i dati intelligenti che ci ha passato la notifica
            val amount = intent.getIntExtra("EXTRA_AMOUNT", 250)
            val drinkName = intent.getStringExtra("EXTRA_DRINK_NAME") ?: "Acqua"

            val db = AppDatabase.getDatabase(context)

            // Salva l'acqua nel database in background
            CoroutineScope(Dispatchers.IO).launch {
                db.waterDao().insert(
                    WaterRecord(
                        amountMl = amount,
                        inputAmountMl = amount,
                        timestamp = System.currentTimeMillis(),
                        drinkName = drinkName
                    )
                )
            }

            // Chiude la notifica dopo aver cliccato "Aggiungi"
            NotificationManagerCompat.from(context).cancel(1001)
        }
    }
}