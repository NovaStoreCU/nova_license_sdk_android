package com.novastore.novalicense;

/**
 * Transporte de la validación contra la API.
 *
 * <p>Interfaz a propósito: los tests y el uso programático pueden inyectar su propia
 * implementación (o un fake) sin tocar la red.
 */
public interface LicenseTransport {

    /**
     * Valida la licencia.
     *
     * @param packageName  paquete de la app protegida (obligatorio).
     * @param deviceId     código de activación.
     * @param apkSha1      huella de firma del APK, o {@code null} para no comprobar.
     * @param baseUrl      base de la API (ya normalizada por {@link LicenseUrls}).
     * @param timeoutMillis timeout de red en milisegundos.
     */
    Outcome isValid(
            String packageName,
            String deviceId,
            String apkSha1,
            String baseUrl,
            long timeoutMillis);

    /**
     * Respuesta de una validación: o bien una respuesta del servidor ({@link #ok}), o bien un
     * fallo de transporte ({@link #failed}).
     *
     * <p>Antes era un {@code sealed class} de Kotlin con dos subtipos; en Java es una clase con
     * dos fábricas, para que desde Kotlin también se pueda construir con la misma claridad.
     */
    final class Outcome {

        private static final int TYPE_OK = 0;
        private static final int TYPE_FAILED = 1;

        private final int type;
        private final boolean valid;
        private final String message;

        private Outcome(int type, boolean valid, String message) {
            this.type = type;
            this.valid = valid;
            this.message = message;
        }

        /** El servidor respondió (con licencia válida o inválida). */
        public static Outcome ok(boolean valid) {
            return new Outcome(TYPE_OK, valid, null);
        }

        /** Falló el transporte: sin red, timeout, servidor caído, JSON ilegible… */
        public static Outcome failed(String message) {
            return new Outcome(TYPE_FAILED, false, message);
        }

        public boolean isOk() {
            return type == TYPE_OK;
        }

        public boolean isFailed() {
            return type == TYPE_FAILED;
        }

        /** True solo si el servidor respondió y dijo que la licencia es válida. */
        public boolean isValid() {
            return type == TYPE_OK && valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return type == TYPE_OK ? ("Outcome.Ok(valid=" + valid + ")") : ("Outcome.Failed(" + message + ")");
        }
    }
}
