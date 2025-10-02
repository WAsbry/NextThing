package com.example.nextthingb1.presentation.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.usecase.UserUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val nickname: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userUseCases: UserUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onNicknameChange(nickname: String) {
        _uiState.value = _uiState.value.copy(nickname = nickname, errorMessage = null)
    }

    fun login() {
        val nickname = _uiState.value.nickname.trim()

        if (nickname.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入昵称")
            return
        }

        if (nickname.length < 2) {
            _uiState.value = _uiState.value.copy(errorMessage = "昵称至少2个字符")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                userUseCases.createUser(nickname)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoginSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "创建用户失败：${e.message}"
                )
            }
        }
    }
}
