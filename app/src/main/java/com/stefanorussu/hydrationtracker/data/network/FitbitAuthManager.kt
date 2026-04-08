package com.stefanorussu.hydrationtracker.data.network

import android.content.Context
import android.util.Base64
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.stefanorussu.hydrationtracker.BuildConfig

class FitbitAuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)

    // INSERISCI QUI I TUOI DATI VERI DI FITBIT
    private val CLIENT_ID = BuildConfig.FITBIT_CLIENT_ID
    private val CLIENT_SECRET = BuildConfig.FITBIT_CLIENT_SECRET
    private val REDIRECT_URI = "hydrationtracker://callback"

    // Questa è la variabile 'api' che mancava!
    private val api: FitbitApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.fitbit.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FitbitApi::class.java)
    }

    // 1. Funzione originale: Primo login
    suspend fun exchangeCodeForToken(code: String): Boolean {
        val authHeader = "Basic " + Base64.encodeToString(
            "$CLIENT_ID:$CLIENT_SECRET".toByteArray(),
            Base64.NO_WRAP
        )

        return try {
            val response = api.exchangeCodeForToken(
                authHeader = authHeader,
                clientId = CLIENT_ID,
                redirectUri = REDIRECT_URI,
                code = code
            )

            // Salva le chiavi
            prefs.edit()
                .putString("access_token", response.access_token)
                .putString("refresh_token", response.refresh_token)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Controlla se abbiamo un token salvato
    fun isLinked(): Boolean {
        return prefs.getString("access_token", null) != null
    }

    // Genera l'URL magico per aprire la pagina di login di Fitbit
    fun getLoginUrl(): String {
        // Richiediamo i permessi esatti: nutrition (per l'acqua) e weight (per il peso)
        val scopes = "nutrition%20weight%20profile"
        return "https://www.fitbit.com/oauth2/authorize?response_type=code&client_id=$CLIENT_ID&redirect_uri=$REDIRECT_URI&scope=$scopes"
    }

    // 2. Nuova funzione: Rinnovo in background
    suspend fun refreshAccessToken(): Boolean {
        val refreshToken = prefs.getString("refresh_token", null) ?: return false

        val authHeader = "Basic " + Base64.encodeToString(
            "$CLIENT_ID:$CLIENT_SECRET".toByteArray(),
            Base64.NO_WRAP
        )

        return try {
            val response = api.refreshToken(
                authHeader = authHeader,
                clientId = CLIENT_ID,
                refreshToken = refreshToken
            )

            // Salva le nuove chiavi sovrascrivendo le vecchie
            prefs.edit()
                .putString("access_token", response.access_token)
                .putString("refresh_token", response.refresh_token)
                .apply()

            android.util.Log.d("FITBIT_AUTH", "Token rinnovato con successo!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("FITBIT_AUTH", "Fallito rinnovo token: ${e.message}")
            false
        }
    }
}