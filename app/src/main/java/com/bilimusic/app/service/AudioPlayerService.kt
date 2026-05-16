package com.bilimusic.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.bilimusic.app.MainActivity
import com.bilimusic.app.api.BiliOkHttp
import com.bilimusic.app.player.PlaybackState
import com.bilimusic.app.player.PlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class AudioPlayerService : Service() {

    companion object {
        private const val CHANNEL_ID = "bilimusic_playback"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_PREV = "com.bilimusic.app.action.PREV"
        private const val ACTION_PLAY = "com.bilimusic.app.action.PLAY"
        private const val ACTION_PAUSE = "com.bilimusic.app.action.PAUSE"
        private const val ACTION_NEXT = "com.bilimusic.app.action.NEXT"

        var instance: AudioPlayerService? = null
            private set
    }

    private var notificationManager: NotificationManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _coverBitmap = MutableStateFlow<Bitmap?>(null)
    private var currentCoverUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()

        startForegroundWithNotification(
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("BiliMusic")
                .setContentText("准备播放")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )

        mediaSession = MediaSessionCompat(this, "BiliMusic").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    PlayerManager.play()
                }
                override fun onPause() {
                    PlayerManager.togglePlayPause()
                }
                override fun onSkipToNext() {
                    PlayerManager.skipToNext()
                }
                override fun onSkipToPrevious() {
                    PlayerManager.skipToPrevious()
                }
                override fun onSeekTo(pos: Long) {
                    PlayerManager.seekTo(pos)
                }
                override fun onStop() {
                    PlayerManager.release()
                    stopSelf()
                }
            })
            isActive = true
        }

        scope.launch {
            combine(
                PlayerManager.state,
                _coverBitmap
            ) { state, bitmap -> Pair(state, bitmap) }
            .collectLatest { (state, bitmap) ->
                updateMediaSession(state, bitmap)
                updateNotification(state, bitmap)
            }
        }

        scope.launch {
            PlayerManager.state.collectLatest { state ->
                val song = state.currentSong
                if (song != null && song.cover.isNotEmpty() && song.cover != currentCoverUrl) {
                    currentCoverUrl = song.cover
                    _coverBitmap.value?.recycle()
                    _coverBitmap.value = null
                    val bitmap = loadCoverBitmap(song.cover)
                    if (bitmap != null) {
                        _coverBitmap.value = bitmap
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREV -> PlayerManager.skipToPrevious()
            ACTION_PLAY -> PlayerManager.play()
            ACTION_PAUSE -> PlayerManager.togglePlayPause()
            ACTION_NEXT -> PlayerManager.skipToNext()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (instance === this) instance = null
        scope.cancel()
        mediaSession?.release()
        mediaSession = null
        _coverBitmap.value?.recycle()
        _coverBitmap.value = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = PlayerManager.player
        if (player == null || !player.playWhenReady) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    private suspend fun loadCoverBitmap(coverUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(coverUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://www.bilibili.com")
                .build()
            BiliOkHttp.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes() ?: return@use null
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    val maxDim = maxOf(opts.outWidth, opts.outHeight)
                    var sampleSize = 1
                    while (sampleSize * 2 < maxDim / 300) sampleSize *= 2
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun updateMediaSession(
        state: com.bilimusic.app.player.PlayerState,
        coverBitmap: Bitmap?
    ) {
        val session = mediaSession ?: return
        val song = state.currentSong

        if (song != null) {
            val metaBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.author)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Bilibili")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration * 1000L)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.cover)

            if (coverBitmap != null) {
                metaBuilder.putBitmap("android.media.metadata.ARTWORK", coverBitmap)
            }

            session.setMetadata(metaBuilder.build())
        }

        val playState = when (state.playbackState) {
            PlaybackState.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            PlaybackState.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            PlaybackState.BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            PlaybackState.ENDED -> PlaybackStateCompat.STATE_STOPPED
            else -> PlaybackStateCompat.STATE_NONE
        }

        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(playState, state.position, 1f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_STOP
                )
                .build()
        )
    }

    private fun updateNotification(
        state: com.bilimusic.app.player.PlayerState,
        coverBitmap: Bitmap?
    ) {
        val song = state.currentSong
        val isPlaying = state.playbackState == PlaybackState.PLAYING

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this, 1, Intent(this, AudioPlayerService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playIntent = PendingIntent.getService(
            this, 2, Intent(this, AudioPlayerService::class.java).apply { action = ACTION_PLAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseIntent = PendingIntent.getService(
            this, 3, Intent(this, AudioPlayerService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 4, Intent(this, AudioPlayerService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 3)
            )
            .setContentTitle(song?.title ?: "BiliMusic")
            .setContentText(song?.author ?: "音乐播放中")
            .setSubText(song?.partTitle?.takeIf { it.isNotEmpty() })
            .setLargeIcon(coverBitmap)
            .addAction(android.R.drawable.ic_media_previous, "上一首", prevIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                if (isPlaying) "暂停" else "播放",
                if (isPlaying) pauseIntent else playIntent
            )
            .addAction(android.R.drawable.ic_media_next, "下一首", nextIntent)
            .build()

        startForegroundWithNotification(notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "音乐播放", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "BiliMusic 后台播放控制"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}
