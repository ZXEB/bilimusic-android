package com.bilimusic.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bilimusic.app.BiliMusicApp
import com.bilimusic.app.player.PlayerManager
import com.bilimusic.app.player.Song
import com.bilimusic.app.ui.screens.favorites.FavoritesViewModel

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFolder: (Long) -> Unit = {},
    favoritesViewModel: FavoritesViewModel = viewModel()
) {
    val prefs = BiliMusicApp.instance.preferences
    val isLoggedIn by prefs.isLoggedIn.collectAsState(initial = false)
    val userName by prefs.userName.collectAsState(initial = "")
    val selectedFolderIds by prefs.selectedFavFolderIds.collectAsState(initial = emptyList())
    val history by PlayerManager.playHistory.collectAsState()
    val favState by favoritesViewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeHeader(
                isLoggedIn = isLoggedIn,
                userName = userName,
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToDebug = onNavigateToDebug
            )
        }

        item {
            QuickActions(
                isLoggedIn = isLoggedIn,
                onLogin = onNavigateToLogin,
                onSettings = onNavigateToSettings,
                onRefresh = { favoritesViewModel.refresh() }
            )
        }

        if (history.isNotEmpty()) {
            item {
                SectionTitle(
                    icon = Icons.Outlined.History,
                    title = "最近播放"
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(history.take(12), key = { it.song.bvid }) { item ->
                        HistoryCard(song = item.song) {
                            PlayerManager.playSong(item.song)
                        }
                    }
                }
            }
        }

        item {
            SectionTitle(
                icon = Icons.Outlined.Bookmark,
                title = "我的收藏夹",
                action = if (selectedFolderIds.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { favoritesViewModel.refresh() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                        }
                    }
                } else {
                    null
                }
            )
        }

        when {
            selectedFolderIds.isEmpty() -> {
                item {
                    EmptyFolderPrompt(onClick = onNavigateToSettings)
                }
            }
            favState.isLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }
            else -> {
                items(favState.folderContents, key = { it.folder.id }) { folderContent ->
                    FolderCard(
                        title = folderContent.folder.title,
                        cover = folderContent.folder.cover,
                        videoCount = folderContent.folder.media_count,
                        onClick = { onNavigateToFolder(folderContent.folder.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    isLoggedIn: Boolean,
    userName: String,
    onNavigateToLogin: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isLoggedIn && userName.isNotBlank()) "Hi, $userName" else "BiliMusic",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Bilibili 收藏夹音乐播放器",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onNavigateToLogin) {
            Icon(Icons.Outlined.Login, contentDescription = "登录")
        }
        IconButton(onClick = onNavigateToDebug) {
            Icon(Icons.Outlined.BugReport, contentDescription = "调试")
        }
    }
}

@Composable
private fun QuickActions(
    isLoggedIn: Boolean,
    onLogin: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isLoggedIn) "从收藏夹继续听" else "先登录 Bilibili",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isLoggedIn) "刷新收藏夹，或到设置里选择要显示的歌单。"
                    else "粘贴 SESSDATA 后，就能把收藏夹当歌单播放。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickPill(
                        icon = if (isLoggedIn) Icons.Outlined.Refresh else Icons.Outlined.Login,
                        text = if (isLoggedIn) "刷新" else "登录",
                        onClick = if (isLoggedIn) onRefresh else onLogin
                    )
                    QuickPill(
                        icon = Icons.Outlined.Settings,
                        text = "设置",
                        onClick = onSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Composable
private fun EmptyFolderPrompt(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "还没有选择收藏夹",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "去设置里添加要显示的收藏夹",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FolderCard(
    title: String,
    cover: String,
    videoCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverBox(
                image = cover,
                icon = Icons.Outlined.Bookmark,
                modifier = Modifier.size(62.dp)
            )
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$videoCount 个视频",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "播放",
                    modifier = Modifier
                        .padding(9.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(song: Song, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(156.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            CoverBox(
                image = song.cover,
                icon = Icons.Outlined.MusicNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
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
    }
}

@Composable
private fun CoverBox(
    image: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (image.isNotEmpty()) {
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
