package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PlaylistRepository
import com.example.model.Track
import com.example.player.PlayerUiState
import com.example.player.RadioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val repo: String = "",
    val branch: String = "main",
    val pat: String = "",
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val currentSha: String? = null
)

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository(application)
    val playerState: StateFlow<PlayerUiState> = RadioPlayerManager.state

    private val _adminState = MutableStateFlow(
        AdminUiState(
            repo = repository.savedRepo,
            branch = repository.savedBranch,
            pat = repository.savedPat
        )
    )
    val adminState: StateFlow<AdminUiState> = _adminState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        RadioPlayerManager.initialize(application)
        loadPlaylist(autoPlay = false)
    }

    fun loadPlaylist(autoPlay: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val tracks = repository.loadPlaylist()
                if (tracks.isNotEmpty()) {
                    RadioPlayerManager.setRawPlaylist(tracks, startPlaying = autoPlay)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun togglePlayPause() {
        RadioPlayerManager.togglePlayPause()
    }

    fun play() {
        RadioPlayerManager.play()
    }

    fun pause() {
        RadioPlayerManager.pause()
    }

    fun next() {
        RadioPlayerManager.next()
    }

    fun previous() {
        RadioPlayerManager.previous()
    }

    fun playTrack(index: Int) {
        RadioPlayerManager.playTrackAtIndex(index)
    }

    fun seekTo(positionMs: Long) {
        RadioPlayerManager.seekTo(positionMs)
    }

    fun setSleepTimer(minutes: Int) {
        RadioPlayerManager.setSleepTimer(minutes)
    }

    // --- Administration & GitHub GitOps ---

    fun updateGithubConfig(repo: String, branch: String, pat: String) {
        repository.savedRepo = repo.trim()
        repository.savedBranch = branch.trim()
        repository.savedPat = pat.trim()
        _adminState.update {
            it.copy(
                repo = repository.savedRepo,
                branch = repository.savedBranch,
                pat = repository.savedPat,
                statusMessage = "Configuration GitHub enregistrée localement.",
                isError = false
            )
        }
    }

    fun fetchFromGithubWithSha() {
        viewModelScope.launch {
            _adminState.update { it.copy(isSaving = true, statusMessage = "Chargement depuis GitHub...") }
            val result = repository.fetchFromGithubApi()
            result.fold(
                onSuccess = { (tracks, sha) ->
                    if (tracks.isNotEmpty()) {
                        RadioPlayerManager.setRawPlaylist(tracks, startPlaying = false)
                    }
                    _adminState.update {
                        it.copy(
                            isSaving = false,
                            currentSha = sha,
                            statusMessage = "${tracks.size} pistes chargées avec succès depuis GitHub !",
                            isError = false
                        )
                    }
                },
                onFailure = { err ->
                    _adminState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Erreur : ${err.message}",
                            isError = true
                        )
                    }
                }
            )
        }
    }

    fun addTrackAndCommit(title: String, artist: String, url: String) {
        if (title.isBlank() || url.isBlank()) {
            _adminState.update {
                it.copy(statusMessage = "Veuillez renseigner le titre et l'URL.", isError = true)
            }
            return
        }

        viewModelScope.launch {
            _adminState.update { it.copy(isSaving = true, statusMessage = "Commit vers GitHub en cours...") }

            val currentRaw = RadioPlayerManager.state.value.rawPlaylist.toMutableList()
            val newTrack = Track(
                id = System.currentTimeMillis(),
                title = title.trim(),
                titre = title.trim(),
                artist = if (artist.isNotBlank()) artist.trim() else "LAYA Origin",
                url = url.trim()
            )
            currentRaw.add(newTrack)

            val result = repository.commitPlaylistToGithub(currentRaw, _adminState.value.currentSha)
            result.fold(
                onSuccess = { commitSha ->
                    RadioPlayerManager.setRawPlaylist(currentRaw, startPlaying = false)
                    _adminState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Titre ajouté ! Commit GitHub GitOps réussi (sha: $commitSha).",
                            isError = false
                        )
                    }
                },
                onFailure = { err ->
                    _adminState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Erreur de commit GitHub : ${err.message}",
                            isError = true
                        )
                    }
                }
            )
        }
    }

    fun deleteTrackAndCommit(index: Int) {
        val currentRaw = RadioPlayerManager.state.value.rawPlaylist.toMutableList()
        if (index !in currentRaw.indices) return

        viewModelScope.launch {
            _adminState.update { it.copy(isSaving = true, statusMessage = "Suppression et commit GitHub...") }
            currentRaw.removeAt(index)

            val result = repository.commitPlaylistToGithub(currentRaw, _adminState.value.currentSha)
            result.fold(
                onSuccess = { commitSha ->
                    RadioPlayerManager.setRawPlaylist(currentRaw, startPlaying = false)
                    _adminState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Titre supprimé. Commit GitHub réussi (sha: $commitSha).",
                            isError = false
                        )
                    }
                },
                onFailure = { err ->
                    _adminState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Erreur lors de la suppression : ${err.message}",
                            isError = true
                        )
                    }
                }
            )
        }
    }

    fun clearAdminStatus() {
        _adminState.update { it.copy(statusMessage = null) }
    }
}
