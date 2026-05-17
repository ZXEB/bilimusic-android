package com.bilimusic.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bilimusic.app.BiliMusicApp
import com.bilimusic.app.api.BiliQuality
import com.bilimusic.app.ui.screens.home.HomeViewModel
import com.bilimusic.app.ui.theme.PRESET_COLORS
import com.bilimusic.app.util.CacheManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToFavoritesManage: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val state by homeViewModel.state.collectAsState()
    val prefs = BiliMusicApp.instance.preferences
    val isDark by prefs.isDarkTheme.collectAsState(initial = true)
    val currentSeed by prefs.seedColor.collectAsState(initial = "00A1D6")
    val currentQuality by prefs.audioQuality.collectAsState(initial = "high")
    val currentSelectedFavIds by prefs.selectedFavFolderIds.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val appContext = BiliMusicApp.instance

    var showQualityDialog by remember { mutableStateOf(false) }
    var cacheStats by remember { mutableStateOf<CacheManager.CacheStats?>(null) }
    var isCalculating by remember { mutableStateOf(true) }
    var isClearing by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var clearDone by remember { mutableStateOf(false) }

    fun refreshStats() {
        scope.launch {
            isCalculating = true
            clearDone = false
            cacheStats = CacheManager.getCacheStats(appContext)
            isCalculating = false
        }
    }

    fun clearAllCache() {
        scope.launch {
            isClearing = true
            CacheManager.clearAllCache(appContext)
            isClearing = false
            clearDone = true
            cacheStats = CacheManager.getCacheStats(appContext)
        }
    }

    LaunchedEffect(Unit) {
        cacheStats = CacheManager.getCacheStats(appContext)
        isCalculating = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsHero()
        }

        item {
            AccountCard(
                isLoggedIn = state.isLoggedIn,
                userName = state.userName,
                onClick = onNavigateToLogin
            )
        }

        item {
            SettingsGroup(title = "外观") {
                SettingRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "深色主题",
                    subtitle = if (isDark) "当前使用深色界面" else "当前跟随浅色界面",
                    trailing = {
                        Switch(
                            checked = isDark,
                            onCheckedChange = { checked: Boolean -> scope.launch { prefs.setDarkTheme(checked) } }
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("主题色", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(PRESET_COLORS) { hex: String ->
                            val color = Color(android.graphics.Color.parseColor("#$hex"))
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (hex == currentSeed) 3.dp else 1.dp,
                                        color = if (hex == currentSeed) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    )
                                    .clickable { scope.launch { prefs.setSeedColor(hex) } }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsGroup(title = "播放") {
                SettingRow(
                    icon = Icons.Outlined.MusicNote,
                    title = "音质选择",
                    subtitle = qualityLabel(currentQuality),
                    onClick = { showQualityDialog = true },
                    trailingIcon = Icons.Outlined.ChevronRight
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                SettingRow(
                    icon = Icons.Outlined.GraphicEq,
                    title = "音效",
                    subtitle = "播放页中可调整倍速、音调、均衡器"
                )
            }
        }

        item {
            SettingsGroup(title = "收藏夹") {
                SettingRow(
                    icon = Icons.Outlined.Bookmark,
                    title = "收藏夹管理",
                    subtitle = if (currentSelectedFavIds.isEmpty()) "还没有选择收藏夹"
                    else "已选择 ${currentSelectedFavIds.size} 个收藏夹",
                    onClick = {
                        if (state.isLoggedIn) onNavigateToFavoritesManage() else onNavigateToLogin()
                    },
                    trailingIcon = Icons.Outlined.ChevronRight
                )
            }
        }

        item {
            SettingsGroup(title = "缓存") {
                CacheSection(
                    cacheStats = cacheStats,
                    isCalculating = isCalculating,
                    isClearing = isClearing,
                    clearDone = clearDone,
                    onRefresh = ::refreshStats,
                    onClearMedia = {
                        scope.launch {
                            isClearing = true
                            CacheManager.clearMediaCache(appContext)
                            isClearing = false
                            refreshStats()
                        }
                    },
                    onClearImage = {
                        scope.launch {
                            isClearing = true
                            CacheManager.clearImageCache(appContext)
                            isClearing = false
                            refreshStats()
                        }
                    },
                    onClearLog = {
                        scope.launch {
                            isClearing = true
                            CacheManager.clearLogCache(appContext)
                            isClearing = false
                            refreshStats()
                        }
                    },
                    onClearAll = { showClearConfirm = true }
                )
            }
        }

        item {
            SettingsGroup(title = "其他") {
                SettingRow(
                    icon = Icons.Outlined.BugReport,
                    title = "调试页面",
                    subtitle = "查看接口、播放和缓存日志",
                    onClick = onNavigateToDebug,
                    trailingIcon = Icons.Outlined.ChevronRight
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                SettingRow(
                    icon = Icons.Outlined.Info,
                    title = "关于",
                    subtitle = "BiliMusic v1.1",
                    onClick = onNavigateToAbout,
                    trailingIcon = Icons.Outlined.ChevronRight
                )
            }
        }
    }

    if (showQualityDialog) {
        QualityDialog(
            currentQuality = currentQuality,
            onSelect = { quality ->
                scope.launch { prefs.setAudioQuality(quality.key) }
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清理缓存") },
            text = { Text("只会删除临时缓存文件，不会删除账号信息和设置。继续清理吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        clearAllCache()
                    }
                ) {
                    Text("清理")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsHero() {
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
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Text(
                    "设置",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "账号、音质、主题和缓存都在这里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    isLoggedIn: Boolean,
    userName: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isLoggedIn) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLoggedIn) Icons.Outlined.AccountCircle else Icons.Outlined.Login,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = if (isLoggedIn) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) userName.ifBlank { "已登录" } else "登录 Bilibili 账号",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isLoggedIn) "可以读取收藏夹并播放音频" else "粘贴 SESSDATA 后开始使用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 2.dp
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(9.dp).size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke()
        if (trailingIcon != null) {
            Icon(
                trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CacheSection(
    cacheStats: CacheManager.CacheStats?,
    isCalculating: Boolean,
    isClearing: Boolean,
    clearDone: Boolean,
    onRefresh: () -> Unit,
    onClearMedia: () -> Unit,
    onClearImage: () -> Unit,
    onClearLog: () -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (isCalculating) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("正在计算缓存...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            CacheStatRow("音频缓存", cacheStats?.mediaCache ?: 0L, onClearMedia)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            CacheStatRow("图片缓存", cacheStats?.imageCache ?: 0L, onClearImage)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            CacheStatRow("日志缓存", cacheStats?.logCache ?: 0L, onClearLog)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("总计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    CacheManager.formatSize(cacheStats?.totalCache ?: 0L),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (clearDone) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "缓存清理完成",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onClearAll,
                    enabled = !isClearing && (cacheStats?.totalCache ?: 0L) > 0L,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isClearing) "清理中" else "一键清理")
                }
                TextButton(onClick = onRefresh, enabled = !isClearing) {
                    Text("刷新")
                }
            }
        }
    }
}

@Composable
private fun CacheStatRow(
    label: String,
    size: Long,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                CacheManager.formatSize(size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onClear, enabled = size > 0) {
            Text(if (size > 0) "清理" else "无缓存")
        }
    }
}

@Composable
private fun QualityDialog(
    currentQuality: String,
    onSelect: (BiliQuality) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("音质选择") },
        text = {
            Column {
                BiliQuality.degradationChain.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(quality) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentQuality == quality.key,
                            onClick = { onSelect(quality) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(qualityLabel(quality.key), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun qualityLabel(key: String): String = when (key) {
    "dolby" -> "Dolby 全景声"
    "hires" -> "Hi-Res 无损"
    "lossless" -> "无损"
    "medium" -> "中等"
    "low" -> "流畅"
    else -> "高质量"
}
