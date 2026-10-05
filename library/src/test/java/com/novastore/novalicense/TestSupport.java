package com.novastore.novalicense;

import java.util.HashMap;
import java.util.Map;

/** Almacenamiento en memoria para tests (reemplaza SharedPreferences). */
final class InMemoryStorage implements LicenseStorage {

    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, Long> longs = new HashMap<>();

    @Override
    public void putString(String key, String value) {
        strings.put(key, value);
    }

    @Override
    public String getString(String key) {
        return strings.get(key);
    }

    @Override
    public void putLong(String key, long value) {
        longs.put(key, value);
    }

    @Override
    public Long getLong(String key) {
        return longs.get(key);
    }

    @Override
    public void remove(String key) {
        strings.remove(key);
        longs.remove(key);
    }
}

/** Transporte falso con un resultado fijo. */
final class FakeTransport implements LicenseTransport {

    private final LicenseTransport.Outcome outcome;

    FakeTransport(LicenseTransport.Outcome outcome) {
        this.outcome = outcome;
    }

    @Override
    public Outcome isValid(
            String packageName,
            String deviceId,
            String apkSha1,
            String baseUrl,
            long timeoutMillis) {
        return outcome;
    }
}

/** Configuraciones de test con los valores por defecto del SDK. */
final class TestConfigs {

    private TestConfigs() {
    }

    private static final String API_BASE = "https://api.novastore.cu/api/v1";
    private static final String STORE_URL = "https://novastore.cu";

    static NovaLicenseGuardConfig testConfig() {
        return testConfig(null, STORE_URL);
    }

    /** @param storeSlug slug en la tienda (null = home). @param storeUrl base de la web. */
    static NovaLicenseGuardConfig testConfig(String storeSlug, String storeUrl) {
        return testConfig(API_BASE, storeUrl, "com.demo.game", null, null, 3L, storeSlug);
    }

    static NovaLicenseGuardConfig testConfig(
            String apiBase,
            String storeUrl,
            String packageName,
            String deviceId,
            String apkSha1,
            long graceDays,
            String storeSlug) {
        // storeUrl es obligatorio en la config; null/vacío = el de producción.
        String store = storeUrl == null || storeUrl.trim().isEmpty() ? STORE_URL : storeUrl;
        return NovaLicenseGuardConfig.builder(apiBase, store, packageName)
                .deviceId(deviceId)
                .apkSha1(apkSha1)
                .graceDays(graceDays)
                .storeSlug(storeSlug)
                .build();
    }
}

