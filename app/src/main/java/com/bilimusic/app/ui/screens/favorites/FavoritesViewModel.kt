package com.bilimusic.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilimusic.app.BiliMusicApp
import com.bilimusic.app.api.FavoriteFolder
import com.bilimusic.app.api.VideoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoriteFolderWithContent(
    val folder: FavoriteFolder,
    val videos: List<VideoInfo> = emptyList(),
    val isLoading: Boolean = false
)

data class FavoritesUiState(
    val folderContents: List<FavoriteFolderWithContent> = emptyList(),
    val isLoading: Boolean = false
)

class FavoritesViewModel : ViewModel() {
    private val app = BiliMusicApp.instance
    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    private var loadedFolderIds: Set<Long> = emptySet()

    init {
        viewModelScope.launch {
            app.preferences.selectedFavFolderIds.collect { ids ->
                val idsSet = ids.toSet()
                if (idsSet != loadedFolderIds) {
                    if (ids.isEmpty()) {
                        _state.value = FavoritesUiState()
                        loadedFolderIds = emptySet()
                    } else {
                        loadFavorites(ids)
                    }
                }
            }
        }
    }

    private suspend fun loadFavorites(ids: List<Long>) {
        _state.value = _state.value.copy(isLoading = true)

        val mid = app.preferences.getUserMid()
        if (mid == 0L) {
            _state.value = _state.value.copy(isLoading = false)
            return
        }

        val result = app.repository.getFavoriteFolders(mid)
        result.onSuccess { allFolders ->
            val selected = allFolders.filter { it.id in ids }
            val contents = selected.map { folder ->
                val videosResult = app.repository.getFavoriteDetailAll(folder.id)
                FavoriteFolderWithContent(
                    folder = folder,
                    videos = videosResult.getOrDefault(emptyList())
                )
            }
            _state.value = _state.value.copy(
                folderContents = contents,
                isLoading = false
            )
            loadedFolderIds = ids.toSet()
        }.onFailure {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val ids = _state.value.folderContents.map { it.folder.id }
            if (ids.isNotEmpty()) loadFavorites(ids)
        }
    }
}
