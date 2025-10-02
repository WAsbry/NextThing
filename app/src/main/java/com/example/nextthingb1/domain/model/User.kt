package com.example.nextthingb1.domain.model

data class User(
    val id: String,
    val nickname: String,
    val avatarUri: String? = null,
    val phoneNumber: String? = null,
    val wechatId: String? = null,
    val qqId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
