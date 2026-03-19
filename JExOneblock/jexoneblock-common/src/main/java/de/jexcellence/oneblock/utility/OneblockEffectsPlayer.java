package de.jexcellence.oneblock.utility;

import de.jexcellence.oneblock.config.EffectsSection;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class OneblockEffectsPlayer {

    private final EffectsSection config;
    private final MiniMessage miniMessage;

    public OneblockEffectsPlayer(@NotNull EffectsSection config) {
        this.config = config;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void playSoundForRarity(@NotNull Player player, @NotNull EEvolutionRarityType rarity) {
        if (!config.getEnabled() || !config.getEnableSounds()) {
            return;
        }

        var effectConfig = config.getRarityEffectConfig(rarity);
        if (effectConfig == null || !effectConfig.isEnabled()) {
            return;
        }

        try {
            var sound = Sound.valueOf(effectConfig.getSound());
            float volume = (float) (effectConfig.getSoundVolume() * config.getGlobalVolumeMultiplier());
            float pitch = effectConfig.getSoundPitch();
            
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
        }
    }

    public void spawnParticlesForRarity(@NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        if (!config.getEnabled() || !config.getEnableParticles() || location.getWorld() == null) {
            return;
        }

        var effectConfig = config.getRarityEffectConfig(rarity);
        if (effectConfig == null || !effectConfig.isEnabled()) {
            return;
        }

        try {
            var particle = Particle.valueOf(effectConfig.getParticle());
            int count = (int) (effectConfig.getParticleCount() * config.getGlobalParticleMultiplier());
            double spread = effectConfig.getParticleSpread();
            
            location.getWorld().spawnParticle(particle, location, count, spread, spread, spread, 0.1);
        } catch (IllegalArgumentException e) {
        }
    }

    public void playBlockBreakEffects(@NotNull Location location, @NotNull Player player, @NotNull EEvolutionRarityType rarity) {
        if (!config.getEnabled() || !config.getEnableBlockBreakEffects()) {
            return;
        }

        if (rarity.getLevel() < config.getMinRarityForEffects()) {
            return;
        }

        playSoundForRarity(player, rarity);
        spawnParticlesForRarity(location, rarity);

        if (rarity.getLevel() >= 8) {
            playEnhancedBreakEffects(location, rarity);
        }
    }

    public void playChestEffects(@NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        if (!config.getEnabled() || !config.getEnableChestEffects() || location.getWorld() == null) {
            return;
        }

        if (rarity.getLevel() < config.getMinRarityForChestEffects()) {
            return;
        }

        location.getWorld().spawnParticle(Particle.CLOUD, location, 50, 0.5, 0.5, 0.5, 0.1);
        location.getWorld().playSound(location, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);

        if (rarity.getLevel() >= 5) {
            location.getWorld().spawnParticle(Particle.ENCHANTED_HIT, location, 30, 0.3, 0.3, 0.3, 0.05);
        }

        if (rarity.getLevel() >= 8) {
            spawnParticlesForRarity(location, rarity);
            
            try {
                var effectConfig = config.getRarityEffectConfig(rarity);
                if (effectConfig != null && effectConfig.isEnabled()) {
                    var raritySound = Sound.valueOf(effectConfig.getSound());
                    float volume = (float) (effectConfig.getSoundVolume() * config.getGlobalVolumeMultiplier() * 0.8);
                    location.getWorld().playSound(location, raritySound, volume, effectConfig.getSoundPitch());
                }
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public void playEntitySpawnEffects(@NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        if (!config.getEnabled() || !config.getEnableEntitySpawnEffects() || location.getWorld() == null) {
            return;
        }

        if (rarity.getLevel() < config.getMinRarityForEntityEffects()) {
            return;
        }

        location.getWorld().spawnParticle(Particle.CLOUD, location, 50, 0.5, 0.5, 0.5, 0.1);
        location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        if (rarity.getLevel() >= 5) {
            location.getWorld().spawnParticle(Particle.DRAGON_BREATH, location, 20, 0.2, 0.2, 0.2, 0.02);
        }

        if (rarity.getLevel() >= 8) {
            spawnParticlesForRarity(location, rarity);
            
            try {
                var effectConfig = config.getRarityEffectConfig(rarity);
                if (effectConfig != null && effectConfig.isEnabled()) {
                    var raritySound = Sound.valueOf(effectConfig.getSound());
                    float volume = (float) (effectConfig.getSoundVolume() * config.getGlobalVolumeMultiplier() * 0.6);
                    location.getWorld().playSound(location, raritySound, volume, effectConfig.getSoundPitch());
                }
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public void playProgressionPreview(@NotNull Location location, @NotNull Player player, 
                                     @NotNull EEvolutionRarityType currentRarity, @NotNull EEvolutionRarityType nextRarity) {
        if (!config.getEnabled() || !config.getEnableProgressionPreview() || location.getWorld() == null) {
            return;
        }

        if (nextRarity.getLevel() <= currentRarity.getLevel() + 1) {
            return;
        }

        var nextConfig = config.getRarityEffectConfig(nextRarity);
        if (nextConfig == null || !nextConfig.isEnabled()) {
            return;
        }

        try {
            var previewParticle = Particle.valueOf(nextConfig.getParticle());
            int previewCount = Math.min(5, (int) (nextConfig.getParticleCount() * config.getGlobalParticleMultiplier() / 10));
            
            location.getWorld().spawnParticle(previewParticle, location.clone().add(0, 2, 0), 
                                            previewCount, 0.2, 0.2, 0.2, 0.01);
        } catch (IllegalArgumentException e) {
        }

        try {
            var previewSound = Sound.valueOf(nextConfig.getSound());
            float volume = (float) (nextConfig.getSoundVolume() * config.getGlobalVolumeMultiplier() * 0.3);
            player.playSound(player.getLocation(), previewSound, volume, nextConfig.getSoundPitch());
        } catch (IllegalArgumentException e) {
        }

        var colorCode = nextConfig.getColorCode();
        var previewMessage = miniMessage.deserialize(
            "<gray><italic>[Preview]</italic></gray> " + colorCode + nextRarity.getDisplayName() + " <gray>rarity approaching...</gray>"
        );
        player.sendMessage(previewMessage);
    }

    public @NotNull String getColorForRarity(@NotNull EEvolutionRarityType rarity) {
        var effectConfig = config.getRarityEffectConfig(rarity);
        return effectConfig != null ? effectConfig.getColorCode() : "<gray>";
    }

    public boolean areEffectsEnabledForRarity(@NotNull EEvolutionRarityType rarity) {
        if (!config.getEnabled()) {
            return false;
        }

        var effectConfig = config.getRarityEffectConfig(rarity);
        return effectConfig != null && effectConfig.isEnabled();
    }

    private void playEnhancedBreakEffects(@NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        if (location.getWorld() == null) {
            return;
        }

        if (rarity.getLevel() >= 12) {
            createSpiralEffect(location, rarity);
        }

        var groundLocation = location.clone().subtract(0, 1, 0);
        location.getWorld().spawnParticle(Particle.BLOCK, groundLocation, 20, 0.5, 0.1, 0.5, 0.1);
    }

    private void createSpiralEffect(@NotNull Location center, @NotNull EEvolutionRarityType rarity) {
        if (center.getWorld() == null) {
            return;
        }

        var effectConfig = config.getRarityEffectConfig(rarity);
        if (effectConfig == null) {
            return;
        }

        try {
            var particle = Particle.valueOf(effectConfig.getParticle());
            
            for (int i = 0; i < 20; i++) {
                double angle = i * 0.3;
                double radius = 0.5 + (i * 0.05);
                double height = i * 0.1;
                
                double x = center.getX() + radius * Math.cos(angle);
                double y = center.getY() + height;
                double z = center.getZ() + radius * Math.sin(angle);
                
                var spiralLocation = new Location(center.getWorld(), x, y, z);
                center.getWorld().spawnParticle(particle, spiralLocation, 1, 0, 0, 0, 0);
            }
        } catch (IllegalArgumentException e) {
        }
    }
}