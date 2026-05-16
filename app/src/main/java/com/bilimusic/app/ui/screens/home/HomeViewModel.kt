package com.bilimusic.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilimusic.app.BiliMusicApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val userAvatar: String = ""
)

class HomeViewModel : ViewModel() {

    private val app = BiliMusicApp.instance
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            app.preferences.isLoggedIn.collect { loggedIn ->
                _state.value = _state.value.copy(isLoggedIn = loggedIn)
            }
        }
        viewModelScope.launch {
            app.preferences.userName.collect { name ->
                _state.value = _state.value.copy(userName = name)
            }
        }
        viewModelScope.launch {
            app.preferences.userAvatar.collect { avatar ->
                _state.value = _state.value.copy(userAvatar = avatar)
            }
        }
    }
}
