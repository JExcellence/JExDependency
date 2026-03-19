package de.jexcellence.oneblock.requirement.generator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Requirement for reaching a specific evolution level.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
@JsonTypeName("EVOLUTION_LEVEL")
public class EvolutionLevelRequirement extends OneBlockRequirement {

    @JsonProperty("requiredLevel")
    private final int requiredLevel;

    @JsonProperty("evolutionName")
    @Nullable
    private final String evolutionName;

    @JsonCreator
    public EvolutionLevelRequirement(
            @JsonProperty("requiredLevel") int requiredLevel,
            @JsonProperty("evolutionName") @Nullable String evolutionName
    ) {
        this.requiredLevel = requiredLevel;
        this.evolutionName = evolutionName;
    }

    public EvolutionLevelRequirement(int requiredLevel) {
        this(requiredLevel, null);
    }

    @Override
    protected long getCurrentValue(@NotNull OneblockIsland island) {
        var oneblock = island.getOneblock();
        return oneblock != null ? oneblock.getEvolutionLevel() : 0;
    }

    @Override
    protected long getRequiredValue() {
        return requiredLevel;
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "generator.requirement.evolution_level";
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    @Nullable
    public String getEvolutionName() {
        return evolutionName;
    }

    @Override
    public String toString() {
        return "EvolutionLevelRequirement{requiredLevel=" + requiredLevel +
                ", evolutionName='" + evolutionName + "'}";
    }
}
