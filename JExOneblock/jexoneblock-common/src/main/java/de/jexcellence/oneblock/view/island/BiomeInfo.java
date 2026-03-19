package de.jexcellence.oneblock.view.island;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

public record BiomeInfo(
    @NotNull Biome biome,
    @NotNull Material icon,
    @NotNull String displayName,
    @NotNull String description,
    int requiredLevel,
    long requiredCoins
) {
    
    @NotNull
    public Biome getBiome() {
        return biome;
    }
    
    @NotNull
    public String displayName() {
        return displayName;
    }
}
