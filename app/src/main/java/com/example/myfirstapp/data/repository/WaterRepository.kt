import com.example.myfirstapp.data.local.WaterDao
import com.example.myfirstapp.data.local.entities.WaterLog
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class WaterRepository(private val waterDao: WaterDao) {

    // Recupera tutti i log per la lista
    val allLogs: Flow<List<WaterLog>> = waterDao.getAllLogs()

    // Calcola il totale bevuto oggi
    fun getTodayTotal(): Flow<Int?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return waterDao.getTodayWaterSum(calendar.timeInMillis)
    }

    // Aggiunge una bevanda calcolando l'acqua reale
    suspend fun addDrink(amountMl: Int, type: DrinkType) {
        val waterContent = (amountMl * type.waterPercentage).toInt()
        val log = WaterLog(
            amountMl = amountMl,
            waterContentMl = waterContent,
            drinkType = type.name
        )
        waterDao.insertLog(log)
    }

    // Elimina un log (i calcoli si aggiornano grazie ai Flow di Room)
    suspend fun deleteLog(log: WaterLog) {
        waterDao.deleteLog(log)
    }
}