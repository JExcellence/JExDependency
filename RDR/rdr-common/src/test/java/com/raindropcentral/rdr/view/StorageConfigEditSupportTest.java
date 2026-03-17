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

package com.raindropcentral.rdr.view;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and formatting behavior for storage config editing helpers.
 */
class StorageConfigEditSupportTest {

    @Test
    void detectsSupportedSettingTypes() {
        assertEquals(StorageConfigEditSupport.SettingType.BOOLEAN, StorageConfigEditSupport.determineType(true));
        assertEquals(StorageConfigEditSupport.SettingType.INTEGER, StorageConfigEditSupport.determineType(3));
        assertEquals(StorageConfigEditSupport.SettingType.LONG, StorageConfigEditSupport.determineType(9L));
        assertEquals(StorageConfigEditSupport.SettingType.DOUBLE, StorageConfigEditSupport.determineType(2.5D));
        assertEquals(StorageConfigEditSupport.SettingType.LIST, StorageConfigEditSupport.determineType(List.of("a")));
        assertEquals(StorageConfigEditSupport.SettingType.STRING, StorageConfigEditSupport.determineType("value"));
    }

    @Test
    void validatesEditableValueCandidates() {
        assertTrue(StorageConfigEditSupport.isEditableValue(null));
        assertTrue(StorageConfigEditSupport.isEditableValue("value"));
        assertTrue(StorageConfigEditSupport.isEditableValue(true));
        assertTrue(StorageConfigEditSupport.isEditableValue(7));
        assertTrue(StorageConfigEditSupport.isEditableValue(List.of("a", "b")));
        assertFalse(StorageConfigEditSupport.isEditableValue(new Object()));
    }

    @Test
    void parsesEachSupportedInputType() {
        final Object parsedBoolean = StorageConfigEditSupport.parseInput("yes", StorageConfigEditSupport.SettingType.BOOLEAN);
        final Object parsedInteger = StorageConfigEditSupport.parseInput("42", StorageConfigEditSupport.SettingType.INTEGER);
        final Object parsedLong = StorageConfigEditSupport.parseInput("9001", StorageConfigEditSupport.SettingType.LONG);
        final Object parsedDouble = StorageConfigEditSupport.parseInput("3.5", StorageConfigEditSupport.SettingType.DOUBLE);
        final Object parsedList = StorageConfigEditSupport.parseInput("alpha, beta, gamma", StorageConfigEditSupport.SettingType.LIST);
        final Object parsedString = StorageConfigEditSupport.parseInput("raw text", StorageConfigEditSupport.SettingType.STRING);

        assertEquals(true, parsedBoolean);
        assertEquals(42, parsedInteger);
        assertEquals(9001L, parsedLong);
        assertEquals(3.5D, parsedDouble);
        assertInstanceOf(List.class, parsedList);
        assertEquals(List.of("alpha", "beta", "gamma"), parsedList);
        assertEquals("raw text", parsedString);
    }

    @Test
    void rejectsInvalidTypedInput() {
        assertThrows(
            IllegalArgumentException.class,
            () -> StorageConfigEditSupport.parseInput("not-a-number", StorageConfigEditSupport.SettingType.INTEGER)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> StorageConfigEditSupport.parseInput("maybe", StorageConfigEditSupport.SettingType.BOOLEAN)
        );
    }

    @Test
    void formatsListAndScalarValues() {
        assertEquals("a, b, c", StorageConfigEditSupport.formatValue(List.of("a", "b", "c")));
        assertEquals("15", StorageConfigEditSupport.formatValue(15));
        assertEquals("", StorageConfigEditSupport.formatValue(null));
    }
}
