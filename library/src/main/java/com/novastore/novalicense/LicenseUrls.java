package com.novastore.novalicense;

/**
 * Construcción de URLs contra la API y la web de NovaStore.
 *
 * <p>Espejo de {@code lib/src/nova_license.dart} del SDK Dart. Todas las funciones toleran
 * {@code null}/vacío devolviendo cadena vacía, para que un dato mal configurado no reviente con
 * una excepción.
 */
public final class LicenseUrls {

    private LicenseUrls() {
    }

    /**
     * Base de la API, sin barra final: {@code https://api.novastore.cu/api/v1}.
     *
     * <p>El transporte le añade el endpoint, así que lo que se le pasa es siempre la base
     * ({@code POST {base}/validate}).
     */
    public static String normalizeBase(String apiBase) {
        String base = apiBase == null ? "" : apiBase.trim();
        if (base.isEmpty()) {
            return "";
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    /** Endpoint de validación por slug: {@code <base>/apps/<slug>/validate}. */
    public static String validateUrl(String apiBase, String storeSlug) {
        String base = normalizeBase(apiBase);
        String slug = storeSlug == null ? "" : storeSlug.trim();
        if (base.isEmpty() || slug.isEmpty()) {
            return "";
        }
        return base + "/apps/" + slug + "/validate";
    }

    /** Endpoint de validación por paquete: {@code <base>/validate} (el que usa el gate). */
    public static String validateUrlByPackage(String apiBase) {
        String base = normalizeBase(apiBase);
        return base.isEmpty() ? "" : base + "/validate";
    }

    /**
     * URL de la ficha de la app en la web, con slug si lo hay y si no la home de la tienda.
     *
     * <p>Nota: la ficha real de NovaStore vive en {@code /store/apps/<slug>/} (mismo formato que
     * {@code StoreLinks.appPageLink} del cliente Flutter). El SDK Dart y el Kotlin anterior
     * devolvían {@code /apps/<slug>}, que no existe y hacía que el botón "Abrir NovaStore" de la
     * pantalla de licencia cayera en el index de la tienda en vez de en la app.
     */
    public static String storeUrl(NovaLicenseGuardConfig config) {
        if (config == null) {
            return "";
        }
        String base = config.getStoreUrl() == null ? "" : config.getStoreUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            return "";
        }
        String slug = config.getStoreSlug();
        if (slug != null && !slug.trim().isEmpty()) {
            return base + "/store/apps/" + slug.trim() + "/";
        }
        return base + "/";
    }
}
