package com.stefanorussu.hydrationtracker.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 1. Definiamo la Snackbar "Stile Pixel" una volta sola
@Composable
fun AppSnackbar(data: SnackbarData) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        actionColor = MaterialTheme.colorScheme.primary,
        snackbarData = data
    )
}

// 2. Creiamo una "Radio Globale" per inviare messaggi da qualsiasi punto dell'app
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("Nessun SnackbarHostState fornito")
}

// 3. Un contenitore speciale per la MainActivity
@Composable
fun GlobalSnackbarProvider(content: @Composable () -> Unit) {
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    AppSnackbar(data)
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0) // Evita che lo Scaffold globale interferisca coi margini
        ) { padding ->
            // Applichiamo il padding ma senza bloccare il contenuto
            content()
        }
    }
}