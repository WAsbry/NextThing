package com.wasbry.nextthing.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wasbry.nextthing.database.model.TodoTask

@Dao
interface TodoTaskDao {

    // 查询所有待办事项，按照截止日期升序排列
    @Query("SELECT * FROM todoTask ORDER BY dueDate ASC")
    fun getAllTodoTasks(): LiveData<List<TodoTask>>

    // 根据任务ID 查询单个Task
    @Query("SELECT * FROM todoTask WHERE id = :taskId") // 这个语法，要想一下
    fun getTodoTaskById(taskId: Long): TodoTask

    // 根据分类Id 查询该类下的所有任务，并按照截止日期进行升序排列
    @Query("SELECT * FROM todoTask WHERE categoryId = :categoryId ORDER BY dueDate ASC")
    fun getTodoTaskByCategoryId(categoryId: Long) : List<TodoTask>

    // 查询所有已完成的待办任务，并按照截止日期升序排列
    @Query("SELECT * FROM todoTask WHERE isCompleted = 1 ORDER BY dueDate ASC")
    fun getCompletedTodoTasks(): List<TodoTask>

    // 查询所有没有完成的待办任务，并按照截止日期升序排列
    @Query("SELECT * FROM todoTask WHERE isCompleted = 0 ORDER BY dueDate ASC")
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
    @Query("DELETE FROM todoTask WHERE id = :taskId")
    fun deleteTodoTaskById(taskId: Long)

    // 清空所有待办任务
    @Query("DELETE FROM todoTask")
    fun deleteAllTodoTasks()
}