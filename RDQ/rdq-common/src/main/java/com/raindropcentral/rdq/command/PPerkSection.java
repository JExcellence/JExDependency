package com.raindropcentral.rdq.command;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section for the /pperk command.
 * Defines the command name and evaluation environment.
 */
public final class PPerkSection extends ACommandSection {

    private static final String COMMAND_NAME = "pperk";

    public PPerkSection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(COMMAND_NAME, evaluationEnvironmentBuilder);
    }
}
