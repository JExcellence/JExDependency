package de.jexcellence.home.command.home;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for the home command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PHomeSection extends ACommandSection {

    private static final String COMMAND_NAME = "home";

    public PHomeSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
