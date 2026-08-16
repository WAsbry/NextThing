package com.nextthing.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AudioPreprocessorImplTest {

    @Test
    fun extractMFCC_withPcmAudio_returnsFinite39DimensionFeatures() {
        val sampleRate = 16_000
        val samples = ShortArray(sampleRate) { index ->
            (sin(2.0 * PI * 440.0 * index / sampleRate) * Short.MAX_VALUE * 0.25).toInt().toShort()
        }

        val features = AudioPreprocessorImpl().extractMFCC(samples, sampleRate)

        assertEquals(39, features.size)
        assertTrue(features.all { it.isNotEmpty() })
        assertTrue(features.flatMap { it.asIterable() }.all { it.isFinite() })
    }
}
