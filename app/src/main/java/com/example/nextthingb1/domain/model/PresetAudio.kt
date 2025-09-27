package com.example.nextthingb1.domain.model

/**
 * 预置音频资源枚举
 */
enum class PresetAudio(
    val displayName: String,
    val fileName: String,
    val description: String,
    val category: String
) {
    // 轻快提示音
    DING("叮当", "ding.wav", "短促清脆的叮当声", "轻快"),
    CHIME("风铃", "chime.wav", "轻柔的风铃声", "轻快"),
    BELL("铃声", "bell.wav", "清澈的铃声", "轻快"),

    // 标准提示音
    BEEP("哔哔", "beep.wav", "双音哔哔声", "标准"),
    TONE("音调", "tone.wav", "简单音调", "标准"),
    CLICK("点击", "click.wav", "点击声效", "标准"),

    // 紧急提示音
    ALARM("警报", "alarm.wav", "紧急警报声", "紧急"),
    SIREN("警笛", "siren.wav", "警笛声", "紧急"),
    HORN("喇叭", "horn.wav", "汽车喇叭声", "紧急"),

    // 自然音效
    BIRD("鸟叫", "bird.wav", "清晨鸟叫声", "自然"),
    WATER("水滴", "water.wav", "水滴声", "自然"),
    WIND("风声", "wind.wav", "轻柔风声", "自然");

    companion object {
        /**
         * 根据类别获取音频列表
         */
        fun getByCategory(category: String): List<PresetAudio> {
            return values().filter { it.category == category }
        }

        /**
         * 获取所有类别
         */
        fun getCategories(): List<String> {
            return values().map { it.category }.distinct()
        }

        /**
         * 根据文件名查找音频
         */
        fun findByFileName(fileName: String): PresetAudio? {
            return values().find { it.fileName == fileName }
        }
    }
}