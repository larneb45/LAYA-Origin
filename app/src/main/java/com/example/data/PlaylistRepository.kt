package com.example.data

import android.content.Context
import android.util.Base64
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class PlaylistRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("laya_radio_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_REPO = "larneb45/LAYA-Origin"
        const val DEFAULT_BRANCH = "main"
        const val PREF_REPO = "pref_repo"
        const val PREF_BRANCH = "pref_branch"
        const val PREF_GITHUB_PAT = "pref_github_pat"
        const val PREF_CACHED_PLAYLIST = "cached_playlist_json"
    }

    var savedRepo: String
        get() = prefs.getString(PREF_REPO, DEFAULT_REPO) ?: DEFAULT_REPO
        set(value) = prefs.edit().putString(PREF_REPO, value).apply()

    var savedBranch: String
        get() = prefs.getString(PREF_BRANCH, DEFAULT_BRANCH) ?: DEFAULT_BRANCH
        set(value) = prefs.edit().putString(PREF_BRANCH, value).apply()

    var savedPat: String
        get() = prefs.getString(PREF_GITHUB_PAT, "") ?: ""
        set(value) = prefs.edit().putString(PREF_GITHUB_PAT, value).apply()

    /**
     * Charge la playlist : d'abord via l'URL 'raw' de GitHub,
     * puis fallback sur le cache local, puis sur l'asset embarqué.
     */
    suspend fun loadPlaylist(): List<Track> = withContext(Dispatchers.IO) {
        // 1. Essayer le GitHub RAW (branche main puis master)
        val rawUrls = listOf(
            "https://raw.githubusercontent.com/$savedRepo/$savedBranch/playlist.json?t=${System.currentTimeMillis()}",
            "https://raw.githubusercontent.com/$savedRepo/master/playlist.json?t=${System.currentTimeMillis()}"
        )

        for (url in rawUrls) {
            try {
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val parsed = parsePlaylistJson(body)
                            if (parsed.isNotEmpty()) {
                                saveCache(body)
                                return@withContext parsed
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to next fallback
            }
        }

        // 2. Fallback sur le cache enregistré
        val cached = prefs.getString(PREF_CACHED_PLAYLIST, null)
        if (!cached.isNullOrBlank()) {
            val parsed = parsePlaylistJson(cached)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        // 3. Fallback sur l'asset bundle playlist.json
        return@withContext loadFromAssets()
    }

    private fun loadFromAssets(): List<Track> {
        return try {
            context.assets.open("playlist.json").use { stream ->
                val reader = InputStreamReader(stream)
                val content = reader.readText()
                parsePlaylistJson(content)
            }
        } catch (e: Exception) {
            getDefaultSeedPlaylist()
        }
    }

    fun parsePlaylistJson(jsonString: String): List<Track> {
        val list = mutableListOf<Track>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optLong("id", (i + 1).toLong())
                val rawTitle = obj.optString("title").ifBlank {
                    obj.optString("titre", "Piste ${i + 1}")
                }
                val artist = obj.optString("artist", "LAYA Origin")
                val url = obj.optString("url", "")
                if (url.isNotBlank()) {
                    list.add(Track(id = id, title = rawTitle, titre = rawTitle, artist = artist, url = url))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveCache(jsonString: String) {
        prefs.edit().putString(PREF_CACHED_PLAYLIST, jsonString).apply()
    }

    /**
     * Récupère la playlist via l'API REST GitHub (avec le SHA pour les commits)
     */
    suspend fun fetchFromGithubApi(): Result<Pair<List<Track>, String?>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$savedRepo/contents/playlist.json?ref=$savedBranch"
            val reqBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")

            if (savedPat.isNotBlank()) {
                reqBuilder.header("Authorization", "token $savedPat")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Erreur GitHub HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Réponse vide"))
                val json = JSONObject(body)
                val sha = json.optString("sha")
                val base64Content = json.optString("content").replace("\n", "").replace("\r", "")
                val decodedBytes = Base64.decode(base64Content, Base64.DEFAULT)
                val jsonContent = String(decodedBytes, Charsets.UTF_8)
                val tracks = parsePlaylistJson(jsonContent)
                saveCache(jsonContent)
                Result.success(Pair(tracks, sha))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Commite la nouvelle playlist sur GitHub via l'API REST (Architecture GitOps)
     */
    suspend fun commitPlaylistToGithub(tracks: List<Track>, currentSha: String?): Result<String> = withContext(Dispatchers.IO) {
        if (savedPat.isBlank()) {
            return@withContext Result.failure(Exception("GitHub Personal Access Token (PAT) manquant."))
        }

        try {
            val jsonArray = JSONArray()
            tracks.forEach { track ->
                val obj = JSONObject().apply {
                    put("id", track.id)
                    put("title", track.displayTitle)
                    put("artist", track.displayArtist)
                    put("url", track.url)
                }
                jsonArray.put(obj)
            }
            val jsonString = jsonArray.toString(2)
            val base64Encoded = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val payload = JSONObject().apply {
                put("message", "feat(radio): synchronisation playlist LAYA Origin (${tracks.size} pistes)")
                put("content", base64Encoded)
                put("branch", savedBranch)
                if (!currentSha.isNullOrBlank()) {
                    put("sha", currentSha)
                }
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)

            val url = "https://api.github.com/repos/$savedRepo/contents/playlist.json"
            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .header("Authorization", "token $savedPat")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    saveCache(jsonString)
                    val respBody = response.body?.string() ?: ""
                    val commitSha = try {
                        JSONObject(respBody).getJSONObject("commit").optString("sha", "")
                    } catch (e: Exception) { "" }
                    Result.success(commitSha.take(7))
                } else {
                    val err = response.body?.string() ?: response.message
                    Result.failure(Exception("Échec GitHub (${response.code}) : $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDefaultSeedPlaylist(): List<Track> {
        return listOf(
            Track(1, "Midnight Reflections", "Midnight Reflections", "LAYA Origin", "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3"),
            Track(2, "Zen Garden Dawn", "Zen Garden Dawn", "LAYA Origin", "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3"),
            Track(3, "Solar Wind Echoes", "Solar Wind Echoes", "LAYA Origin", "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3"),
            Track(4, "Soft Rain Memories", "Soft Rain Memories", "LAYA Origin", "https://cdn.pixabay.com/download/audio/2021/08/04/audio_12b0c7443c.mp3"),
            Track(5, "Cosmic Lofi Drift", "Cosmic Lofi Drift", "LAYA Origin", "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f792cb.mp3")
        )
    }
}
