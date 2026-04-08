package de.jexcellence.oneblock.command.player.generator;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.List;

/**
 * Command section configuration for generator command.
 * Configures the generator command with aliases and basic settings.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public class PGeneratorSection extends ACommandSection {
    
    private static final String COMMAND_NAME = "generator";
    
    /**
     * Creates a new generator command section.
     * 
     * @param environmentBuilder the evaluation environment builder
     */
    public PGeneratorSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
    
    /**
     * Gets the command aliases.
     * 
     * @return array of command aliases
     */
    public List<String> getAliases() {
        return List.of("gen", "generators", "cobblegen");
    }
    
    /**
     * Gets the command description.
     * 
     * @return command description
     */
    public String getDescription() {
        return "Manage cobblestone generator structures";
    }
    
    /**
     * Gets the command usage.
     * 
     * @return command usage string
     */
    public String getUsage() {
        return "/island generator <subcommand>";
    }
    
    /**
     * Gets the required permission.
     * 
     * @return permission string
     */
    public String getPermission() {
        return "jexoneblock.command.generator";
    }
}