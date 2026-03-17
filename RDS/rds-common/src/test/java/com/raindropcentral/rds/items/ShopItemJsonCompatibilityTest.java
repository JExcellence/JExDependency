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

/*
 * ShopItemJsonCompatibilityTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.items;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies JSON compatibility safeguards on {@link ShopItem}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopItemJsonCompatibilityTest {

    /**
     * Ensures legacy {@code availableNow} payload data is ignored during deserialization.
     */
    @Test
    void ignoresLegacyAvailableNowProperty() {
        final JsonIgnoreProperties ignoreProperties = ShopItem.class.getAnnotation(JsonIgnoreProperties.class);
        assertNotNull(ignoreProperties);
        assertTrue(Arrays.asList(ignoreProperties.value()).contains("availableNow"));
        assertTrue(Arrays.asList(ignoreProperties.value()).contains("estimatedValue"));
        assertTrue(Arrays.asList(ignoreProperties.value()).contains("typeId"));
        assertTrue(Arrays.asList(ignoreProperties.value()).contains("descriptionKey"));
        assertTrue(ignoreProperties.ignoreUnknown());
    }

    /**
     * Ensures runtime availability snapshots are not serialized to persisted shop item payloads.
     *
     * @throws NoSuchMethodException when the method signature changes unexpectedly
     */
    @Test
    void doesNotSerializeComputedAvailabilitySnapshot() throws NoSuchMethodException {
        final Method availabilityMethod = ShopItem.class.getMethod("isAvailableNow");
        assertNotNull(availabilityMethod.getAnnotation(JsonIgnore.class));
    }
}
