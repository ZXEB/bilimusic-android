package com.bilimusic.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bilimusic.app.BiliMusicApp
import com.bilimusic.app.api.BiliQuality
import com.bilimusic.app.ui.screens.home.HomeViewModel
import com.bilimusic.app.ui.theme.PRESET_COLORS
import com.bilimusic.app.util.CacheManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    var showQualityDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val currentSelectedFavIds by prefs.selectedFavFolderIds.collectAsState(initial = emptyList())

    val qualityLabels = mapOf(
        "dolby" to "Dolby 全景声",
        "hires" to "Hi-Res 无损",
        "lossless" to "无损",
        "high" to "高质量",
        "medium" to "中等",
        "low" to "流畅"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 账号
            if (state.isLoggedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = state.userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "已登录",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToLogin
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Login,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "登录 Bilibili 账号",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 外观
            Text(
                text = "外观",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("深色主题")
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { checked ->
                                scope.launch { prefs.setDarkTheme(checked) }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("主题色")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(PRESET_COLORS) { hex ->
                                val color = Color(android.graphics.Color.parseColor("#$hex"))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (hex == currentSeed) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier.border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        )
                                        .clickable {
                                            scope.launch { prefs.setSeedColor(hex) }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 音频质量
            Text(
                text = "音频",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showQualityDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("音质选择")
                        Text(
                            text = qualityLabels[currentQuality] ?: "高质量",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showQualityDialog) {
                AlertDialog(
                    onDismissRequest = { showQualityDialog = false },
                    title = { Text("音质选择") },
                    text = {
                        Column {
                            BiliQuality.degradationChain.forEach { quality ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch { prefs.setAudioQuality(quality.key) }
                                            showQualityDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentQuality == quality.key,
                                        onClick = {
                                            scope.launch { prefs.setAudioQuality(quality.key) }
                                            showQualityDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = qualityLabels[quality.key] ?: quality.key,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showQualityDialog = false }) {
                            Text("关闭")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 缓存
            Text(
                text = "缓存",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            var cacheStats by remember { mutableStateOf<CacheManager.CacheStats?>(null) }
            var isCalculating by remember { mutableStateOf(true) }
            var isClearing by remember { mutableStateOf(false) }
            var showClearConfirm by remember { mutableStateOf(false) }
            var clearDone by remember { mutableStateOf(false) }
            val appContext = com.bilimusic.app.BiliMusicApp.instance

            LaunchedEffect(Unit) {
                isCalculating = true
                cacheStats = CacheManager.getCacheStats(appContext)
                isCalculating = false
            }

            fun refreshStats() {
                scope.launch {
                    isCalculating = true
                    clearDone = false
                    cacheStats = CacheManager.getCacheStats(appContext)
                    isCalculating = false
                }
            }

            fun doClearAll() {
                scope.launch {
                    isClearing = true
                    CacheManager.clearAllCache(appContext)
                    isClearing = false
                    clearDone = true
                    refreshStats()
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isCalculating) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "正在计算缓存...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column {
                            CacheStatRow(
                                label = "音频缓存",
                                size = cacheStats?.mediaCache ?: 0,
                                onClear = {
                                    scope.launch {
                                        isClearing = true
                                        CacheManager.clearMediaCache(appContext)
                                        isClearing = false
                                        refreshStats()
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            CacheStatRow(
                                label = "图片缓存",
                                size = cacheStats?.imageCache ?: 0,
                                onClear = {
                                    scope.launch {
                                        isClearing = true
                                        CacheManager.clearImageCache(appContext)
                                        isClearing = false
                                        refreshStats()
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            CacheStatRow(
                                label = "日志缓存",
                                size = cacheStats?.logCache ?: 0,
                                onClear = {
                                    scope.launch {
                                        isClearing = true
                                        CacheManager.clearLogCache(appContext)
                                        isClearing = false
                                        refreshStats()
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "总计",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    CacheManager.formatSize(cacheStats?.totalCache ?: 0),
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

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showClearConfirm = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                enabled = !isClearing && (cacheStats?.totalCache ?: 0) > 0,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isClearing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (isClearing) "正在清理..." else "一键清理缓存")
                            }
                        }
                    }
                }
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("清理缓存") },
                    text = {
                        Text("清理缓存不会删除账号信息和设置，仅删除临时文件。是否继续？")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showClearConfirm = false
                            doClearAll()
                        }) {
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

            Spacer(modifier = Modifier.height(24.dp))

            // 收藏夹
            Text(
                text = "收藏夹",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            OutlinedCard(
                onClick = {
                    if (state.isLoggedIn) {
                        onNavigateToFavoritesManage()
                    } else {
                        onNavigateToLogin()
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("收藏夹管理")
                        Text(
                            text = if (currentSelectedFavIds.isEmpty()) "未添加收藏夹"
                                   else "已选择 ${currentSelectedFavIds.size} 个收藏夹",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 其他
            Text(
                text = "其他",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            OutlinedCard(
                onClick = onNavigateToDebug
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("调试页面")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedCard(
                onClick = onNavigateToAbout
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("关于")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "BiliMusic v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = CacheManager.formatSize(size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (size > 0) {
            TextButton(onClick = onClear) {
                Text("清理", color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Text(
                "无缓存",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
