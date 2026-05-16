package com.bilimusic.app.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AddAlarm

import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bilimusic.app.player.LyricEntry
import com.bilimusic.app.player.PlayerManager
import com.bilimusic.app.player.RepeatMode
import com.bilimusic.app.player.model.DEFAULT_PLAYBACK_LOUDNESS_GAIN_MB
import com.bilimusic.app.player.model.DEFAULT_PLAYBACK_PITCH
import com.bilimusic.app.player.model.DEFAULT_PLAYBACK_SPEED
import com.bilimusic.app.player.model.PlaybackEqualizerPresetId
import com.bilimusic.app.player.model.encodePlaybackEqualizerBandLevels
import com.bilimusic.app.ui.component.PlaybackSoundSheet
import com.bilimusic.app.ui.component.SleepTimerSheet
import com.bilimusic.app.ui.util.rememberDominantColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onDismiss: () -> Unit
) {
    val state by PlayerManager.state.collectAsState()
    val song = state.currentSong ?: return
    val dominantColor by rememberDominantColor(song.cover)
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSoundSheet by remember { mutableStateOf(false) }
    val playbackSoundState by PlayerManager.playbackSoundState.collectAsState()
    val soundScope = rememberCoroutineScope()
    val app = com.bilimusic.app.BiliMusicApp.instance
    val soundPrefs = app.preferences
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember(song.bvid) { mutableStateOf<List<LyricEntry>>(emptyList()) }
    var isLoadingLyrics by remember(song.bvid) { mutableStateOf(false) }

    LaunchedEffect(song.bvid) {
        if (song.bvid.isNotEmpty()) {
            isLoadingLyrics = true
            val result = app.repository.getSubtitles(song.bvid)
            lyrics = result.getOrNull() ?: emptyList()
            isLoadingLyrics = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 封面取色背景：从主色渐变到黑
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dominantColor,
                                Color.Black
                            )
                        )
                    )
            )

            // 主内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "收起",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (showLyrics) {
                    LyricsView(
                        lyrics = lyrics,
                        isLoading = isLoadingLyrics,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showLyrics = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = song.cover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = song.author,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showLyrics) {
                    IconButton(onClick = { showLyrics = false }) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = "显示封面",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(if (showLyrics) 0f else 1f))

                ProgressSection()
                Spacer(modifier = Modifier.height(16.dp))
                PlaybackControls(
                    onSleepTimerClick = { showSleepTimer = true },
                    onSoundClick = { showSoundSheet = true },
                    onQueueClick = { showQueue = true }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

    if (showQueue) {
        QueueSheet(onDismiss = { showQueue = false })
    }

    if (showSleepTimer) {
        SleepTimerSheet(onDismiss = { showSleepTimer = false })
    }

    if (showSoundSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { showSoundSheet = false },
            sheetState = sheetState
        ) {
            PlaybackSoundSheet(
                state = playbackSoundState,
                onSpeedChange = { value, persist ->
                    PlayerManager.setPlaybackSpeed(value)
                    if (persist) soundScope.launch { soundPrefs.setPlaybackSpeed(value) }
                },
                onPitchChange = { value, persist ->
                    PlayerManager.setPlaybackPitch(value)
                    if (persist) soundScope.launch { soundPrefs.setPlaybackPitch(value) }
                },
                onLoudnessGainChange = { value, persist ->
                    PlayerManager.setPlaybackLoudnessGain(value)
                    if (persist) soundScope.launch { soundPrefs.setPlaybackLoudnessGainMb(value) }
                },
                onEqualizerEnabledChange = { enabled ->
                    PlayerManager.setPlaybackEqualizerEnabled(enabled)
                    soundScope.launch { soundPrefs.setPlaybackEqualizerEnabled(enabled) }
                },
                onPresetSelected = { presetId ->
                    PlayerManager.selectPlaybackEqualizerPreset(presetId)
                    soundScope.launch { soundPrefs.setPlaybackEqualizerPreset(presetId) }
                },
                onBandLevelChange = { index, value, persist ->
                    PlayerManager.updatePlaybackEqualizerBandLevel(index, value)
                    if (persist) {
                        soundScope.launch {
                            soundPrefs.setPlaybackEqualizerPreset(PlaybackEqualizerPresetId.CUSTOM)
                            val levels = playbackSoundState.bands.map { it.levelMb }.toMutableList().also { if (index < it.size) it[index] = value }
                            soundPrefs.setPlaybackEqualizerCustomBandLevels(
                                encodePlaybackEqualizerBandLevels(levels) ?: ""
                            )
                        }
                    }
                },
                onReset = {
                    PlayerManager.resetPlaybackSoundSettings()
                    soundScope.launch {
                        soundPrefs.setPlaybackSpeed(DEFAULT_PLAYBACK_SPEED)
                        soundPrefs.setPlaybackPitch(DEFAULT_PLAYBACK_PITCH)
                        soundPrefs.setPlaybackLoudnessGainMb(DEFAULT_PLAYBACK_LOUDNESS_GAIN_MB)
                        soundPrefs.setPlaybackEqualizerEnabled(false)
                        soundPrefs.setPlaybackEqualizerPreset(PlaybackEqualizerPresetId.FLAT)
                        soundPrefs.setPlaybackEqualizerCustomBandLevels("")
                    }
                },
                onDismiss = { showSoundSheet = false }
            )
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(onDismiss: () -> Unit) {
    val state by PlayerManager.state.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "播放队列 (${state.queue.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn {
                itemsIndexed(state.queue, key = { _, song -> song.bvid }) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { PlayerManager.seekTo(index.toLong()) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (index == state.currentIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp)
                        )

                        AsyncImage(
                            model = song.cover,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (index != state.currentIndex) {
                            IconButton(
                                onClick = { PlayerManager.removeFromQueue(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "移除",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LyricsView(
    lyrics: List<LyricEntry>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val s by PlayerManager.state.collectAsState()
    val currentPositionMs = s.position

    val currentLineIndex = remember(lyrics, currentPositionMs) {
        val idx = lyrics.indexOfFirst {
            currentPositionMs >= it.startTimeMs && currentPositionMs < it.endTimeMs
        }
        if (idx < 0 && lyrics.isNotEmpty() && currentPositionMs >= lyrics.last().endTimeMs) {
            lyrics.lastIndex
        } else idx
    }

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem(currentLineIndex)
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
            }
            lyrics.isEmpty() -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "暂无歌词",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击封面可切换至封面显示",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(lyrics) { index, entry ->
                        val isCurrent = index == currentLineIndex
                        Text(
                            text = entry.text,
                            fontSize = if (isCurrent) 20.sp else 14.sp,
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.45f),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .padding(vertical = 6.dp, horizontal = 24.dp)
                                .then(
                                    if (isCurrent) Modifier else Modifier.padding(horizontal = 8.dp)
                                ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressSection() {
    val s by PlayerManager.state.collectAsState()
    val position = s.position
    val duration = s.duration

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(position),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Slider(
            value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
            onValueChange = { fraction ->
                PlayerManager.seekTo((fraction * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun PlaybackControls(
    onSleepTimerClick: () -> Unit,
    onSoundClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    val s by PlayerManager.state.collectAsState()
    val isPlaying = s.playbackState.name == "PLAYING"
    val shuffleMode = s.shuffleMode
    val repeatMode = s.repeatMode
    val sleepTimerEndTime = s.sleepTimerEndTime

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { PlayerManager.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "随机播放",
                    tint = if (shuffleMode) Color.White
                    else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = { PlayerManager.skipToPrevious() }) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一首",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = { PlayerManager.togglePlayPause() },
                modifier = Modifier.size(72.dp)
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (scaleIn(tween(150), initialScale = 0.6f) + fadeIn(tween(100)))
                            .togetherWith(scaleOut(tween(100), targetScale = 0.6f) + fadeOut(tween(80)))
                    },
                    label = "play_pause"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Outlined.PauseCircle
                        else Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            IconButton(onClick = { PlayerManager.skipToNext() }) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一首",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = { PlayerManager.cycleRepeatMode() }) {
                Icon(
                    imageVector = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    },
                    contentDescription = "循环模式",
                    tint = if (repeatMode != RepeatMode.OFF) Color.White
                    else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSleepTimerClick) {
                Icon(
                    imageVector = Icons.Outlined.AddAlarm,
                    contentDescription = "睡眠定时",
                    tint = if (sleepTimerEndTime != null) Color.White
                    else Color.White.copy(alpha = 0.5f)
                )
            }

            IconButton(onClick = onSoundClick) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "音效",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onQueueClick) {
                Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = "播放队列",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
