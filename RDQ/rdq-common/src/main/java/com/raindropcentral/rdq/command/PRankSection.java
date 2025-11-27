package com.raindropcentral.rdq.command;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section for the /prank command.
 * Defines the command name and evaluation environment.
 */
public final class PRankSection extends ACommandSection {

    private static final String COMMAND_NAME = "prank";

    public PRankSection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(COMMAND_NAME, evaluationEnvironmentBuilder);
    }
}
