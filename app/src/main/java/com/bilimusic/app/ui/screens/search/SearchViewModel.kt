package com.bilimusic.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilimusic.app.BiliMusicApp
import com.bilimusic.app.api.BiliQuality
import com.bilimusic.app.api.FavoriteFolder
import com.bilimusic.app.api.VideoInfo
import com.bilimusic.app.player.PlayerManager
import com.bilimusic.app.player.Song
import com.bilimusic.app.util.DebugLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<VideoInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFavFolderDialog: Boolean = false,
    val favFolders: List<FavoriteFolder> = emptyList()
)

class SearchViewModel : ViewModel() {

    private val app = BiliMusicApp.instance
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        if (query.isNotEmpty()) {
            searchJob = viewModelScope.launch {
                delay(500)
                search(query)
            }
        } else {
            _state.value = _state.value.copy(results = emptyList())
        }
    }

    fun playVideo(video: VideoInfo) {
        viewModelScope.launch {
            DebugLog.i("playVideo: ${video.bvid} ${video.title}")
            val qualityKey = app.preferences.getAudioQuality()
            val quality = BiliQuality.fromKey(qualityKey)
            val result = app.repository.getAudioUrl(video.bvid, quality)
            result.fold(
                onSuccess = { audioUrl ->
                    val song = Song(
                        bvid = video.bvid,
                        title = video.title,
                        author = video.owner?.name ?: "",
                        cover = video.pic,
                        audioUrl = audioUrl,
                        duration = video.duration.toLong(),
                        cid = video.cid
                    )
                    DebugLog.i("Got audio URL, playing: ${song.title}")
                    PlayerManager.playSong(song)
                },
                onFailure = { error ->
                    DebugLog.e("Failed to get audio URL: ${error.message}", error)
                }
            )
        }
    }

    fun addToQueue(video: VideoInfo) {
        viewModelScope.launch {
            val qualityKey = app.preferences.getAudioQuality()
            val quality = BiliQuality.fromKey(qualityKey)
            val url = app.repository.getAudioUrl(video.bvid, quality).getOrNull() ?: return@launch
            val song = Song(
                bvid = video.bvid,
                title = video.title,
                author = video.owner?.name ?: "",
                cover = video.pic,
                audioUrl = url,
                duration = video.duration.toLong(),
                cid = video.cid
            )
            PlayerManager.addToQueue(song)
        }
    }

    fun showFavFolderDialog() {
        viewModelScope.launch {
            val mid = app.preferences.getUserMid()
            if (mid > 0) {
                val result = app.repository.getFavoriteFolders(mid)
                result.fold(
                    onSuccess = { folders ->
                        _state.value = _state.value.copy(
                            showFavFolderDialog = true,
                            favFolders = folders
                        )
                    },
                    onFailure = { }
                )
            }
        }
    }

    fun dismissFavFolderDialog() {
        _state.value = _state.value.copy(showFavFolderDialog = false)
    }

    fun addToFavFolder(folderId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(showFavFolderDialog = false)
        }
    }

    private suspend fun search(query: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        val result = app.repository.search(query)
        result.fold(
            onSuccess = { videos ->
                _state.value = _state.value.copy(
                    results = videos,
                    isLoading = false
                )
            },
            onFailure = { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        )
    }
}
