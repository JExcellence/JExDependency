package com.raindropcentral.rdq.config.perk.sections;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.sections.forge.AmplificationForgeSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for the Amplification perk.
 * <p>
 * This section defines the parameters for amplifying potions, including the chance,
 * amplification rate, a whitelist of applicable potions, and an optional forge section
 * for advanced configuration.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class AmplificationSection extends PerkSection {
	
	/**
	 * Chance to amplify potion (trigger perk).
	 */
	@CSAlways
	private double chance;

	/**
	 * Rate to amplify potion, e.g., 0.5 = 50% increase.
	 */
	@CSAlways
	private double rate;
	
	/**
	 * Whitelist of potions the perk triggers on.
	 * <p>
	 * Uses potion type names as strings. If not set, defaults to all available potion types.
	 * </p>
	 */
	//TODO JExcellence to review if this should be improved. Currently uses PotionType.
	@CSAlways
	private List<String> potions;
	
	/**
	 * Forge section for advanced amplification configuration.
	 */
	@CSAlways
	private AmplificationForgeSection amplificationForgeSection;
	
	/**
	 * Constructs a new {@code AmplificationSection} with the specified evaluation environment.
	 *
	 * @param evaluationEnvironmentBuilder the base evaluation environment for this configuration section
	 */
	public AmplificationSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Gets the chance to amplify a potion (trigger the perk).
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
	 * Gets the whitelist of potion types the perk triggers on.
	 * <p>
	 * If not set, returns a list of all default potion types.
	 * </p>
	 *
	 * @return a list of potion type names as strings
	 */
	public List<String> getPotions() {
		if (this.potions == null) {
			this.potions = new ArrayList<>(getPotions());
		}
		return this.potions;
	}
	
	/**
	 * Gets the forge section for advanced amplification configuration.
	 * <p>
	 * If not set, returns a new default {@link AmplificationForgeSection}.
	 * </p>
	 *
	 * @return the {@link AmplificationForgeSection} instance
	 */
	public AmplificationForgeSection getForgeAmplificationSection() {
		return
			this.amplificationForgeSection == null ?
				new AmplificationForgeSection(new EvaluationEnvironmentBuilder()) :
				this.amplificationForgeSection;
	}
}