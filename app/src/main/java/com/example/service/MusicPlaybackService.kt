package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState as SystemPlaybackState
import android.os.Build
import android.os.IBinder
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioPlaybackController

/**
 * Foreground Media Service providing a full Spotify-style Android MediaSession widget
 * in the Notification Shade & Lock Screen with album cover art, interactive seekbar,
 * and Previous / Play-Pause / Next transport buttons.
 */
class MusicPlaybackService : Service() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaSession()
    }

    private fun initMediaSession() {
        val controller = AudioPlaybackController.getInstance(applicationContext)
        val session = MediaSession(this, "AURA_Music_MediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    controller.setPlaying(true)
                }

                override fun onPause() {
                    controller.setPlaying(false)
                }

                override fun onSkipToNext() {
                    controller.skipNext()
                }

                override fun onSkipToPrevious() {
                    controller.skipPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    controller.seekTo(pos)
                }

                override fun onStop() {
                    controller.setPlaying(false)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }
        mediaSession = session
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controller = AudioPlaybackController.getInstance(applicationContext)
        when (intent?.action) {
            ACTION_TOGGLE_PLAY -> controller.togglePlayPause()
            ACTION_NEXT -> controller.skipNext()
            ACTION_PREV -> controller.skipPrevious()
            ACTION_STOP -> {
                controller.setPlaying(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        updateMediaSessionState(controller)
        val notification = buildMediaStyleNotification(controller)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
        }

        return START_STICKY
    }

    private fun updateMediaSessionState(controller: AudioPlaybackController) {
        val session = mediaSession ?: return
        val state = controller.playbackState.value
        val track = state.currentTrack

        val playbackStateCode = if (state.isPlaying) {
            SystemPlaybackState.STATE_PLAYING
        } else {
            SystemPlaybackState.STATE_PAUSED
        }

        val actions = SystemPlaybackState.ACTION_PLAY or
            SystemPlaybackState.ACTION_PAUSE or
            SystemPlaybackState.ACTION_PLAY_PAUSE or
            SystemPlaybackState.ACTION_SKIP_TO_NEXT or
            SystemPlaybackState.ACTION_SKIP_TO_PREVIOUS or
            SystemPlaybackState.ACTION_SEEK_TO or
            SystemPlaybackState.ACTION_STOP

        val sysPlaybackState = SystemPlaybackState.Builder()
            .setActions(actions)
            .setState(playbackStateCode, state.currentPositionMs, state.playbackSpeed)
            .build()

        session.setPlaybackState(sysPlaybackState)

        if (track != null) {
            val metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs.coerceAtLeast(1000L))

            state.currentCoverBitmap?.let { bmp ->
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bmp)
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bmp)
            }
            session.setMetadata(metadataBuilder.build())
        }
    }

    private fun buildMediaStyleNotification(controller: AudioPlaybackController): Notification {
        val state = controller.playbackState.value
        val eqState = controller.equalizerState.value
        val track = state.currentTrack

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_TOGGLE_PLAY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = track?.title ?: getString(R.string.app_name)
        val artist = track?.artist ?: "Локальный аудиоплеер"
        val subText = "EQ: ${eqState.activePresetName}"

        val playPauseLabel = if (state.isPlaying) "Пауза" else "Играть"
        val playPauseIconRes = if (state.isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_previous),
            "Назад",
            prevPendingIntent
        ).build()

        val playPauseAction = Notification.Action.Builder(
            Icon.createWithResource(this, playPauseIconRes),
            playPauseLabel,
            playPausePendingIntent
        ).build()

        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_next),
            "Далее",
            nextPendingIntent
        ).build()

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(subText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(state.isPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)

        state.currentCoverBitmap?.let { bmp ->
            builder.setLargeIcon(bmp)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Exception) {
        }
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "aura_music_playback_channel"
        const val NOTIFICATION_ID = 4041
        const val ACTION_TOGGLE_PLAY = "com.example.action.TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_STOP = "com.example.action.STOP"

        fun startOrUpdateService(context: Context) {
            try {
                val intent = Intent(context, MusicPlaybackService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }
    }
}
