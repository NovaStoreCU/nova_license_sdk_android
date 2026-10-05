package com.novastore.novalicense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests del transporte real contra un servidor HTTP mínimo montado sobre un {@link ServerSocket}
 * (no se usa {@code com.sun.net.httpserver}: no está disponible con {@code -source 11}).
 */
public class HttpUrlConnectionTransportTest {

    /** Ruta registrada → cuerpo de respuesta. */
    private static final String VALID_BODY = "{\"data\":{\"valid\":true}}";
    private static final String INVALID_BODY = "{\"data\":{\"valid\":false}}";

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running = true;
    private final AtomicReference<String> capturedBody = new AtomicReference<>();

    @Before
    public void setUp() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));
        acceptThread = new Thread(this::acceptLoop, "fake-http");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @After
    public void tearDown() throws IOException {
        running = false;
        serverSocket.close();
    }

    private void acceptLoop() {
        while (running) {
            try (Socket socket = serverSocket.accept()) {
                handle(socket);
            } catch (IOException e) {
                // Socket cerrado en tearDown o cliente que se fue: fin del bucle.
                if (running) {
                    // Un fallo puntual no debe tumbar el servidor del test.
                    continue;
                }
                return;
            }
        }
    }

    private void handle(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        String requestLine = readLine(in);
        if (requestLine == null || requestLine.isEmpty()) {
            return;
        }
        int contentLength = 0;
        String header;
        while ((header = readLine(in)) != null && !header.isEmpty()) {
            int colon = header.indexOf(':');
            if (colon > 0 && "content-length".equalsIgnoreCase(header.substring(0, colon).trim())) {
                try {
                    contentLength = Integer.parseInt(header.substring(colon + 1).trim());
                } catch (NumberFormatException ignored) {
                    contentLength = 0;
                }
            }
        }
        byte[] body = readFully(in, contentLength);
        capturedBody.set(new String(body, StandardCharsets.UTF_8));

        String target = requestLine.split(" ")[1];
        int status = 200;
        String payload = "";
        if (target.endsWith("/api/validate")) {
            payload = VALID_BODY;
        } else if (target.endsWith("/api-invalid/validate")) {
            payload = INVALID_BODY;
        } else if (target.endsWith("/api-down/validate")) {
            status = 500;
        } else {
            // Cualquier otra ruta (p. ej. /api/validate/validate) no existe.
            status = 404;
        }

        OutputStream out = socket.getOutputStream();
        if (payload.isEmpty()) {
            out.write(("HTTP/1.1 " + status + " X\r\nContent-Length: 0\r\n"
                    + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        } else {
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            out.write(("HTTP/1.1 " + status + " X\r\nContent-Type: application/json\r\n"
                    + "Content-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.write(bytes);
        }
        out.flush();
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                continue;
            }
            if (c == '\n') {
                return new String(buffer.toByteArray(), StandardCharsets.US_ASCII);
            }
            buffer.write(c);
        }
        return buffer.size() == 0 ? null : new String(buffer.toByteArray(), StandardCharsets.US_ASCII);
    }

    private static byte[] readFully(InputStream in, int length) throws IOException {
        byte[] data = new byte[Math.max(length, 0)];
        int read = 0;
        while (read < data.length) {
            int n = in.read(data, read, data.length - read);
            if (n < 0) {
                break;
            }
            read += n;
        }
        return data;
    }

    private String url(String path) {
        return "http://127.0.0.1:" + serverSocket.getLocalPort() + path;
    }

    private HttpUrlConnectionTransport transport() {
        return new HttpUrlConnectionTransport();
    }

    @Test
    public void validTrueFromServer() {
        LicenseTransport.Outcome outcome =
                transport().isValid("com.demo.game", "dev1", null, url("/api"), 5000L);
        assertTrue(outcome.isOk());
        assertTrue(outcome.isValid());
    }

    @Test
    public void validFalseFromServer() {
        LicenseTransport.Outcome outcome =
                transport().isValid("com.demo.game", "dev1", null, url("/api-invalid"), 5000L);
        assertTrue(outcome.isOk());
        assertFalse(outcome.isValid());
    }

    @Test
    public void serverErrorFailsWithoutThrowing() {
        LicenseTransport.Outcome outcome =
                transport().isValid("com.demo.game", "dev1", null, url("/api-down"), 5000L);
        assertTrue(outcome.isFailed());
    }

    @Test
    public void postsPackageNameDeviceIdAndApkSha1() {
        transport().isValid("com.demo.game", "dev1", "AA:BB:00", url("/api"), 5000L);
        String body = capturedBody.get();
        assertNotNull(body);
        assertTrue(body.contains("\"package_name\":\"com.demo.game\""));
        assertTrue(body.contains("\"device_id\":\"dev1\""));
        assertTrue(body.contains("\"apk_sha1\":\"AA:BB:00\""));
    }

    @Test
    public void apkSha1OmittedWhenEmpty() {
        transport().isValid("com.demo.game", "dev1", null, url("/api"), 5000L);
        assertNotNull(capturedBody.get());
        assertFalse(capturedBody.get().contains("apk_sha1"));
    }

    /** Regresión: un valor con comillas/backslash no debe romper el JSON del cuerpo. */
    @Test
    public void jsonBodyEscapesSpecialCharacters() {
        transport().isValid("com.demo.game", "de\"v\\ice", "AA\nBB", url("/api"), 5000L);
        String body = capturedBody.get();
        assertTrue(body.contains("\"device_id\":\"de\\\"v\\\\ice\""));
        assertTrue(body.contains("\"apk_sha1\":\"AA\\nBB\""));
    }

    @Test
    public void unreachableServerFails() {
        LicenseTransport.Outcome outcome =
                transport().isValid("com.demo.game", "dev1", null, "http://127.0.0.1:1/api/v1", 500L);
        assertTrue(outcome.isFailed());
    }

    /** Regresión: el transporte añade "/validate" a la base; nunca debe duplicarlo. */
    @Test
    public void transportAppendsValidateExactlyOnce() {
        LicenseTransport.Outcome good =
                transport().isValid("com.demo.game", "dev1", null, url("/api"), 5000L);
        assertTrue("debe existir /api/validate", good.isOk());

        LicenseTransport.Outcome doubled =
                transport().isValid("com.demo.game", "dev1", null, url("/api/validate"), 5000L);
        assertTrue("no debe existir /api/validate/validate", doubled.isFailed());
    }
}
