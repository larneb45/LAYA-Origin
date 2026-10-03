package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LinenCream
import com.example.ui.theme.SageDark
import com.example.ui.theme.SageLight
import com.example.ui.theme.SagePrimary
import com.example.util.MidnightMixer

@Composable
fun MidnightInfoDialog(
    todaySeed: Long,
    countdownText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LinenCream,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = SagePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Le Mix de Minuit", fontWeight = FontWeight.Bold, color = SageDark)
            }
        },
        text = {
            Column {
                Text(
                    text = "Algorithme de mélange déterministe (Seed-based shuffle) côté client.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SageDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Chaque jour à minuit pile, la graine (seed) change automatiquement avec la date calendaire. Tous les auditeurs écoutent la même séquence de morceaux au même moment, sans aucun serveur complexe.",
                    fontSize = 12.sp,
                    color = Color(0xFF5F6E66),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SageLight)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Graine du jour (Seed) :", fontSize = 12.sp, color = SageDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$todaySeed",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SagePrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Prochain Mix dans :", fontSize = 12.sp, color = SageDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = countdownText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SagePrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
            ) {
                Text("Compris")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
