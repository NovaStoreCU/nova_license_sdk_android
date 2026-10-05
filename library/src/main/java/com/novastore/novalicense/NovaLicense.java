package com.novastore.novalicense;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

/**
 * Punto de entrada del SDK (equivalente a {@code novaLicenseGuard(...)} del SDK Dart).
 *
 * <p>Uso típico en Android nativo:
 *
 * <pre>{@code
 * public class MiApp extends Application {
 *     @Override public void onCreate() {
 *         super.onCreate();
 *         NovaLicense.configure(new NovaLicenseGuardConfig(
 *                 "https://api.novastore.cu/api/v1",
 *                 "https://novastore.cu",
 *                 "com.miapp.app",
 *                 MainActivity.class));
 *     }
 * }
 * }</pre>
 *
 * <p>y en el manifest {@code NovaLicenseActivity} como actividad principal: si la licencia es
 * válida salta a {@code config.mainActivity}, y si no muestra la pantalla de licencia.
 *
 * <p>Desde Unity no hay {@code Application} propio obligatorio: ver la sección "Unity" del README.
 */
public final class NovaLicense {

    /** Callback de {@link #checkAsync}. Interfaz (no lambda) para no depender de desugaring. */
    public interface Callback {
        void onResult(NovaLicenseResult result);
    }

    /** Configuración global del gate (la consume {@link NovaLicenseActivity}). */
    private static volatile NovaLicenseGuardConfig config;

    private NovaLicense() {
    }

    /** Registra la configuración del gate. Llamar una vez antes de arrancar la app protegida. */
    public static void configure(NovaLicenseGuardConfig config) {
        NovaLicense.config = config;
    }

    /** Configuración actual (o {@code null} si nadie llamó a {@link #configure}). */
    public static NovaLicenseGuardConfig getConfig() {
        return config;
    }

    /** Almacenamiento persistente del dispositivo (SharedPreferences). */
    public static LicenseStorage store(Context context) {
        return new PreferencesLicenseStorage(context);
    }

    /** Comprobación síncrona (hace red): NO llamar desde el hilo de UI. */
    public static NovaLicenseResult check(Context context) {
        return check(context, requireConfig(), new HttpUrlConnectionTransport(), store(context));
    }

    public static NovaLicenseResult check(Context context, NovaLicenseGuardConfig config) {
        return check(context, config, new HttpUrlConnectionTransport(), store(context));
    }

    public static NovaLicenseResult check(
            Context context,
            NovaLicenseGuardConfig config,
            LicenseTransport transport) {
        return check(context, config, transport, store(context));
    }

    /**
     * Comprobación síncrona con transporte y almacenamiento inyectados (tests / lógica propia).
     *
     * @param context sólo se usa por las sobrecargas de conveniencia; no hace falta aquí.
     */
    public static NovaLicenseResult check(
            Context context,
            NovaLicenseGuardConfig config,
            LicenseTransport transport,
            LicenseStorage storage) {
        String deviceId = LicenseChecker.resolveDeviceId(config, storage);
        return LicenseChecker.check(config, deviceId, storage, transport);
    }

    /** Comprobación asíncrona con la configuración global. Seguro llamarla desde el hilo de UI. */
    public static void checkAsync(Context context, Callback callback) {
        checkAsync(context, requireConfig(), new HttpUrlConnectionTransport(), store(context), callback);
    }

    /** Comprobación asíncrona en un hilo de fondo; el callback se invoca en el hilo principal. */
    public static void checkAsync(
            Context context,
            NovaLicenseGuardConfig config,
            LicenseTransport transport,
            LicenseStorage storage,
            Callback callback) {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                final NovaLicenseResult result = LicenseChecker.check(
                        config,
                        LicenseChecker.resolveDeviceId(config, storage),
                        storage,
                        transport);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(result);
                    }
                });
            }
        });
        worker.start();
    }

    /**
     * Abre NovaStore en el navegador en la URL de la app (o la base de la tienda si no hay slug).
     *
     * @return false si no hay URL o si no hay ninguna app capaz de abrirla.
     */
    public static boolean openStore(Context context) {
        return openStore(context, requireConfig());
    }

    public static boolean openStore(Context context, NovaLicenseGuardConfig config) {
        String url = LicenseUrls.storeUrl(config);
        if (url.isEmpty()) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(context.getPackageManager()) == null) {
                return false;
            }
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static NovaLicenseGuardConfig requireConfig() {
        NovaLicenseGuardConfig current = config;
        if (current == null) {
            throw new IllegalStateException(
                    "NovaLicense: llama a NovaLicense.configure(...) antes de comprobar la licencia.");
        }
        return current;
    }
}
