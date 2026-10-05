package com.novastore.novalicense;

import android.content.Context;
import android.content.SharedPreferences;

/** Implementación por defecto de {@link LicenseStorage} basada en {@link SharedPreferences}. */
public final class PreferencesLicenseStorage implements LicenseStorage {

    private final SharedPreferences preferences;

    public PreferencesLicenseStorage(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences("novalicense", Context.MODE_PRIVATE);
    }

    /** Para testear con unas preferencias concretas sin pasar por un Context. */
    public PreferencesLicenseStorage(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getString(String key) {
        return preferences.getString(key, null);
    }

    @Override
    public void putString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    @Override
    public Long getLong(String key) {
        return preferences.contains(key) ? Long.valueOf(preferences.getLong(key, 0L)) : null;
    }

    @Override
    public void putLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }

    @Override
    public void remove(String key) {
        preferences.edit().remove(key).apply();
    }
}
