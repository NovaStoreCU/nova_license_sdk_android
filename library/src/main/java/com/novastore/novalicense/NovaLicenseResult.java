package com.novastore.novalicense;

/**
 * Resultado de una comprobación de licencia.
 *
 * <p>Equivalente al {@code NovaLicenseCheck} del SDK Dart. Desde Kotlin se lee igual que antes
 * ({@code result.status}, {@code result.allowed}, {@code result.deviceCode}) porque los getters
 * {@code getX()} / {@code isX()} se ven como propiedades.
 */
public final class NovaLicenseResult {

    private final NovaLicenseStatus status;
    private final String errorMessage;
    private final String deviceCode;

    public NovaLicenseResult(NovaLicenseStatus status) {
        this(status, null, null);
    }

    public NovaLicenseResult(NovaLicenseStatus status, String errorMessage, String deviceCode) {
        this.status = status;
        this.errorMessage = errorMessage;
        this.deviceCode = deviceCode;
    }

    public NovaLicenseStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Código de activación (el mismo que muestra la pantalla de licencia). */
    public String getDeviceCode() {
        return deviceCode;
    }

    /** True si la app puede abrirse. */
    public boolean isAllowed() {
        return status == NovaLicenseStatus.VALID;
    }

    /** True cuando la comprobación falló por red/error y no hay licencia fresca. */
    public boolean isOffline() {
        return status == NovaLicenseStatus.OFFLINE || status == NovaLicenseStatus.ERROR;
    }

    @Override
    public String toString() {
        return "NovaLicenseResult(status=" + status
                + ", deviceCode=" + deviceCode
                + ", errorMessage=" + errorMessage + ")";
    }
}
