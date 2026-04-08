package de.jexcellence.oneblock.command.admin.generator;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for admin generator command.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class PAGeneratorSection extends ACommandSection {
    
    private static final String COMMAND_NAME = "admingenerator";
    
    public PAGeneratorSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
