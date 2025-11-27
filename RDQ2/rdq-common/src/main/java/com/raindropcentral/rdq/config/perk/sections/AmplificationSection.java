package com.raindropcentral.rdq.config.perk.sections;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.sections.forge.AmplificationForgeSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section that encapsulates the amplification perk parameters.
 * <p>
 * The section describes how potion amplification behaves by capturing the trigger chance,
 * the amplification multiplier, the optional list of potion identifiers that should be
 * considered eligible, and any advanced forge overrides supplied by pack developers.
 * Each accessor guarantees a non-{@code null} value so calling code can rely on safe
 * defaults when configuration entries are omitted.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public class AmplificationSection extends PerkSection {

    /**
     * Chance for the amplification perk to trigger when a player brews or consumes a potion.
     */
    @CSAlways
    private double chance;

    /**
     * Multiplier applied to the base potion amplification (for example, {@code 0.5} equals a 50% boost).
     */
    @CSAlways
    private double rate;

    /**
     * Whitelist of potion identifiers that are eligible for amplification.
     * <p>
     * Entries are stored as MiniMessage-compatible potion names. When left undefined the
     * whitelist is considered unrestricted and callers receive an empty list to indicate
     * that all potion types are allowed.
     * </p>
     */
    @CSAlways
    private List<String> potions;

    /**
     * Advanced configuration overrides supplied through the perk forge system.
     */
    @CSAlways
    private AmplificationForgeSection amplificationForgeSection;

    /**
     * Constructs a new {@code AmplificationSection} backed by the provided evaluation environment.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment used for expression parsing
     */
    public AmplificationSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Retrieves the probability that the amplification perk will activate.
     *
     * @return the amplification trigger chance
     */
    public double getChance() {
        return this.chance;
    }

    /**
     * Provides the multiplier that should be applied to the amplified potion.
     *
     * @return the configured amplification rate
     */
    public double getRate() {
        return this.rate;
    }

    /**
     * Supplies the whitelist of potion identifiers that can be amplified.
     * <p>
     * When the configuration omits this node the method returns an empty list, signalling
     * to consumers that all potion types may be considered for amplification.
     * </p>
     *
     * @return a mutable list of potion identifiers that should be treated as eligible
     */
    public List<String> getPotions() {
        if (this.potions == null) {
            this.potions = new ArrayList<>();
        }
        return this.potions;
    }

    /**
     * Exposes the advanced forge configuration for the amplification perk.
     * <p>
     * Returning a new {@link AmplificationForgeSection} when the node is absent ensures callers
     * can safely read forge-specific overrides without performing null checks.
     * </p>
     *
     * @return a non-{@code null} {@link AmplificationForgeSection}
     */
    public AmplificationForgeSection getForgeAmplificationSection() {
        return this.amplificationForgeSection == null
                ? new AmplificationForgeSection(new EvaluationEnvironmentBuilder())
                : this.amplificationForgeSection;
    }
}