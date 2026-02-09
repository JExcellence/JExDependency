package com.raindropcentral.rds.commands;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

@SuppressWarnings("unused")
public class PRSSection extends ACommandSection{

    private static final String COMMAND_NAME = "prs";

    public PRSSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
