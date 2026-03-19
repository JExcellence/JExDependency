package de.jexcellence.home.command.delhome;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for the delhome command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PDelHomeSection extends ACommandSection {

    private static final String COMMAND_NAME = "delhome";

    public PDelHomeSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
