package de.jexcellence.oneblock.requirement.generator;

import com.raindropcentral.rplatform.requirement.PluginRequirementProvider;
import com.raindropcentral.rplatform.requirement.RequirementType;
import de.jexcellence.oneblock.requirement.EvolutionCurrencyRequirement;
import de.jexcellence.oneblock.requirement.EvolutionCustomRequirement;
import de.jexcellence.oneblock.requirement.EvolutionExperienceRequirement;
import de.jexcellence.oneblock.requirement.EvolutionItemRequirement;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Registers all OneBlock-specific requirements with RPlatform.
 * <p>
 * This provider registers both generator requirements and evolution requirements,
 * allowing them to be used throughout the requirement system with auto-detection,
 * JSON serialization, and caching support.
 * </p>
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
public class OneBlockRequirementProvider implements PluginRequirementProvider {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");

    @Override
    @NotNull
    public String getPluginId() {
        return "jexoneblock";
    }

    @Override
    @NotNull
    public Map<String, RequirementType> getRequirementTypes() {
        return Map.of(
                "EVOLUTION_LEVEL", RequirementType.plugin("EVOLUTION_LEVEL", "jexoneblock", EvolutionLevelRequirement.class),
                "BLOCKS_BROKEN", RequirementType.plugin("BLOCKS_BROKEN", "jexoneblock", BlocksBrokenRequirement.class),
                "PRESTIGE_LEVEL", RequirementType.plugin("PRESTIGE_LEVEL", "jexoneblock", PrestigeLevelRequirement.class),
                "ISLAND_LEVEL", RequirementType.plugin("ISLAND_LEVEL", "jexoneblock", IslandLevelRequirement.class),
                "GENERATOR_TIER", RequirementType.plugin("GENERATOR_TIER", "jexoneblock", GeneratorTierRequirement.class),

                "EVOLUTION_CURRENCY", RequirementType.plugin("EVOLUTION_CURRENCY", "jexoneblock", EvolutionCurrencyRequirement.class),
                "EVOLUTION_EXPERIENCE", RequirementType.plugin("EVOLUTION_EXPERIENCE", "jexoneblock", EvolutionExperienceRequirement.class),
                "EVOLUTION_ITEM", RequirementType.plugin("EVOLUTION_ITEM", "jexoneblock", EvolutionItemRequirement.class),
                "EVOLUTION_CUSTOM", RequirementType.plugin("EVOLUTION_CUSTOM", "jexoneblock", EvolutionCustomRequirement.class)
        );
    }

    @Override
    public void onRegister() {
        OneBlockRequirementConverters.register();
        LOGGER.info("Registered OneBlock requirement provider with " + getRequirementTypes().size() + " types");
    }

    @Override
    public void onUnregister() {
        OneBlockRequirementConverters.unregister();
        LOGGER.info("Unregistered OneBlock requirement provider");
    }
}
