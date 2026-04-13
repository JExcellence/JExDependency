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

package com.raindropcentral.rdt.configs;

import com.raindropcentral.rdt.utils.TownArchetype;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and default fallback behavior for {@link TaxConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class TaxConfigSectionTest {

    @Test
    void createDefaultExposesExpectedTaxDefaults() {
        final TaxConfigSection section = TaxConfigSection.createDefault();

        assertEquals(1728000L, section.getSchedule().durationTicks());
        assertEquals(LocalTime.NOON, section.getSchedule().startTime());
        assertEquals(ZoneId.systemDefault(), section.getSchedule().timeZoneId());
        assertEquals(2.0D, section.getCurrency().baseTaxPercent());
        assertEquals(0.0D, section.getCurrency().minimumTaxPercent());
        assertEquals(25.0D, section.getCurrency().maximumTaxPercent());
        assertEquals(java.util.List.of("vault", "raindrops"), section.getCurrency().currencyIds());
        assertEquals("base_tax_percent", section.getCurrency().rateExpression());
        assertEquals(0.0D, section.getArchetypeModifier(TownArchetype.CAPITALIST));
        assertTrue(section.getTownItemTaxes().isEmpty());
        assertTrue(section.getNationItemTaxes().isEmpty());
        assertEquals(12096000L, section.getDebt().gracePeriodTicks());
        assertEquals(72000L, section.getDebt().warningIntervalTicks());
        assertTrue(section.getDebt().joinReminderEnabled());
        assertTrue(section.getDebt().broadcastTownFall());
        assertTrue(section.getDebt().broadcastNationDisband());
    }

    @Test
    void fromInputStreamParsesConfiguredScheduleExpressionAndItemTaxes() {
        final TaxConfigSection section = TaxConfigSection.fromInputStream(new ByteArrayInputStream("""
            duration_ticks: 24000
            start_time: "8:30pm"
            time_zone: "America/Los_Angeles"
            currency_tax:
              base_tax_percent: 3.5
              minimum_tax_percent: 1.0
              maximum_tax_percent: 12.0
              currency_ids:
                - " Vault "
                - "RAINDROPS"
                - "vault"
              rate_expression: "base_tax_percent + town_level + archetype_modifier + chunk_type_bank_count"
            archetype_modifiers:
              capitalist: 0.5
              monarchy: 0.2
            debt:
              grace_period_ticks: 400
              warning_interval_ticks: 200
              join_reminder_enabled: false
              broadcast_town_fall: false
              broadcast_nation_disband: true
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(24000L, section.getSchedule().durationTicks());
        assertEquals(LocalTime.of(20, 30), section.getSchedule().startTime());
        assertEquals(ZoneId.of("America/Los_Angeles"), section.getSchedule().timeZoneId());
        assertEquals(3.5D, section.getCurrency().baseTaxPercent());
        assertEquals(1.0D, section.getCurrency().minimumTaxPercent());
        assertEquals(12.0D, section.getCurrency().maximumTaxPercent());
        assertEquals(java.util.List.of("vault", "raindrops"), section.getCurrency().currencyIds());
        assertEquals(
            "base_tax_percent + town_level + archetype_modifier + chunk_type_bank_count",
            section.getCurrency().rateExpression()
        );
        assertEquals(0.5D, section.getArchetypeModifier(TownArchetype.CAPITALIST));
        assertEquals(0.2D, section.getArchetypeModifier(TownArchetype.MONARCHY));
        assertTrue(section.getTownItemTaxes().isEmpty());
        assertTrue(section.getNationItemTaxes().isEmpty());
        assertEquals(400L, section.getDebt().gracePeriodTicks());
        assertEquals(200L, section.getDebt().warningIntervalTicks());
        assertFalse(section.getDebt().joinReminderEnabled());
        assertFalse(section.getDebt().broadcastTownFall());
        assertTrue(section.getDebt().broadcastNationDisband());
    }

    @Test
    void invalidValuesFallBackToSafeTaxDefaults() {
        final TaxConfigSection section = TaxConfigSection.fromInputStream(new ByteArrayInputStream("""
            duration_ticks: 0
            start_time: "not-a-time"
            time_zone: "Not/A_Zone"
            currency_tax:
              minimum_tax_percent: -5
              maximum_tax_percent: -1
              currency_ids: []
              rate_expression: "("
            debt:
              grace_period_ticks: 0
              warning_interval_ticks: 1
              join_reminder_enabled: false
              broadcast_town_fall: false
              broadcast_nation_disband: false
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1L, section.getSchedule().durationTicks());
        assertEquals(LocalTime.NOON, section.getSchedule().startTime());
        assertEquals(ZoneId.systemDefault(), section.getSchedule().timeZoneId());
        assertEquals(0.0D, section.getCurrency().minimumTaxPercent());
        assertEquals(0.0D, section.getCurrency().maximumTaxPercent());
        assertEquals(java.util.List.of("vault", "raindrops"), section.getCurrency().currencyIds());
        assertEquals("base_tax_percent", section.getCurrency().rateExpression());
        assertTrue(section.getTownItemTaxes().isEmpty());
        assertEquals(1L, section.getDebt().gracePeriodTicks());
        assertEquals(20L, section.getDebt().warningIntervalTicks());
        assertFalse(section.getDebt().joinReminderEnabled());
        assertFalse(section.getDebt().broadcastTownFall());
        assertFalse(section.getDebt().broadcastNationDisband());
        assertEquals(0.0D, section.getCurrency().clampRate(99.0D));
    }
}
