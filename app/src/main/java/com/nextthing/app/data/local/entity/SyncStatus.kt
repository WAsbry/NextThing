package com.nextthing.app.data.local.entity

/**
 * 数据同步状态
 */
enum class SyncStatus {
    PENDING,      // 等待同步（新建/修改）
    SYNCED,       // 已同步
    CONFLICT,     // 冲突需要解决
    ERROR         // 同步失败
}
