package de.jexcellence.oneblock.command.player.infrastructure;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Permission nodes for infrastructure command
 */
@Getter
public enum EInfrastructurePermission implements IPermissionNode {
    
    COMMAND("command", "jexoneblock.infrastructure.command"),
    STATS("commandStats", "jexoneblock.infrastructure.stats"),
    ENERGY("commandEnergy", "jexoneblock.infrastructure.energy"),
    STORAGE("commandStorage", "jexoneblock.infrastructure.storage"),
    AUTOMATION("commandAutomation", "jexoneblock.infrastructure.automation"),
    PROCESSORS("commandProcessors", "jexoneblock.infrastructure.processors"),
    GENERATORS("commandGenerators", "jexoneblock.infrastructure.generators"),
    CRAFTING("commandCrafting", "jexoneblock.infrastructure.crafting");
    
    private final String internalName;
    private final String fallbackNode;
    
    EInfrastructurePermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
