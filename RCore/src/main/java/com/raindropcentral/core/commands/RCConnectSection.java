package com.raindropcentral.core.commands;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section configuration for /rcconnect command.
 * Loads configuration from resources/commands/rcconnect.yml
 */
public final class RCConnectSection extends ACommandSection {

    private static final String COMMAND_NAME = "rcconnect";

    /**
     * Executes RCConnectSection.
     */
    public RCConnectSection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(COMMAND_NAME, evaluationEnvironmentBuilder);
    }
}
