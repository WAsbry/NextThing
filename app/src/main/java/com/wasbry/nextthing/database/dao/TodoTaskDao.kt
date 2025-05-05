package com.wasbry.nextthing.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wasbry.nextthing.database.model.TodoTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoTaskDao {

    // 查询所有待办事项，按照截止日期升序排列
    @Query("SELECT * FROM TodoTaskTable ORDER BY madeDate ASC")
    fun getAllTodoTasks(): Flow<List<TodoTask>>

    // 根据任务ID 查询单个Task
    @Query("SELECT * FROM TodoTaskTable WHERE id = :taskId")
    fun getTodoTaskById(taskId: Long): TodoTask

    // 查询所有已完成的待办任务，并按照制定日期升序排列
    @Query("SELECT * FROM TodoTaskTable WHERE status = 'COMPLETED' ORDER BY madeDate ASC")
    fun getCompletedTodoTasks(): List<TodoTask>

    // 查询所有没有完成的待办任务，并按照制定日期升序排列
    @Query("SELECT * FROM TodoTaskTable WHERE status = 'INCOMPLETE' ORDER BY madeDate ASC")
    fun getIncompleteTodoTasks(): List<TodoTask>

    // 插入单个任务，并返回id 如果主键重复，则替换原有数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTodoTask(todoTask: TodoTask): Long

    // 插入多个任务，若有冲突，则替换原有数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTodoTasks(todoTasks: List<TodoTask>)

    // 更新单个任务
    @Update
    fun updateTodoTask(todoTask: TodoTask)

    // 更新多个任务
    @Update
    fun updateTodoTasks(todoTasks: List<TodoTask>)

    // 删除单个待办任务
    @Delete
    fun deleteTodoTask(task: TodoTask)

    // 根据id 删除单个待办任务
    @Query("DELETE FROM TodoTaskTable WHERE id = :taskId")
    fun deleteTodoTaskById(taskId: Long)

    // 清空所有待办任务
    @Query("DELETE FROM TodoTaskTable")
    fun deleteAllTodoTasks()

    // 根据关联的 PersonalTime 的 ID 查询相关的 TodoTask
    @Query("SELECT * FROM TodoTaskTable WHERE personalTimeId = :personalTimeId")
    fun getTodoTasksByPersonalTimeId(personalTimeId: Long): Flow<List<TodoTask>>

    // 查询所有关联了特定 PersonalTime 的待办任务，并按照截止日期升序排列
    @Query("SELECT * FROM TodoTaskTable WHERE personalTimeId = :personalTimeId ORDER BY madeDate ASC")
    fun getTodoTasksByPersonalTimeIdSorted(personalTimeId: Long): Flow<List<TodoTask>>
}