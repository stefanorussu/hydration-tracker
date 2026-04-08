package com.stefanorussu.hydrationtracker.data.network

object FitbitConfig {
    const val CLIENT_ID = "23VDBG"
    const val CLIENT_SECRET = "cf4d0653134f4b6e29c18f5cca5fba98"
    const val REDIRECT_URI = "hydrationtracker://callback"
    const val AUTH_URI = "https://www.fitbit.com/oauth2/authorize"
    const val TOKEN_URI = "https://api.fitbit.com/oauth2/token"
    const val API_BASE_URL = "https://api.fitbit.com/1/"

    // Gli "Scope" sono i permessi che chiediamo all'utente (peso, acqua e profilo base)
    const val SCOPES = "weight nutrition profile"
}