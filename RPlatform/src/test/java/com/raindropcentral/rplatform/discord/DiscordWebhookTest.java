package com.raindropcentral.rplatform.discord;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DiscordWebhookTest {

    private static final AtomicReference<Supplier<MockHttpURLConnection>> CONNECTION_SUPPLIER = new AtomicReference<>();

    private JavaPlugin plugin;
    private Logger logger;
    private TestLogHandler logHandler;

    @BeforeAll
    static void registerMockProtocol() {
        try {
            URL.setURLStreamHandlerFactory(protocol -> {
                if ("mock".equals(protocol)) {
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(final URL u) {
                            final Supplier<MockHttpURLConnection> supplier = CONNECTION_SUPPLIER.get();
                            if (supplier == null) {
                                throw new IllegalStateException("No mock connection supplier configured");
                            }
                            return supplier.get();
                        }
                    };
                }
                return null;
            });
        } catch (final Error error) {
            if (!Objects.equals("factory already defined", error.getMessage())) {
                throw error;
            }
        }
    }

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(JavaPlugin.class);
        logger = Logger.getLogger("DiscordWebhookTest");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        for (final Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        logHandler = new TestLogHandler();
        logger.addHandler(logHandler);
        when(plugin.getLogger()).thenReturn(logger);
    }

    @AfterEach
    void tearDown() {
        if (logger != null && logHandler != null) {
            logger.removeHandler(logHandler);
        }
        CONNECTION_SUPPLIER.set(null);
    }

    @Test
    @DisplayName("Builder returns webhook when plugin and URL are provided")
    void buildReturnsWebhookWhenFieldsPresent() {
        final DiscordWebhook webhook = new DiscordWebhook.Builder()
                .plugin(plugin)
                .webhookUrl("mock://discord")
                .build();

        assertNotNull(webhook);
    }

    @Test
    @DisplayName("Builder validation returns null when plugin missing")
    void buildReturnsNullWhenPluginMissing() {
        final DiscordWebhook webhook = new DiscordWebhook.Builder()
                .webhookUrl("mock://discord")
                .build();

        assertNull(webhook);
    }

    @Test
    @DisplayName("Builder validation returns null when URL missing or blank")
    void buildReturnsNullWhenUrlInvalid() {
        final DiscordWebhook.Builder builder = new DiscordWebhook.Builder();
        builder.plugin(plugin);

        assertNull(builder.build());

        builder.webhookUrl("   ");
        assertNull(builder.build());
    }

    @Test
    @DisplayName("sendMessage serializes content payload and returns success for 204 response")
    void sendMessageSerializesPayloadAndReturnsSuccess() throws Exception {
        final MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setResponseCode(204);
        CONNECTION_SUPPLIER.set(() -> connection);

        final DiscordWebhook webhook = new DiscordWebhook(plugin, "mock://discord");
        final CompletableFuture<Boolean> result = webhook.sendMessage("Hello \"World\"\nLine");

        assertTrue(result.get(2, TimeUnit.SECONDS));
        assertEquals("POST", connection.getMethod());
        assertEquals("application/json", connection.getHeader("Content-Type"));
        assertEquals("RPlatform-Discord-Webhook/2.0", connection.getHeader("User-Agent"));
        assertTrue(connection.isDoOutput());
        assertEquals(5000, connection.getConnectTimeout());
        assertEquals(5000, connection.getReadTimeout());
        assertTrue(connection.isDisconnected());
        assertEquals(
                "{\"content\":\"Hello \\\"World\\\"\\nLine\"}",
                connection.getSentPayload()
        );
    }

    @Test
    @DisplayName("sendEmbed serializes embed payload and returns success for 200 response")
    void sendEmbedSerializesPayloadAndReturnsSuccess() throws Exception {
        final MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setResponseCode(200);
        CONNECTION_SUPPLIER.set(() -> connection);

        final DiscordWebhook webhook = new DiscordWebhook(plugin, "mock://discord");
        final CompletableFuture<Boolean> result = webhook.sendEmbed("Alert", "An issue occurred", 123456);

        assertTrue(result.get(2, TimeUnit.SECONDS));
        assertEquals(
                "{\"embeds\":[{\"title\":\"Alert\",\"description\":\"An issue occurred\",\"color\":123456}]}",
                connection.getSentPayload()
        );
    }

    @Test
    @DisplayName("sendMessage returns false when HTTP response is not 2xx")
    void sendMessageReturnsFalseForErrorResponse() throws Exception {
        final MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setResponseCode(500);
        CONNECTION_SUPPLIER.set(() -> connection);

        final DiscordWebhook webhook = new DiscordWebhook(plugin, "mock://discord");
        final CompletableFuture<Boolean> result = webhook.sendMessage("failure");

        assertFalse(result.get(2, TimeUnit.SECONDS));
        assertNull(logHandler.getLastRecord());
    }

    @Test
    @DisplayName("sendEmbed logs exception and returns false when transmission fails")
    void sendEmbedLogsExceptionOnFailure() throws Exception {
        final MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setThrowOnOutput(true);
        CONNECTION_SUPPLIER.set(() -> connection);

        final DiscordWebhook webhook = new DiscordWebhook(plugin, "mock://discord");
        final CompletableFuture<Boolean> result = webhook.sendEmbed("Alert", "Failure", 654321);

        assertFalse(result.get(2, TimeUnit.SECONDS));
        final LogRecord record = logHandler.getLastRecord();
        assertNotNull(record);
        assertEquals(Level.WARNING, record.getLevel());
        assertTrue(record.getMessage().contains("Failed to send Discord embed"));
    }

    @Test
    @DisplayName("sendMessage logs exception and returns false when transmission fails")
    void sendMessageLogsExceptionOnFailure() throws Exception {
        final MockHttpURLConnection connection = new MockHttpURLConnection();
        connection.setThrowOnOutput(true);
        CONNECTION_SUPPLIER.set(() -> connection);

        final DiscordWebhook webhook = new DiscordWebhook(plugin, "mock://discord");
        final CompletableFuture<Boolean> result = webhook.sendMessage("payload");

        assertFalse(result.get(2, TimeUnit.SECONDS));
        final LogRecord record = logHandler.getLastRecord();
        assertNotNull(record);
        assertEquals(Level.WARNING, record.getLevel());
        assertTrue(record.getMessage().contains("Failed to send Discord message"));
    }

    private static final class TestLogHandler extends Handler {

        private LogRecord lastRecord;

        @Override
        public void publish(final LogRecord record) {
            lastRecord = record;
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }

        LogRecord getLastRecord() {
            return lastRecord;
        }
    }

    private static final class MockHttpURLConnection extends HttpURLConnection {

        private final Map<String, String> headers = new HashMap<>();
        private final ByteArrayOutputStream payload = new ByteArrayOutputStream();
        private String method;
        private int responseCode;
        private int connectTimeout;
        private int readTimeout;
        private boolean doOutput;
        private boolean disconnected;
        private boolean throwOnOutput;

        private MockHttpURLConnection() {
            super(null);
        }

        @Override
        public void disconnect() {
            disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
            // no-op
        }

        @Override
        public void setRequestMethod(final String method) {
            this.method = method;
        }

        @Override
        public String getRequestMethod() {
            return method;
        }

        @Override
        public void setRequestProperty(final String key, final String value) {
            headers.put(key, value);
        }

        @Override
        public void setConnectTimeout(final int timeout) {
            this.connectTimeout = timeout;
        }

        @Override
        public void setReadTimeout(final int timeout) {
            this.readTimeout = timeout;
        }

        @Override
        public void setDoOutput(final boolean doOutput) {
            this.doOutput = doOutput;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            if (throwOnOutput) {
                throw new IOException("Simulated failure");
            }
            return payload;
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        String getMethod() {
            return method;
        }

        String getHeader(final String key) {
            return headers.get(key);
        }

        int getConnectTimeout() {
            return connectTimeout;
        }

        int getReadTimeout() {
            return readTimeout;
        }

        boolean isDoOutput() {
            return doOutput;
        }

        boolean isDisconnected() {
            return disconnected;
        }

        String getSentPayload() {
            return payload.toString(StandardCharsets.UTF_8);
        }

        void setResponseCode(final int responseCode) {
            this.responseCode = responseCode;
        }

        void setThrowOnOutput(final boolean throwOnOutput) {
            this.throwOnOutput = throwOnOutput;
        }
    }
}
