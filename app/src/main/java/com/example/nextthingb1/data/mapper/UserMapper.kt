package com.example.nextthingb1.data.mapper

import com.example.nextthingb1.data.local.entity.UserEntity
import com.example.nextthingb1.domain.model.User

fun UserEntity.toDomain(): User {
    return User(
        id = id,
        nickname = nickname,
        avatarUri = avatarUri,
        phoneNumber = phoneNumber,
        wechatId = wechatId,
        qqId = qqId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        nickname = nickname,
        avatarUri = avatarUri,
        phoneNumber = phoneNumber,
        wechatId = wechatId,
        qqId = qqId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
