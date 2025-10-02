package com.example.nextthingb1.domain.usecase

import com.example.nextthingb1.domain.model.User
import com.example.nextthingb1.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

data class UserUseCases(
    val getCurrentUser: GetCurrentUserUseCase,
    val createUser: CreateUserUseCase,
    val updateUser: UpdateUserUseCase,
    val updateNickname: UpdateNicknameUseCase,
    val updateAvatar: UpdateAvatarUseCase,
    val updatePhoneNumber: UpdatePhoneNumberUseCase,
    val updateWechatId: UpdateWechatIdUseCase,
    val updateQqId: UpdateQqIdUseCase,
    val logout: LogoutUseCase
)

class GetCurrentUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<User?> = repository.getCurrentUser()
}

class CreateUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(nickname: String): User {
        val userId = generateUserId()
        val user = User(
            id = userId,
            nickname = nickname
        )
        repository.createUser(user)
        return user
    }

    private fun generateUserId(): String {
        // 生成8位随机字符串
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).uppercase()
    }
}

class UpdateUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(user: User) {
        repository.updateUser(user)
    }
}

class UpdateNicknameUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String, nickname: String) {
        repository.updateNickname(userId, nickname)
    }
}

class UpdateAvatarUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String, avatarUri: String?) {
        repository.updateAvatar(userId, avatarUri)
    }
}

class UpdatePhoneNumberUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String, phoneNumber: String?) {
        repository.updatePhoneNumber(userId, phoneNumber)
    }
}

class UpdateWechatIdUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String, wechatId: String?) {
        repository.updateWechatId(userId, wechatId)
    }
}

class UpdateQqIdUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String, qqId: String?) {
        repository.updateQqId(userId, qqId)
    }
}

class LogoutUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke() {
        repository.deleteAllUsers()
    }
}
