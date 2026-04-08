package de.jexcellence.oneblock.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for OneBlock visual and audio effects.
 * Controls particles, sounds, and animations based on evolution rarity.
 */
@CSAlways
@SuppressWarnings("ALL")
public class EffectsSection extends AConfigSection {

    // ==================== GENERAL SETTINGS ====================
    private Boolean enabled;
    private Boolean enableParticles;
    private Boolean enableSounds;
    private Boolean enableAnimations;
    private Double globalVolumeMultiplier;
    private Double globalParticleMultiplier;

    // ==================== RARITY EFFECTS ====================
    private Map<String, RarityEffectConfig> rarityEffects;
    
    // ==================== BLOCK BREAK EFFECTS ====================
    private Boolean enableBlockBreakEffects;
    private Integer minRarityForEffects;
    private Boolean enableProgressionPreview;
    
    // ==================== CHEST EFFECTS ====================
    private Boolean enableChestEffects;
    private Integer minRarityForChestEffects;
    
    // ==================== ENTITY SPAWN EFFECTS ====================
    private Boolean enableEntitySpawnEffects;
    private Integer minRarityForEntityEffects;

    public EffectsSection(@NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
        initializeDefaults();
    }

    private void initializeDefaults() {
        // General settings
        this.enabled = true;
        this.enableParticles = true;
        this.enableSounds = true;
        this.enableAnimations = true;
        this.globalVolumeMultiplier = 1.0;
        this.globalParticleMultiplier = 1.0;

        // Block break effects
        this.enableBlockBreakEffects = true;
        this.minRarityForEffects = 3; // RARE and above
        this.enableProgressionPreview = true;

        // Chest effects
        this.enableChestEffects = true;
        this.minRarityForChestEffects = 3; // RARE and above

        // Entity spawn effects
        this.enableEntitySpawnEffects = true;
        this.minRarityForEntityEffects = 3; // RARE and above

        initializeDefaultRarityEffects();
    }

    private void initializeDefaultRarityEffects() {
        this.rarityEffects = new HashMap<>();

        // Configure effects for each rarity
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            RarityEffectConfig config = new RarityEffectConfig();
            config.setEnabled(rarity.getLevel() >= 3); // Enable for RARE and above
            config.setSound(getDefaultSound(rarity));
            config.setSoundPitch(getDefaultPitch(rarity));
            config.setSoundVolume(1.0f);
            config.setParticle(getDefaultParticle(rarity));
            config.setParticleCount(getDefaultParticleCount(rarity));
            config.setParticleSpread(getDefaultParticleSpread(rarity));
            config.setColorCode(getDefaultColorCode(rarity));

            this.rarityEffects.put(rarity.name(), config);
        }
    }

    // ==================== GETTERS ====================

    public @NotNull Boolean getEnabled() {
        return this.enabled != null ? this.enabled : true;
    }

    public @NotNull Boolean getEnableParticles() {
        return this.enableParticles != null ? this.enableParticles : true;
    }

    public @NotNull Boolean getEnableSounds() {
        return this.enableSounds != null ? this.enableSounds : true;
    }

    public @NotNull Boolean getEnableAnimations() {
        return this.enableAnimations != null ? this.enableAnimations : true;
    }

    public @NotNull Double getGlobalVolumeMultiplier() {
        return this.globalVolumeMultiplier != null ? this.globalVolumeMultiplier : 1.0;
    }

    public @NotNull Double getGlobalParticleMultiplier() {
        return this.globalParticleMultiplier != null ? this.globalParticleMultiplier : 1.0;
    }

    public @NotNull Boolean getEnableBlockBreakEffects() {
        return this.enableBlockBreakEffects != null ? this.enableBlockBreakEffects : true;
    }

    public @NotNull Integer getMinRarityForEffects() {
        return this.minRarityForEffects != null ? this.minRarityForEffects : 3;
    }

    public @NotNull Boolean getEnableProgressionPreview() {
        return this.enableProgressionPreview != null ? this.enableProgressionPreview : true;
    }

    public @NotNull Boolean getEnableChestEffects() {
        return this.enableChestEffects != null ? this.enableChestEffects : true;
    }

    public @NotNull Integer getMinRarityForChestEffects() {
        return this.minRarityForChestEffects != null ? this.minRarityForChestEffects : 3;
    }

    public @NotNull Boolean getEnableEntitySpawnEffects() {
        return this.enableEntitySpawnEffects != null ? this.enableEntitySpawnEffects : true;
    }

    public @NotNull Integer getMinRarityForEntityEffects() {
        return this.minRarityForEntityEffects != null ? this.minRarityForEntityEffects : 3;
    }

    public @NotNull Map<String, RarityEffectConfig> getRarityEffects() {
        return this.rarityEffects != null ? this.rarityEffects : new HashMap<>();
    }

    public RarityEffectConfig getRarityEffectConfig(@NotNull EEvolutionRarityType rarity) {
        return getRarityEffects().get(rarity.name());
    }

    // ==================== DEFAULT VALUE HELPERS ====================

    private String getDefaultSound(@NotNull EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> "BLOCK_NOTE_BLOCK_PLING";
            case UNCOMMON -> "BLOCK_NOTE_BLOCK_BELL";
            case RARE -> "BLOCK_NOTE_BLOCK_CHIME";
            case EPIC -> "BLOCK_NOTE_BLOCK_XYLOPHONE";
            case LEGENDARY -> "ENTITY_ENDER_DRAGON_GROWL";
            case SPECIAL -> "ENTITY_WITHER_SPAWN";
            case UNIQUE -> "UI_TOAST_CHALLENGE_COMPLETE";
            case MYTHICAL -> "BLOCK_BEACON_ACTIVATE";
            case DIVINE -> "BLOCK_CONDUIT_ACTIVATE";
            case CELESTIAL -> "ENTITY_LIGHTNING_BOLT_THUNDER";
            case TRANSCENDENT -> "BLOCK_END_PORTAL_SPAWN";
            case ETHEREAL -> "ENTITY_PHANTOM_AMBIENT";
            case COSMIC -> "MUSIC_DISC_OTHERSIDE";
            case INFINITE -> "ENTITY_WARDEN_SONIC_BOOM";
            case OMNIPOTENT -> "ENTITY_WARDEN_HEARTBEAT";
            case RESERVED -> "ENTITY_WARDEN_SONIC_BOOM";
        };
    }

    private float getDefaultPitch(@NotNull EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 1.0f;
            case UNCOMMON -> 1.2f;
            case RARE -> 1.4f;
            case EPIC -> 1.6f;
            case LEGENDARY -> 0.8f;
            case SPECIAL -> 0.6f;
            case UNIQUE -> 2.0f;
            case MYTHICAL -> 0.5f;
            case DIVINE -> 0.4f;
            case CELESTIAL -> 0.3f;
            case TRANSCENDENT -> 0.2f;
            case ETHEREAL -> 1.8f;
            case COSMIC -> 0.1f;
            case INFINITE -> 0.05f;
            case OMNIPOTENT -> 0.01f;
            case RESERVED -> 0.005f;
        };
    }

    private String getDefaultParticle(@NotNull EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> "HAPPY_VILLAGER";
            case UNCOMMON -> "BUBBLE";
            case RARE -> "WITCH";
            case EPIC -> "ENCHANTED_HIT";
            case LEGENDARY -> "DRAGON_BREATH";
            case SPECIAL -> "END_ROD";
            case UNIQUE -> "TOTEM_OF_UNDYING";
            case MYTHICAL -> "SOUL_FIRE_FLAME";
            case DIVINE -> "WHITE_ASH";
            case CELESTIAL -> "ELECTRIC_SPARK";
            case TRANSCENDENT -> "PORTAL";
            case ETHEREAL -> "SCULK_SOUL";
            case COSMIC -> "REVERSE_PORTAL";
            case INFINITE -> "SONIC_BOOM";
            case OMNIPOTENT -> "SHRIEK";
            case RESERVED -> "SHRIEK";
        };
    }

    private int getDefaultParticleCount(@NotNull EEvolutionRarityType rarity) {
        return Math.max(10, rarity.getLevel() * 10);
    }

    private double getDefaultParticleSpread(@NotNull EEvolutionRarityType rarity) {
        return 0.3 + (rarity.getLevel() * 0.1);
    }

    private String getDefaultColorCode(@NotNull EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> "<white>";
            case UNCOMMON -> "<green>";
            case RARE -> "<blue>";
            case EPIC -> "<dark_purple>";
            case LEGENDARY -> "<gold>";
            case SPECIAL -> "<red>";
            case UNIQUE -> "<yellow>";
            case MYTHICAL -> "<light_purple>";
            case DIVINE -> "<aqua>";
            case CELESTIAL -> "<dark_aqua>";
            case TRANSCENDENT -> "<dark_blue>";
            case ETHEREAL -> "<dark_gray>";
            case COSMIC -> "<black>";
            case INFINITE -> "<dark_red>";
            case OMNIPOTENT -> "<obfuscated><dark_red>";
            case RESERVED -> "<obfuscated><black>";
        };
    }

    // ==================== RARITY EFFECT CONFIG CLASS ====================

    public static class RarityEffectConfig {
        private boolean enabled = true;
        private String sound = "BLOCK_NOTE_BLOCK_PLING";
        private float soundPitch = 1.0f;
        private float soundVolume = 1.0f;
        private String particle = "CRIT";
        private int particleCount = 15;
        private double particleSpread = 0.5;
        private String colorCode = "<white>";

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public @NotNull String getSound() { return sound; }
        public void setSound(@NotNull String sound) { this.sound = sound; }

        public float getSoundPitch() { return soundPitch; }
        public void setSoundPitch(float soundPitch) { this.soundPitch = soundPitch; }

        public float getSoundVolume() { return soundVolume; }
        public void setSoundVolume(float soundVolume) { this.soundVolume = soundVolume; }

        public @NotNull String getParticle() { return particle; }
        public void setParticle(@NotNull String particle) { this.particle = particle; }

        public int getParticleCount() { return particleCount; }
        public void setParticleCount(int particleCount) { this.particleCount = particleCount; }

        public double getParticleSpread() { return particleSpread; }
        public void setParticleSpread(double particleSpread) { this.particleSpread = particleSpread; }

        public @NotNull String getColorCode() { return colorCode; }
        public void setColorCode(@NotNull String colorCode) { this.colorCode = colorCode; }
    }
}