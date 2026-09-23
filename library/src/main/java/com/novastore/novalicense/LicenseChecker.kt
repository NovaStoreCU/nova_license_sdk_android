package com.novastore.novalicense

import java.security.SecureRandom

/**
 * Lógica pura de la licencia: resolución del `device_id`, validación contra la
 * API y gracia offline. Espejo de la lógica del SDK Dart.
 */
object LicenseChecker {

    const val KEY_DEVICE_ID = "nova_license_sdk_device_id"
    const val KEY_CACHED_AT = "nova_license_sdk_cached_valid_at"

    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * Resuelve el identificador de dispositivo: el de la config si viene, si no
     * el persistido, si no genera uno nuevo de 32 hex y lo guarda.
     */
    fun resolveDeviceId(config: NovaLicenseGuardConfig, storage: LicenseStorage): String {
        val explicit = config.deviceId?.trim()
        if (!explicit.isNullOrEmpty()) {
            return explicit
        }
        val persisted = storage.getString(KEY_DEVICE_ID)
        if (!persisted.isNullOrEmpty()) {
            return persisted
        }
        val fresh = randomDeviceId()
        storage.putString(KEY_DEVICE_ID, fresh)
        return fresh
    }

    /** Genera un `device_id` de 16 bytes aleatorios como hex (32 caracteres). */
    fun randomDeviceId(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Quita espacios y guiones de un código de dispositivo (pe. para copiarlo). */
    fun normalizeDeviceCode(code: String): String = code.replace(Regex("[\\s-]+"), "")

    /**
     * Ejecuta la validación: llama a la API y decide el estado según su
     * respuesta, aplicando la gracia offline ante fallos de red.
     */
    fun check(
        config: NovaLicenseGuardConfig,
        deviceId: String,
        storage: LicenseStorage,
        transport: LicenseTransport,
        nowMillis: Long = System.currentTimeMillis(),
    ): NovaLicenseResult {
        val outcome = transport.isValid(
            packageName = config.packageName,
            deviceId = deviceId,
            apkSha1 = config.apkSha1?.trim()?.takeIf { it.isNotEmpty() },
            baseUrl = LicenseUrls.normalizeBase(config.apiBase),
            timeoutMillis = config.httpTimeoutMillis,
        )
        return when (outcome) {
            is LicenseTransport.Outcome.Ok ->
                if (outcome.valid) {
                    storage.putLong(KEY_CACHED_AT, nowMillis)
                    NovaLicenseResult(NovaLicenseStatus.VALID, deviceCode = deviceId)
                } else {
                    storage.remove(KEY_CACHED_AT)
                    NovaLicenseResult(NovaLicenseStatus.INVALID, deviceCode = deviceId)
                }

            is LicenseTransport.Outcome.Failed ->
                offlineFallback(storage, config.graceDays, outcome.message, nowMillis, deviceId)
        }
    }

    /**
     * Fallback sin red: si hay una licencia válida cacheadada dentro de la
     * gracia, se permite abrir; si no, estado [NovaLicenseStatus.OFFLINE].
     */
    fun offlineFallback(
        storage: LicenseStorage,
        graceDays: Long,
        errorMessage: String?,
        nowMillis: Long = System.currentTimeMillis(),
        deviceCode: String? = null,
    ): NovaLicenseResult {
        val cachedAt = storage.getLong(KEY_CACHED_AT)
        if (cachedAt == null) {
            return NovaLicenseResult(NovaLicenseStatus.OFFLINE, errorMessage, deviceCode)
        }
        val age = nowMillis - cachedAt
        return if (age <= graceDays * MILLIS_PER_DAY) {
            NovaLicenseResult(NovaLicenseStatus.VALID, deviceCode = deviceCode)
        } else {
            NovaLicenseResult(NovaLicenseStatus.OFFLINE, errorMessage, deviceCode)
        }
    }
}