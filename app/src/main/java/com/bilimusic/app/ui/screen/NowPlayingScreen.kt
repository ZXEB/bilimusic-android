package com.bilimusic.app.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AddAlarm
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import coil.compose.AsyncImage
import com.bilimusic.app.BiliMusicApp
import com.bilimusic.app.player.LyricEntry
import com.bilimusic.app.player.PlaybackState
import com.bilimusic.app.player.PlayerManager
import com.bilimusic.app.player.RepeatMode
import com.bilimusic.app.player.Song
import com.bilimusic.app.player.model.DEFAULT_PLAYBACK_LOUDNESS_GAIN_MB
import com.bilimusic.app.player.model.DEFAULT_PLAYBACK_PITCH
import com.bilimusic.app.player.model.DEFAULT_PLAYBACK_SPEED
import com.bilimusic.app.player.model.PlaybackEqualizerPresetId
import com.bilimusic.app.player.model.encodePlaybackEqualizerBandLevels
import com.bilimusic.app.ui.component.AppleMusicLyric
import com.bilimusic.app.ui.component.PlaybackSoundSheet
import com.bilimusic.app.ui.component.SleepTimerSheet
import com.bilimusic.app.ui.util.rememberDominantColor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onDismiss: () -> Unit
) {
    val currentSong by remember {
        PlayerManager.state.map { it.currentSong }.distinctUntilChanged()
    }.collectAsState(initial = PlayerManager.state.value.currentSong)
    val song = currentSong ?: return
    val dominantColor by rememberDominantColor(song.cover)
    val app = BiliMusicApp.instance
    val soundPrefs = app.preferences
    val soundScope = rememberCoroutineScope()
    val playbackSoundState by PlayerManager.playbackSoundState.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSoundSheet by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember(song.bvid) { mutableStateOf<List<LyricEntry>>(emptyList()) }
    var isLoadingLyrics by remember(song.bvid) { mutableStateOf(false) }

    LaunchedEffect(song.bvid) {
        if (song.bvid.isNotEmpty()) {
            isLoadingLyrics = true
            lyrics = app.repository.getSubtitles(song.bvid).getOrNull().orEmpty()
            isLoadingLyrics = false
        } else {
            lyrics = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        dominantColor.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.background,
                        Color.Black
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useTabletLayout = maxWidth >= 720.dp && maxWidth > maxHeight * 1.12f

            if (useTabletLayout) {
                TabletNowPlayingContent(
                    song = song,
                    lyrics = lyrics,
                    isLoadingLyrics = isLoadingLyrics,
                    onDismiss = onDismiss,
                    onSleepTimerClick = { showSleepTimer = true },
                    onSoundClick = { showSoundSheet = true },
                    onQueueClick = { showQueue = true }
                )
            } else {
                PhoneNowPlayingContent(
                    song = song,
                    lyrics = lyrics,
                    isLoadingLyrics = isLoadingLyrics,
                    showLyrics = showLyrics,
                    onShowLyrics = { showLyrics = true },
                    onShowCover = { showLyrics = false },
                    onDismiss = onDismiss,
                    onSleepTimerClick = { showSleepTimer = true },
                    onSoundClick = { showSoundSheet = true },
                    onQueueClick = { showQueue = true }
                )
            }
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
                            val levels = playbackSoundState.bands.map { it.levelMb }.toMutableList()
                                .also { if (index < it.size) it[index] = value }
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

@Composable
private fun PhoneNowPlayingContent(
    song: Song,
    lyrics: List<LyricEntry>,
    isLoadingLyrics: Boolean,
    showLyrics: Boolean,
    onShowLyrics: () -> Unit,
    onShowCover: () -> Unit,
    onDismiss: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSoundClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NowPlayingHeader(onDismiss = onDismiss)
        Spacer(modifier = Modifier.height(18.dp))

        AnimatedContent(
            targetState = showLyrics,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.98f))
                    .togetherWith(fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.98f))
            },
            label = "cover_lyrics_switch",
            modifier = Modifier.weight(1f)
        ) { lyricsVisible ->
            if (lyricsVisible) {
                LyricsSurface(
                    lyrics = lyrics,
                    isLoading = isLoadingLyrics,
                    onShowCover = onShowCover
                )
            } else {
                CoverSurface(
                    song = song,
                    onShowLyrics = onShowLyrics
                )
            }
        }

        SongTitleBlock(song = song)
        Spacer(modifier = Modifier.height(18.dp))
        ProgressSection()
        Spacer(modifier = Modifier.height(14.dp))
        PlaybackControls(
            onSleepTimerClick = onSleepTimerClick,
            onSoundClick = onSoundClick,
            onQueueClick = onQueueClick
        )
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun TabletNowPlayingContent(
    song: Song,
    lyrics: List<LyricEntry>,
    isLoadingLyrics: Boolean,
    onDismiss: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSoundClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 30.dp, vertical = 14.dp)
    ) {
        NowPlayingHeader(onDismiss = onDismiss)
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(34.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = song.cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                SongTitleBlock(song = song)
            }

            TabletLyricsPanel(
                lyrics = lyrics,
                isLoading = isLoadingLyrics,
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProgressSection()
        Spacer(modifier = Modifier.height(10.dp))
        PlaybackControls(
            onSleepTimerClick = onSleepTimerClick,
            onSoundClick = onSoundClick,
            onQueueClick = onQueueClick
        )
    }
}

@Composable
private fun NowPlayingHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "BiliMusic",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.13f)
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "收起",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CoverSurface(
    song: Song,
    onShowLyrics: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(34.dp))
                .clickable(onClick = onShowLyrics),
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
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.34f))
                        )
                    )
            )
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.42f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Subtitles, contentDescription = null, tint = Color.White)
                    Text(
                        text = "歌词",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsSurface(
    lyrics: List<LyricEntry>,
    isLoading: Boolean,
    onShowCover: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.18f),
        shape = RoundedCornerShape(30.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
                lyrics.isEmpty() -> Text(
                    text = "暂无歌词",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.titleMedium
                )
                else -> LyricsPositionConsumer(lyrics = lyrics)
            }

            IconButton(
                onClick = onShowCover,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = "显示封面",
                    tint = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun TabletLyricsPanel(
    lyrics: List<LyricEntry>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.18f),
        shape = RoundedCornerShape(30.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
                lyrics.isEmpty() -> Text(
                    text = "暂无歌词",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.titleMedium
                )
                else -> LyricsPositionConsumer(
                    lyrics = lyrics,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp, bottom = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricsPositionConsumer(lyrics: List<LyricEntry>) {
    LyricsPositionConsumer(
        lyrics = lyrics,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 42.dp, bottom = 20.dp)
    )
}

@Composable
private fun LyricsPositionConsumer(
    lyrics: List<LyricEntry>,
    modifier: Modifier = Modifier
) {
    val currentIndex by remember(lyrics) {
        PlayerManager.state
            .map { lyrics.findActiveIndex(it.position) }
            .distinctUntilChanged()
    }.collectAsState(initial = lyrics.findActiveIndex(PlayerManager.state.value.position))

    AppleMusicLyric(
        lyrics = lyrics,
        currentIndex = currentIndex,
        textColor = Color.White,
        onLyricClick = { line -> PlayerManager.seekTo(line.startTimeMs) },
        modifier = modifier
    )
}

private fun List<LyricEntry>.findActiveIndex(currentTimeMs: Long): Int {
    if (isEmpty()) return -1
    var low = 0
    var high = lastIndex
    while (low <= high) {
        val mid = (low + high) ushr 1
        val line = this[mid]
        when {
            currentTimeMs < line.startTimeMs -> high = mid - 1
            currentTimeMs >= line.endTimeMs -> low = mid + 1
            else -> return mid
        }
    }
    return if (currentTimeMs >= last().endTimeMs) lastIndex else -1
}

@Composable
private fun SongTitleBlock(song: Song) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = song.author,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.66f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 28.dp)) {
            Text(
                text = "播放队列 (${state.queue.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(state.queue, key = { _, song -> song.bvid }) { index, song ->
                    QueueRow(
                        song = song,
                        index = index,
                        selected = index == state.currentIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    song: Song,
    index: Int,
    selected: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                PlayerManager.player?.seekTo(index, 0L)
                PlayerManager.play()
            },
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
            AsyncImage(
                model = song.cover,
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
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
            if (!selected) {
                IconButton(
                    onClick = { PlayerManager.removeFromQueue(index) },
                    modifier = Modifier.size(36.dp)
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

@Composable
private fun ProgressSection() {
    val state by PlayerManager.state.collectAsState()
    val position = state.position
    val duration = state.duration
    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = { fraction -> PlayerManager.seekTo((fraction * duration).toLong()) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(position), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.64f))
            Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.64f))
        }
    }
}

@Composable
private fun PlaybackControls(
    onSleepTimerClick: () -> Unit,
    onSoundClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    val state by PlayerManager.state.collectAsState()
    val isPlaying = state.playbackState == PlaybackState.PLAYING

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryControlIcon(
                selected = state.shuffleMode,
                onClick = { PlayerManager.toggleShuffle() }
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = "随机播放", modifier = Modifier.size(22.dp))
            }
            MainControlIcon(onClick = { PlayerManager.skipToPrevious() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(38.dp))
            }
            PlayPauseButton(isPlaying = isPlaying)
            MainControlIcon(onClick = { PlayerManager.skipToNext() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "下一首", modifier = Modifier.size(38.dp))
            }
            SecondaryControlIcon(
                selected = state.repeatMode != RepeatMode.OFF,
                onClick = { PlayerManager.cycleRepeatMode() }
            ) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "循环模式",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color.White.copy(alpha = 0.12f),
            contentColor = Color.White.copy(alpha = 0.78f),
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallToolButton(selected = state.sleepTimerEndTime != null, onClick = onSleepTimerClick) {
                    Icon(Icons.Outlined.AddAlarm, contentDescription = "睡眠定时")
                }
                SmallToolButton(onClick = onSoundClick) {
                    Icon(Icons.Outlined.GraphicEq, contentDescription = "音效")
                }
                SmallToolButton(onClick = onQueueClick) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = "播放队列")
                }
            }
        }
    }
}

@Composable
private fun MainControlIcon(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides Color.White
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean) {
    Surface(
        color = Color.White,
        contentColor = Color.Black,
        shape = CircleShape,
        shadowElevation = 12.dp,
        modifier = Modifier.size(74.dp)
    ) {
        IconButton(onClick = { PlayerManager.togglePlayPause() }) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(tween(150), initialScale = 0.7f) + fadeIn(tween(90)))
                        .togetherWith(scaleOut(tween(110), targetScale = 0.7f) + fadeOut(tween(70)))
                },
                label = "play_pause"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun SecondaryControlIcon(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (selected) Color.White else Color.White.copy(alpha = 0.46f)
        ) {
            content()
        }
    }
}

@Composable
private fun SmallToolButton(
    selected: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (selected) Color.White else Color.White.copy(alpha = 0.72f)
        ) {
            content()
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
