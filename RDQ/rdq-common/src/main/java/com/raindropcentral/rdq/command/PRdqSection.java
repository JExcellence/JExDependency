package com.raindropcentral.rdq.command;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section for the /prdq command.
 * Defines the command name and evaluation environment.
 */
public final class PRdqSection extends ACommandSection {

    private static final String COMMAND_NAME = "prdq";

    public PRdqSection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(COMMAND_NAME, evaluationEnvironmentBuilder);
    }
}
