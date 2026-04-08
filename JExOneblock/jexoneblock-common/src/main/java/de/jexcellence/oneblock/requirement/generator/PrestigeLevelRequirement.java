package de.jexcellence.oneblock.requirement.generator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import org.jetbrains.annotations.NotNull;

/**
 * Requirement for reaching a specific prestige level.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
@JsonTypeName("PRESTIGE_LEVEL")
public class PrestigeLevelRequirement extends OneBlockRequirement {

    @JsonProperty("requiredPrestige")
    private final int requiredPrestige;

    @JsonCreator
    public PrestigeLevelRequirement(@JsonProperty("requiredPrestige") int requiredPrestige) {
        this.requiredPrestige = requiredPrestige;
    }

    @Override
    protected long getCurrentValue(@NotNull OneblockIsland island) {
        var oneblock = island.getOneblock();
        return oneblock != null ? oneblock.getPrestigeLevel() : 0;
    }

    @Override
    protected long getRequiredValue() {
        return requiredPrestige;
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "generator.requirement.prestige_level";
    }

    public int getRequiredPrestige() {
        return requiredPrestige;
    }

    @Override
    public String toString() {
        return "PrestigeLevelRequirement{requiredPrestige=" + requiredPrestige + '}';
    }
}
