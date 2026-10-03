package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.AdminUiState
import com.example.ui.theme.LinenCream
import com.example.ui.theme.LiveRed
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.SageDark
import com.example.ui.theme.SageLight
import com.example.ui.theme.SagePrimary
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSheet(
    adminState: AdminUiState,
    rawTracks: List<Track>,
    onSaveConfig: (String, String, String) -> Unit,
    onFetchFromGithub: () -> Unit,
    onAddTrack: (String, String, String) -> Unit,
    onDeleteTrack: (Int) -> Unit,
    onClearStatus: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var repoInput by remember { mutableStateOf(adminState.repo) }
    var branchInput by remember { mutableStateOf(adminState.branch) }
    var patInput by remember { mutableStateOf(adminState.pat) }

    var newTitle by remember { mutableStateOf("") }
    var newArtist by remember { mutableStateOf("LAYA Origin") }
    var newUrl by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LinenCream,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CloudSync,
                        contentDescription = null,
                        tint = SagePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GitOps Admin — GitHub",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SageDark
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_admin_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Fermer",
                        tint = TextSecondaryLight
                    )
                }
            }

            // Status message
            adminState.statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (adminState.isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                        .padding(10.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = if (adminState.isError) LiveRed else Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Config Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Connexion au Dépôt GitHub",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SageDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = repoInput,
                            onValueChange = { repoInput = it },
                            label = { Text("Dépôt (owner/repo)") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_repo_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = branchInput,
                            onValueChange = { branchInput = it },
                            label = { Text("Branche") },
                            modifier = Modifier.fillMaxWidth().testTag("admin_branch_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = patInput,
                            onValueChange = { patInput = it },
                            label = { Text("Personal Access Token (PAT)") },
                            visualTransformation = PasswordVisualTransformation(),
                            trailingIcon = {
                                Icon(Icons.Rounded.Key, contentDescription = null, tint = SagePrimary)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("admin_pat_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onFetchFromGithub,
                                enabled = !adminState.isSaving
                            ) {
                                Text("Recharger depuis GitHub", color = SagePrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onSaveConfig(repoInput, branchInput, patInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                            ) {
                                Text("Sauvegarder")
                            }
                        }
                    }
                }

                // GitHub Releases Tip
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PastelPeach)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Astuce Hébergement Gratuit : Attachez vos fichiers .mp3 aux Releases de votre dépôt GitHub et copiez l'URL directe de téléchargement ici !",
                            fontSize = 12.sp,
                            color = Color(0xFF6B4533),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Add Track Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Ajouter un titre (GitOps PUT)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SageDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Titre du morceau") },
                            modifier = Modifier.fillMaxWidth().testTag("new_track_title_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = newArtist,
                            onValueChange = { newArtist = it },
                            label = { Text("Artiste") },
                            modifier = Modifier.fillMaxWidth().testTag("new_track_artist_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = newUrl,
                            onValueChange = { newUrl = it },
                            label = { Text("URL directe du fichier MP3") },
                            placeholder = { Text("https://github.com/.../release/.../track.mp3") },
                            modifier = Modifier.fillMaxWidth().testTag("new_track_url_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onAddTrack(newTitle, newArtist, newUrl)
                                newTitle = ""
                                newUrl = ""
                            },
                            enabled = !adminState.isSaving && newTitle.isNotBlank() && newUrl.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("commit_add_track_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                        ) {
                            if (adminState.isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Commit en cours...")
                            } else {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ajouter & Commiter sur GitHub")
                            }
                        }
                    }
                }

                // Current Tracks in Repo
                item {
                    Text(
                        text = "Musiques sur le dépôt (${rawTracks.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SageDark
                    )
                }

                itemsIndexed(rawTracks) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${track.displayTitle}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.url,
                                fontSize = 11.sp,
                                color = TextSecondaryLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onDeleteTrack(index) },
                            enabled = !adminState.isSaving,
                            modifier = Modifier.testTag("delete_track_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Supprimer",
                                tint = LiveRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
