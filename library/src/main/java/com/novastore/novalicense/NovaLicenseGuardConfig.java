package com.novastore.novalicense;

import android.app.Activity;

/**
 * Configuración del gate de licencia.
 *
 * <p>Equivalente al {@code NovaLicenseGuardConfig} del SDK Dart. Se puede crear de tres formas:
 *
 * <pre>{@code
 * // 1) Lo mínimo (Java):
 * new NovaLicenseGuardConfig("https://api.novastore.cu/api/v1", "https://novastore.cu", "com.miapp.app");
 *
 * // 2) Indicando la actividad protegida:
 * new NovaLicenseGuardConfig(apiBase, storeUrl, packageName, MainActivity.class);
 *
 * // 3) Con opciones extra (o desde Unity/C# con una sola llamada estática):
 * NovaLicenseGuardConfig.builder(apiBase, storeUrl, packageName)
 *         .storeSlug("miapp")
 *         .apkSha1("CC:DE:...")
 *         .graceDays(3)
 *         .mainActivity(MainActivity.class)
 *         .build();
 * }</pre>
 */
public final class NovaLicenseGuardConfig {

    private final String apiBase;
    private final String storeUrl;
    private final String packageName;
    private final String storeSlug;
    private final String deviceId;
    private final String apkSha1;
    private final long graceDays;
    private final long httpTimeoutMillis;
    private final Integer brandColor;
    private final Class<? extends Activity> mainActivity;

    /** Constructor mínimo: los 3 campos obligatorios. */
    public NovaLicenseGuardConfig(String apiBase, String storeUrl, String packageName) {
        this(builder(apiBase, storeUrl, packageName));
    }

    /** Constructor habitual en Android: los 3 obligatorios + la actividad protegida. */
    public NovaLicenseGuardConfig(
            String apiBase,
            String storeUrl,
            String packageName,
            Class<? extends Activity> mainActivity) {
        this(builder(apiBase, storeUrl, packageName).mainActivity(mainActivity));
    }

    private NovaLicenseGuardConfig(Builder builder) {
        this.apiBase = requireText(builder.apiBase, "apiBase");
        this.storeUrl = requireText(builder.storeUrl, "storeUrl");
        this.packageName = requireText(builder.packageName, "packageName");
        this.storeSlug = emptyToNull(builder.storeSlug);
        this.deviceId = emptyToNull(builder.deviceId);
        this.apkSha1 = emptyToNull(builder.apkSha1);
        this.graceDays = builder.graceDays;
        this.httpTimeoutMillis = builder.httpTimeoutMillis;
        this.brandColor = builder.brandColor;
        this.mainActivity = builder.mainActivity;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("NovaLicenseGuardConfig: '" + name + "' es obligatorio.");
        }
        return value.trim();
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static Builder builder(String apiBase, String storeUrl, String packageName) {
        return new Builder(apiBase, storeUrl, packageName);
    }

    /**
     * Fábrica con TODOS los parámetros en una sola llamada: es la forma cómoda de configurar el
     * gate desde Unity/C# con {@code AndroidJavaObject}, donde encadenar un builder es incómodo.
     *
     * @param brandColor color de marca ARGB, o {@code null} para el azul por defecto.
     */
    public static NovaLicenseGuardConfig create(
            String apiBase,
            String storeUrl,
            String packageName,
            Class<? extends Activity> mainActivity,
            String storeSlug,
            String deviceId,
            String apkSha1,
            long graceDays,
            long httpTimeoutMillis,
            Integer brandColor) {
        return builder(apiBase, storeUrl, packageName)
                .mainActivity(mainActivity)
                .storeSlug(storeSlug)
                .deviceId(deviceId)
                .apkSha1(apkSha1)
                .graceDays(graceDays)
                .httpTimeoutMillis(httpTimeoutMillis)
                .brandColor(brandColor)
                .build();
    }

    /**
     * Igual que {@link #create} pero recibiendo el nombre completo de la actividad protegida en
     * lugar de su {@code Class}.
     *
     * <p>Es la vía recomendada desde Unity/C#: un parámetro de tipo {@code Class} no se puede pasar
     * desde C#, pero un {@code String} sí (por ejemplo {@code
     * "com.unity3d.player.UnityPlayerActivity"} o {@code "com.miapp.MainActivity"}).
     *
     * @param mainActivityClassName clase completa de la Activity, o {@code null}/vacío si se
     *                              prefiere abrir la app de otro modo.
     */
    public static NovaLicenseGuardConfig createByClassName(
            String apiBase,
            String storeUrl,
            String packageName,
            String mainActivityClassName,
            String storeSlug,
            String deviceId,
            String apkSha1,
            long graceDays,
            long httpTimeoutMillis,
            Integer brandColor) {
        return create(apiBase, storeUrl, packageName, resolveActivityClass(mainActivityClassName),
                storeSlug, deviceId, apkSha1, graceDays, httpTimeoutMillis, brandColor);
    }

    /**
     * Resuelve una clase por su nombre completo usando el classloader de la librería.
     *
     * @return la clase, o {@code null} si el nombre viene vacío o no existe.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Activity> resolveActivityClass(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        try {
            Class<?> resolved = Class.forName(className.trim(), false, NovaLicenseActivity.class.getClassLoader());
            if (!Activity.class.isAssignableFrom(resolved)) {
                return null;
            }
            return (Class<? extends Activity>) resolved;
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    public String getApiBase() {
        return apiBase;
    }

    public String getStoreUrl() {
        return storeUrl;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getStoreSlug() {
        return storeSlug;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getApkSha1() {
        return apkSha1;
    }

    /** Días de gracia sin red con licencia en caché. Por defecto 3. */
    public long getGraceDays() {
        return graceDays;
    }

    /** Timeout de red en milisegundos. Por defecto 10 000. */
    public long getHttpTimeoutMillis() {
        return httpTimeoutMillis;
    }

    public Integer getBrandColor() {
        return brandColor;
    }

    /** Actividad a la que se salta cuando la licencia es válida (opcional). */
    public Class<? extends Activity> getMainActivity() {
        return mainActivity;
    }

    /** Builder con los valores por defecto del SDK. */
    public static final class Builder {

        private static final long DEFAULT_GRACE_DAYS = 3L;
        private static final long DEFAULT_HTTP_TIMEOUT_MILLIS = 10_000L;

        private final String apiBase;
        private String storeUrl;
        private String packageName;
        private String storeSlug;
        private String deviceId;
        private String apkSha1;
        private long graceDays = DEFAULT_GRACE_DAYS;
        private long httpTimeoutMillis = DEFAULT_HTTP_TIMEOUT_MILLIS;
        private Integer brandColor;
        private Class<? extends Activity> mainActivity;

        Builder(String apiBase, String storeUrl, String packageName) {
            this.apiBase = apiBase;
            this.storeUrl = storeUrl;
            this.packageName = packageName;
        }

        public Builder storeUrl(String value) {
            this.storeUrl = value;
            return this;
        }

        public Builder packageName(String value) {
            this.packageName = value;
            return this;
        }

        public Builder storeSlug(String value) {
            this.storeSlug = value;
            return this;
        }

        public Builder deviceId(String value) {
            this.deviceId = value;
            return this;
        }

        public Builder apkSha1(String value) {
            this.apkSha1 = value;
            return this;
        }

        public Builder graceDays(long value) {
            this.graceDays = value;
            return this;
        }

        public Builder httpTimeoutMillis(long value) {
            this.httpTimeoutMillis = value;
            return this;
        }

        /** Color de marca ARGB (o {@code null} para el azul por defecto). */
        public Builder brandColor(Integer value) {
            this.brandColor = value;
            return this;
        }

        public Builder mainActivity(Class<? extends Activity> value) {
            this.mainActivity = value;
            return this;
        }

        public NovaLicenseGuardConfig build() {
            return new NovaLicenseGuardConfig(this);
        }
    }
}
