package com.wasbry.nextthing.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wasbry.nextthing.database.model.Category

// 使用 @Dao 注解将该接口标记为 Room 数据库的 DAO 接口
@Dao
interface CategoryDao {

    // 查询所有分类
    @Query("SELECT * FROM category")
    fun getAllCategories(): List<Category>

    // 根据分类 ID 查询单个分类
    @Query("SELECT * FROM category WHERE id = :categoryId")
    fun getCategoryById(categoryId: Long): Category?

    // 插入单个分类，如果发生冲突（如主键重复），则替换原有数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategory(category: Category): Long

    // 插入多个分类，如果发生冲突，则替换原有数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategories(categories: List<Category>)

    // 更新单个分类
    @Update
    fun updateCategory(category: Category)

    // 更新多个分类
    @Update
    fun updateCategories(categories: List<Category>)

    // 删除单个分类
    @Delete
    fun deleteCategory(category: Category)

    // 根据分类 ID 删除单个分类
    @Query("DELETE FROM category WHERE id = :categoryId")
    fun deleteCategoryById(categoryId: Long)

    // 删除所有分类
    @Query("DELETE FROM category")
    fun deleteAllCategories()
}