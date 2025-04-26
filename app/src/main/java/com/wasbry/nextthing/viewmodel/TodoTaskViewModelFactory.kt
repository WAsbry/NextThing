package com.wasbry.nextthing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wasbry.nextthing.database.repository.TodoTaskRepository

class TodoTaskViewModelFactory(private val repository: TodoTaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoTaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoTaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}