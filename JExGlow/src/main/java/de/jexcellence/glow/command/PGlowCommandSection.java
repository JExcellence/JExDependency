package de.jexcellence.glow.command;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for the glow command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PGlowCommandSection extends ACommandSection {

    private static final String COMMAND_NAME = "glow";

    public PGlowCommandSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
