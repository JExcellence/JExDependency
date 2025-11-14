package com.raindropcentral.rdq.config.bounty;

import com.raindropcentral.rdq.type.EBountyClaimMode;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for bounty system settings.
 * <p>
 * This section manages bounty-related configuration including claim modes,
 * creation costs, and bounty limits. The {@code claimMode} determines how
 * bounty rewards are distributed (e.g., last hit vs. most damage). Creation
 * costs can be configured to require payment when placing bounties.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
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
     * Cost in the primary currency required to create a bounty.
     * <p>
     * When set to zero or negative, bounty creation is free. Otherwise,
     * players must have at least this amount to place a bounty.
     * </p>
     */
    private Double creationCost;

    /**
     * Maximum number of active bounties a single player can create.
     * <p>
     * Defaults to 1 if not specified. Set to -1 for unlimited bounties.
     * </p>
     */
    private Integer maxBountiesPerPlayer;

    /**
     * Whether to broadcast bounty creation to all online players.
     */
    private Boolean broadcastCreation;

    /**
     * Whether to broadcast bounty completion to all online players.
     */
    private Boolean broadcastCompletion;

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

    /**
     * Gets the cost required to create a bounty.
     *
     * @return the creation cost, or 0.0 if not configured
     */
    public double getCreationCost() {
        return this.creationCost != null ? this.creationCost : 0.0;
    }

    /**
     * Gets the maximum number of active bounties per player.
     *
     * @return the bounty limit, or 1 if not configured
     */
    public int getMaxBountiesPerPlayer() {
        return this.maxBountiesPerPlayer != null ? this.maxBountiesPerPlayer : 1;
    }

    /**
     * Checks if bounty creation should be broadcast.
     *
     * @return true if broadcasts are enabled, false otherwise
     */
    public boolean shouldBroadcastCreation() {
        return this.broadcastCreation != null && this.broadcastCreation;
    }

    /**
     * Checks if bounty completion should be broadcast.
     *
     * @return true if broadcasts are enabled, false otherwise
     */
    public boolean shouldBroadcastCompletion() {
        return this.broadcastCompletion != null && this.broadcastCompletion;
    }
}
