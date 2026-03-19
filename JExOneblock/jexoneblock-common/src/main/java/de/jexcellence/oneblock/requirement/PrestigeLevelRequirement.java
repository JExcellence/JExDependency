package de.jexcellence.oneblock.requirement;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.Requirement;
import de.jexcellence.oneblock.api.OneBlockAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Requirement that checks if a player has reached a specific prestige level.
 * This is a simple requirement that doesn't need database persistence.
 */
public final class PrestigeLevelRequirement extends AbstractRequirement {
    
    private final int requiredLevel;
    
    public PrestigeLevelRequirement(int requiredLevel) {
        super("PRESTIGE_LEVEL");
        this.requiredLevel = requiredLevel;
    }
    
    @Override
    public boolean isMet(@NotNull Player player) {
        return OneBlockAPI.getPrestigeLevel(player) >= requiredLevel;
    }
    
    @Override
    public double calculateProgress(@NotNull Player player) {
        int currentLevel = OneBlockAPI.getPrestigeLevel(player);
        return Math.min(1.0, (double) currentLevel / requiredLevel);
    }
    
    @Override
    public void consume(@NotNull Player player) {
        // Prestige levels are not consumed
    }
    
    @Override
    public @NotNull String getDescriptionKey() {
        return "requirement.prestige_level.description";
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
}
