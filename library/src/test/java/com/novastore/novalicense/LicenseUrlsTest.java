package com.novastore.novalicense;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LicenseUrlsTest {

    @Test
    public void normalizeBaseStripsTrailingSlashes() {
        assertEquals("https://novastore.cu/api/v1",
                LicenseUrls.normalizeBase("https://novastore.cu/api/v1///"));
        assertEquals("https://novastore.cu/api/v1",
                LicenseUrls.normalizeBase("https://novastore.cu/api/v1"));
    }

    @Test
    public void storeUrlWithSlug() {
        // La ficha real vive en /store/apps/<slug>/ (formato de StoreLinks.appPageLink en el
        // cliente Flutter), no en /apps/<slug>.
        NovaLicenseGuardConfig config = TestConfigs.testConfig("mi-juego", "https://novastore.cu");
        assertEquals("https://novastore.cu/store/apps/mi-juego/", LicenseUrls.storeUrl(config));
    }

    @Test
    public void storeUrlWithoutSlug() {
        NovaLicenseGuardConfig config = TestConfigs.testConfig(null, "https://novastore.cu/");
        assertEquals("https://novastore.cu/", LicenseUrls.storeUrl(config));
    }

    @Test
    public void validateEndpoints() {
        assertEquals("https://api.novastore.cu/api/v1/validate",
                LicenseUrls.validateUrlByPackage("https://api.novastore.cu/api/v1"));
        assertEquals("https://api.novastore.cu/api/v1/apps/mi-juego/validate",
                LicenseUrls.validateUrl("https://api.novastore.cu/api/v1", "mi-juego"));
    }

    @Test
    public void nullAndEmptyInputsDoNotThrow() {
        assertEquals("", LicenseUrls.normalizeBase(null));
        assertEquals("", LicenseUrls.normalizeBase("   "));
        assertEquals("", LicenseUrls.storeUrl(null));
        assertEquals("", LicenseUrls.validateUrlByPackage(null));
        assertEquals("", LicenseUrls.validateUrl("https://api.novastore.cu/api/v1", null));
    }
}
