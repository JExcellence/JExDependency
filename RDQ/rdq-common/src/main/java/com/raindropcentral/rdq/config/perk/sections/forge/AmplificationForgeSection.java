package com.raindropcentral.rdq.config.perk.sections.forge;

import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.Map;

/**
 * Configuration section for the Amplification Forge perk.
 * <p>
 * This section defines the parameters for amplifying nearby player potions,
 * including the chance, amplification rate, distance, and associated cost.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class AmplificationForgeSection extends AConfigSection {
	
	/**
	 * Chance to amplify nearby player potion.
	 */
	@CSAlways
	private double chance;
	
	/**
	 * Rate to amplify potion, e.g., 0.5 = 50% increase.
	 */
	@CSAlways
	private double rate;
	
	/**
	 * Distance from player to amplify.
	 */
	@CSAlways
	private int distance;
	
	/**
	 * Cost associated with the amplification, mapped by currency type.
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
	 * @return the amplification rate as a double (e.g., 0.5 for 50% increase)
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
	 * Map containing cost associated with forging perk
	 *
	 * @return Map of costs
	 */
	public Map<String, PluginCurrencySection> getCost() {
		return this.cost;
	}
}