/*
 * StoreRequirementIconSection.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Icon metadata for a configured RDS shop-store requirement.
 *
 * <p>This mirrors the lightweight icon subsection shape used by other Raindrop configs, allowing
 * entries such as {@code icon.type: "DIAMOND"}.</p>
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