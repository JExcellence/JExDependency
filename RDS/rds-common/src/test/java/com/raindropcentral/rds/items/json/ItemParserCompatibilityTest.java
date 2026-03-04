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
