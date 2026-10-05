package com.novastore.novalicense;

/**
 * Almacenamiento mínimo (claves y valores) usado por el gate de licencia.
 *
 * <p>La implementación por defecto persiste en {@code SharedPreferences}. Mantiene el contrato del
 * SDK 1.0.0 (incluidos {@code putLong}/{@code getLong}) porque el archivo de preferencias es el
 * mismo: un valor escrito como {@code long} y leído como {@code String} lanza
 * {@code ClassCastException} en Android.
 */
public interface LicenseStorage {

    void putString(String key, String value);

    String getString(String key);

    /** Guarda el instante de la última validación válida ({@link LicenseChecker#KEY_CACHED_AT}). */
    void putLong(String key, long value);

    /** Instante guardado, o {@code null} si la clave no existe. */
    Long getLong(String key);

    void remove(String key);
}
