package com.raindropcentral.core.commands;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section configuration for /rcdisconnect command.
 * Loads configuration from resources/commands/rcdisconnect.yml
 */
public final class RCDisconnectSection extends ACommandSection {

    private static final String COMMAND_NAME = "rcdisconnect";

    /**
     * Executes RCDisconnectSection.
     */
    public RCDisconnectSection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(COMMAND_NAME, evaluationEnvironmentBuilder);
    }
}
