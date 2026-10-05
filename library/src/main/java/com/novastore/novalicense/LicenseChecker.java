package com.novastore.novalicense;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * Lógica de verificación (licencia válida / inválida / offline) sin depender de Android.
 *
 * <p>Espejo 1:1 de {@code NovaLicense.verify(...)} del SDK Dart: misma gracia offline, mismo
 * caché de validación, mismo tratamiento del {@code apk_sha1} anti-refirma.
 */
public final class LicenseChecker {

    /** Clave de preferencias con el código de activación del dispositivo. */
    public static final String KEY_DEVICE_ID = "nova_license_sdk_device_id";

    /** Clave de preferencias con el timestamp (ms) de la última validación válida. */
    public static final String KEY_CACHED_AT = "nova_license_sdk_cached_valid_at";

    private static final long MILLIS_PER_DAY = 86_400_000L;
    private static final Pattern SEPARATORS = Pattern.compile("[\\s-]+");

    private LicenseChecker() {
    }

    /**
     * Devuelve el código de activación a usar: el de la config, el ya persistido, o genera uno
     * nuevo de 32 hex (y lo guarda) en el primer arranque.
     */
    public static String resolveDeviceId(NovaLicenseGuardConfig config, LicenseStorage storage) {
        String explicit = config.getDeviceId();
        String trimmedExplicit = explicit == null ? null : explicit.trim();
        if (trimmedExplicit != null && !trimmedExplicit.isEmpty()) {
            return trimmedExplicit;
        }
        String persisted = storage.getString(KEY_DEVICE_ID);
        if (persisted != null && !persisted.isEmpty()) {
            return persisted;
        }
        String generated = randomDeviceId();
        storage.putString(KEY_DEVICE_ID, generated);
        return generated;
    }

    /** 32 caracteres hexadecimales minúsculos, con entropía criptográfica. */
    public static String randomDeviceId() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder hex = new StringBuilder(32);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    /**
     * Normaliza un código de activación pegado a mano: quita espacios y guiones (el SDK Dart hace
     * lo mismo en {@code WalletApi.activateDevice}).
     */
    public static String normalizeDeviceCode(String code) {
        if (code == null) {
            return null;
        }
        return SEPARATORS.matcher(code).replaceAll("");
    }

    /** Verificación completa; usa la hora actual del sistema. */
    public static NovaLicenseResult check(
            NovaLicenseGuardConfig config,
            String deviceId,
            LicenseStorage storage,
            LicenseTransport transport) {
        return check(config, deviceId, storage, transport, System.currentTimeMillis());
    }

    /**
     * Verificación completa con instante inyectable (los tests pasan el tiempo a mano).
     *
     * @return {@link NovaLicenseStatus#VALID} si la licencia vale (y cachea el instante),
     *         {@link NovaLicenseStatus#INVALID} si el servidor dijo que no (y limpia el caché),
     *         o el resultado offline si falló el transporte.
     */
    public static NovaLicenseResult check(
            NovaLicenseGuardConfig config,
            String deviceId,
            LicenseStorage storage,
            LicenseTransport transport,
            long nowMillis) {
        // La base va SIN "/validate": el transporte es el que le añade el endpoint
        // (`POST {base}/validate`). Pasarle la URL completa duplicaría el sufijo.
        String baseUrl = LicenseUrls.normalizeBase(config.getApiBase());
        String apkSha1 = config.getApkSha1();
        if (apkSha1 != null) {
            apkSha1 = apkSha1.trim();
            if (apkSha1.isEmpty()) {
                apkSha1 = null;
            }
        }
        LicenseTransport.Outcome outcome =
                transport.isValid(config.getPackageName(), deviceId, apkSha1, baseUrl,
                        config.getHttpTimeoutMillis());
        if (outcome.isOk()) {
            if (outcome.isValid()) {
                storage.putLong(KEY_CACHED_AT, nowMillis);
                return new NovaLicenseResult(NovaLicenseStatus.VALID, null, deviceId);
            }
            storage.remove(KEY_CACHED_AT);
            return new NovaLicenseResult(NovaLicenseStatus.INVALID, null, deviceId);
        }
        return offlineFallback(storage, config.getGraceDays(), outcome.getMessage(), nowMillis, deviceId);
    }

    /** Verificación offline usando la hora actual del sistema. */
    public static NovaLicenseResult offlineFallback(
            LicenseStorage storage,
            long graceDays,
            String errorMessage) {
        return offlineFallback(storage, graceDays, errorMessage, System.currentTimeMillis(), null);
    }

    /**
     * Sin red: abre la app solo si hay una validación válida cacheada dentro de la gracia.
     *
     * <p>Fuera de la gracia (o sin caché) devuelve OFFLINE con el mensaje del error de red, para
     * que la UI ofrezca "Reintentar" en vez de fingir que la licencia es válida.
     */
    public static NovaLicenseResult offlineFallback(
            LicenseStorage storage,
            long graceDays,
            String errorMessage,
            long nowMillis,
            String deviceCode) {
        Long cachedAt = storage.getLong(KEY_CACHED_AT);
        if (cachedAt == null) {
            return new NovaLicenseResult(NovaLicenseStatus.OFFLINE, errorMessage, deviceCode);
        }
        long age = nowMillis - cachedAt;
        if (age <= graceDays * MILLIS_PER_DAY) {
            return new NovaLicenseResult(NovaLicenseStatus.VALID, null, deviceCode);
        }
        return new NovaLicenseResult(NovaLicenseStatus.OFFLINE, errorMessage, deviceCode);
    }
}
