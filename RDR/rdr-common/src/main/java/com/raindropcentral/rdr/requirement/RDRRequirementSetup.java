/*
 * RDRRequirementSetup.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rdr.requirement;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.StoreRequirementSection;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import com.raindropcentral.rplatform.requirement.config.RequirementSectionAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers RDR-specific requirement support with the shared RPlatform requirement system.
 *
 * <p>This setup registers the config-section adapter used to convert RDR store requirement sections
 * into standard RPlatform requirements.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class RDRRequirementSetup {

    private static final Logger LOGGER = Logger.getLogger("RDR");

    private RDRRequirementSetup() {
    }

    /**
     * Initializes RDR requirement integration.
     *
     * @param plugin active RDR plugin instance
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public static void initialize(final @NotNull RDR plugin) {
        Objects.requireNonNull(plugin, "plugin");
        final RequirementFactory requirementFactory = RequirementFactory.getInstance();

        if (requirementFactory.getSectionAdapter(StoreRequirementSection.class) == null) {
            requirementFactory.registerSectionAdapter(
                StoreRequirementSection.class,
                new StoreRequirementSectionAdapter()
            );
        }

        LOGGER.info("Initialized RDR requirement integration");
    }

    /**
     * Removes the RDR requirement provider and section adapter.
     */
    public static void shutdown() {
        final RequirementFactory requirementFactory = RequirementFactory.getInstance();
        requirementFactory.unregisterSectionAdapter(StoreRequirementSection.class);
    }

    private static final class StoreRequirementSectionAdapter
        implements RequirementSectionAdapter<StoreRequirementSection> {

        @Override
        public @Nullable AbstractRequirement convert(
            final @NotNull StoreRequirementSection section,
            final @Nullable Map<String, Object> context
        ) {
            return RequirementFactory.getInstance().fromMap(section.toRequirementMap());
        }
    }
}