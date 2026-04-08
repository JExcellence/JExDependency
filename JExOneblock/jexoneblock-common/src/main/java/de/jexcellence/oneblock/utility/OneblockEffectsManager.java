package de.jexcellence.oneblock.utility;

import de.jexcellence.oneblock.config.OneblockGameplaySection;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class OneblockEffectsManager {

    private static final Logger LOGGER = Logger.getLogger(OneblockEffectsManager.class.getName());

    private final OneblockGameplaySection config;

    public OneblockEffectsManager(@NotNull OneblockGameplaySection config) {
        this.config = config;
    }

    public void playBreakEffects(@NotNull Player player, @NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        playRaritySound(player, location, rarity);
        spawnRarityParticles(player, location, rarity);
        
        if (rarity.getTier() >= EEvolutionRarityType.LEGENDARY.getTier()) {
            playHighRarityEffects(player, location, rarity);
        }
    }

    public void playRaritySound(@NotNull Player player, @NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        var soundName = config.getRaritySound(rarity);
        if (soundName == null) {
            return;
        }

        try {
            var sound = Sound.valueOf(soundName);
            float volume = getRarityVolume(rarity);
            float pitch = getRarityPitch(rarity);
            
            player.playSound(location, sound, volume, pitch);
            
            if (rarity.getTier() >= EEvolutionRarityType.DIVINE.getTier()) {
                location.getWorld().getNearbyPlayers(location, 50.0).forEach(nearbyPlayer -> {
                    if (!nearbyPlayer.equals(player)) {
                        nearbyPlayer.playSound(location, sound, volume * 0.5f, pitch);
                    }
                });
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid sound for rarity " + rarity + ": " + soundName);
        }
    }

    public void spawnRarityParticles(@NotNull Player player, @NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        var particleName = config.getRarityParticle(rarity);
        if (particleName == null) {
            return;
        }

        try {
            var particle = Particle.valueOf(particleName);
            int count = config.getParticleCount(rarity);
            double spread = getRarityParticleSpread(rarity);
            
            var centerLocation = location.clone().add(0.5, 0.5, 0.5);
            
            player.spawnParticle(particle, centerLocation, count, spread, spread, spread, 0.1);
            
            if (rarity.getTier() >= EEvolutionRarityType.MYTHICAL.getTier()) {
                location.getWorld().getNearbyPlayers(location, 30.0).forEach(nearbyPlayer -> {
                    if (!nearbyPlayer.equals(player)) {
                        nearbyPlayer.spawnParticle(particle, centerLocation, count / 2, spread, spread, spread, 0.1);
                    }
                });
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid particle for rarity " + rarity + ": " + particleName);
        }
    }

    private void playHighRarityEffects(@NotNull Player player, @NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        if (rarity == EEvolutionRarityType.OMNIPOTENT) {
            location.getWorld().strikeLightningEffect(location);
        }
        
        if (rarity.getTier() >= EEvolutionRarityType.DIVINE.getTier()) {
            spawnFireworkEffect(location, rarity);
        }
        
        if (rarity.getTier() >= EEvolutionRarityType.CELESTIAL.getTier()) {
            createScreenFlash(player);
        }
    }

    private void spawnFireworkEffect(@NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        var firework = org.bukkit.FireworkEffect.builder();
        
        switch (rarity) {
            case DIVINE -> firework.withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.ORANGE);
            case CELESTIAL -> firework.withColor(org.bukkit.Color.AQUA, org.bukkit.Color.BLUE);
            case TRANSCENDENT -> firework.withColor(org.bukkit.Color.PURPLE, org.bukkit.Color.FUCHSIA);
            case ETHEREAL -> firework.withColor(org.bukkit.Color.WHITE, org.bukkit.Color.SILVER);
            case COSMIC -> firework.withColor(org.bukkit.Color.RED, org.bukkit.Color.MAROON);
            case INFINITE -> firework.withColor(org.bukkit.Color.BLACK, org.bukkit.Color.GRAY);
            case OMNIPOTENT -> firework.withColor(org.bukkit.Color.fromRGB(255, 215, 0));
            default -> firework.withColor(org.bukkit.Color.WHITE);
        }
        
        firework.with(org.bukkit.FireworkEffect.Type.BALL_LARGE);
        firework.withTrail();
        firework.withFlicker();
        
        var fw = location.getWorld().spawn(location.clone().add(0, 1, 0), org.bukkit.entity.Firework.class);
        var meta = fw.getFireworkMeta();
        meta.addEffect(firework.build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    private void createScreenFlash(@NotNull Player player) {
        player.sendTitle("", "", 0, 5, 10);
        
        var playerLoc = player.getLocation().add(0, 1, 0);
        player.spawnParticle(Particle.FIREWORK, playerLoc, 50, 2, 2, 2, 0);
    }

    public void playEvolutionAdvancementEffects(@NotNull Player player, @NotNull String newEvolution) {
        var location = player.getLocation();
        
        player.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, location.add(0, 1, 0), 100, 1, 1, 1, 0.1);
        
        spawnFireworkEffect(location, EEvolutionRarityType.DIVINE);
        
        player.sendTitle(
            "§6§lEVOLUTION ADVANCED!",
            "§e" + newEvolution,
            10, 60, 20
        );
    }

    public void playLevelUpEffects(@NotNull Player player, int newLevel) {
        var location = player.getLocation();
        
        player.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        player.spawnParticle(Particle.HAPPY_VILLAGER, location.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
        
        player.sendActionBar("§a§lLEVEL UP! §7Level " + newLevel);
    }

    public void playPrestigeEffects(@NotNull Player player, int prestigeLevel) {
        var location = player.getLocation();
        
        player.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.5f);
        
        location.getWorld().strikeLightningEffect(location);
        
        player.spawnParticle(Particle.EXPLOSION, location.add(0, 1, 0), 10, 2, 2, 2, 0);
        player.spawnParticle(Particle.FIREWORK, location, 200, 3, 3, 3, 0.2);
        
        player.sendTitle(
            "§5§lPRESTIGE!",
            "§d§lLevel " + prestigeLevel,
            20, 80, 30
        );
        
        org.bukkit.Bukkit.broadcastMessage(
            "§5§l" + player.getName() + " §d§lhas reached Prestige Level " + prestigeLevel + "!"
        );
    }

    /**
     * Gets the volume for a rarity.
     */
    private float getRarityVolume(@NotNull EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 0.3f;
            case UNCOMMON -> 0.4f;
            case RARE -> 0.5f;
            case EPIC -> 0.6f;
            case LEGENDARY -> 0.8f;
            case SPECIAL -> 0.9f;
            case UNIQUE -> 1.0f;
            case MYTHICAL -> 1.2f;
            case DIVINE -> 1.5f;
            case CELESTIAL -> 1.8f;
            default -> 2.0f;
        };
    }

    /**
     * Gets the pitch for a rarity.
     */
    private float getRarityPitch(@NotNull EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 1.0f;
            case UNCOMMON -> 1.1f;
            case RARE -> 1.2f;
            case EPIC -> 1.3f;
            case LEGENDARY -> 1.4f;
            case SPECIAL -> 1.5f;
            case UNIQUE -> 1.6f;
            case MYTHICAL -> 1.7f;
            case DIVINE -> 1.8f;
            case CELESTIAL -> 1.9f;
            default -> 2.0f;
        };
    }

    /**
     * Gets the particle spread for a rarity.
     */
    private double getRarityParticleSpread(@NotNull EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 0.2;
            case UNCOMMON -> 0.3;
            case RARE -> 0.4;
            case EPIC -> 0.5;
            case LEGENDARY -> 0.7;
            case SPECIAL -> 0.9;
            case UNIQUE -> 1.1;
            case MYTHICAL -> 1.3;
            case DIVINE -> 1.5;
            case CELESTIAL -> 1.8;
            default -> 2.0;
        };
    }

    /**
     * Plays progression preview effects for high rarities.
     */
    public void playProgressionPreview(@NotNull Player player, @NotNull Location location, 
                                     @NotNull EEvolutionRarityType currentRarity, 
                                     @NotNull EEvolutionRarityType previewRarity) {
        // Play preview sound
        player.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.5f);
        
        // Spawn preview particles
        player.spawnParticle(Particle.ENCHANT, location.add(0.5, 1, 0.5), 15, 0.5, 0.5, 0.5, 0.1);
        
        // Send preview message
        player.sendActionBar("§7Preview: " + previewRarity.getFormattedName() + " §7rarity incoming!");
    }

    /**
     * Creates a rarity announcement effect.
     */
    public void announceRareBreak(@NotNull Player player, @NotNull EEvolutionRarityType rarity) {
        if (rarity.getTier() < EEvolutionRarityType.LEGENDARY.getTier()) {
            return;
        }
        
        String message = rarity.getFormattedName() + " §7break by §f" + player.getName() + "§7!";
        
        // Broadcast for very high rarities
        if (rarity.getTier() >= EEvolutionRarityType.DIVINE.getTier()) {
            org.bukkit.Bukkit.broadcastMessage("§6§l[RARE BREAK] " + message);
        } else {
            // Send to nearby players
            player.getLocation().getWorld().getNearbyPlayers(player.getLocation(), 100.0)
                .forEach(nearbyPlayer -> nearbyPlayer.sendMessage("§6[Rare Break] " + message));
        }
    }
}