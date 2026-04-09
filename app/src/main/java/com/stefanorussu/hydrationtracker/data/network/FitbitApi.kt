package com.stefanorussu.hydrationtracker.data.network

import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// --- RISPOSTE API FITBIT ---

data class FitbitTokenResponse(
    val access_token: String,
    val refresh_token: String,
    val user_id: String,
    val expires_in: Int
)

// Risposta di Fitbit quando salviamo l'acqua (ci restituisce l'ID del log creato)
data class FitbitWaterLogResponse(
    val waterLog: FitbitWaterLog
)

data class FitbitWaterLog(
    val logId: Long,
    val amount: Int
)

// --- INTERFACCIA RETROFIT ---

interface FitbitApi {

    // 1. Scambio del Codice con il Token
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun exchangeCodeForToken(
        @Header("Authorization") authHeader: String,
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("redirect_uri") redirectUri: String,
        @Field("code") code: String
    ): FitbitTokenResponse

    // 2. Invio dell'Acqua a Fitbit
    @POST("1/user/-/foods/log/water.json")
    suspend fun logWater(
        @Header("Authorization") authHeader: String,
        @Query("date") date: String, // Formato richiesto: yyyy-MM-dd
        @Query("amount") amount: Int,
        @Query("unit") unit: String = "ml" // Forziamo i millilitri per sicurezza
    ): FitbitWaterLogResponse

    // 3. Rinnovo del Token (Refresh)
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun refreshToken(
        @Header("Authorization") authHeader: String,
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String
    ): FitbitTokenResponse

    // 4. Elimina un record di acqua specifico
    @DELETE("1/user/-/foods/log/water/{logId}.json")
    suspend fun deleteWaterLog(
        @Header("Authorization") authHeader: String,
        @Path("logId") logId: String
    ): retrofit2.Response<Unit>

    // 5. Scarica il registro dell'acqua di una data specifica
    @GET("1/user/-/foods/log/water/date/{date}.json")
    suspend fun getWaterLogs(
        @Header("Authorization") authHeader: String,
        @Path("date") dateString: String // Formato "yyyy-MM-dd"
    ): FitbitWaterSummaryResponse

    @GET("1/user/-/profile.json")
    suspend fun getUserProfile(@Header("Authorization") authHeader: String): FitbitProfileResponse
}

// Contenitori per leggere la risposta di Fitbit (Mettili in fondo, fuori dalla classe)
data class FitbitWaterSummaryResponse(
    val water: List<FitbitWaterLogEntry>
)

data class FitbitWaterLogEntry(
    val logId: Long,
    val amount: Int
)

data class FitbitProfileResponse(val user: FitbitUser)
data class FitbitUser(val weight: Float)