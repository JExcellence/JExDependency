package com.raindropcentral.rdq.config.bounty;

import com.raindropcentral.rdq.type.EBountyClaimMode;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section describing how bounties determine the claimant.
 * <p>
 * The section is backed by {@link AConfigSection} and exposes a single property,
 * {@code claimMode}, which resolves to an {@link EBountyClaimMode}. If the value
 * is absent the section defaults to {@link EBountyClaimMode#LAST_HIT} to match
 * the legacy behaviour where the final attacker always receives the bounty.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class BountySection extends AConfigSection {

    /**
     * Raw configuration value resolving to an {@link EBountyClaimMode}.
     * <p>
     * The configuration must contain the enum constant name, for example
     * {@code MOST_DAMAGE}. A null value indicates that the default value of
     * {@link EBountyClaimMode#LAST_HIT} should be used.
     * </p>
     */
    private String claimMode;

    /**
     * Creates a new bounty configuration section using the provided evaluation environment.
     *
     * @param baseEnvironment the base evaluation environment used while parsing configuration values
     */
    public BountySection(final EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Resolves the configured {@link EBountyClaimMode}.
     * <p>
     * When the configuration omits {@code claimMode} this method falls back to
     * {@link EBountyClaimMode#LAST_HIT}. Otherwise it validates the configured
     * enum name and returns the corresponding value.
     * </p>
     *
     * @return the resolved {@link EBountyClaimMode}, defaulting to {@link EBountyClaimMode#LAST_HIT}
     * @throws IllegalArgumentException if {@code claimMode} does not correspond to a valid enum constant
     */
    public EBountyClaimMode getClaimMode() throws IllegalArgumentException {
        return this.claimMode == null
                ? EBountyClaimMode.LAST_HIT
                : EBountyClaimMode.valueOf(this.claimMode);
    }

}
