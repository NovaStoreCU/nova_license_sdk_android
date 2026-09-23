package com.novastore.novalicense

import org.junit.Assert.assertEquals
import org.junit.Test

class LicenseUrlsTest {

    @Test
    fun normalizeBaseStripsTrailingSlashes() {
        assertEquals("https://novastore.cu/api/v1", LicenseUrls.normalizeBase("https://novastore.cu/api/v1///"))
        assertEquals("https://novastore.cu/api/v1", LicenseUrls.normalizeBase("https://novastore.cu/api/v1"))
    }

    @Test
    fun storeUrlWithSlug() {
        val config = testConfig(storeUrl = "https://novastore.cu", storeSlug = "mi-juego")
        assertEquals("https://novastore.cu/apps/mi-juego", LicenseUrls.storeUrl(config))
    }

    @Test
    fun storeUrlWithoutSlug() {
        val config = testConfig(storeUrl = "https://novastore.cu/", storeSlug = null)
        assertEquals("https://novastore.cu", LicenseUrls.storeUrl(config))
    }
}

class DeviceIdTest {

    @Test
    fun randomDeviceIdIs32HexChars() {
        val id = LicenseChecker.randomDeviceId()
        assertEquals(32, id.length)
        assertEquals(true, id.matches(Regex("[0-9a-f]{32}")))
    }

    @Test
    fun normalizeDeviceCodeRemovesSpacesAndDashes() {
        assertEquals("abcd1234", LicenseChecker.normalizeDeviceCode("ab-cd 1234"))
        assertEquals("abcd1234", LicenseChecker.normalizeDeviceCode("abcd1234"))
    }
}