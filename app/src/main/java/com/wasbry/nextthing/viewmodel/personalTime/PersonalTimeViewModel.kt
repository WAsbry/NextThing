package com.wasbry.nextthing.viewmodel.personalTime

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.database.repository.PersonalTimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PersonalTimeViewModel(private val repository: PersonalTimeRepository) : ViewModel() {

    private val _personalTimes = MutableStateFlow(emptyList<PersonalTime>())
    val personalTimes: StateFlow<List<PersonalTime>> = _personalTimes

    // 用于缓存已加载的 PersonalTime（避免重复查询）
    private val personalTimeCache = mutableMapOf<Long, StateFlow<PersonalTime>>()

    // ViewModel
    fun getPersonalTimeByPersonTimeId(personTimeId: Long): StateFlow<PersonalTime> {
        return personalTimeCache.getOrPut(personTimeId) {
            val flow = repository.getPersonalTimeById(personTimeId)
                .flowOn(Dispatchers.IO)

            // 同步获取初始值，确保 StateFlow 从一开始就有真实数据
            flow.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = runBlocking(Dispatchers.IO) { flow.first() }
            )
        }
    }


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