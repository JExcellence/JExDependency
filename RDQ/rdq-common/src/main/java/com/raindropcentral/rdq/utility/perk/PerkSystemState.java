package com.raindropcentral.rdq.utility.perk;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSystemSection;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the cached state of the perk system, including both configuration sections and
 * materialized database entities required for runtime operations.
 *
 * <p>The state bundles the raw configuration definitions for perks alongside the
 * resolved {@link RPerk} instances that are loaded from the database. The
 * combination allows the calling context to quickly evaluate perk metadata without repeatedly
 * querying the configuration or persistence layers.</p>
 *
 * <p>The builder should be used when consolidating state from multiple sources, while
 * {@link #empty()} provides a clean baseline for scenarios where the configuration is not
 * available.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
public final class PerkSystemState {

    private final @Nullable PerkSystemSection perkSystemSection;
    private final @NotNull Map<String, PerkSection> perkSections;
    private final @NotNull Map<String, RPerk> perks;

    /**
     * Creates a new state instance with explicit configuration sections and database projections.
     *
     * @param perkSystemSection the root system configuration, or {@code null} when no system is defined
     * @param perkSections      the collection of configured perks keyed by perk identifier
     * @param perks             the resolved perk entities keyed by perk identifier
     */
    private PerkSystemState(@Nullable PerkSystemSection perkSystemSection,
                            @NotNull Map<String, PerkSection> perkSections,
                            @NotNull Map<String, RPerk> perks) {
        this.perkSystemSection = perkSystemSection;
        this.perkSections = perkSections;
        this.perks = perks;
    }

    /**
     * Creates an empty state with no configuration or perk data.
     *
     * @return a state instance with empty maps
     */
    static @NotNull PerkSystemState empty() {
        return new PerkSystemState(null, new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a builder for progressively constructing a state instance.
     *
     * @return a new builder instance
     */
    static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Gets the configuration for the perk system.
     *
     * @return the perk system section or {@code null} when the configuration is missing
     */
    @Nullable PerkSystemSection perkSystemSection() {
        return perkSystemSection;
    }

    /**
     * Gets the configured perks.
     *
     * @return a map of perk identifiers to their configuration sections
     */
    @NotNull Map<String, PerkSection> perkSections() {
        return perkSections;
    }

    /**
     * Gets the resolved perk entities.
     *
     * @return a map of perk identifiers to their database entities
     */
    @NotNull Map<String, RPerk> perks() {
        return perks;
    }

    /**
     * Builder for aggregating configuration and database state into a {@link PerkSystemState}.
     */
    static final class Builder {
        private PerkSystemSection perkSystemSection;
        private Map<String, PerkSection> perkSections = new HashMap<>();
        private Map<String, RPerk> perks = new HashMap<>();

        /**
         * Sets the root perk system configuration for the resulting state.
         *
         * @param section the perk system configuration
         * @return this builder instance
         */
        Builder perkSystemSection(PerkSystemSection section) {
            this.perkSystemSection = section;
            return this;
        }

        /**
         * Sets the collection of perk configurations.
         *
         * @param sections the configured perks, or {@code null} to clear the collection
         * @return this builder instance
         */
        Builder perkSections(Map<String, PerkSection> sections) {
            this.perkSections = sections != null ? sections : new HashMap<>();
            return this;
        }

        /**
         * Sets the resolved perk entities.
         *
         * @param map the perk entities, or {@code null} to clear the collection
         * @return this builder instance
         */
        Builder perks(Map<String, RPerk> map) {
            this.perks = map != null ? map : new HashMap<>();
            return this;
        }

        /**
         * Builds a new {@link PerkSystemState} with the configured values.
         *
         * @return the constructed state
         */
        PerkSystemState build() {
            return new PerkSystemState(perkSystemSection, perkSections, perks);
        }
    }
}
