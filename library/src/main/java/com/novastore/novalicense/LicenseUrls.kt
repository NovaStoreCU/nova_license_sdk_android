package com.novastore.novalicense

/** Normalización de URLs y construcción de la URL de la tienda. */
object LicenseUrls {

    /** Quita los `/` finales de una base, como hace el SDK Dart. */
    fun normalizeBase(url: String): String {
        var u = url.trim()
        while (u.endsWith("/")) {
            u = u.dropLast(1)
        }
        return u
    }

    /**
     * URL de la tienda: `{storeUrl}` o `{storeUrl}/apps/{storeSlug}` cuando hay
     * slug configurado.
     */
    fun storeUrl(config: NovaLicenseGuardConfig): String {
        val base = normalizeBase(config.storeUrl)
        val slug = config.storeSlug?.trim()
        return if (!slug.isNullOrEmpty()) "$base/apps/$slug" else base
    }
}