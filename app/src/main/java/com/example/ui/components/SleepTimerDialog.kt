package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LinenCream
import com.example.ui.theme.SageDark
import com.example.ui.theme.SagePrimary

@Composable
fun SleepTimerDialog(
    currentMinutesRemaining: Int,
    isTimerActive: Boolean,
    onSetTimer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(if (isTimerActive) currentMinutesRemaining else 30) }
    val options = listOf(15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LinenCream,
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = SagePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Minuterie de Sommeil", fontWeight = FontWeight.Bold, color = SageDark)
            }
        },
        text = {
            Column {
                Text(
                    text = "Arrête automatiquement la lecture après le temps sélectionné pour vous endormir en toute sérénité.",
                    fontSize = 13.sp,
                    color = Color(0xFF5F6E66)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.take(3).forEach { min ->
                        FilterChip(
                            selected = selectedMinutes == min,
                            onClick = { selectedMinutes = min },
                            label = { Text("${min}m") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.drop(3).forEach { min ->
                        FilterChip(
                            selected = selectedMinutes == min,
                            onClick = { selectedMinutes = min },
                            label = { Text("${min}m") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSetTimer(selectedMinutes)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
            ) {
                Text("Activer (${selectedMinutes} min)")
            }
        },
        dismissButton = {
            if (isTimerActive) {
                TextButton(
                    onClick = {
                        onSetTimer(0)
                        onDismiss()
                    }
                ) {
                    Text("Désactiver", color = Color(0xFFC62828))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = SageDark)
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
