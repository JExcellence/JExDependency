package com.raindropcentral.rdt.commands;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section definition for the primary player command {@code /prt}.
 * <p>
 * This class binds the command name to the underlying evaluation environment used
 * by the command framework. Command logic itself is handled by {@link PRT} and
 * delegated to {@link com.raindropcentral.rdt.factory.CommandFactory}.
 */
@SuppressWarnings("unused")
public class PRTSection extends ACommandSection{
    /** Base command name players will use in chat. */
    private static final String COMMAND_NAME = "prt";

    /**
     * Create a new command section for {@code /prt} bound to the provided environment.
     *
     * @param environmentBuilder evaluation environment builder
     */
    public PRTSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
