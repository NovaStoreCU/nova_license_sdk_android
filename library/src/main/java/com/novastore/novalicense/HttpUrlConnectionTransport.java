package com.novastore.novalicense;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transporte real contra la API de NovaStore con {@link HttpURLConnection}.
 *
 * <p>Sin dependencias externas (ni OkHttp, ni Gson, ni Kotlin): solo el framework de Android.
 */
public class HttpUrlConnectionTransport implements LicenseTransport {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final Pattern VALID_FIELD = Pattern.compile("\"valid\"\\s*:\\s*(true|false)");

    @Override
    public Outcome isValid(
            String packageName,
            String deviceId,
            String apkSha1,
            String baseUrl,
            long timeoutMillis) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(baseUrl + "/validate").openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout((int) timeoutMillis);
            connection.setReadTimeout((int) timeoutMillis);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            byte[] body = buildBody(packageName, deviceId, apkSha1).getBytes(UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            OutputStream out = connection.getOutputStream();
            try {
                out.write(body);
                out.flush();
            } finally {
                out.close();
            }

            if (connection.getResponseCode() != 200) {
                return Outcome.failed(null);
            }
            return Outcome.ok(parseValid(readBody(connection.getInputStream())));
        } catch (Exception e) {
            return Outcome.failed(e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Extrae el booleano de {@code data.valid} de la respuesta del servidor. */
    private static boolean parseValid(String body) {
        if (body == null) {
            return false;
        }
        Matcher matcher = VALID_FIELD.matcher(body);
        return matcher.find() && "true".equals(matcher.group(1));
    }

    private static String buildBody(String packageName, String deviceId, String apkSha1) {
        StringBuilder body = new StringBuilder();
        body.append("{\"package_name\":");
        appendJsonString(body, packageName);
        body.append(",\"device_id\":");
        appendJsonString(body, deviceId);
        if (apkSha1 != null && !apkSha1.isEmpty()) {
            body.append(",\"apk_sha1\":");
            appendJsonString(body, apkSha1);
        }
        body.append('}');
        return body.toString();
    }

    /** Escribe una cadena JSON escapando comillas y barras invertidas. */
    private static void appendJsonString(StringBuilder out, String value) {
        if (value == null) {
            out.append("\"\"");
            return;
        }
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                    out.append('\\').append(c);
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    out.append(c);
                    break;
            }
        }
        out.append('"');
    }

    private static String readBody(InputStream stream) throws Exception {
        if (stream == null) {
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF_8));
        try {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            return out.toString();
        } finally {
            reader.close();
        }
    }
}
