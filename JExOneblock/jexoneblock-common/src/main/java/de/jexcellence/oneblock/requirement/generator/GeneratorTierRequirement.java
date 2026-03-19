package de.jexcellence.oneblock.requirement.generator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.service.GeneratorDesignRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Requirement for having unlocked a previous generator tier.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
@JsonTypeName("GENERATOR_TIER")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratorTierRequirement extends OneBlockRequirement {

    @JsonProperty("requiredTier")
    private final String requiredTier;

    @JsonCreator
    public GeneratorTierRequirement(@JsonProperty("requiredTier") String requiredTier) {
        this.requiredTier = requiredTier;
    }

    public GeneratorTierRequirement(@NotNull EGeneratorDesignType requiredTier) {
        this(requiredTier.name());
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return getPlayerIsland(player)
                .map(island -> hasUnlockedTier(island, getRequiredDesignType()))
                .orElse(false);
    }

    @Override
    protected long getCurrentValue(@NotNull OneblockIsland island) {
        // For tier requirements, we use binary: 1 if unlocked, 0 if not
        return hasUnlockedTier(island, getRequiredDesignType()) ? 1 : 0;
    }

    @Override
    protected long getRequiredValue() {
        return 1; // Must have unlocked the tier
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "generator.requirement.generator_tier";
    }

    private boolean hasUnlockedTier(@NotNull OneblockIsland island, EGeneratorDesignType tier) {
        if (tier == null) return false;

        try {
            RegisteredServiceProvider<GeneratorDesignRegistry> provider =
                    Bukkit.getServicesManager().getRegistration(GeneratorDesignRegistry.class);
            if (provider != null) {
                GeneratorDesignRegistry registry = provider.getProvider();
                return registry.hasUnlockedDesign(island, tier);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public String getRequiredTier() {
        return requiredTier;
    }

    public EGeneratorDesignType getRequiredDesignType() {
        return EGeneratorDesignType.fromKey(requiredTier);
    }

    @Override
    public void validate() {
        super.validate();
        if (requiredTier == null || requiredTier.isEmpty()) {
            throw new IllegalStateException("Required tier cannot be null or empty");
        }
        if (EGeneratorDesignType.fromKey(requiredTier) == null) {
            throw new IllegalStateException("Invalid generator tier: " + requiredTier);
        }
    }

    @Override
    public String toString() {
        return "GeneratorTierRequirement{requiredTier='" + requiredTier + "'}";
    }
}
