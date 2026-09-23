package com.novastore.novalicense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseCheckerTest {

    private val now = System.currentTimeMillis()

    @Test
    fun validOnlineWritesCacheAndOpens() {
        val storage = InMemoryStorage()
        val result = LicenseChecker.check(
            testConfig(), "dev1", storage, FakeTransport(LicenseTransport.Outcome.Ok(true)), now,
        )
        assertEquals(NovaLicenseStatus.VALID, result.status)
        assertTrue(result.allowed)
        assertNotNull(storage.getLong(LicenseChecker.KEY_CACHED_AT))
    }

    @Test
    fun invalidOnlineClearsCacheAndShowsRequired() {
        val storage = InMemoryStorage().apply {
            putLong(LicenseChecker.KEY_CACHED_AT, now - 60_000)
        }
        val result = LicenseChecker.check(
            testConfig(), "dev1", storage, FakeTransport(LicenseTransport.Outcome.Ok(false)), now,
        )
        assertEquals(NovaLicenseStatus.INVALID, result.status)
        assertTrue(!result.allowed)
        assertNull(storage.getLong(LicenseChecker.KEY_CACHED_AT))
    }

    @Test
    fun transportFailureWithoutCacheIsOffline() {
        val storage = InMemoryStorage()
        val result = LicenseChecker.check(
            testConfig(), "dev1", storage, FakeTransport(LicenseTransport.Outcome.Failed("network down")), now,
        )
        assertEquals(NovaLicenseStatus.OFFLINE, result.status)
        assertTrue(result.offline)
        assertEquals("network down", result.errorMessage)
    }

    @Test
    fun cachedLicenseSurvivesOutageWithinGrace() {
        val storage = InMemoryStorage().apply {
            putLong(LicenseChecker.KEY_CACHED_AT, now - 86_400_000L)
        }
        val result = LicenseChecker.check(
            testConfig(), "dev1", storage, FakeTransport(LicenseTransport.Outcome.Failed(null)), now,
        )
        assertEquals(NovaLicenseStatus.VALID, result.status)
    }

    @Test
    fun cachedLicenseExpiredAfterGraceIsOffline() {
        val storage = InMemoryStorage().apply {
            putLong(LicenseChecker.KEY_CACHED_AT, now - 4 * 86_400_000L)
        }
        val result = LicenseChecker.check(
            testConfig(), "dev1", storage, FakeTransport(LicenseTransport.Outcome.Failed(null)), now,
        )
        assertEquals(NovaLicenseStatus.OFFLINE, result.status)
    }

    @Test
    fun offlineFallbackWithoutCacheIsOffline() {
        val storage = InMemoryStorage()
        val result = LicenseChecker.offlineFallback(storage, 3, null, now)
        assertEquals(NovaLicenseStatus.OFFLINE, result.status)
    }

    @Test
    fun explicitDeviceIdIsUsedAndNotPersisted() {
        val storage = InMemoryStorage()
        val deviceId = LicenseChecker.resolveDeviceId(testConfig(deviceId = "mi-device"), storage)
        assertEquals("mi-device", deviceId)
        assertNull(storage.getString(LicenseChecker.KEY_DEVICE_ID))
    }

    @Test
    fun persistedDeviceIdIsReused() {
        val storage = InMemoryStorage().apply {
            putString(LicenseChecker.KEY_DEVICE_ID, "abc123")
        }
        val deviceId = LicenseChecker.resolveDeviceId(testConfig(), storage)
        assertEquals("abc123", deviceId)
    }

    @Test
    fun generatedDeviceIdIsPersisted() {
        val storage = InMemoryStorage()
        val deviceId = LicenseChecker.resolveDeviceId(testConfig(), storage)
        assertEquals(32, deviceId.length)
        assertTrue(deviceId.matches(Regex("[0-9a-f]{32}")))
        assertEquals(deviceId, storage.getString(LicenseChecker.KEY_DEVICE_ID))
    }
}