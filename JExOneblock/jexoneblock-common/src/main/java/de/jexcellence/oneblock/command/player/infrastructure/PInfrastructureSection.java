package de.jexcellence.oneblock.command.player.infrastructure;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for infrastructure command
 */
public class PInfrastructureSection extends ACommandSection {
    
    private static final String COMMAND_NAME = "infrastructure";
    
    public PInfrastructureSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
