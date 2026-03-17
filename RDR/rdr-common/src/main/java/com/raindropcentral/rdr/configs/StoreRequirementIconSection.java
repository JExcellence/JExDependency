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

package com.raindropcentral.rdr.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Icon metadata for a configured storage-store requirement.
 *
 * <p>This mirrors the lightweight icon subsection shape already used elsewhere in Raindrop configs,
 * allowing entries like {@code icon.type: "DIAMOND"}.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class StoreRequirementIconSection {

    private final String type;

    /**
     * Creates a new icon section.
     *
     * @param type icon material identifier
     */
    public StoreRequirementIconSection(final @Nullable String type) {
        this.type = type == null || type.isBlank() ? "PAPER" : type.trim();
    }

    /**
     * Creates an icon section from YAML.
     *
     * @param section icon configuration section
     * @return parsed icon metadata
     */
    public static @NotNull StoreRequirementIconSection fromConfigurationSection(
        final @NotNull ConfigurationSection section
    ) {
        return new StoreRequirementIconSection(section.getString("type", "PAPER"));
    }

    /**
     * Returns the configured icon material type.
     *
     * @return icon material identifier
     */
    public @NotNull String getType() {
        return this.type;
    }
}