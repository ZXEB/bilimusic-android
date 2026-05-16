package com.bilimusic.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilimusic.app.BiliMusicApp
import com.bilimusic.app.api.BiliApiClient
import com.bilimusic.app.api.BilibiliRepository
import com.bilimusic.app.util.DebugLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val sessdata: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val error: String? = null
)

class LoginViewModel : ViewModel() {

    private val app = BiliMusicApp.instance
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val loggedIn = app.preferences.getLoggedIn()
                if (loggedIn) {
                    val name = app.preferences.getUserName()
                    _state.value = _state.value.copy(
                        isLoggedIn = true,
                        userName = name
                    )
                }
            } catch (e: Exception) {
                // silently ignore init errors
            }
        }
    }

    fun updateSessdata(value: String) {
        _state.value = _state.value.copy(sessdata = value, error = null)
    }

    fun login() {
        val sessdata = _state.value.sessdata.trim().filter { it >= ' ' && it <= '~' }
        if (sessdata.isEmpty()) {
            _state.value = _state.value.copy(error = "请输入 SESSDATA")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val api = BiliApiClient(sessdata)
                val repository = BilibiliRepository(api)
                DebugLog.i("Logging in...")
                val result = repository.login(sessdata)

                result.fold(
                    onSuccess = { user ->
                        DebugLog.i("Login success: ${user.uname} (mid=${user.mid})")
                        try {
                            app.preferences.saveLoginInfo(
                                sessdata = sessdata,
                                mid = user.mid,
                                name = user.uname,
                                avatar = user.face
                            )
                        } catch (e: Exception) {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "保存登录信息失败: ${e.localizedMessage}"
                            )
                            return@launch
                        }
                        app.repository = repository
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userName = user.uname
                        )
                    },
                    onFailure = { error ->
                        DebugLog.e("Login failed: ${error.message}", error)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = error.message ?: "登录失败"
                        )
                    }
                )
            } catch (e: Exception) {
                DebugLog.e("Login exception", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "登录异常: ${e.localizedMessage}"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                app.preferences.clearLoginInfo()
            } catch (_: Exception) { }
            _state.value = LoginUiState()
        }
    }
}
