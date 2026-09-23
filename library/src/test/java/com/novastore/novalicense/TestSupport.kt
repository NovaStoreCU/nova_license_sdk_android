package com.novastore.novalicense

/** Almacenamiento en memoria para tests (reemplaza SharedPreferences). */
class InMemoryStorage : LicenseStorage {
    private val strings = mutableMapOf<String, String>()
    private val longs = mutableMapOf<String, Long>()

    override fun putString(key: String, value: String) {
        strings[key] = value
    }

    override fun getString(key: String): String? = strings[key]

    override fun putLong(key: String, value: Long) {
        longs[key] = value
    }

    override fun getLong(key: String): Long? = longs[key]

    override fun remove(key: String) {
        strings.remove(key)
        longs.remove(key)
    }
}

/** Transporte falso con un resultado fijo. */
class FakeTransport(private val outcome: LicenseTransport.Outcome) : LicenseTransport {
    override fun isValid(
        packageName: String,
        deviceId: String,
        apkSha1: String?,
        baseUrl: String,
        timeoutMillis: Long,
    ): LicenseTransport.Outcome = outcome
}

internal fun testConfig(
    apiBase: String = "https://novastore.cu/api/v1",
    storeUrl: String = "https://novastore.cu",
    packageName: String = "com.demo.game",
    deviceId: String? = null,
    apkSha1: String? = null,
    graceDays: Long = 3,
    storeSlug: String? = null,
): NovaLicenseGuardConfig = NovaLicenseGuardConfig(
    apiBase = apiBase,
    storeUrl = storeUrl,
    packageName = packageName,
    deviceId = deviceId,
    apkSha1 = apkSha1,
    graceDays = graceDays,
    storeSlug = storeSlug,
)