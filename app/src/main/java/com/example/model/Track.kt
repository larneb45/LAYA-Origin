package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Track(
    @Json(name = "id") val id: Long = System.currentTimeMillis(),
    @Json(name = "title") val title: String? = null,
    @Json(name = "titre") val titre: String? = null,
    @Json(name = "artist") val artist: String? = "LAYA Origin",
    @Json(name = "url") val url: String = ""
) {
    val displayTitle: String
        get() = when {
            !title.isNullOrBlank() -> title
            !titre.isNullOrBlank() -> titre
            else -> "Sans titre"
        }

    val displayArtist: String
        get() = if (!artist.isNullOrBlank()) artist else "LAYA Origin"
}
