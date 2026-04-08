package de.jexcellence.oneblock.requirement.generator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import org.jetbrains.annotations.NotNull;

/**
 * Requirement for reaching a specific island level.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
@JsonTypeName("ISLAND_LEVEL")
public class IslandLevelRequirement extends OneBlockRequirement {

    @JsonProperty("requiredLevel")
    private final int requiredLevel;

    @JsonCreator
    public IslandLevelRequirement(@JsonProperty("requiredLevel") int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    @Override
    protected long getCurrentValue(@NotNull OneblockIsland island) {
        return island.getLevel();
    }

    @Override
    protected long getRequiredValue() {
        return requiredLevel;
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "generator.requirement.island_level";
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    @Override
    public String toString() {
        return "IslandLevelRequirement{requiredLevel=" + requiredLevel + '}';
    }
}
