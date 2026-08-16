package com.nextthing.app.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class AIProviderTest {

    @Test
    fun `DeepSeek defaults to V4 Flash`() {
        assertEquals("deepseek-v4-flash", AIProvider.DEEPSEEK.defaultModel)
    }

    @Test
    fun `legacy DeepSeek models migrate to current default`() {
        assertEquals("deepseek-v4-flash", AIProvider.DEEPSEEK.resolveModel("deepseek-chat"))
        assertEquals("deepseek-v4-flash", AIProvider.DEEPSEEK.resolveModel("deepseek-reasoner"))
    }

    @Test
    fun `explicit non legacy model is preserved`() {
        assertEquals("deepseek-v4-pro", AIProvider.DEEPSEEK.resolveModel("deepseek-v4-pro"))
    }
}
