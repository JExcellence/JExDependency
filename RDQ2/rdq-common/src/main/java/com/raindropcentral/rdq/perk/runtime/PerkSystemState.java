package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSystemSection;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the cached state of the perk system, including both configuration sections and
 * materialized database entities required for runtime operations.
 *
 * <p>The state bundles the raw configuration definitions for perks alongside the resolved
 * {@link RPerk} instances that are loaded from the database. The combination allows the calling
 * context to quickly evaluate perk metadata without repeatedly querying the configuration or
 * persistence layers.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public final class PerkSystemState {

    private final @NotNull PerkSystemSection perkSystemSection;
    private final @NotNull Map<String, PerkSection> perkSections;
    private final @NotNull Map<String, RPerk> perks;

    /**
     * Creates a new state instance with explicit configuration sections and database projections.
     *
     * @param perkSections the collection of configured perks keyed by perk identifier
     * @param perks        the resolved perk entities keyed by perk identifier
     */
    private PerkSystemState(
            final @NotNull PerkSystemSection perkSystemSection,
            final @NotNull Map<String, PerkSection> perkSections,
            final @NotNull Map<String, RPerk> perks
    ) {
        this.perkSystemSection = perkSystemSection;
        this.perkSections = perkSections;
        this.perks = perks;
    }

    /**
     * Creates an empty state with no configuration or perk data.
     *
     * @return a state instance with empty maps
     */
    public static @NotNull PerkSystemState empty() {
        return new PerkSystemState(new PerkSystemSection(new EvaluationEnvironmentBuilder()), new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a builder for progressively constructing a state instance.
     *
     * @return a new builder instance
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Gets the configured perk definitions.
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
    public static final class Builder {

        private PerkSystemSection perkSystemSection;
        private Map<String, PerkSection> perkSections = new HashMap<>();
        private Map<String, RPerk> perks = new HashMap<>();

        /**
         * Sets the collection of perk configurations.
         *
         * @param sections the configured perks, or {@code null} to clear the collection
         * @return this builder instance
         */
        public Builder perkSections(Map<String, PerkSection> sections) {
            this.perkSections = sections != null ? sections : new HashMap<>();
            return this;
        }

        public Builder perkSystemSection(PerkSystemSection section) {
            this.perkSystemSection = section != null ? section :  new PerkSystemSection(new EvaluationEnvironmentBuilder());
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
        public PerkSystemState build() {
            return new PerkSystemState(this.perkSystemSection, this.perkSections, this.perks);
        }
    }
}
