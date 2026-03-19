package de.jexcellence.multiverse.command.multiverse;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for the multiverse command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PMultiverseSection extends ACommandSection {

    private static final String COMMAND_NAME = "pmultiverse";

    public PMultiverseSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
