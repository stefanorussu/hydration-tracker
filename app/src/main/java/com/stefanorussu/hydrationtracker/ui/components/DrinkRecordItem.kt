package com.stefanorussu.hydrationtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DrinkRecordItem(
    record: WaterRecord,
    dynamicIcon: ImageVector,
    themeBg: Color,
    themeFg: Color,
    onEdit: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Usiamo una Column piatta, niente Card, niente ombre
    Column(
        modifier = Modifier
            .fillMaxWidth() // Larga quanto tutto lo schermo
            .background(MaterialTheme.colorScheme.surface) // Sfondo bianco solido
            .clickable { onEdit() } // Apre il popup per gestire
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // Spazio interno per non appiccicare il testo ai lati
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(themeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = dynamicIcon, contentDescription = null, tint = themeFg, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.drinkName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Tocca per gestire", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "+${record.amountMl} ml", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = timeFormatter.format(Date(record.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // L'unica cosa che separa i record: una sottilissima linea grigio chiaro
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            thickness = 1.dp
        )
    }
}