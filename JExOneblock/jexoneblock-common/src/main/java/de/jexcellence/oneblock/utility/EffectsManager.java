package de.jexcellence.oneblock.utility;

import de.jexcellence.oneblock.config.OneblockEffectsConfig;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EffectsManager {

    private final OneblockEffectsConfig config;
    private final OneblockEffectsPlayer effectsPlayer;

    public EffectsManager(@NotNull OneblockEffectsConfig config) {
        this.config = config;
        this.effectsPlayer = new OneblockEffectsPlayer(config.getEffectsSection());
    }

    public void onBlockBreak(@NotNull Location location, @NotNull Player player, @NotNull EEvolutionRarityType rarity) {
        if (!config.areEffectsEnabled()) {
            return;
        }

        effectsPlayer.playBlockBreakEffects(location, player, rarity);
    }

    public void onChestOpen(@NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        if (!config.areEffectsEnabled()) {
            return;
        }

        effectsPlayer.playChestEffects(location, rarity);
    }

    public void onEntitySpawn(@NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        if (!config.areEffectsEnabled()) {
            return;
        }

        effectsPlayer.playEntitySpawnEffects(location, rarity);
    }

    public void showProgressionPreview(@NotNull Location location, @NotNull Player player, 
                                     @NotNull EEvolutionRarityType currentRarity, @NotNull EEvolutionRarityType nextRarity) {
        if (!config.areEffectsEnabled()) {
            return;
        }

        effectsPlayer.playProgressionPreview(location, player, currentRarity, nextRarity);
    }

    public void playSound(@NotNull Player player, @NotNull EEvolutionRarityType rarity) {
        if (!config.areSoundsEnabled()) {
            return;
        }

        effectsPlayer.playSoundForRarity(player, rarity);
    }

    public void spawnParticles(@NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        if (!config.areParticlesEnabled()) {
            return;
        }

        effectsPlayer.spawnParticlesForRarity(location, rarity);
    }

    public @NotNull String getColorForRarity(@NotNull EEvolutionRarityType rarity) {
        return effectsPlayer.getColorForRarity(rarity);
    }

    public boolean areEffectsEnabledForRarity(@NotNull EEvolutionRarityType rarity) {
        return config.areEffectsEnabled() && effectsPlayer.areEffectsEnabledForRarity(rarity);
    }

    public void reloadConfig() {
        config.reloadConfig();
    }

    public @NotNull OneblockEffectsPlayer getEffectsPlayer() {
        return effectsPlayer;
    }

    public @NotNull OneblockEffectsConfig getConfig() {
        return config;
    }
}