package com.nextthing.app.presentation.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AuthApi
import com.nextthing.app.data.remote.dto.LoginRequest
import com.nextthing.app.data.remote.dto.RegisterRequest
import com.nextthing.app.domain.usecase.UserUseCases
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String? = null,
    val isRegisterMode: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username, errorMessage = null)
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword, errorMessage = null)
    }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            errorMessage = null
        )
    }

    fun submit() {
        val state = _uiState.value

        if (state.isRegisterMode) {
            register(state.username, state.email, state.password)
        } else {
            login(state.username, state.password)
        }
    }

    fun continueAsLocalUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                tokenManager.clear()
                userUseCases.createUser("Local User")
                _uiState.value = _uiState.value.copy(isLoading = false, isLoginSuccess = true)
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "当前设备无法连接网络，请联网后重试，也可以先进入本地模式。"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "本地体验启动失败：${e.message ?: "未知错误"}"
                )
            }
        }
    }

    private fun register(username: String, email: String, password: String) {
        val trimmedUsername = username.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedUsername.length < 3) {
            _uiState.value = _uiState.value.copy(errorMessage = "用户名至少3个字符")
            return
        }
        if (!trimmedEmail.contains("@")) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入有效邮箱")
            return
        }
        if (trimmedPassword.length < 8) {
            _uiState.value = _uiState.value.copy(errorMessage = "密码至少8位")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = authApi.register(RegisterRequest(trimmedUsername, trimmedEmail, trimmedPassword))
                if (result.code == 0 && result.data != null) {
                    val auth = result.data
                    tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                    tokenManager.saveServerUserId(auth.user.id)
                    val localUser = userUseCases.createUser(auth.user.nickname ?: auth.user.username)
                    if (!auth.user.avatarUrl.isNullOrBlank()) {
                        userUseCases.updateAvatar(localUser.id, auth.user.avatarUrl)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoginSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "注册失败"
                    )
                }
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = parseServerError(e) ?: "注册失败"
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "当前设备无法连接网络，请联网后重试，也可以先进入本地模式。"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "网络错误：${e.message}"
                )
            }
        }
    }

    private fun login(username: String, password: String) {
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()

        if (trimmedUsername.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入用户名或邮箱")
            return
        }
        if (trimmedPassword.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入密码")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = authApi.login(LoginRequest(trimmedUsername, trimmedPassword))
                if (result.code == 0 && result.data != null) {
                    val auth = result.data
                    tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                    tokenManager.saveServerUserId(auth.user.id)
                    val localUser = userUseCases.createUser(auth.user.nickname ?: auth.user.username)
                    if (!auth.user.avatarUrl.isNullOrBlank()) {
                        userUseCases.updateAvatar(localUser.id, auth.user.avatarUrl)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoginSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "登录失败"
                    )
                }
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = parseServerError(e) ?: "登录失败"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "网络错误：${e.message}"
                )
            }
        }
    }

    private fun parseServerError(e: HttpException): String? {
        return try {
            val body = e.response()?.errorBody()?.string() ?: return null
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(body, type)
            map["message"] as? String
        } catch (_: Exception) {
            null
        }
    }
}
