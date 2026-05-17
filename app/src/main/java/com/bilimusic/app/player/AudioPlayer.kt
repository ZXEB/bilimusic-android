package com.bilimusic.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

class AudioPlayer(
    private val exoPlayer: ExoPlayer,
    private val context: Context
) {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val playlistManager = PlaylistManager()
    private var positionJob: Job? = null
    private var serviceStarted = false

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val playbackState = when (state) {
                    Player.STATE_IDLE -> PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                    Player.STATE_READY -> {
                        val duration = exoPlayer.duration.coerceAtLeast(0)
                        _state.value = _state.value.copy(duration = duration)
                        if (exoPlayer.playWhenReady) PlaybackState.PLAYING else PlaybackState.PAUSED
                    }
                    Player.STATE_ENDED -> {
                        stopPositionTracking()
                        PlaybackState.ENDED
                    }
                    else -> PlaybackState.IDLE
                }
                _state.value = _state.value.copy(playbackState = playbackState)
                if (playbackState == PlaybackState.PLAYING) startPositionTracking()
                else stopPositionTracking()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(
                    playbackState = when {
                        isPlaying -> PlaybackState.PLAYING
                        exoPlayer.playbackState != Player.STATE_ENDED -> PlaybackState.PAUSED
                        else -> PlaybackState.ENDED
                    }
                )
                if (isPlaying) startPositionTracking()
                else stopPositionTracking()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exoPlayer.currentMediaItemIndex
                if (index in playlistManager.queue.indices) {
                    playlistManager.currentIndex = index
                    val song = playlistManager.queue[index]
                    _state.value = _state.value.copy(
                        currentSong = song,
                        currentIndex = index
                    )
                }
            }
        })
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
        applyRepeatMode(next)
    }

    private fun applyRepeatMode(mode: RepeatMode) {
        when (mode) {
            RepeatMode.OFF -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.shuffleModeEnabled = false
            }
            RepeatMode.ONE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.shuffleModeEnabled = false
            }
            RepeatMode.ALL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                exoPlayer.shuffleModeEnabled = _state.value.shuffleMode
            }
        }
        _state.value = _state.value.copy(repeatMode = mode)
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.shuffleMode
        exoPlayer.shuffleModeEnabled = newShuffle
        _state.value = _state.value.copy(shuffleMode = newShuffle)
    }

    private fun toMediaItem(song: Song): MediaItem {
        return MediaItem.Builder()
            .setMediaId(song.bvid)
            .setUri(song.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.author)
                    .setArtworkUri(Uri.parse(song.cover))
                    .build()
            )
            .build()
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        startService()
        playlistManager.setQueue(songs, startIndex)
        val mediaItems = songs.map { toMediaItem(it) }
        exoPlayer.apply {
            setMediaItems(mediaItems, startIndex, 0L)
            prepare()
        }
        _state.value = _state.value.copy(
            queue = songs,
            currentIndex = startIndex,
            currentSong = songs.getOrNull(startIndex)
        )
    }

    fun play() {
        startService()
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) pause() else play()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _state.value = _state.value.copy(position = position)
    }

    fun skipToNext() {
        val nextIndex = playlistManager.nextIndex()
        if (nextIndex >= 0) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val prevIndex = playlistManager.previousIndex()
        if (prevIndex >= 0) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun addToQueue(song: Song) {
        startService()
        playlistManager.add(song)
        exoPlayer.addMediaItem(toMediaItem(song))
        _state.value = _state.value.copy(queue = playlistManager.queue)
    }

    fun removeFromQueue(index: Int) {
        playlistManager.remove(index)
        exoPlayer.removeMediaItem(index)
        _state.value = _state.value.copy(queue = playlistManager.queue)
    }

    fun clearQueue() {
        playlistManager.clear()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _state.value = PlayerState()
    }

    fun release() {
        exoPlayer.release()
    }

    fun stop() {
        exoPlayer.stop()
    }

    private fun startService() {
        if (serviceStarted) return
        serviceStarted = true
        val intent = Intent(context, AudioPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private val trackScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun startPositionTracking() {
        if (positionJob?.isActive == true) return
        positionJob = trackScope.launch {
            while (isActive) {
                _state.value = _state.value.copy(
                    position = exoPlayer.currentPosition.coerceAtLeast(0)
                )
                delay(250)
            }
        }
    }

    private fun stopPositionTracking() {
        positionJob?.cancel()
        positionJob = null
    }
}
