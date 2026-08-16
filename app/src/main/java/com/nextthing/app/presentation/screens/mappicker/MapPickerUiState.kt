package com.nextthing.app.presentation.screens.mappicker

data class MapPickerUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val isLoadingAddress: Boolean = false,
    val isLocating: Boolean = false,
    val addressHint: String? = null,
    val errorMessage: String? = null,
    val hasSelectedLocation: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<PlaceSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val moveToken: Long = 0L
)

data class PlaceSearchResult(
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)
