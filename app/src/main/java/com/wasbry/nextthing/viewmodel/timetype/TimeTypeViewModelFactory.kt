package com.wasbry.nextthing.viewmodel.timetype

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wasbry.nextthing.database.repository.TimeTypeRepository

class TimeTypeViewModelFactory(private val repository: TimeTypeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d("TimeTypeViewModelFactory", "进入工厂代码")
        if (modelClass.isAssignableFrom(TimeTypeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeTypeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}