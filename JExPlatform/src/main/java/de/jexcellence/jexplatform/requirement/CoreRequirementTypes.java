package de.jexcellence.jexplatform.requirement;

import de.jexcellence.jexplatform.requirement.impl.*;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all core platform requirement types with a registry.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class CoreRequirementTypes {

    /** Item possession requirement. */
    public static final RequirementType ITEM =
            RequirementType.core("ITEM", ItemRequirement.class);

    /** Currency balance requirement. */
    public static final RequirementType CURRENCY =
            RequirementType.core("CURRENCY", CurrencyRequirement.class);

    /** Experience level requirement. */
    public static final RequirementType EXPERIENCE_LEVEL =
            RequirementType.core("EXPERIENCE_LEVEL", ExperienceLevelRequirement.class);

    /** Permission requirement. */
    public static final RequirementType PERMISSION =
            RequirementType.core("PERMISSION", PermissionRequirement.class);

    /** Location proximity requirement. */
    public static final RequirementType LOCATION =
            RequirementType.core("LOCATION", LocationRequirement.class);

    /** Playtime requirement. */
    public static final RequirementType PLAYTIME =
            RequirementType.core("PLAYTIME", PlaytimeRequirement.class);

    /** Time-windowed requirement. */
    public static final RequirementType TIME_BASED =
            RequirementType.core("TIME_BASED", TimedRequirement.class);

    /** Composite (AND/OR/MINIMUM) requirement. */
    public static final RequirementType COMPOSITE =
            RequirementType.core("COMPOSITE", CompositeRequirement.class);

    /** Choice-based requirement. */
    public static final RequirementType CHOICE =
            RequirementType.core("CHOICE", ChoiceRequirement.class);

    /** Plugin-delegated requirement. */
    public static final RequirementType PLUGIN =
            RequirementType.core("PLUGIN", PluginRequirement.class);

    private CoreRequirementTypes() {
    }

    /**
     * Registers all core types with the given registry.
     *
     * @param registry the requirement registry
     */
    public static void registerAll(@NotNull RequirementRegistry registry) {
        registry.register(ITEM);
        registry.register(CURRENCY);
        registry.register(EXPERIENCE_LEVEL);
        registry.register(PERMISSION);
        registry.register(LOCATION);
        registry.register(PLAYTIME);
        registry.register(TIME_BASED);
        registry.register(COMPOSITE);
        registry.register(CHOICE);
        registry.register(PLUGIN);
    }
}
