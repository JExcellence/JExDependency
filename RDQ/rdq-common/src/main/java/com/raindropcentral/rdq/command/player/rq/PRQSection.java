package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class PRQSection extends ACommandSection {

    private static final String COMMAND_NAME = "prq";

    public PRQSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
