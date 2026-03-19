package de.jexcellence.oneblock.command.player.island;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.List;

public class PIslandSection extends ACommandSection {
    
    private static final String COMMAND_NAME = "island";

    public PIslandSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }

    public List<String> getAliases() {
        return List.of("is", "oneblock", "ob");
    }

    public String getDescription() {
        return "Manage your OneBlock island";
    }

    public String getUsage() {
        return "/island [action] [args...]";
    }

    public String getBasePermission() {
        return EIslandPermission.COMMAND.getFallbackNode();
    }
}