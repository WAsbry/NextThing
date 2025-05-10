package com.wasbry.nextthing.viewmodel.personalTime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wasbry.nextthing.database.repository.PersonalTimeRepository

class PersonalTimeViewModelFactory(private val repository: PersonalTimeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonalTimeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PersonalTimeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}