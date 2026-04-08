package de.jexcellence.multiverse.command.spawn;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for the spawn command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PSpawnSection extends ACommandSection {

    private static final String COMMAND_NAME = "pspawn";

    public PSpawnSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
