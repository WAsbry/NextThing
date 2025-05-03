package com.wasbry.nextthing.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wasbry.nextthing.database.repository.TodoTaskRepository
import com.wasbry.nextthing.viewmodel.TodoTaskViewModel

class TodoTaskViewModelFactory(private val repository: TodoTaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d("TodoTaskViewModelFactory","进入工厂代码")
        if (modelClass.isAssignableFrom(TodoTaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoTaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}