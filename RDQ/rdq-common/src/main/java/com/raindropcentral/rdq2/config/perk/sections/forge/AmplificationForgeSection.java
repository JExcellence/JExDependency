package com.raindropcentral.rdq2.config.perk.sections.forge;

import com.raindropcentral.rdq2.config.perk.PluginCurrencySection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.Map;

/**
 * Configuration section for the Amplification Forge perk.
 * <p>
 * Defines the parameters used to evaluate potion amplification, including the
 * success probability, the resulting multiplier, the maximum player radius, and
 * the currency costs required to activate the perk.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.1
 * @since 1.0.0
 */
public class AmplificationForgeSection extends AConfigSection {

    /**
     * Chance to amplify a nearby player's potion effect.
     */
    @CSAlways
    private double chance;

    /**
     * Percentage increase applied to the potion effect (e.g., {@code 0.5} for a 50% boost).
     */
    @CSAlways
    private double rate;

    /**
     * Maximum distance from the triggering player where amplification is evaluated, in blocks.
     */
    @CSAlways
    private int distance;

    /**
     * Currency requirements keyed by currency identifier for running the forge amplification.
     */
    @CSAlways
    private Map<String, PluginCurrencySection> cost;

    /**
     * Constructs a new {@code AmplificationForgeSection} with the specified evaluation environment.
     *
     * @param baseEnvironment the base evaluation environment for this configuration section
     */
    public AmplificationForgeSection(final EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Gets the chance to amplify a nearby player's potion.
     *
     * @return the amplification chance as a double
     */
    public double getChance() {
        return this.chance;
    }

    /**
     * Gets the amplification rate for the potion.
     *
     * @return the amplification rate as a double (e.g., {@code 0.5} for a 50% increase)
     */
    public double getRate() {
        return this.rate;
    }

    /**
     * Gets the distance from the player within which amplification can occur.
     *
     * @return the amplification distance in blocks
     */
    public int getDistance() {
        return this.distance;
    }

    /**
     * Gets the currency costs required to trigger the forge amplification perk.
     *
     * @return a map keyed by currency identifier with their corresponding {@link PluginCurrencySection} definitions
     */
    public Map<String, PluginCurrencySection> getCost() {
        return this.cost;
    }
}
