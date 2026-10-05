package com.novastore.novalicense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LicenseCheckerTest {

    private final long now = System.currentTimeMillis();

    @Test
    public void validOnlineWritesCacheAndOpens() {
        InMemoryStorage storage = new InMemoryStorage();
        NovaLicenseResult result = LicenseChecker.check(
                TestConfigs.testConfig(), "dev1", storage, new FakeTransport(LicenseTransport.Outcome.ok(true)), now);
        assertEquals(NovaLicenseStatus.VALID, result.getStatus());
        assertTrue(result.isAllowed());
        assertEquals("dev1", result.getDeviceCode());
        assertNotNull(storage.getLong(LicenseChecker.KEY_CACHED_AT));
    }

    @Test
    public void invalidOnlineClearsCacheAndShowsRequired() {
        InMemoryStorage storage = new InMemoryStorage();
        storage.putLong(LicenseChecker.KEY_CACHED_AT, now - 60_000L);
        NovaLicenseResult result = LicenseChecker.check(
                TestConfigs.testConfig(), "dev1", storage, new FakeTransport(LicenseTransport.Outcome.ok(false)), now);
        assertEquals(NovaLicenseStatus.INVALID, result.getStatus());
        assertFalse(result.isAllowed());
        assertNull(storage.getLong(LicenseChecker.KEY_CACHED_AT));
    }

    @Test
    public void transportFailureWithoutCacheIsOffline() {
        InMemoryStorage storage = new InMemoryStorage();
        NovaLicenseResult result = LicenseChecker.check(
                TestConfigs.testConfig(), "dev1", storage, new FakeTransport(LicenseTransport.Outcome.failed("network down")), now);
        assertEquals(NovaLicenseStatus.OFFLINE, result.getStatus());
        assertTrue(result.isOffline());
        assertEquals("network down", result.getErrorMessage());
        assertEquals("dev1", result.getDeviceCode());
    }

    @Test
    public void cachedLicenseSurvivesOutageWithinGrace() {
        InMemoryStorage storage = new InMemoryStorage();
        storage.putLong(LicenseChecker.KEY_CACHED_AT, now - 86_400_000L);
        NovaLicenseResult result = LicenseChecker.check(
                TestConfigs.testConfig(), "dev1", storage, new FakeTransport(LicenseTransport.Outcome.failed(null)), now);
        assertEquals(NovaLicenseStatus.VALID, result.getStatus());
    }

    @Test
    public void cachedLicenseExpiredAfterGraceIsOffline() {
        InMemoryStorage storage = new InMemoryStorage();
        storage.putLong(LicenseChecker.KEY_CACHED_AT, now - 4 * 86_400_000L);
        NovaLicenseResult result = LicenseChecker.check(
                TestConfigs.testConfig(), "dev1", storage, new FakeTransport(LicenseTransport.Outcome.failed(null)), now);
        assertEquals(NovaLicenseStatus.OFFLINE, result.getStatus());
    }

    @Test
    public void offlineFallbackWithoutCacheIsOffline() {
        InMemoryStorage storage = new InMemoryStorage();
        NovaLicenseResult result = LicenseChecker.offlineFallback(storage, 3L, null, now, "dev1");
        assertEquals(NovaLicenseStatus.OFFLINE, result.getStatus());
        assertFalse(result.isAllowed());
        assertEquals("dev1", result.getDeviceCode());
    }

    @Test
    public void cachedValidationOpensWithoutNetworkAndKeepsDeviceCode() {
        InMemoryStorage storage = new InMemoryStorage();
        storage.putLong(LicenseChecker.KEY_CACHED_AT, now - 2 * 86_400_000L);
        NovaLicenseResult result =
                LicenseChecker.offlineFallback(storage, 3L, null, now, "dev1");
        assertEquals(NovaLicenseStatus.VALID, result.getStatus());
        assertTrue(result.isAllowed());
        assertEquals("dev1", result.getDeviceCode());
    }

    @Test
    public void explicitDeviceIdIsUsedAndNotPersisted() {
        InMemoryStorage storage = new InMemoryStorage();
        String deviceId = LicenseChecker.resolveDeviceId(
                TestConfigs.testConfig("https://api.novastore.cu/api/v1", "https://novastore.cu", "com.demo.game",
                        "mi-device", null, 3L, null),
                storage);
        assertEquals("mi-device", deviceId);
        assertNull(storage.getString(LicenseChecker.KEY_DEVICE_ID));
    }

    @Test
    public void persistedDeviceIdIsReused() {
        InMemoryStorage storage = new InMemoryStorage();
        storage.putString(LicenseChecker.KEY_DEVICE_ID, "abc123");
        String deviceId = LicenseChecker.resolveDeviceId(TestConfigs.testConfig(), storage);
        assertEquals("abc123", deviceId);
    }

    @Test
    public void generatedDeviceIdIsPersisted() {
        InMemoryStorage storage = new InMemoryStorage();
        String deviceId = LicenseChecker.resolveDeviceId(TestConfigs.testConfig(), storage);
        assertEquals(32, deviceId.length());
        assertTrue(deviceId.matches("[0-9a-f]{32}"));
        assertEquals(deviceId, storage.getString(LicenseChecker.KEY_DEVICE_ID));
    }

    /**
     * Regresión: el transporte le añade "/validate" a la base que recibe. Si el checker le pasa
     * una URL ya completa se pediría "/validate/validate" → 404 → OFFLINE y la app quedaría
     * bloqueada para siempre aunque la licencia sea válida.
     */
    @Test
    public void checkerSendsTheBaseUrlWithoutTheValidateSuffix() {
        final String[] seen = new String[1];
        final String[] seenSha1 = new String[1];
        LicenseTransport recording = new LicenseTransport() {
            @Override
            public Outcome isValid(String packageName, String deviceId, String apkSha1,
                    String baseUrl, long timeoutMillis) {
                seen[0] = baseUrl;
                seenSha1[0] = apkSha1;
                return LicenseTransport.Outcome.ok(true);
            }
        };
        NovaLicenseGuardConfig config = TestConfigs.testConfig(
                "https://api.novastore.cu/api/v1///", "https://novastore.cu", "com.demo.game",
                null, "  AA:BB:CC  ", 3L, null);
        LicenseChecker.check(config, "dev1", new InMemoryStorage(), recording, now);

        assertEquals("https://api.novastore.cu/api/v1", seen[0]);
        assertFalse("no debe duplicar el sufijo", seen[0].endsWith("/validate"));
        // El SHA-1 se normaliza (trim) antes de mandarlo.
        assertEquals("AA:BB:CC", seenSha1[0]);
    }

    @Test
    public void blankApkSha1IsNotSent() {
        final String[] seenSha1 = new String[]{"sentinel"};
        LicenseTransport recording = new LicenseTransport() {
            @Override
            public Outcome isValid(String packageName, String deviceId, String apkSha1,
                    String baseUrl, long timeoutMillis) {
                seenSha1[0] = apkSha1;
                return LicenseTransport.Outcome.ok(true);
            }
        };
        NovaLicenseGuardConfig config = TestConfigs.testConfig(
                "https://api.novastore.cu/api/v1", "https://novastore.cu", "com.demo.game",
                null, "   ", 3L, null);
        LicenseChecker.check(config, "dev1", new InMemoryStorage(), recording, now);
        assertNull(seenSha1[0]);
    }
}
