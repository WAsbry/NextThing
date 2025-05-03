package com.wasbry.nextthing.viewmodel.todoTask

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.database.repository.TodoTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


class TodoTaskViewModel(private val repository: TodoTaskRepository) : ViewModel() {

    // 获取所有待办任务
    // 直接暴露 Flow，不在 ViewModel 中收集！
    val allTodoTasks: Flow<List<TodoTask>> = repository.allTodoTasks

    // 插入单个待办任务
    fun insertTodoTask(todoTask: TodoTask) = viewModelScope.launch {
        repository.insertTodoTask(todoTask)
    }

    // 更新单个待办任务
    fun updateTodoTask(todoTask: TodoTask) = viewModelScope.launch {
        repository.updateTodoTask(todoTask)
    }

    // 删除单个待办任务
    fun deleteTodoTask(todoTask: TodoTask) = viewModelScope.launch {
        repository.deleteTodoTask(todoTask)
    }
}