package com.nextthing.app.data.mapper

import com.nextthing.app.data.local.entity.UserEntity
import com.nextthing.app.domain.model.User

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
