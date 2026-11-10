package com.music.bitchord.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What the "now playing" notification shows, and when. Saved with the other desktop preferences. */
internal object DesktopNotificationSettings {
    private const val KEY_ENABLED = "notify_track_enabled"
    private const val KEY_ONLY_UNFOCUSED = "notify_track_only_unfocused"
    private const val KEY_ARTIST = "notify_track_artist"
    private const val KEY_ALBUM = "notify_track_album"
    private const val KEY_COVER = "notify_track_cover"

    private val persistence = DesktopPersistence()

    private val _enabled = MutableStateFlow(persistence.boolean(KEY_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled

    private val _onlyWhenUnfocused = MutableStateFlow(persistence.boolean(KEY_ONLY_UNFOCUSED, true))
    val onlyWhenUnfocused: StateFlow<Boolean> = _onlyWhenUnfocused

    private val _showArtist = MutableStateFlow(persistence.boolean(KEY_ARTIST, true))
    val showArtist: StateFlow<Boolean> = _showArtist

    private val _showAlbum = MutableStateFlow(persistence.boolean(KEY_ALBUM, false))
    val showAlbum: StateFlow<Boolean> = _showAlbum

    private val _showCover = MutableStateFlow(persistence.boolean(KEY_COVER, true))
    val showCover: StateFlow<Boolean> = _showCover

    fun setEnabled(value: Boolean) { _enabled.value = value; persistence.saveBoolean(KEY_ENABLED, value) }
    fun setOnlyWhenUnfocused(value: Boolean) { _onlyWhenUnfocused.value = value; persistence.saveBoolean(KEY_ONLY_UNFOCUSED, value) }
    fun setShowArtist(value: Boolean) { _showArtist.value = value; persistence.saveBoolean(KEY_ARTIST, value) }
    fun setShowAlbum(value: Boolean) { _showAlbum.value = value; persistence.saveBoolean(KEY_ALBUM, value) }
    fun setShowCover(value: Boolean) { _showCover.value = value; persistence.saveBoolean(KEY_COVER, value) }
}
