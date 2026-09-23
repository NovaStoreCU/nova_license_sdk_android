package com.novastore.novalicense

import android.content.Context
import android.content.SharedPreferences

/**
 * Almacenamiento mínimo (claves y valores) usado por el gate de licencia.
 * La implementación por defecto persiste en SharedPreferences.
 */
interface LicenseStorage {
    fun putString(key: String, value: String)

    fun getString(key: String): String?

    fun putLong(key: String, value: Long)

    fun getLong(key: String): Long?

    fun remove(key: String)
}

/** Implementación por defecto basada en [SharedPreferences]. */
class PreferencesLicenseStorage(private val prefs: SharedPreferences) : LicenseStorage {

    constructor(context: Context) : this(
        context.getSharedPreferences("novalicense", Context.MODE_PRIVATE)
    )

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getLong(key: String): Long? =
        if (prefs.contains(key)) prefs.getLong(key, 0L) else null

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}