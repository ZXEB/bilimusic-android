package com.bilimusic.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.bilimusic.app.api.BiliOkHttp
import com.bilimusic.app.player.model.PlaybackEqualizerPresetId
import com.bilimusic.app.util.DebugLog
import com.bilimusic.app.player.model.PlaybackSoundConfig
import com.bilimusic.app.player.model.PlaybackSoundState
import com.bilimusic.app.player.model.decodePlaybackEqualizerBandLevels
import com.bilimusic.app.player.model.encodePlaybackEqualizerBandLevels
import com.bilimusic.app.service.AudioPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class PlaybackState { IDLE, BUFFERING, PLAYING, PAUSED, ENDED }
enum class RepeatMode { OFF, ONE, ALL }

data class PlayerState(
    val currentSong: Song? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isFavorite: Boolean = false,
    val sleepTimerEndTime: Long? = null
)

data class PlayHistoryItem(
    val song: Song,
    val timestamp: Long
)

object PlayerManager {
    var player: ExoPlayer? = null
        private set
    private var cache: SimpleCache? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _playHistory = MutableStateFlow<List<PlayHistoryItem>>(emptyList())
    val playHistory: StateFlow<List<PlayHistoryItem>> = _playHistory.asStateFlow()

    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var serviceStarted = false

    private val effectsController = PlaybackEffectsController()
    private val _playbackSoundState = MutableStateFlow(PlaybackSoundState())
    val playbackSoundState: StateFlow<PlaybackSoundState> = _playbackSoundState.asStateFlow()

    var onSongChanged: ((Song) -> Unit)? = null
    var onPlaybackChanged: (() -> Unit)? = null
    var onResolveAudioUrl: (suspend (Song) -> String?)? = null

    fun initialize(context: Context) {
        if (player != null) return

        val httpDataSource = OkHttpDataSource.Factory(
            okhttp3.Call.Factory { request ->
                BiliOkHttp.client.newCall(
                    request.newBuilder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .addHeader("Referer", "https://www.bilibili.com")
                        .build()
                )
            }
        )

        val cacheDir = File(context.cacheDir, "bilimusic_cache")
        cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(256L * 1024 * 1024))

        val cacheDataSource = CacheDataSource.Factory()
            .setCache(cache!!)
            .setUpstreamDataSourceFactory(httpDataSource)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSource))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val ps = when (state) {
                    Player.STATE_IDLE -> PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                    Player.STATE_READY -> {
                        _state.value = _state.value.copy(duration = p.duration.coerceAtLeast(0))
                        if (p.playWhenReady) PlaybackState.PLAYING else PlaybackState.PAUSED
                    }
                    Player.STATE_ENDED -> PlaybackState.ENDED
                    else -> PlaybackState.IDLE
                }
                _state.value = _state.value.copy(playbackState = ps)
                if (ps == PlaybackState.PLAYING) startPositionTracking()
                else stopPositionTracking()
                onPlaybackChanged?.invoke()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(
                    playbackState = when {
                        isPlaying -> PlaybackState.PLAYING
                        p.playbackState != Player.STATE_ENDED -> PlaybackState.PAUSED
                        else -> PlaybackState.ENDED
                    }
                )
                if (isPlaying) startPositionTracking()
                else stopPositionTracking()
                onPlaybackChanged?.invoke()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = p.currentMediaItemIndex
                val queue = _state.value.queue
                if (index in queue.indices) {
                    val song = queue[index]
                    _state.value = _state.value.copy(
                        currentSong = song,
                        currentIndex = index
                    )
                    if (song.audioUrl.isEmpty()) {
                        resolveAndPlay(index, song)
                    } else {
                        recordPlayHistory(song)
                        onSongChanged?.invoke(song)
                        onPlaybackChanged?.invoke()
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                DebugLog.e("Player error [${error.errorCodeName}]: ${error.message}", error)
                super.onPlayerError(error)
            }
        })

        var lastAudioSessionId = p.audioSessionId
        p.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != lastAudioSessionId && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    lastAudioSessionId = audioSessionId
                    _playbackSoundState.value = effectsController.onAudioSessionIdChanged(audioSessionId)
                }
            }
        })

        _playbackSoundState.value = effectsController.attachPlayer(p)
        player = p
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        val p = player ?: return
        startService()
        val mediaItems = songs.map { toMediaItem(it) }
        p.setMediaItems(mediaItems, startIndex, 0L)
        p.prepare()
        val song = songs.getOrNull(startIndex)
        _state.value = _state.value.copy(
            queue = songs,
            currentIndex = startIndex,
            currentSong = song
        )
        if (song != null) {
            recordPlayHistory(song)
            onSongChanged?.invoke(song)
        }
    }

    fun playSong(song: Song) {
        DebugLog.i("playSong: ${song.bvid} ${song.title} url=${song.audioUrl.take(80)}...")
        setQueue(listOf(song), 0)
        play()
    }

    fun play() {
        val p = player ?: return
        startService()
        p.play()
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else { startService(); p.play() }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
        _state.value = _state.value.copy(position = position)
    }

    fun skipToNext() {
        val p = player ?: return
        val queue = _state.value.queue
        val current = p.currentMediaItemIndex
        if (current >= queue.size - 1 && queue.isNotEmpty()) {
            p.seekTo(0, 0L)
            p.prepare()
            p.play()
        } else {
            p.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val p = player ?: return
        val current = p.currentMediaItemIndex
        if (current <= 0 && _state.value.queue.isNotEmpty()) {
            p.seekTo(_state.value.queue.size - 1, 0L)
            p.prepare()
            p.play()
        } else {
            p.seekToPreviousMediaItem()
        }
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.OFF -> { player?.repeatMode = Player.REPEAT_MODE_ONE; RepeatMode.ONE }
            RepeatMode.ONE -> { player?.repeatMode = Player.REPEAT_MODE_ALL; RepeatMode.ALL }
            RepeatMode.ALL -> { player?.repeatMode = Player.REPEAT_MODE_OFF; RepeatMode.OFF }
        }
        _state.value = _state.value.copy(repeatMode = next)
    }

    fun toggleShuffle() {
        val new = !_state.value.shuffleMode
        player?.shuffleModeEnabled = new
        _state.value = _state.value.copy(shuffleMode = new)
    }

    fun addToQueue(song: Song) {
        val p = player ?: return
        startService()
        p.addMediaItem(toMediaItem(song))
        _state.value = _state.value.copy(queue = _state.value.queue + song)
    }

    fun removeFromQueue(index: Int) {
        player?.removeMediaItem(index)
        val queue = _state.value.queue.toMutableList()
        if (index in queue.indices) queue.removeAt(index)
        _state.value = _state.value.copy(queue = queue)
    }

    fun clearQueue() {
        player?.stop()
        player?.clearMediaItems()
        _state.value = PlayerState()
    }

    fun release() {
        _playbackSoundState.value = effectsController.release()
        player?.release()
        player = null
        cache?.release()
        cache = null
    }

    fun clearCache(context: Context) {
        cache?.release()
        cache = null
        val cacheDir = File(context.cacheDir, "bilimusic_cache")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
        cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(256L * 1024 * 1024))
    }

    fun updatePlaybackSoundConfig(config: PlaybackSoundConfig) {
        _playbackSoundState.value = effectsController.updateConfig(config)
    }

    fun setPlaybackSpeed(speed: Float) {
        val current = _playbackSoundState.value
        val newConfig = PlaybackSoundConfig(
            speed = speed,
            pitch = current.pitch,
            loudnessGainMb = current.loudnessGainMb,
            equalizerEnabled = current.equalizerEnabled,
            presetId = current.presetId
        )
        _playbackSoundState.value = effectsController.updateConfig(newConfig)
    }

    fun setPlaybackPitch(pitch: Float) {
        val current = _playbackSoundState.value
        val newConfig = PlaybackSoundConfig(
            speed = current.speed,
            pitch = pitch,
            loudnessGainMb = current.loudnessGainMb,
            equalizerEnabled = current.equalizerEnabled,
            presetId = current.presetId
        )
        _playbackSoundState.value = effectsController.updateConfig(newConfig)
    }

    fun setPlaybackLoudnessGain(gainMb: Int) {
        val current = _playbackSoundState.value
        val newConfig = PlaybackSoundConfig(
            speed = current.speed,
            pitch = current.pitch,
            loudnessGainMb = gainMb,
            equalizerEnabled = current.equalizerEnabled,
            presetId = current.presetId
        )
        _playbackSoundState.value = effectsController.updateConfig(newConfig)
    }

    fun setPlaybackEqualizerEnabled(enabled: Boolean) {
        val current = _playbackSoundState.value
        val bandsEncoded = current.bands.joinToString(",") { it.levelMb.toString() }
        val newConfig = PlaybackSoundConfig(
            speed = current.speed,
            pitch = current.pitch,
            loudnessGainMb = current.loudnessGainMb,
            equalizerEnabled = enabled,
            presetId = current.presetId,
            customBandLevelsMb = decodePlaybackEqualizerBandLevels(bandsEncoded)
        )
        _playbackSoundState.value = effectsController.updateConfig(newConfig)
    }

    fun selectPlaybackEqualizerPreset(presetId: String) {
        val current = _playbackSoundState.value
        val newConfig = PlaybackSoundConfig(
            speed = current.speed,
            pitch = current.pitch,
            loudnessGainMb = current.loudnessGainMb,
            equalizerEnabled = current.equalizerEnabled,
            presetId = presetId
        )
        _playbackSoundState.value = effectsController.updateConfig(newConfig)
    }

    fun updatePlaybackEqualizerBandLevel(index: Int, levelMb: Int) {
        val current = _playbackSoundState.value
        val customLevels = current.bands.map { it.levelMb }.toMutableList()
        if (index in customLevels.indices) {
            customLevels[index] = levelMb
        }
        val newConfig = PlaybackSoundConfig(
            speed = current.speed,
            pitch = current.pitch,
            loudnessGainMb = current.loudnessGainMb,
            equalizerEnabled = current.equalizerEnabled,
            presetId = PlaybackEqualizerPresetId.CUSTOM,
            customBandLevelsMb = customLevels
        )
        _playbackSoundState.value = effectsController.updateConfig(newConfig)
    }

    fun resetPlaybackSoundSettings() {
        val newConfig = PlaybackSoundConfig()
        _playbackSoundState.value = effectsController.updateConfig(newConfig)
    }

    fun setSleepTimer(minutes: Int) {
        val endTime = System.currentTimeMillis() + minutes * 60_000L
        _state.value = _state.value.copy(sleepTimerEndTime = endTime)
        startSleepTimer()
    }

    fun cancelSleepTimer() {
        _state.value = _state.value.copy(sleepTimerEndTime = null)
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    fun getSleepTimerRemaining(): Long {
        val end = _state.value.sleepTimerEndTime ?: return 0L
        return (end - System.currentTimeMillis()).coerceAtLeast(0)
    }

    private fun startSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = scope.launch {
            while (isActive) {
                val remaining = getSleepTimerRemaining()
                if (remaining <= 0) {
                    player?.pause()
                    _state.value = _state.value.copy(sleepTimerEndTime = null)
                    break
                }
                delay(1000)
            }
        }
    }

    private fun recordPlayHistory(song: Song) {
        val item = PlayHistoryItem(song = song, timestamp = System.currentTimeMillis())
        val current = _playHistory.value.toMutableList()
        current.removeAll { it.song.bvid == song.bvid }
        current.add(0, item)
        if (current.size > 200) current.removeAt(current.lastIndex)
        _playHistory.value = current
    }

    fun loadPlayHistory(items: List<PlayHistoryItem>) {
        _playHistory.value = items
    }

    private fun toMediaItem(song: Song): MediaItem {
        return MediaItem.Builder()
            .setMediaId(song.bvid)
            .setUri(song.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.author)
                    .setAlbumTitle("Bilibili")
                    .setArtworkUri(Uri.parse(song.cover))
                    .build()
            )
            .build()
    }

    private fun startService() {
        if (serviceStarted) return
        serviceStarted = true
        val ctx = com.bilimusic.app.BiliMusicApp.instance
        val intent = Intent(ctx, AudioPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    private fun startPositionTracking() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                _state.value = _state.value.copy(
                    position = (player?.currentPosition ?: 0L).coerceAtLeast(0)
                )
                delay(500)
            }
        }
    }

    private fun stopPositionTracking() {
        positionJob?.cancel()
        positionJob = null
    }

    fun replaceMediaItem(index: Int, song: Song) {
        val p = player ?: return
        val current = _state.value.currentIndex
        if (index == current) {
            val mediaItem = toMediaItem(song)
            p.replaceMediaItem(index, mediaItem)
            p.seekTo(index, 0L)
            p.prepare()
        } else {
            p.replaceMediaItem(index, toMediaItem(song))
        }
        val queue = _state.value.queue.toMutableList()
        if (index in queue.indices) {
            queue[index] = song
        }
        _state.value = _state.value.copy(queue = queue)
    }

    private fun resolveAndPlay(index: Int, song: Song) {
        scope.launch {
            val url = onResolveAudioUrl?.invoke(song)
            if (url != null) {
                val resolved = song.copy(audioUrl = url)
                replaceMediaItem(index, resolved)
                player?.play()
            } else {
                DebugLog.e("resolveAndPlay: failed to resolve audio URL for ${song.bvid} ${song.title}")
            }
        }
    }
}
