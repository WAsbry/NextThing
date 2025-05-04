package com.wasbry.nextthing.viewmodel.personalTime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.database.repository.PersonalTimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PersonalTimeViewModel(private val repository: PersonalTimeRepository) : ViewModel() {

    private val _personalTimes = MutableStateFlow(emptyList<PersonalTime>())
    val personalTimes: StateFlow<List<PersonalTime>> = _personalTimes

    private val _isExpanded = MutableStateFlow(true)
    val isExpanded: StateFlow<Boolean> = _isExpanded

    init {
        loadPersonalTimes()
    }

    private fun loadPersonalTimes() {
        viewModelScope.launch {
            try {
                repository.allPersonalTime.collect { times ->
                    _personalTimes.value = times
                }
            } catch (e: Exception) {
                // 处理异常，例如打印日志
                e.printStackTrace()
            }
        }
    }

    fun insertPersonalTime(personalTime: PersonalTime) {
        viewModelScope.launch {
            try {
                repository.insertPersonalTime(personalTime)
                loadPersonalTimes() // 插入后重新加载数据
            } catch (e: Exception) {
                // 处理异常，例如打印日志
                e.printStackTrace()
            }
        }
    }

    fun toggleExpansion() {
        viewModelScope.launch {
            _isExpanded.value = !_isExpanded.value
        }
    }

    fun updatePersonalTime(personalTime: PersonalTime) {
        viewModelScope.launch {
            try {
                repository.updatePersonalTime(personalTime)
                loadPersonalTimes() // 更新后重新加载数据，这个就是写在ViewModel 里面的噻
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePersonalTime(personalTime: PersonalTime) {
        viewModelScope.launch {
            try {
                repository.deletePersonalTime(personalTime)
                loadPersonalTimes()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}