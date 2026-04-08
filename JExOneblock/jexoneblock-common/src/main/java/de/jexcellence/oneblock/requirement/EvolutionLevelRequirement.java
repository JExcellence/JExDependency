package de.jexcellence.oneblock.requirement;

import com.raindropcentral.rplatform.requirement.async.AsyncRequirement;
import de.jexcellence.oneblock.api.OneBlockAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Requirement that checks if a player has reached a specific evolution level.
 */
public final class EvolutionLevelRequirement extends AsyncRequirement {
    
    private final int requiredLevel;
    private final String islandId;
    
    public EvolutionLevelRequirement(int requiredLevel, @NotNull String islandId) {
        super("EVOLUTION_LEVEL");
        this.requiredLevel = requiredLevel;
        this.islandId = islandId;
    }
    
    @Override
    public @NotNull CompletableFuture<Boolean> isMetAsync(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            int currentLevel = OneBlockAPI.getEvolutionLevel(player, islandId);
            return currentLevel >= requiredLevel;
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Double> calculateProgressAsync(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            int currentLevel = OneBlockAPI.getEvolutionLevel(player, islandId);
            return Math.min(1.0, (double) currentLevel / requiredLevel);
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Void> consumeAsync(@NotNull Player player) {
        // Evolution levels are not consumed
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public @NotNull String getDescriptionKey() {
        return "requirement.evolution_level.description";
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public String getIslandId() {
        return islandId;
    }
}
