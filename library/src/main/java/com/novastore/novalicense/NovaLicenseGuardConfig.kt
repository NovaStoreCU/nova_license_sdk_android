package com.novastore.novalicense

import android.app.Activity

/**
 * Configuración global del gate de licencias de NovaStore.
 *
 * Es el equivalente Android de `NovaLicenseGuardConfig` del SDK Dart: mismas
 * opciones más [mainActivity], la actividad de la app protegida que se abre
 * cuando la licencia es válida.
 */
data class NovaLicenseGuardConfig(
    /**
     * Base de la API v1 de NovaStore, por ejemplo `https://novastore.cu/api/v1`.
     */
    val apiBase: String,
    /**
     * Base pública de la tienda NovaStore (por ejemplo `https://novastore.cu`),
     * usada por el botón "Abrir NovaStore".
     */
    val storeUrl: String,
    /**
     * `package_name` de esta app tal y como está registrado en NovaStore
     * (por ejemplo `com.miempresa.mijuego`). Es lo que identifica la licencia.
     */
    val packageName: String,
    /**
     * Slug de la app en la tienda. Si va, el botón de compra abre el detalle
     * directo (`{storeUrl}/apps/{storeSlug}`).
     */
    val storeSlug: String? = null,
    /**
     * Identificador de dispositivo estable. Cuando es nulo el SDK lee el
     * persistido o genera uno nuevo en el primer arranque y lo guarda.
     */
    val deviceId: String? = null,
    /**
     * SHA-1 de firma del APK instalado (el certificado de release). Cuando se
     * informa, la tienda rechaza APKs re-firmados con otra clave.
     */
    val apkSha1: String? = null,
    /**
     * Gracia offline en días: cuánto tiempo se confía en una licencia ya
     * válida sin acceso a red. Por defecto **3 días**.
     */
    val graceDays: Long = 3,
    /**
     * Timeout de la petición de validación en milisegundos (por defecto 10 s).
     */
    val httpTimeoutMillis: Long = 10_000,
    /**
     * Color de acento de la pantalla de licencia (ARGB). Por defecto un
     * violeta tipo tienda.
     */
    val brandColor: Int? = null,
    /**
     * Actividad principal de la app protegida. Se abre cuando la licencia es
     * válida; requerida por el patrón de *launcher activity* (NovaLicenseActivity).
     */
    val mainActivity: Class<out Activity>? = null,
)