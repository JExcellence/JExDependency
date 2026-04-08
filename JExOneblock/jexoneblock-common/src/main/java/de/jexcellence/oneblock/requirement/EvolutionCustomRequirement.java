package de.jexcellence.oneblock.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Evolution Custom Requirement - supports custom OneBlock-specific requirements.
 * Extends RPlatform's AbstractRequirement for unified requirement handling.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
@JsonTypeName("EVOLUTION_CUSTOM")
public class EvolutionCustomRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");

    public enum CustomType {
        BLOCKS_BROKEN("Blocks Broken"),
        ISLANDS_VISITED("Islands Visited"),
        STRUCTURES_BUILT("Structures Built"),
        CHESTS_OPENED("Chests Opened"),
        ENTITIES_KILLED("Entities Killed"),
        ITEMS_CRAFTED("Items Crafted"),
        EVOLUTION_TIME("Time in Evolution"),
        ISLAND_LEVEL("Island Level"),
        STORAGE_ITEMS("Storage Items"),
        INFRASTRUCTURE_LEVEL("Infrastructure Level");

        private final String displayName;

        CustomType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @JsonProperty("customType")
    private final CustomType customType;

    @JsonProperty("requiredValue")
    private final double requiredValue;

    @JsonProperty("parameters")
    private final Map<String, Object> parameters;

    @JsonProperty("evolutionName")
    @Nullable
    private final String evolutionName;

    @JsonCreator
    public EvolutionCustomRequirement(
            @JsonProperty("customType") @NotNull CustomType customType,
            @JsonProperty("requiredValue") double requiredValue,
            @JsonProperty("parameters") @Nullable Map<String, Object> parameters,
            @JsonProperty("evolutionName") @Nullable String evolutionName,
            @JsonProperty("consumeOnComplete") @Nullable Boolean consumeOnComplete
    ) {
        super("CUSTOM", consumeOnComplete != null ? consumeOnComplete : false);
        this.customType = customType;
        this.requiredValue = requiredValue;
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        this.evolutionName = evolutionName;
    }

    public EvolutionCustomRequirement(@NotNull CustomType customType, double requiredValue) {
        this(customType, requiredValue, null, null, false);
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return getCurrentValue(player) >= requiredValue;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (requiredValue <= 0) return 1.0;
        return Math.min(1.0, getCurrentValue(player) / requiredValue);
    }

    @Override
    public void consume(@NotNull Player player) {
        // Most custom requirements are milestone-based and not consumed
        if (!shouldConsume()) return;

        if (customType == CustomType.STORAGE_ITEMS) {
            consumeStorageItems(player);
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "evolution.requirement.custom." + customType.name().toLowerCase();
    }

    private double getCurrentValue(@NotNull Player player) {
        return switch (customType) {
            case BLOCKS_BROKEN -> getBlocksBroken(player);
            case ISLANDS_VISITED -> getIslandsVisited(player);
            case STRUCTURES_BUILT -> getStructuresBuilt(player);
            case CHESTS_OPENED -> getChestsOpened(player);
            case ENTITIES_KILLED -> getEntitiesKilled(player);
            case ITEMS_CRAFTED -> getItemsCrafted(player);
            case EVOLUTION_TIME -> getEvolutionTime(player);
            case ISLAND_LEVEL -> getIslandLevel(player);
            case STORAGE_ITEMS -> getStorageItems(player);
            case INFRASTRUCTURE_LEVEL -> getInfrastructureLevel(player);
        };
    }

    private double getBlocksBroken(@NotNull Player player) {
        return getIslandStat(player, "getTotalBlocksBroken");
    }

    private double getIslandsVisited(@NotNull Player player) {
        // Would integrate with visit tracking
        return 0;
    }

    private double getStructuresBuilt(@NotNull Player player) {
        // Would integrate with structure service
        return 0;
    }

    private double getChestsOpened(@NotNull Player player) {
        // Would integrate with chest tracking
        return 0;
    }

    private double getEntitiesKilled(@NotNull Player player) {
        // Would integrate with kill tracking
        return 0;
    }

    private double getItemsCrafted(@NotNull Player player) {
        // Would integrate with crafting stats
        return 0;
    }

    private double getEvolutionTime(@NotNull Player player) {
        // Would integrate with time tracking
        return 0;
    }

    private double getIslandLevel(@NotNull Player player) {
        return getIslandStat(player, "getLevel");
    }

    private double getStorageItems(@NotNull Player player) {
        // Would integrate with storage system
        return 0;
    }

    private double getInfrastructureLevel(@NotNull Player player) {
        // Would integrate with infrastructure system
        return 0;
    }

    private double getIslandStat(@NotNull Player player, String methodName) {
        try {
            var provider = Bukkit.getServicesManager()
                    .getRegistration(Class.forName("de.jexcellence.oneblock.repository.OneblockIslandRepository"));
            if (provider != null) {
                var repo = provider.getProvider();
                var method = repo.getClass().getMethod("findByOwnerUuid", java.util.UUID.class);
                @SuppressWarnings("unchecked")
                var result = (java.util.Optional<?>) method.invoke(repo, player.getUniqueId());
                if (result.isPresent()) {
                    var island = result.get();
                    var statMethod = island.getClass().getMethod(methodName);
                    return ((Number) statMethod.invoke(island)).doubleValue();
                }
            }
        } catch (Exception e) {
            LOGGER.fine("Could not get island stat " + methodName + ": " + e.getMessage());
        }
        return 0;
    }

    private void consumeStorageItems(@NotNull Player player) {
        // Would integrate with storage system
        LOGGER.fine("Storage item consumption not yet implemented");
    }

    @NotNull
    public CustomType getCustomType() {
        return customType;
    }

    public double getRequiredValue() {
        return requiredValue;
    }

    @NotNull
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Nullable
    public Object getParameter(@NotNull String key) {
        return parameters.get(key);
    }

    @Nullable
    public String getEvolutionName() {
        return evolutionName;
    }

    @Override
    public String toString() {
        return "EvolutionCustomRequirement{type=" + customType +
                ", value=" + requiredValue + ", evolution='" + evolutionName + "'}";
    }
}
