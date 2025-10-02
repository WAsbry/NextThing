package com.example.nextthingb1.data.local.dao

import androidx.room.*
import com.example.nextthingb1.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET nickname = :nickname, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateNickname(userId: String, nickname: String, updatedAt: Long)

    @Query("UPDATE users SET avatarUri = :avatarUri, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateAvatar(userId: String, avatarUri: String?, updatedAt: Long)

    @Query("UPDATE users SET phoneNumber = :phoneNumber, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updatePhoneNumber(userId: String, phoneNumber: String?, updatedAt: Long)

    @Query("UPDATE users SET wechatId = :wechatId, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateWechatId(userId: String, wechatId: String?, updatedAt: Long)

    @Query("UPDATE users SET qqId = :qqId, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateQqId(userId: String, qqId: String?, updatedAt: Long)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
