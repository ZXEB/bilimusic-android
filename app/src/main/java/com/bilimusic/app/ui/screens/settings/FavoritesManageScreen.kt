package com.bilimusic.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bilimusic.app.BiliMusicApp
import com.bilimusic.app.ui.screens.home.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesManageScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val state by homeViewModel.state.collectAsState()
    val prefs = BiliMusicApp.instance.preferences
    val currentSelectedFavIds by prefs.selectedFavFolderIds.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var favFolders by remember { mutableStateOf<List<com.bilimusic.app.api.FavoriteFolder>>(emptyList()) }
    var selectedFavIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!state.isLoggedIn) return@LaunchedEffect
        isLoading = true
        error = null
        val mid = prefs.getUserMid()
        val result = BiliMusicApp.instance.repository.getFavoriteFolders(mid)
        result.fold(
            onSuccess = { folders ->
                favFolders = folders
                selectedFavIds = currentSelectedFavIds.toSet()
                isLoading = false
            },
            onFailure = { e ->
                error = e.message
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理收藏夹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Button(
                    onClick = {
                        scope.launch { prefs.setSelectedFavFolderIds(selectedFavIds.toList()) }
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    enabled = state.isLoggedIn
                ) {
                    Text("保存")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.isLoggedIn -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("请先登录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "加载失败: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                favFolders.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "没有找到收藏夹",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(favFolders) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFavIds = if (folder.id in selectedFavIds) {
                                            selectedFavIds - folder.id
                                        } else {
                                            selectedFavIds + folder.id
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = folder.id in selectedFavIds,
                                    onCheckedChange = { checked ->
                                        selectedFavIds = if (checked) {
                                            selectedFavIds + folder.id
                                        } else {
                                            selectedFavIds - folder.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = folder.title,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${folder.media_count} 个视频",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
