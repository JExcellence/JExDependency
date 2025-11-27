package com.raindropcentral.rdq.config.reward;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Represents the configuration mapping for a reward declared in YAML files.
 *
 * <p>The section exposes the minimal set of fields required for reward parsing:
 * the reward {@linkplain #type type identifier}, the {@linkplain #target target
 * audience or sink}, and the {@linkplain #amount numeric payload}. These values
 * are resolved through the {@link EvaluationEnvironmentBuilder} supplied by the
 * config mapper so expressions can be evaluated consistently across editions.</p>
 *
 * <p>Reward sections are primarily consumed by rank definitions, but the
 * structure is deliberately generic so other systems can reuse the mapping by
 * referencing identical YAML keys.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class RewardSection extends AConfigSection {

    /**
     * The reward type identifier.
     *
     * <p>Common values include {@code currency}, {@code item}, or module-specific
     * reward handlers. YAML key: {@code type}.</p>
     */
    private String type;

    /**
     * The reward target or sink.
     *
     * <p>Represents the player identifier, currency bucket, or other contextual
     * reference the reward system should apply to. YAML key: {@code target}.</p>
     */
    private String target;

    /**
     * The amount associated with the reward.
     *
     * <p>Interpreted according to the {@linkplain #type type}. YAML key:
     * {@code amount}.</p>
     */
    private Long amount;

    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected RewardSection() {
        super(new EvaluationEnvironmentBuilder());
    }

    public RewardSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    public String getType() {
        return this.type == null ? "" : this.type;
    }

    public String getTarget() {
        return this.target == null ? "" : this.target;
    }

    public Long getAmount() {
        return this.amount == null ? -1 : this.amount;
    }
}
