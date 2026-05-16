package com.bilimusic.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilimusic.app.BiliMusicApp
import com.bilimusic.app.api.BiliQuality
import com.bilimusic.app.api.VideoInfo
import com.bilimusic.app.player.PlayerManager
import com.bilimusic.app.player.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FolderDetailUiState(
    val folderName: String = "",
    val videos: List<VideoInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FolderDetailViewModel : ViewModel() {
    private val app = BiliMusicApp.instance
    private val _state = MutableStateFlow(FolderDetailUiState())
    val state: StateFlow<FolderDetailUiState> = _state.asStateFlow()

    fun loadFolder(folderId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val mid = app.preferences.getUserMid()
            val folderResult = app.repository.getFavoriteFolders(mid)
            folderResult.onSuccess { folders ->
                val folder = folders.find { it.id == folderId }
                _state.value = _state.value.copy(folderName = folder?.title ?: "")
            }

            val result = app.repository.getFavoriteDetailAll(folderId)
            result.fold(
                onSuccess = { videos ->
                    _state.value = _state.value.copy(
                        videos = videos,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun playFromFolder(videos: List<VideoInfo>, startIndex: Int) {
        viewModelScope.launch {
            val qualityKey = app.preferences.getAudioQuality()
            val quality = BiliQuality.fromKey(qualityKey)
            val firstUrl = app.repository.getAudioUrl(videos[startIndex].bvid, quality).getOrNull()

            val songs = videos.mapIndexed { i, video ->
                Song(
                    bvid = video.bvid,
                    title = video.title,
                    author = video.owner?.name ?: "",
                    cover = video.pic,
                    audioUrl = if (i == startIndex && firstUrl != null) firstUrl else "",
                    duration = video.duration.toLong(),
                    cid = video.cid
                )
            }
            PlayerManager.setQueue(songs, startIndex)
            PlayerManager.play()
        }
    }

    fun addToQueue(video: VideoInfo) {
        val song = Song(
            bvid = video.bvid,
            title = video.title,
            author = video.owner?.name ?: "",
            cover = video.pic,
            duration = video.duration.toLong(),
            cid = video.cid
        )
        PlayerManager.addToQueue(song)
    }
}
