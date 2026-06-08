package com.nextthing.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonObject

object AIJsonHelper {

    fun parseAIJson(gson: Gson, raw: String): JsonObject {
        var cleanJson = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        // 1. 直接解析
        try {
            return gson.fromJson(cleanJson, JsonObject::class.java)
        } catch (_: Exception) {}

        // 2. 提取 { ... } 再解析
        val start = cleanJson.indexOf('{')
        val end = cleanJson.lastIndexOf('}')
        if (start >= 0 && end > start) {
            try {
                return gson.fromJson(cleanJson.substring(start, end + 1), JsonObject::class.java)
            } catch (_: Exception) {}
        }

        // 3. 提取 { 到末尾，尝试修复截断的 JSON
        if (start >= 0) {
            val repaired = repairTruncatedJson(cleanJson.substring(start))
            try {
                return gson.fromJson(repaired, JsonObject::class.java)
            } catch (_: Exception) {}
        }

        throw Exception("无法从 AI 响应中提取 JSON: ${raw.take(100)}")
    }

    private fun repairTruncatedJson(json: String): String {
        var result = json
        var braceCount = 0
        var bracketCount = 0
        var inString = false
        var escape = false

        for (c in result) {
            if (escape) { escape = false; continue }
            if (c == '\\') { escape = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (inString) continue
            when (c) {
                '{' -> braceCount++
                '}' -> braceCount--
                '[' -> bracketCount++
                ']' -> bracketCount--
            }
        }

        if (inString) result += "\""
        repeat(maxOf(0, bracketCount)) { result += "]" }
        repeat(maxOf(0, braceCount)) { result += "}" }

        return result
    }
}
