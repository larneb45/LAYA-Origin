package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RadioViewModel
import com.example.ui.components.AdminSheet
import com.example.ui.components.LiveIndicator
import com.example.ui.components.MidnightInfoDialog
import com.example.ui.components.PlaylistSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.ZenDiscVisualizer
import com.example.ui.theme.LinenCream
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelRose
import com.example.ui.theme.SageDark
import com.example.ui.theme.SageLight
import com.example.ui.theme.SagePrimary
import com.example.ui.theme.SageSoft
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.MidnightMixer
import java.util.Locale

@Composable
fun MainPlayerScreen(
    viewModel: RadioViewModel,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsState()
    val adminState by viewModel.adminState.collectAsState()

    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showAdminSheet by remember { mutableStateOf(false) }
    var showMidnightDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var isAdminAuthenticated by remember { mutableStateOf(false) }
    var showPasswordPrompt by remember { mutableStateOf(false) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val currentTrack = playerState.currentTrack
    val currentTitle = currentTrack?.displayTitle ?: "LAYA Origin Radio"
    val currentArtist = currentTrack?.displayArtist ?: "Le Mix de Minuit"

    Surface(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = LinenCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. Top Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LAYA Origin",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SageDark,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Webradio Serverless",
                        fontSize = 11.sp,
                        color = TextSecondaryLight
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveIndicator(isPlaying = playerState.isPlaying)

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (isAdminAuthenticated) {
                                showAdminSheet = true
                            } else {
                                showPasswordPrompt = true
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("admin_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Console GitOps Admin",
                            tint = SagePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showPlaylistSheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = "File d'attente",
                            tint = SagePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // --- 2. Middle Content (Artwork + Title + Progress) ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // "Mix de Minuit" Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SageLight)
                        .clickable { showMidnightDialog = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("midnight_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LE MIX DE MINUIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = SageDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Info",
                        tint = SagePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Disc Visualizer
                ZenDiscVisualizer(
                    isPlaying = playerState.isPlaying,
                    isBuffering = playerState.isBuffering
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Track Title & Artist
                Text(
                    text = currentTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(horizontal = 16.dp).testTag("track_title_text")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$currentArtist • Piste ${playerState.currentIndex + 1} / ${playerState.playlist.size.coerceAtLeast(1)}",
                    fontSize = 13.sp,
                    color = TextSecondaryLight,
                    textAlign = TextAlign.Center
                )

                // Error alert if any
                playerState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        fontSize = 11.sp,
                        color = Color(0xFFD32F2F),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress scrubber
                val currentPos = playerState.currentPositionMs
                val duration = playerState.durationMs
                val progressFraction = if (duration > 0) (currentPos.toFloat() / duration).coerceIn(0f, 1f) else 0f
                var sliderValue by remember(progressFraction) { mutableFloatStateOf(progressFraction) }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            if (duration > 0) {
                                viewModel.seekTo((sliderValue * duration).toLong())
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = SagePrimary,
                            activeTrackColor = SagePrimary,
                            inactiveTrackColor = SageLight
                        ),
                        modifier = Modifier.testTag("audio_progress_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPos),
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                        Text(
                            text = if (duration > 0) formatTime(duration) else "Direct",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                }
            }

            // --- 3. Playback Controls ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bouton Précédent
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SageLight)
                            .clickable { viewModel.previous() }
                            .testTag("previous_track_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Précédent",
                            tint = SagePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    // Gros bouton START / PAUSE central tactile
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape,
                                spotColor = SagePrimary.copy(alpha = 0.35f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SagePrimary, Color(0xFF354E43))
                                )
                            )
                            .clickable { viewModel.togglePlayPause() }
                            .testTag("play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                color = LinenCream,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Text(
                                text = if (playerState.isPlaying) "PAUSE" else "START",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LinenCream,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    // Bouton Suivant
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SageLight)
                            .clickable { viewModel.next() }
                            .testTag("next_track_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Suivant",
                            tint = SagePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Secondary row: Sleep timer & Midnight countdown pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sleep Timer button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (playerState.isSleepTimerActive) PastelRose else Color.White)
                            .clickable { showSleepTimerDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("sleep_timer_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = "Minuterie",
                            tint = if (playerState.isSleepTimerActive) Color(0xFFC2410C) else SagePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playerState.isSleepTimerActive) "${playerState.sleepTimerRemainingMinutes}m" else "Sommeil",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (playerState.isSleepTimerActive) Color(0xFFC2410C) else SageDark
                        )
                    }

                    // Midnight countdown
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable { showMidnightDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("countdown_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mix dans : ${playerState.countdownText.ifBlank { "00:00:00" }}",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                }
            }
        }
    }

    // --- Bottom Sheets and Dialogs ---

    if (showPlaylistSheet) {
        PlaylistSheet(
            playlist = playerState.playlist,
            currentIndex = playerState.currentIndex,
            isPlaying = playerState.isPlaying,
            onSelectTrack = { idx ->
                viewModel.playTrack(idx)
                showPlaylistSheet = false
            },
            onRefresh = { viewModel.loadPlaylist(autoPlay = false) },
            onDismiss = { showPlaylistSheet = false }
        )
    }

    if (showPasswordPrompt) {
        var inputPass by remember { mutableStateOf("") }
        var isPassError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPasswordPrompt = false },
            containerColor = LinenCream,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = SagePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accès Administrateur", fontWeight = FontWeight.Bold, color = SageDark)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Veuillez entrer le mot de passe pour accéder à la console GitOps.",
                        fontSize = 13.sp,
                        color = TextSecondaryLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputPass,
                        onValueChange = {
                            inputPass = it
                            isPassError = false
                        },
                        label = { Text("Mot de passe") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = isPassError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_field")
                    )
                    if (isPassError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mot de passe incorrect.",
                            fontSize = 12.sp,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPass == "energy") {
                            isAdminAuthenticated = true
                            showPasswordPrompt = false
                            showAdminSheet = true
                        } else {
                            isPassError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SagePrimary),
                    modifier = Modifier.testTag("admin_password_confirm_button")
                ) {
                    Text("Déverrouiller")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordPrompt = false }) {
                    Text("Annuler", color = SageDark)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAdminSheet) {
        AdminSheet(
            adminState = adminState,
            rawTracks = playerState.rawPlaylist,
            onSaveConfig = { repo, branch, pat ->
                viewModel.updateGithubConfig(repo, branch, pat)
            },
            onFetchFromGithub = { viewModel.fetchFromGithubWithSha() },
            onAddTrack = { title, artist, url ->
                viewModel.addTrackAndCommit(title, artist, url)
            },
            onDeleteTrack = { idx ->
                viewModel.deleteTrackAndCommit(idx)
            },
            onClearStatus = { viewModel.clearAdminStatus() },
            onDismiss = { showAdminSheet = false }
        )
    }

    if (showMidnightDialog) {
        MidnightInfoDialog(
            todaySeed = playerState.todaySeed,
            countdownText = playerState.countdownText,
            onDismiss = { showMidnightDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutesRemaining = playerState.sleepTimerRemainingMinutes,
            isTimerActive = playerState.isSleepTimerActive,
            onSetTimer = { min -> viewModel.setSleepTimer(min) },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSecs = (millis / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(Locale.ROOT, "%d:%02d", mins, secs)
}
