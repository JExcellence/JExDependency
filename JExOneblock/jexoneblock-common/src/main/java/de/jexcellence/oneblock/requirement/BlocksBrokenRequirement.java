package de.jexcellence.oneblock.requirement;

import com.raindropcentral.rplatform.requirement.async.AsyncRequirement;
import de.jexcellence.oneblock.api.OneBlockAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Requirement that checks if a player has broken a specific number of blocks.
 */
public final class BlocksBrokenRequirement extends AsyncRequirement {
    
    private final long requiredAmount;
    private final Material blockType;
    private final String islandId;
    
    public BlocksBrokenRequirement(
        long requiredAmount,
        @Nullable Material blockType,
        @NotNull String islandId
    ) {
        super("BLOCKS_BROKEN");
        this.requiredAmount = requiredAmount;
        this.blockType = blockType;
        this.islandId = islandId;
    }
    
    @Override
    public @NotNull CompletableFuture<Boolean> isMetAsync(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            long broken = blockType == null
                ? OneBlockAPI.getTotalBlocksBroken(player, islandId)
                : OneBlockAPI.getBlocksBroken(player, islandId, blockType);
            return broken >= requiredAmount;
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Double> calculateProgressAsync(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            long broken = blockType == null
                ? OneBlockAPI.getTotalBlocksBroken(player, islandId)
                : OneBlockAPI.getBlocksBroken(player, islandId, blockType);
            return Math.min(1.0, (double) broken / requiredAmount);
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Void> consumeAsync(@NotNull Player player) {
        // Blocks broken are not consumed
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public @NotNull String getDescriptionKey() {
        return blockType == null
            ? "requirement.blocks_broken.any.description"
            : "requirement.blocks_broken.specific.description";
    }
    
    public long getRequiredAmount() {
        return requiredAmount;
    }
    
    @Nullable
    public Material getBlockType() {
        return blockType;
    }
    
    public String getIslandId() {
        return islandId;
    }
}
