package com.example.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.model.Track
import com.example.service.LayaRadioService
import com.example.util.MidnightMixer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class PlayerUiState(
    val playlist: List<Track> = emptyList(),
    val rawPlaylist: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val todaySeed: Long = 0L,
    val countdownText: String = "",
    val errorMessage: String? = null,
    val isSleepTimerActive: Boolean = false,
    val sleepTimerRemainingMinutes: Int = 0
) {
    val currentTrack: Track?
        get() = playlist.getOrNull(currentIndex)
}

object RadioPlayerManager {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var appContext: Context? = null
    private var mediaPlayer: MediaPlayer? = null
    private var nextPreloadedPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var progressJob: Job? = null
    private var countdownJob: Job? = null
    private var sleepTimerJob: Job? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        startMidnightCountdown()
    }

    private fun startMidnightCountdown() {
        countdownJob?.cancel()
        countdownJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val millis = MidnightMixer.getMillisUntilMidnight()
                val text = MidnightMixer.formatCountdown(millis)
                _state.update { it.copy(countdownText = text) }

                // À minuit pile, rafraîchir le mix !
                if (millis <= 1000L) {
                    delay(2000L)
                    val raw = _state.value.rawPlaylist
                    if (raw.isNotEmpty()) {
                        setRawPlaylist(raw, startPlaying = _state.value.isPlaying)
                    }
                }
                delay(1000L)
            }
        }
    }

    fun setRawPlaylist(rawList: List<Track>, startPlaying: Boolean = false) {
        val seed = MidnightMixer.getTodaySeed()
        val shuffled = MidnightMixer.shuffleWithSeed(rawList, seed)
        _state.update {
            it.copy(
                rawPlaylist = rawList,
                playlist = shuffled,
                todaySeed = seed,
                currentIndex = 0,
                errorMessage = null
            )
        }
        if (startPlaying && shuffled.isNotEmpty()) {
            playTrackAtIndex(0)
        }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        val current = _state.value.currentTrack
        if (current == null) {
            if (_state.value.playlist.isNotEmpty()) {
                playTrackAtIndex(0)
            }
            return
        }

        mediaPlayer?.let { player ->
            try {
                player.start()
                _state.update { it.copy(isPlaying = true, isBuffering = false) }
                startProgressTracker()
                notifyService(LayaRadioService.ACTION_UPDATE_NOTIFICATION)
                return
            } catch (e: Exception) {
                // Relancer si état corrompu
            }
        }
        playTrackAtIndex(_state.value.currentIndex)
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _state.update { it.copy(isPlaying = false) }
        notifyService(LayaRadioService.ACTION_UPDATE_NOTIFICATION)
    }

    fun next() {
        val list = _state.value.playlist
        if (list.isEmpty()) return
        val nextIdx = (_state.value.currentIndex + 1) % list.size
        playTrackAtIndex(nextIdx)
    }

    fun previous() {
        val list = _state.value.playlist
        if (list.isEmpty()) return
        val prevIdx = (_state.value.currentIndex - 1 + list.size) % list.size
        playTrackAtIndex(prevIdx)
    }

    fun playTrackAtIndex(index: Int) {
        val list = _state.value.playlist
        if (list.isEmpty() || index !in list.indices) return

        val track = list[index]
        _state.update {
            it.copy(
                currentIndex = index,
                isBuffering = true,
                currentPositionMs = 0L,
                durationMs = 0L,
                errorMessage = null
            )
        }

        startForegroundService()

        scope.launch(Dispatchers.IO) {
            // Vérifier si le cache local a ce fichier
            val cachedFile = getCachedAudioFile(track.url)
            val playSource = if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 10000) {
                cachedFile.absolutePath
            } else {
                // Télécharger en tâche de fond pour la prochaine fois
                preloadAudioToCache(track.url)
                track.url
            }

            // Pré-charger également le morceau suivant pour éviter toute latence !
            val nextTrackIdx = (index + 1) % list.size
            preloadAudioToCache(list[nextTrackIdx].url)

            launch(Dispatchers.Main) {
                setupAndPlay(playSource)
            }
        }
    }

    private fun setupAndPlay(sourceUrl: String) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            val player = MediaPlayer().apply {
                setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(sourceUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    _state.update {
                        it.copy(
                            isPlaying = true,
                            isBuffering = false,
                            durationMs = mp.duration.toLong().coerceAtLeast(0L)
                        )
                    }
                    startProgressTracker()
                    notifyService(LayaRadioService.ACTION_UPDATE_NOTIFICATION)
                }
                setOnCompletionListener {
                    next()
                }
                setOnErrorListener { _, what, extra ->
                    _state.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = false,
                            errorMessage = "Erreur lecture audio (code: $what/$extra)"
                        )
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = "Erreur d'initialisation : ${e.message}"
                )
            }
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _state.update { it.copy(currentPositionMs = positionMs) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _state.update { it.copy(isSleepTimerActive = false, sleepTimerRemainingMinutes = 0) }
            return
        }

        _state.update { it.copy(isSleepTimerActive = true, sleepTimerRemainingMinutes = minutes) }

        sleepTimerJob = scope.launch(Dispatchers.Default) {
            var remaining = minutes
            while (remaining > 0 && isActive) {
                delay(60_000L)
                remaining--
                _state.update { it.copy(sleepTimerRemainingMinutes = remaining) }
            }
            launch(Dispatchers.Main) {
                pause()
                _state.update { it.copy(isSleepTimerActive = false, sleepTimerRemainingMinutes = 0) }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val pos = try { player.currentPosition.toLong() } catch (e: Exception) { 0L }
                        val dur = try { player.duration.toLong().coerceAtLeast(0L) } catch (e: Exception) { 0L }
                        _state.update { it.copy(currentPositionMs = pos, durationMs = dur) }
                    }
                }
                delay(500L)
            }
        }
    }

    private fun startForegroundService() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, LayaRadioService::class.java).apply {
            action = LayaRadioService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(ctx, intent)
        } else {
            ctx.startService(intent)
        }
    }

    private fun notifyService(actionName: String) {
        val ctx = appContext ?: return
        val intent = Intent(ctx, LayaRadioService::class.java).apply {
            action = actionName
        }
        try {
            ctx.startService(intent)
        } catch (e: Exception) {
            // Service not running yet or background restriction
        }
    }

    // Gestion du cache audio pour éviter la latence inter-pistes
    private fun getCachedAudioFile(url: String): File? {
        val ctx = appContext ?: return null
        val cacheDir = File(ctx.cacheDir, "audio_cache").apply { if (!exists()) mkdirs() }
        val filename = "track_" + url.hashCode().toString() + ".mp3"
        return File(cacheDir, filename)
    }

    private fun preloadAudioToCache(audioUrl: String) {
        if (!audioUrl.startsWith("http")) return
        val targetFile = getCachedAudioFile(audioUrl) ?: return
        if (targetFile.exists() && targetFile.length() > 50000) return // Déjà en cache

        try {
            val url = URL(audioUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val tempFile = File(targetFile.parentFile, targetFile.name + ".tmp")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.renameTo(targetFile)
            }
        } catch (e: Exception) {
            // Ignorer silencieusement le cache s'il échoue, le streaming direct fonctionnera
        }
    }

    fun release() {
        progressJob?.cancel()
        countdownJob?.cancel()
        sleepTimerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
