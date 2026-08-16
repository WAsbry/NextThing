package com.nextthing.app.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationProviderAvailabilityTest {

    @Test
    fun providerAvailable_onlyWhenAmapIsInitialized() {
        assertTrue(
            hasAvailableLocationProvider(
                amapInitialized = true
            )
        )
        assertFalse(
            hasAvailableLocationProvider(
                amapInitialized = false
            )
        )
    }

    @Test
    fun statusError_distinguishesMissingKeyAndNoProvider() {
        val message = locationServiceStatusError(
            hasPermission = true,
            isEnabled = true,
            amapInitialized = false,
            amapInitializationFailure = LocationServiceFailure.AMAP_KEY_MISSING,
            lastRequestFailure = LocationServiceFailure.NO_PROVIDER
        )

        assertEquals(
            "无可用定位服务：高德定位 API Key 未配置",
            message
        )
    }

    @Test
    fun statusError_reportsRuntimeTimeoutWhenProviderExists() {
        val message = locationServiceStatusError(
            hasPermission = true,
            isEnabled = true,
            amapInitialized = true,
            amapInitializationFailure = null,
            lastRequestFailure = LocationServiceFailure.TIMEOUT
        )

        assertEquals("定位请求超时", message)
    }

    @Test
    fun statusError_reportsMissingAmapKeyWhenNoProviderExists() {
        val message = locationServiceStatusError(
            hasPermission = true,
            isEnabled = true,
            amapInitialized = false,
            amapInitializationFailure = LocationServiceFailure.AMAP_KEY_MISSING,
            lastRequestFailure = null
        )

        assertEquals("无可用定位服务：高德定位 API Key 未配置", message)
    }
}
