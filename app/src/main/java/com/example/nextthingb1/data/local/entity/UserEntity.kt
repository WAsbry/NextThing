package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String, // 8位随机字符串
    val nickname: String, // 昵称
    val avatarUri: String? = null, // 头像URI，可为空
    val phoneNumber: String? = null, // 手机号，可为空
    val wechatId: String? = null, // 微信ID，可为空
    val qqId: String? = null, // QQ号，可为空
    val createdAt: Long = System.currentTimeMillis(), // 创建时间
    val updatedAt: Long = System.currentTimeMillis() // 更新时间
)
