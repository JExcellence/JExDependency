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

import com.google.gson.JsonObject;
import com.raindropcentral.core.config.RCentralConfig;
import com.raindropcentral.core.service.central.cookie.DropletCookieDefinitions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RCentralApiClientTest {

    @Test
    void appliesDropletStoreCompatibilitySnapshotToPayload() {
        final JsonObject payload = new JsonObject();

        RCentralApiClient.applyDropletStoreCompatibility(
                payload,
                new RCentralConfig.DropletStoreCompatibilitySnapshot(true, DropletCookieDefinitions.allItemCodes())
        );

        assertTrue(payload.get("dropletStoreEnabled").getAsBoolean());
        assertEquals(DropletCookieDefinitions.allItemCodes().size(), payload.getAsJsonArray("enabledDropletStoreItemCodes").size());
        assertEquals(
                DropletCookieDefinitions.allItemCodes(),
                payload.getAsJsonArray("enabledDropletStoreItemCodes")
                        .asList()
                        .stream()
                        .map(element -> element.getAsString())
                        .toList()
        );
    }

    @Test
    void skipsDropletStoreCompatibilityWhenSnapshotIsMissing() {
        final JsonObject payload = new JsonObject();

        RCentralApiClient.applyDropletStoreCompatibility(payload, null);

        assertTrue(payload.entrySet().isEmpty());
    }
}
