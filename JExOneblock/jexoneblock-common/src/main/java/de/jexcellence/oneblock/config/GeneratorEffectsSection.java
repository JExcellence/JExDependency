package de.jexcellence.oneblock.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for generator particle effects.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@CSAlways
@SuppressWarnings("ALL")
public class GeneratorEffectsSection extends AConfigSection {

    // ==================== GLOBAL SETTINGS ====================
    private Boolean effectsEnabled;
    private Double globalDensityMultiplier;
    private Integer maxParticlesPerTick;
    private Boolean respectPlayerSettings;
    
    // ==================== BUILD EFFECTS ====================
    private Boolean buildEffectsEnabled;
    private String buildParticle;
    private Integer buildParticleCount;
    private Double buildParticleSpread;
    private Boolean buildTrailEnabled;
    private Double buildTrailDensity;
    
    // ==================== VALIDATION EFFECTS ====================
    private Boolean validationEffectsEnabled;
    private String validParticle;
    private String invalidParticle;
    private String missingParticle;
    private Boolean outlineEnabled;
    
    // ==================== IDLE EFFECTS ====================
    private Boolean idleEffectsEnabled;
    private Integer idleTickInterval;
    private Integer burstInterval;
    
    // ==================== PER-DESIGN EFFECTS ====================
    private Map<String, DesignEffectConfig> designEffects;

    public GeneratorEffectsSection(@NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
        initializeDefaults();
    }

    private void initializeDefaults() {
        if (this.designEffects == null) {
            this.designEffects = new HashMap<>();
            initializeDefaultEffects();
        }
    }

    private void initializeDefaultEffects() {
        // Foundry - Fire/smoke theme
        designEffects.put("foundry", new DesignEffectConfig("FLAME", 5, 0.2, "SMOKE", 3));
        
        // Aquatic - Water theme
        designEffects.put("aquatic", new DesignEffectConfig("DRIPPING_WATER", 8, 0.3, "WATER_SPLASH", 5));
        
        // Volcanic - Lava/fire theme
        designEffects.put("volcanic", new DesignEffectConfig("LAVA", 4, 0.2, "FLAME", 6));
        
        // Crystal - Sparkle theme
        designEffects.put("crystal", new DesignEffectConfig("END_ROD", 6, 0.25, "PORTAL", 4));
        
        // Mechanical - Crit/spark theme
        designEffects.put("mechanical", new DesignEffectConfig("CRIT", 7, 0.2, "ENCHANTED_HIT", 5));
        
        // Nature - Green/growth theme
        designEffects.put("nature", new DesignEffectConfig("COMPOSTER", 5, 0.3, "HAPPY_VILLAGER", 4));
        
        // Nether - Soul fire theme
        designEffects.put("nether", new DesignEffectConfig("SOUL_FIRE_FLAME", 5, 0.2, "SOUL", 3));
        
        // End - Portal theme
        designEffects.put("end", new DesignEffectConfig("PORTAL", 10, 0.4, "REVERSE_PORTAL", 6));
        
        // Ancient - Sculk theme
        designEffects.put("ancient", new DesignEffectConfig("SCULK_CHARGE_POP", 4, 0.2, "SCULK_SOUL", 3));
        
        // Celestial - Star/beacon theme
        designEffects.put("celestial", new DesignEffectConfig("END_ROD", 8, 0.3, "FIREWORK", 10));
    }

    // ==================== GLOBAL SETTINGS GETTERS ====================

    public boolean isEffectsEnabled() {
        return this.effectsEnabled != null ? this.effectsEnabled : true;
    }

    public double getGlobalDensityMultiplier() {
        return this.globalDensityMultiplier != null ? this.globalDensityMultiplier : 1.0;
    }

    public int getMaxParticlesPerTick() {
        return this.maxParticlesPerTick != null ? this.maxParticlesPerTick : 50;
    }

    public boolean isRespectPlayerSettings() {
        return this.respectPlayerSettings != null ? this.respectPlayerSettings : true;
    }

    // ==================== BUILD EFFECTS GETTERS ====================

    public boolean isBuildEffectsEnabled() {
        return this.buildEffectsEnabled != null ? this.buildEffectsEnabled : true;
    }

    @NotNull
    public String getBuildParticle() {
        return this.buildParticle != null ? this.buildParticle : "CLOUD";
    }

    public int getBuildParticleCount() {
        return this.buildParticleCount != null ? this.buildParticleCount : 5;
    }

    public double getBuildParticleSpread() {
        return this.buildParticleSpread != null ? this.buildParticleSpread : 0.2;
    }

    public boolean isBuildTrailEnabled() {
        return this.buildTrailEnabled != null ? this.buildTrailEnabled : true;
    }

    public double getBuildTrailDensity() {
        return this.buildTrailDensity != null ? this.buildTrailDensity : 2.0;
    }

    // ==================== VALIDATION EFFECTS GETTERS ====================

    public boolean isValidationEffectsEnabled() {
        return this.validationEffectsEnabled != null ? this.validationEffectsEnabled : true;
    }

    @NotNull
    public String getValidParticle() {
        return this.validParticle != null ? this.validParticle : "HAPPY_VILLAGER";
    }

    @NotNull
    public String getInvalidParticle() {
        return this.invalidParticle != null ? this.invalidParticle : "SMOKE";
    }

    @NotNull
    public String getMissingParticle() {
        return this.missingParticle != null ? this.missingParticle : "DUST";
    }

    public boolean isOutlineEnabled() {
        return this.outlineEnabled != null ? this.outlineEnabled : true;
    }

    // ==================== IDLE EFFECTS GETTERS ====================

    public boolean isIdleEffectsEnabled() {
        return this.idleEffectsEnabled != null ? this.idleEffectsEnabled : true;
    }

    public int getIdleTickInterval() {
        return this.idleTickInterval != null ? this.idleTickInterval : 5;
    }

    public int getBurstInterval() {
        return this.burstInterval != null ? this.burstInterval : 80;
    }

    // ==================== PER-DESIGN EFFECTS GETTERS ====================

    @NotNull
    public Map<String, DesignEffectConfig> getDesignEffects() {
        return this.designEffects != null ? this.designEffects : new HashMap<>();
    }

    @Nullable
    public DesignEffectConfig getDesignEffect(@NotNull EGeneratorDesignType type) {
        return this.designEffects != null ? this.designEffects.get(type.getKey()) : null;
    }

    @Nullable
    public DesignEffectConfig getDesignEffect(@NotNull String key) {
        return this.designEffects != null ? this.designEffects.get(key) : null;
    }

    /**
     * Configuration for design-specific particle effects.
     */
    public static class DesignEffectConfig {
        private String idleParticle;
        private int idleParticleCount;
        private double idleParticleSpread;
        private String burstParticle;
        private int burstParticleCount;
        private boolean enabled = true;

        public DesignEffectConfig() {}

        public DesignEffectConfig(String idleParticle, int idleParticleCount, double idleParticleSpread,
                                   String burstParticle, int burstParticleCount) {
            this.idleParticle = idleParticle;
            this.idleParticleCount = idleParticleCount;
            this.idleParticleSpread = idleParticleSpread;
            this.burstParticle = burstParticle;
            this.burstParticleCount = burstParticleCount;
        }

        @NotNull
        public String getIdleParticle() { return idleParticle != null ? idleParticle : "FLAME"; }
        public void setIdleParticle(String idleParticle) { this.idleParticle = idleParticle; }

        public int getIdleParticleCount() { return idleParticleCount > 0 ? idleParticleCount : 5; }
        public void setIdleParticleCount(int idleParticleCount) { this.idleParticleCount = idleParticleCount; }

        public double getIdleParticleSpread() { return idleParticleSpread > 0 ? idleParticleSpread : 0.2; }
        public void setIdleParticleSpread(double idleParticleSpread) { this.idleParticleSpread = idleParticleSpread; }

        @NotNull
        public String getBurstParticle() { return burstParticle != null ? burstParticle : "FIREWORK"; }
        public void setBurstParticle(String burstParticle) { this.burstParticle = burstParticle; }

        public int getBurstParticleCount() { return burstParticleCount > 0 ? burstParticleCount : 10; }
        public void setBurstParticleCount(int burstParticleCount) { this.burstParticleCount = burstParticleCount; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        @Nullable
        public Particle getIdleParticleType() {
            try {
                return Particle.valueOf(getIdleParticle());
            } catch (IllegalArgumentException e) {
                return Particle.FLAME;
            }
        }

        @Nullable
        public Particle getBurstParticleType() {
            try {
                return Particle.valueOf(getBurstParticle());
            } catch (IllegalArgumentException e) {
                return Particle.FIREWORK;
            }
        }
    }
}
