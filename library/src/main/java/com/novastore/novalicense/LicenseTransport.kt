package com.novastore.novalicense

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Canal HTTP usado para llamar a `POST {apiBase}/validate`.
 *
 * Se abstrae para poder inyectar una implementación falsa en los tests.
 */
interface LicenseTransport {

    /** Resultado de la llamada de validación. */
    sealed class Outcome {
        /** El servidor respondió 200 con `data.valid = true|false`. */
        data class Ok(val valid: Boolean) : Outcome()

        /** Sin respuesta del servidor (red, timeout, 5xx...). */
        data class Failed(val message: String?) : Outcome()
    }

    /**
     * Llama a `POST {baseUrl}/validate` con el cuerpo JSON
     * `{package_name, device_id, apk_sha1?}`.
     */
    fun isValid(
        packageName: String,
        deviceId: String,
        apkSha1: String?,
        baseUrl: String,
        timeoutMillis: Long,
    ): Outcome
}

/** Implementación por defecto sobre [HttpURLConnection] (sin dependencias externas). */
class HttpUrlConnectionTransport : LicenseTransport {

    override fun isValid(
        packageName: String,
        deviceId: String,
        apkSha1: String?,
        baseUrl: String,
        timeoutMillis: Long,
    ): LicenseTransport.Outcome =
        try {
            val conn = URL("$baseUrl/validate").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = timeoutMillis.toInt()
                conn.readTimeout = timeoutMillis.toInt()
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { out ->
                    out.write(buildBody(packageName, deviceId, apkSha1).toByteArray(StandardCharsets.UTF_8))
                }
                when (conn.responseCode) {
                    200 -> parseValid(readBody(conn))
                    else -> LicenseTransport.Outcome.Failed(null)
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            LicenseTransport.Outcome.Failed(e.message)
        }

    private fun buildBody(packageName: String, deviceId: String, apkSha1: String?): String {
        val sb = StringBuilder()
        sb.append('{')
        sb.append("\"package_name\":").append(jsonString(packageName)).append(',')
        sb.append("\"device_id\":").append(jsonString(deviceId))
        if (!apkSha1.isNullOrEmpty()) {
            sb.append(",\"apk_sha1\":").append(jsonString(apkSha1))
        }
        sb.append('}')
        return sb.toString()
    }

    private fun jsonString(s: String): String {
        val escaped = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun readBody(conn: HttpURLConnection): String =
        BufferedReader(conn.inputStream.reader(StandardCharsets.UTF_8)).use { it.readText() }

    private fun parseValid(body: String): LicenseTransport.Outcome {
        val match = Regex("\"valid\"\\s*:\\s*(true|false)").find(body)
        val valid = match?.groupValues?.get(1) == "true"
        return LicenseTransport.Outcome.Ok(valid)
    }
}