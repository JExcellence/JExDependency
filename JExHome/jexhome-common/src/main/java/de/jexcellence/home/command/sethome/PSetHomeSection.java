package de.jexcellence.home.command.sethome;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for the sethome command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PSetHomeSection extends ACommandSection {

    private static final String COMMAND_NAME = "sethome";

    public PSetHomeSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
