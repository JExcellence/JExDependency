package com.raindropcentral.rds.commands;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Represents the p r s configuration section.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public class PRSSection extends ACommandSection{

    private static final String COMMAND_NAME = "prs";

    /**
     * Creates a new p r s section.
     *
     * @param environmentBuilder evaluation environment used for command expressions
     */
    public PRSSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
