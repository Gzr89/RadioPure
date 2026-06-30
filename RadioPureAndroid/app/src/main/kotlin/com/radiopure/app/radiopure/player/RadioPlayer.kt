package com.radiopure.app.radiopure.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.radiopure.app.radiopure.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 播放逻辑与 [RadioPureShared/RadioPlayer.swift] 对齐。
 */
class RadioPlayer(
    private val exoPlayer: ExoPlayer,
) {
    private val _state = MutableStateFlow(RadioPlayerState())
    val state: StateFlow<RadioPlayerState> = _state.asStateFlow()

    private var resolvedStreamURLString: String? = null
    private var didTryFallbackForCurrentStation = false
    private var pendingStationForFallback: RadioStation? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying && exoPlayer.playbackState != Player.STATE_IDLE) }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val station = pendingStationForFallback ?: _state.value.currentStation ?: return
            handlePlaybackFailure(station)
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        exoPlayer.volume = _state.value.volume
    }

    fun play(station: RadioStation) {
        val current = _state.value
        if (current.currentStation?.id == station.id) {
            if (current.isPlaying) pause() else resume()
            return
        }

        stopAndRelease(resettingFallback = true)
        _state.update {
            it.copy(
                currentStation = station,
                isError = false,
                isPlaying = false,
            )
        }
        val primary = streamingURL(station, primary = true) ?: return
        resolvedStreamURLString = primary
        attachPlayer(station, Uri.parse(primary))
    }

    fun pause() {
        exoPlayer.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        exoPlayer.play()
        _state.update { it.copy(isPlaying = true, isError = false) }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else resume()
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clamped
        _state.update { it.copy(volume = clamped) }
    }

    fun detach() {
        exoPlayer.removeListener(playerListener)
        stopAndRelease(resettingFallback = true)
    }

    private fun streamingURL(station: RadioStation, primary: Boolean): String? {
        return if (primary) station.url else station.fallbackURL
    }

    private fun attachPlayer(station: RadioStation, streamUri: Uri) {
        pendingStationForFallback = station
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist("RadioPure")
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(streamUri)
            .setMediaMetadata(metadata)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        _state.update { it.copy(isPlaying = true, isError = false) }
    }

    private fun handlePlaybackFailure(station: RadioStation) {
        val fallback = station.fallbackURL
        if (
            fallback != null &&
            !didTryFallbackForCurrentStation &&
            resolvedStreamURLString != fallback
        ) {
            didTryFallbackForCurrentStation = true
            _state.update { it.copy(isError = false) }
            resolvedStreamURLString = fallback
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            attachPlayer(station, Uri.parse(fallback))
            return
        }

        _state.update { it.copy(isPlaying = false, isError = true) }
        pendingStationForFallback = null
    }

    private fun stopAndRelease(resettingFallback: Boolean) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _state.update { it.copy(isPlaying = false, isError = false) }
        resolvedStreamURLString = null
        if (resettingFallback) {
            didTryFallbackForCurrentStation = false
            pendingStationForFallback = null
        }
    }
}
