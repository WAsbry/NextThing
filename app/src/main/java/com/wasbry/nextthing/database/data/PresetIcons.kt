package com.wasbry.nextthing.database.data

import android.content.Context
import android.util.Log
import com.wasbry.nextthing.R
import com.wasbry.nextthing.database.model.TimeType

// data/PresetIcons.kt
object PresetIcons {

    val tag = "PresetIcons"

    // 硬编码：资源名与描述的映射（不含分类前缀）
    private val iconDescriptionMap = mapOf(
        // 预置图标：健身
        "arm_training" to "手臂训练",
        "basketball" to "篮球",
        "bench_press" to "卧推",
        "cycling" to "骑行",
        "deadlift" to "硬拉",
        "football" to "足球",
        "jogging" to "慢跑",
        "rope_skipping" to "跳绳",
        "sprinting" to "快跑",
        "rope_skipping" to "跳绳",
        "squat" to "深蹲",
        "swimming" to "游泳",
        "yoga" to "瑜伽",
        // 预置图标：工作
        "meeting" to "开会",
        "working" to "工作",
        // 预置图标：生活
        "cooking" to "做饭",
        "reading" to "阅读",
        "house_working" to "打扫",
        // 预置图标：娱乐
        "movie" to "电影",
        "party" to "聚会",
        "shopping" to "购物",
    )

    // 分类与前缀的映射
    private val categoryPrefixMap = mapOf(
        "fitness" to "icon_fitness_",
        "work" to "icon_work_",
        "life" to "icon_life_",
        "entertainment" to "icon_entertainment_"
    )

    // 生成所有预置TimeType对象
    fun generatePresetTimeTypes(context: Context): List<TimeType> {
        Log.d(tag,"生成所有预置TimeType对象")
        val resources = context.resources
        val packageName = context.packageName
        val fields = R.mipmap::class.java.fields
        val result = mutableListOf<TimeType>()

        // 遍历所有分类
        categoryPrefixMap.forEach { (category, prefix) ->
            // 遍历所有mipmap资源
            fields.forEach { field ->
                if (field.name.startsWith(prefix)) {
                    try {
                        // 获取资源名（如"icon_fitness_swimming"）
                        val resName = field.name
                        Log.d(tag,"获取资源名，prefix = ${prefix} resName = ${resName}")
                        // 获取描述（通过硬编码映射）
                        Log.d(tag,"准备获取描述，prefix = ${prefix} indexColumn = ${resName.removePrefix(prefix)}")
                        val description = iconDescriptionMap[resName.removePrefix(prefix)]
                            ?: "未知图标"
                        Log.d(tag,"description = ${description}")

                        result.add(
                            TimeType(
                                resPath = resName,
                                description = description,
                                category = category,
                                isUserUploaded = false
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("PresetIcons", "Error processing resource: ${field.name}", e)
                    }
                }
            }
        }

        return result
    }
}