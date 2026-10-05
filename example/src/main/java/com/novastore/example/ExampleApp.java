package com.novastore.example;

import android.app.Application;

import com.novastore.novalicense.NovaLicense;
import com.novastore.novalicense.NovaLicenseGuardConfig;

/** Configura el gate al arrancar: a partir de aquí {@code NovaLicenseActivity} valida. */
public class ExampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NovaLicense.configure(NovaLicenseGuardConfig.createByClassName(
                // Base de la API v1 de NovaStore.
                "https://api.novastore.cu/api/v1",
                // Web pública de la tienda (botón "Abrir NovaStore" de la pantalla de licencia).
                "https://novastore.cu",
                // package_name de esta app tal y como está registrada en NovaStore.
                "com.novastore.sdktestkotlin",
                // Actividad protegida. Desde Android se puede pasar la Class directamente;
                // aquí usamos la variante por nombre (la misma que usaría Unity/C#).
                MainActivity.class.getName(),
                // Slug en la tienda (null = abrir la home).
                null,
                // deviceId (null = usar/generar el del dispositivo).
                null,
                // apkSha1 (null = no comprobar la firma).
                null,
                // Gracia offline: 3 días.
                3L,
                // Timeout de red: 10 s.
                10_000L,
                // Color de marca ARGB (null = el de la librería).
                null));
    }
}
