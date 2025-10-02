package com.example.nextthingb1.data.repository

import com.example.nextthingb1.data.local.dao.UserDao
import com.example.nextthingb1.data.mapper.toDomain
import com.example.nextthingb1.data.mapper.toEntity
import com.example.nextthingb1.domain.model.User
import com.example.nextthingb1.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().map { it?.toDomain() }
    }

    override suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)?.toDomain()
    }

    override suspend fun createUser(user: User) {
        userDao.insertUser(user.toEntity())
    }

    override suspend fun updateUser(user: User) {
        userDao.updateUser(user.toEntity())
    }

    override suspend fun updateNickname(userId: String, nickname: String) {
        userDao.updateNickname(userId, nickname, System.currentTimeMillis())
    }

    override suspend fun updateAvatar(userId: String, avatarUri: String?) {
        userDao.updateAvatar(userId, avatarUri, System.currentTimeMillis())
    }

    override suspend fun updatePhoneNumber(userId: String, phoneNumber: String?) {
        userDao.updatePhoneNumber(userId, phoneNumber, System.currentTimeMillis())
    }

    override suspend fun updateWechatId(userId: String, wechatId: String?) {
        userDao.updateWechatId(userId, wechatId, System.currentTimeMillis())
    }

    override suspend fun updateQqId(userId: String, qqId: String?) {
        userDao.updateQqId(userId, qqId, System.currentTimeMillis())
    }

    override suspend fun deleteUser(user: User) {
        userDao.deleteUser(user.toEntity())
    }

    override suspend fun deleteAllUsers() {
        userDao.deleteAllUsers()
    }
}
