package com.example.nextthingb1.presentation.screens.mappicker

data class MapPickerUiState(
    val latitude: Double = 39.9042, // 默认北京天安门
    val longitude: Double = 116.4074,
    val address: String = "",
    val isLoadingAddress: Boolean = false,
    val errorMessage: String? = null,
    val hasSelectedLocation: Boolean = false
)
