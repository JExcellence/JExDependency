package de.jexcellence.oneblock.config;

import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.oneblock.JExOneblock;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration loader for OneBlock effects system.
 * Handles loading and reloading of effects configuration from YAML files.
 */
public final class OneblockEffectsConfig {

    private static final String FOLDER_NAME = "config";
    private static final String FILE_NAME = "effects.yml";

    private final JExOneblock plugin;
    private EffectsSection effectsSection;

    public OneblockEffectsConfig(@NotNull JExOneblock plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Loads the effects configuration from file.
     */
    public void loadConfig() {
        try {
            final ConfigManager cfgManager = new ConfigManager(plugin.getPlugin(), FOLDER_NAME);
            final ConfigKeeper<EffectsSection> cfgKeeper = new ConfigKeeper<>(
                cfgManager,
                FILE_NAME,
                EffectsSection.class
            );
            this.effectsSection = cfgKeeper.rootSection;

            Logger.getLogger(OneblockEffectsConfig.class.getName())
                .info("OneBlock effects configuration loaded successfully");

        } catch (Exception e) {
            this.effectsSection = new EffectsSection(new EvaluationEnvironmentBuilder());
            Logger.getLogger(OneblockEffectsConfig.class.getName())
                .log(Level.WARNING, "Failed to load OneBlock effects config, using defaults", e);
        }
    }

    /**
     * Reloads the effects configuration from file.
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * Gets the effects configuration section.
     * 
     * @return the effects section
     */
    public @NotNull EffectsSection getEffectsSection() {
        return this.effectsSection != null ? this.effectsSection : 
               new EffectsSection(new EvaluationEnvironmentBuilder());
    }

    /**
     * Checks if effects are globally enabled.
     * 
     * @return true if effects are enabled
     */
    public boolean areEffectsEnabled() {
        return getEffectsSection().getEnabled();
    }

    /**
     * Checks if particles are enabled.
     * 
     * @return true if particles are enabled
     */
    public boolean areParticlesEnabled() {
        return getEffectsSection().getEnableParticles();
    }

    /**
     * Checks if sounds are enabled.
     * 
     * @return true if sounds are enabled
     */
    public boolean areSoundsEnabled() {
        return getEffectsSection().getEnableSounds();
    }

    /**
     * Gets the global volume multiplier.
     * 
     * @return volume multiplier
     */
    public double getGlobalVolumeMultiplier() {
        return getEffectsSection().getGlobalVolumeMultiplier();
    }

    /**
     * Gets the global particle multiplier.
     * 
     * @return particle multiplier
     */
    public double getGlobalParticleMultiplier() {
        return getEffectsSection().getGlobalParticleMultiplier();
    }
}