package com.novastore.novalicense

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Estado de una comprobación de licencia, espejo de `NovaLicenseStatus` del SDK Dart.
 */
enum class NovaLicenseStatus {
    /** La licencia es válida: se debe abrir la app protegida. */
    VALID,

    /** Falta la licencia o el APK está re-firmado: mostrar la pantalla de licencia. */
    INVALID,

    /** Sin red Y sin licencia en caché dentro de la gracia. */
    OFFLINE,

    /** Error inesperado durante la validación (no es un simple offline). */
    ERROR,
}

/** Resultado de una comprobación de licencia. */
data class NovaLicenseResult(
    val status: NovaLicenseStatus,
    val errorMessage: String? = null,
) {
    /** True si la app puede abrirse. */
    val allowed: Boolean get() = status == NovaLicenseStatus.VALID

    /** True cuando la comprobación falló por red/error y no hay licencia fresca. */
    val offline: Boolean get() = status == NovaLicenseStatus.OFFLINE || status == NovaLicenseStatus.ERROR
}

/**
 * Punto de entrada (equivalente a `novaLicenseGuard(...)` del SDK Dart).
 *
 * Uso recomendado: en un `Application` (o antes de lanzar la app) llamar a
 * [configure], y usar `NovaLicenseActivity` como actividad principal del
 * manifest; sobre licencia válida la actividad salta a `config.mainActivity`.
 */
object NovaLicense {

    /** Configuración global del gate (se consume desde `NovaLicenseActivity`). */
    @Volatile
    var config: NovaLicenseGuardConfig? = null
        private set

    /**
     * Registra la configuración del gate. Llamar una vez antes de arrancar la
     * app protegida (por ejemplo en `Application.onCreate`).
     */
    fun configure(config: NovaLicenseGuardConfig) {
        this.config = config
    }

    /** Almacenamiento persistente del dispositivo (SharedPreferences). */
    fun store(context: Context): LicenseStorage = PreferencesLicenseStorage(context)

    /**
     * Comprobación síncrona (hace red): NO llamar desde el hilo de UI.
     */
    fun check(
        context: Context,
        config: NovaLicenseGuardConfig = requireConfig(),
        transport: LicenseTransport = HttpUrlConnectionTransport(),
        storage: LicenseStorage = store(context),
    ): NovaLicenseResult {
        val deviceId = LicenseChecker.resolveDeviceId(config, storage)
        return LicenseChecker.check(config, deviceId, storage, transport)
    }

    /**
     * Comprobación asíncrona en un hilo de fondo; el callback se invoca en el
     * hilo principal. Seguro llamarlo desde el hilo de UI.
     */
    fun checkAsync(
        context: Context,
        config: NovaLicenseGuardConfig = requireConfig(),
        transport: LicenseTransport = HttpUrlConnectionTransport(),
        storage: LicenseStorage = store(context),
        callback: (NovaLicenseResult) -> Unit,
    ) {
        Thread {
            val result = LicenseChecker.check(config, LicenseChecker.resolveDeviceId(config, storage), storage, transport)
            Handler(Looper.getMainLooper()).post { callback(result) }
        }.start()
    }

    /**
     * Abre NovaStore en el navegador en la URL de la app (o la base de la
     * tienda si no hay slug). Devuelve false si no se pudo abrir.
     */
    fun openStore(
        context: Context,
        config: NovaLicenseGuardConfig = requireConfig(),
    ): Boolean {
        val url = LicenseUrls.storeUrl(config)
        if (url.isEmpty()) {
            return false
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) == null) {
                false
            } else {
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun requireConfig(): NovaLicenseGuardConfig =
        config ?: error("NovaLicense: llama a NovaLicense.configure(...) antes de comprobar la licencia.")
}