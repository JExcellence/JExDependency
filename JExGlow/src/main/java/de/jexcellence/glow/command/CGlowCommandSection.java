package de.jexcellence.glow.command;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for the console glow command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class CGlowCommandSection extends ACommandSection {

    private static final String COMMAND_NAME = "cglow";

    public CGlowCommandSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
