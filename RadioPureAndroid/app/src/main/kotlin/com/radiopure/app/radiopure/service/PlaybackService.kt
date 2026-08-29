package com.radiopure.app.radiopure.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.radiopure.app.radiopure.MainActivity
import com.radiopure.app.radiopure.R
import com.radiopure.app.radiopure.player.RadioPlayer

/**
 * 由 [MediaController] bind 拉起，不要从 Activity 调用 startForegroundService。
 * Media3 会在真正开始播放时自行进入前台并展示通知。
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    companion object {
        private const val STREAM_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

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
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.playback_channel_name)
                .build(),
        )
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(STREAM_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(8_000)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer = player
        radioPlayer = RadioPlayer(player)
        val sessionActivity = PendingIntent.getActivity(
            this,
            /* requestCode = */ 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
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
