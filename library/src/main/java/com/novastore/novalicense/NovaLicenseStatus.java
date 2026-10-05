package com.novastore.novalicense;

/**
 * Estado de una comprobación de licencia, espejo de {@code NovaLicenseStatus} del SDK Dart.
 */
public enum NovaLicenseStatus {

    /** La licencia es válida: se debe abrir la app protegida. */
    VALID,

    /** Falta la licencia o el APK está re-firmado: mostrar la pantalla de licencia. */
    INVALID,

    /** Sin red Y sin licencia en caché dentro de la gracia. */
    OFFLINE,

    /** Error inesperado durante la validación (no es un simple offline). */
    ERROR,
}
