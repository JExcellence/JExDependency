package de.jexcellence.oneblock.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for the generator structure system.
 * Manages settings for structure building, detection, and visualization.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@CSAlways
@SuppressWarnings("ALL")
public class GeneratorStructureSection extends AConfigSection {

    // ==================== GENERAL SETTINGS ====================
    private Boolean enabled;
    private Boolean debugMode;
    
    // ==================== BUILD SETTINGS ====================
    private Integer buildAnimationSpeed;
    private Boolean buildSoundEnabled;
    private Boolean buildParticlesEnabled;
    private Double buildParticleDensity;
    private Boolean consumeMaterialsFromInventory;
    private Boolean requireAllMaterialsUpfront;
    
    // ==================== DETECTION SETTINGS ====================
    private Integer detectionRadius;
    private Integer validationCooldown;
    private Boolean autoDetectOnBlockPlace;
    private Boolean showValidationParticles;
    
    // ==================== VISUALIZATION SETTINGS ====================
    private Boolean previewEnabled;
    private Integer previewDuration;
    private Boolean idleParticlesEnabled;
    private Double idleParticleDensity;
    private Integer maxParticlesPerTick;
    
    // ==================== PER-DESIGN SETTINGS ====================
    private Map<String, GeneratorDesignConfig> designs;

    public GeneratorStructureSection(@NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
        initializeDefaults();
    }

    private void initializeDefaults() {
        if (this.designs == null) {
            this.designs = new HashMap<>();
            initializeDefaultDesigns();
        }
    }

    private void initializeDefaultDesigns() {
        for (EGeneratorDesignType type : EGeneratorDesignType.values()) {
            GeneratorDesignConfig config = new GeneratorDesignConfig();
            config.setEnabled(true);
            config.setSpeedMultiplier(type.getDefaultSpeedMultiplier());
            config.setXpMultiplier(type.getDefaultXpMultiplier());
            designs.put(type.getKey(), config);
        }
    }

    // ==================== GENERAL SETTINGS GETTERS ====================

    public boolean isEnabled() {
        return this.enabled != null ? this.enabled : true;
    }

    public boolean isDebugMode() {
        return this.debugMode != null ? this.debugMode : false;
    }

    // ==================== BUILD SETTINGS GETTERS ====================

    public int getBuildAnimationSpeed() {
        return this.buildAnimationSpeed != null ? this.buildAnimationSpeed : 5;
    }

    public boolean isBuildSoundEnabled() {
        return this.buildSoundEnabled != null ? this.buildSoundEnabled : true;
    }

    public boolean isBuildParticlesEnabled() {
        return this.buildParticlesEnabled != null ? this.buildParticlesEnabled : true;
    }

    public double getBuildParticleDensity() {
        return this.buildParticleDensity != null ? this.buildParticleDensity : 1.0;
    }

    public boolean isConsumeMaterialsFromInventory() {
        return this.consumeMaterialsFromInventory != null ? this.consumeMaterialsFromInventory : true;
    }

    public boolean isRequireAllMaterialsUpfront() {
        return this.requireAllMaterialsUpfront != null ? this.requireAllMaterialsUpfront : false;
    }

    // ==================== DETECTION SETTINGS GETTERS ====================

    public int getDetectionRadius() {
        return this.detectionRadius != null ? this.detectionRadius : 32;
    }

    public int getValidationCooldown() {
        return this.validationCooldown != null ? this.validationCooldown : 60;
    }

    public boolean isAutoDetectOnBlockPlace() {
        return this.autoDetectOnBlockPlace != null ? this.autoDetectOnBlockPlace : true;
    }

    public boolean isShowValidationParticles() {
        return this.showValidationParticles != null ? this.showValidationParticles : true;
    }

    // ==================== VISUALIZATION SETTINGS GETTERS ====================

    public boolean isPreviewEnabled() {
        return this.previewEnabled != null ? this.previewEnabled : true;
    }

    public int getPreviewDuration() {
        return this.previewDuration != null ? this.previewDuration : 300;
    }

    public boolean isIdleParticlesEnabled() {
        return this.idleParticlesEnabled != null ? this.idleParticlesEnabled : true;
    }

    public double getIdleParticleDensity() {
        return this.idleParticleDensity != null ? this.idleParticleDensity : 1.0;
    }

    public int getMaxParticlesPerTick() {
        return this.maxParticlesPerTick != null ? this.maxParticlesPerTick : 50;
    }

    // ==================== PER-DESIGN SETTINGS GETTERS ====================

    @NotNull
    public Map<String, GeneratorDesignConfig> getDesigns() {
        return this.designs != null ? this.designs : new HashMap<>();
    }

    @Nullable
    public GeneratorDesignConfig getDesignConfig(@NotNull EGeneratorDesignType type) {
        return this.designs != null ? this.designs.get(type.getKey()) : null;
    }

    @Nullable
    public GeneratorDesignConfig getDesignConfig(@NotNull String key) {
        return this.designs != null ? this.designs.get(key) : null;
    }

    public boolean isDesignEnabled(@NotNull EGeneratorDesignType type) {
        GeneratorDesignConfig config = getDesignConfig(type);
        return config != null && config.isEnabled();
    }

    /**
     * Configuration for a specific generator design.
     */
    public static class GeneratorDesignConfig {
        private boolean enabled = true;
        private double speedMultiplier = 1.0;
        private double xpMultiplier = 1.0;
        private double fortuneMultiplier = 1.0;
        private String particleEffect;
        private Map<String, Double> customBonuses = new HashMap<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public double getSpeedMultiplier() { return speedMultiplier; }
        public void setSpeedMultiplier(double speedMultiplier) { this.speedMultiplier = speedMultiplier; }

        public double getXpMultiplier() { return xpMultiplier; }
        public void setXpMultiplier(double xpMultiplier) { this.xpMultiplier = xpMultiplier; }

        public double getFortuneMultiplier() { return fortuneMultiplier; }
        public void setFortuneMultiplier(double fortuneMultiplier) { this.fortuneMultiplier = fortuneMultiplier; }

        @Nullable
        public String getParticleEffect() { return particleEffect; }
        public void setParticleEffect(String particleEffect) { this.particleEffect = particleEffect; }

        @NotNull
        public Map<String, Double> getCustomBonuses() { return customBonuses; }
        public void setCustomBonuses(Map<String, Double> customBonuses) { this.customBonuses = customBonuses; }
    }
}
