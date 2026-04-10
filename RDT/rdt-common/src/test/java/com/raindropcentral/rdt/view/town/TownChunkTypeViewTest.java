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

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.utils.ChunkType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TownChunkTypeViewTest {

    @Test
    void benefitLoreKeysAreTypeSpecific() {
        assertEquals("entry.benefits.farm", TownChunkTypeView.resolveBenefitLoreKey(ChunkType.FARM));
        assertEquals("entry.benefits.security", TownChunkTypeView.resolveBenefitLoreKey(ChunkType.SECURITY));
        assertEquals("entry.benefits.medic", TownChunkTypeView.resolveBenefitLoreKey(ChunkType.MEDIC));
        assertEquals("entry.benefits.bank", TownChunkTypeView.resolveBenefitLoreKey(ChunkType.BANK));
    }

    @Test
    void translationFileIncludesFarmSecurityAndMedicBenefitDescriptions() {
        final YamlConfiguration translations = YamlConfiguration.loadConfiguration(new InputStreamReader(
            Objects.requireNonNull(
                TownChunkTypeViewTest.class.getClassLoader().getResourceAsStream("translations/en_US.yml"),
                "translations/en_US.yml"
            ),
            StandardCharsets.UTF_8
        ));

        assertFalse(translations.getStringList("town_chunk_type_ui.entry.benefits.farm").isEmpty());
        assertFalse(translations.getStringList("town_chunk_type_ui.entry.benefits.security").isEmpty());
        assertFalse(translations.getStringList("town_chunk_type_ui.entry.benefits.medic").isEmpty());
        assertFalse(translations.getStringList("town_chunk_ui.medic.food_regen.lore").isEmpty());
    }

    @Test
    void fobIsNotSelectableInTheChunkTypeSelector() {
        assertFalse(TownChunkTypeView.isSelectableType(ChunkType.FOB));
        assertFalse(TownChunkTypeView.isSelectableType(ChunkType.NEXUS));
    }
}
