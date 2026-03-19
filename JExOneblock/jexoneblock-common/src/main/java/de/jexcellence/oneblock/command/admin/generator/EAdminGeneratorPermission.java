package de.jexcellence.oneblock.command.admin.generator;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Admin generator command permissions.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Getter
public enum EAdminGeneratorPermission implements IPermissionNode {
    
    COMMAND("command", "jexoneblock.admin.generator"),
    LIST("commandList", "jexoneblock.admin.generator.list"),
    INFO("commandInfo", "jexoneblock.admin.generator.info"),
    BUILD("commandBuild", "jexoneblock.admin.generator.build"),
    REMOVE("commandRemove", "jexoneblock.admin.generator.remove"),
    RELOAD("commandReload", "jexoneblock.admin.generator.reload"),
    STATUS("commandStatus", "jexoneblock.admin.generator.status"),
    ENABLE("commandEnable", "jexoneblock.admin.generator.enable"),
    DISABLE("commandDisable", "jexoneblock.admin.generator.disable"),
    GIVE("commandGive", "jexoneblock.admin.generator.give"),
    GUI("commandGui", "jexoneblock.admin.generator.gui");

    private final String internalName;
    private final String fallbackNode;

    EAdminGeneratorPermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
