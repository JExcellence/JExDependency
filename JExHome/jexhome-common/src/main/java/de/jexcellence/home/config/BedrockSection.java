package de.jexcellence.home.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for Bedrock Edition player support.
 * <p>
 * Controls whether Bedrock forms are used for Bedrock players
 * and allows forcing all players to use chest GUIs.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@CSAlways
public class BedrockSection extends AConfigSection {

    /** Whether Bedrock form support is enabled. */
    private Boolean enabled;

    /** Whether to force all players to use chest GUI (ignores Bedrock detection). */
    private Boolean forceChestGui;

    /**
     * Constructs a new BedrockSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public BedrockSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Checks if Bedrock form support is enabled.
     *
     * @return true if Bedrock forms are enabled (default: true)
     */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    /**
     * Checks if chest GUI should be forced for all players.
     *
     * @return true if chest GUI is forced for all players (default: false)
     */
    public boolean isForceChestGui() {
        return forceChestGui != null && forceChestGui;
    }
}
