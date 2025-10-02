package com.example.nextthingb1.presentation.screens.userinfo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.usecase.UserUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserInfoUiState(
    val currentUserId: String = "",
    val avatarUri: Uri? = null,
    val nickname: String = "",
    val userId: String = "",
    val phoneNumber: String = "",
    val wechatId: String = "",
    val qqId: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showBindDialog: Boolean = false,
    val bindDialogType: BindType? = null
)

enum class BindType {
    PHONE, WECHAT, QQ
}

@HiltViewModel
class UserInfoViewModel @Inject constructor(
    private val userUseCases: UserUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserInfoUiState())
    val uiState: StateFlow<UserInfoUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            userUseCases.getCurrentUser().collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        currentUserId = user.id,
                        avatarUri = user.avatarUri?.let { Uri.parse(it) },
                        nickname = user.nickname,
                        userId = user.id,
                        phoneNumber = user.phoneNumber ?: "",
                        wechatId = user.wechatId ?: "",
                        qqId = user.qqId ?: "",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserId
            if (currentUserId.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(avatarUri = uri)
                userUseCases.updateAvatar(currentUserId, uri.toString())
            }
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserId
            if (currentUserId.isNotEmpty() && nickname.isNotBlank()) {
                userUseCases.updateNickname(currentUserId, nickname)
            }
        }
    }

    fun showBindDialog(type: BindType) {
        _uiState.value = _uiState.value.copy(
            showBindDialog = true,
            bindDialogType = type
        )
    }

    fun hideBindDialog() {
        _uiState.value = _uiState.value.copy(
            showBindDialog = false,
            bindDialogType = null
        )
    }

    fun bindAccount(value: String) {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserId
            if (currentUserId.isEmpty() || value.isBlank()) return@launch

            when (_uiState.value.bindDialogType) {
                BindType.PHONE -> {
                    userUseCases.updatePhoneNumber(currentUserId, value)
                }
                BindType.WECHAT -> {
                    userUseCases.updateWechatId(currentUserId, value)
                }
                BindType.QQ -> {
                    userUseCases.updateQqId(currentUserId, value)
                }
                null -> {}
            }

            hideBindDialog()
        }
    }

    fun logout() {
        viewModelScope.launch {
            userUseCases.logout()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            // TODO: 弹出确认对话框
            userUseCases.logout()
        }
    }
}
