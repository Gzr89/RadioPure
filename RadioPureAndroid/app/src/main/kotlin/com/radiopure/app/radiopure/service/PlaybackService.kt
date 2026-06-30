package com.radiopure.app.radiopure.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.radiopure.app.radiopure.player.RadioPlayer

class PlaybackService : MediaSessionService() {

    companion object {
        @Volatile
        var instance: PlaybackService? = null
            private set
    }

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    var radioPlayer: RadioPlayer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer = player
        radioPlayer = RadioPlayer(player)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        radioPlayer?.detach()
        radioPlayer = null
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        instance = null
        super.onDestroy()
    }
}
