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
 * ItemParserCompatibilityTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.items.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies compatibility-focused ObjectMapper settings for item payload parsing.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ItemParserCompatibilityTest {

    /**
     * Ensures unknown JSON properties are ignored so legacy persisted payloads remain readable.
     */
    @Test
    void disablesFailOnUnknownProperties() {
        assertFalse(ItemParser.getObjectMapper().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }
}
