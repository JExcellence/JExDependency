/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.service.central;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RCentralApiClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (this.server != null) {
            this.server.stop(0);
            this.server = null;
        }
    }

    @Test
    void connectRequestOmitsLegacyDropletStoreCompatibilityFields() throws Exception {
        final AtomicReference<String> requestBody = new AtomicReference<>("");
        this.server = HttpServer.create(new InetSocketAddress(0), 0);
        this.server.createContext("/api/server-data/connect", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJson(exchange, 200, """
                    {"success":true,"message":"ok"}
                    """);
        });
        this.server.start();

        final RCentralApiClient client = new RCentralApiClient(createPlugin(), baseUrl());
        final RCentralApiClient.ApiResponse response = client.connectServer(
                "api-key",
                "server-uuid",
                "Paper 1.21.5",
                "2.0.0",
                "player-uuid",
                "Brent",
                75
        ).join();

        assertTrue(response.isSuccess());
        assertTrue(requestBody.get().contains("\"serverUuid\":\"server-uuid\""));
        assertFalse(requestBody.get().contains("dropletStoreEnabled"));
        assertFalse(requestBody.get().contains("enabledDropletStoreItemCodes"));
    }

    @Test
    void fetchesDropletStoreAllowlistFromBackend() throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(0), 0);
        this.server.createContext("/api/server-data/droplet-store/allowlist", exchange -> writeJson(exchange, 200, """
                {
                  "success": true,
                  "data": {
                    "allowedItemCodes": [
                      "skill-level-cookie",
                      "job-level-cookie"
                    ]
                  }
                }
                """));
        this.server.start();

        final RCentralApiClient client = new RCentralApiClient(createPlugin(), baseUrl());
        final RCentralApiClient.ParsedApiResponse<RCentralApiClient.DropletStoreAllowlistData> response =
                client.getDropletStoreAllowlist("api-key").join();

        assertTrue(response.isSuccess());
        assertEquals(
                List.of("skill-level-cookie", "job-level-cookie"),
                response.data().allowedItemCodesOrEmpty()
        );
    }

    private @NotNull String baseUrl() {
        return "http://127.0.0.1:" + this.server.getAddress().getPort();
    }

    private static void writeJson(
            final com.sun.net.httpserver.HttpExchange exchange,
            final int statusCode,
            final @NotNull String body
    ) throws IOException {
        final byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    private static @NotNull Plugin createPlugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class[]{Plugin.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getLogger" -> Logger.getLogger("RCentralApiClientTest");
                        case "toString" -> "RCentralApiClientTestPlugin";
                        case "hashCode" -> 0;
                        case "equals" -> proxy == args[0];
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
    }

    private static Object defaultValue(final @NotNull Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
