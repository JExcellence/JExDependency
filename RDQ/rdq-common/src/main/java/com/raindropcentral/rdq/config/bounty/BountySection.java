package com.raindropcentral.rdq.config.bounty;

import com.raindropcentral.rdq.type.EBountyClaimMode;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for bounty-related settings.
 * <p>
 * This section allows configuration of how bounty claims are handled,
 * such as whether the bounty is awarded to the player with the most damage
 * or the player who lands the last hit.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class BountySection extends AConfigSection {
	
	/**
	 * The claim mode for the bounty.
	 * <p>
	 * This should be the name of an {@link EBountyClaimMode} enum constant,
	 * such as "MOST_DAMAGE" or "LAST_HIT". If null, defaults to {@link EBountyClaimMode#LAST_HIT}.
	 * </p>
	 */
	private String claimMode;
	
	/**
	 * Constructs a new {@code BountySection} with the specified base evaluation environment.
	 *
	 * @param baseEnvironment the base evaluation environment used for configuration evaluation
	 */
	public BountySection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	/**
	 * Gets the configured bounty claim mode.
	 * <p>
	 * If {@code claimMode} is null, returns {@link EBountyClaimMode#LAST_HIT} as the default.
	 * Otherwise, attempts to parse the string value to an {@link EBountyClaimMode} enum constant.
	 * </p>
	 *
	 * @return the configured {@link EBountyClaimMode}, or {@link EBountyClaimMode#LAST_HIT} if not set
	 * @throws IllegalArgumentException if the {@code claimMode} string does not match any enum constant
	 */
	public EBountyClaimMode getClaimMode() throws IllegalArgumentException {
		return
			this.claimMode == null ?
				EBountyClaimMode.LAST_HIT :
				EBountyClaimMode.valueOf(this.claimMode)
			;
	}
	
}