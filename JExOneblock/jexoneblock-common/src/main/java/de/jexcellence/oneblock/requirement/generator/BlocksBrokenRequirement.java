package de.jexcellence.oneblock.requirement.generator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import org.jetbrains.annotations.NotNull;

/**
 * Requirement for breaking a specific number of blocks on the oneblock.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
@JsonTypeName("BLOCKS_BROKEN")
public class BlocksBrokenRequirement extends OneBlockRequirement {

    @JsonProperty("requiredBlocks")
    private final long requiredBlocks;

    @JsonCreator
    public BlocksBrokenRequirement(@JsonProperty("requiredBlocks") long requiredBlocks) {
        this.requiredBlocks = requiredBlocks;
    }

    @Override
    protected long getCurrentValue(@NotNull OneblockIsland island) {
        return island.getTotalBlocksBroken();
    }

    @Override
    protected long getRequiredValue() {
        return requiredBlocks;
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "generator.requirement.blocks_broken";
    }

    public long getRequiredBlocks() {
        return requiredBlocks;
    }

    @Override
    public String toString() {
        return "BlocksBrokenRequirement{requiredBlocks=" + requiredBlocks + '}';
    }
}
