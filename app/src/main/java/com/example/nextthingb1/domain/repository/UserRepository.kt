package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    fun getCurrentUser(): Flow<User?>

    suspend fun getUserById(userId: String): User?

    suspend fun createUser(user: User)

    suspend fun updateUser(user: User)

    suspend fun updateNickname(userId: String, nickname: String)

    suspend fun updateAvatar(userId: String, avatarUri: String?)

    suspend fun updatePhoneNumber(userId: String, phoneNumber: String?)

    suspend fun updateWechatId(userId: String, wechatId: String?)

    suspend fun updateQqId(userId: String, qqId: String?)

    suspend fun deleteUser(user: User)

    suspend fun deleteAllUsers()
}
