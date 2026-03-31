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
 * ShopConfigEditSupportTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.service.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and formatting helpers for editable config values.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopConfigEditSupportTest {

    @Test
    void determinesSettingTypes() {
        assertEquals(ShopConfigEditSupport.SettingType.BOOLEAN, ShopConfigEditSupport.determineType(true));
        assertEquals(ShopConfigEditSupport.SettingType.INTEGER, ShopConfigEditSupport.determineType(10));
        assertEquals(ShopConfigEditSupport.SettingType.LONG, ShopConfigEditSupport.determineType(10L));
        assertEquals(ShopConfigEditSupport.SettingType.DOUBLE, ShopConfigEditSupport.determineType(10.5D));
        assertEquals(ShopConfigEditSupport.SettingType.LIST, ShopConfigEditSupport.determineType(List.of("a")));
        assertEquals(ShopConfigEditSupport.SettingType.STRING, ShopConfigEditSupport.determineType("abc"));
    }

    @Test
    void parsesBooleanValues() {
        assertEquals(true, ShopConfigEditSupport.parseInput("true", ShopConfigEditSupport.SettingType.BOOLEAN));
        assertEquals(false, ShopConfigEditSupport.parseInput("off", ShopConfigEditSupport.SettingType.BOOLEAN));
        assertThrows(
                IllegalArgumentException.class,
                () -> ShopConfigEditSupport.parseInput("maybe", ShopConfigEditSupport.SettingType.BOOLEAN)
        );
    }

    @Test
    void parsesNumberValues() {
        assertEquals(42, ShopConfigEditSupport.parseInput("42", ShopConfigEditSupport.SettingType.INTEGER));
        assertEquals(1200L, ShopConfigEditSupport.parseInput("1200", ShopConfigEditSupport.SettingType.LONG));
        assertEquals(12.5D, ShopConfigEditSupport.parseInput("12.5", ShopConfigEditSupport.SettingType.DOUBLE));
    }

    @Test
    void parsesListValues() {
        assertEquals(
                List.of("coins", "gems", "vault"),
                ShopConfigEditSupport.parseInput("coins, gems, vault", ShopConfigEditSupport.SettingType.LIST)
        );
        assertEquals(
                List.of(),
                ShopConfigEditSupport.parseInput("   ", ShopConfigEditSupport.SettingType.LIST)
        );
    }

    @Test
    void formatsDisplayValues() {
        assertEquals("", ShopConfigEditSupport.formatValue(null));
        assertEquals("coins, gems", ShopConfigEditSupport.formatValue(List.of("coins", "gems")));
        assertEquals("10", ShopConfigEditSupport.formatValue(10));
    }

    @Test
    void checksEditableValues() {
        assertTrue(ShopConfigEditSupport.isEditableValue(null));
        assertTrue(ShopConfigEditSupport.isEditableValue("abc"));
        assertTrue(ShopConfigEditSupport.isEditableValue(true));
        assertTrue(ShopConfigEditSupport.isEditableValue(12));
        assertTrue(ShopConfigEditSupport.isEditableValue(List.of("coins")));
        assertFalse(ShopConfigEditSupport.isEditableValue(new Object()));
    }
}
