package de.jexcellence.multiverse.command.multiverse;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Permission nodes for the multiverse command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Getter
public enum EMultiversePermission implements IPermissionNode {

    COMMAND("command", "jexmultiverse.command"),
    CREATE("commandCreate", "jexmultiverse.command.create"),
    DELETE("commandDelete", "jexmultiverse.command.delete"),
    EDIT("commandEdit", "jexmultiverse.command.edit"),
    TELEPORT("commandTeleport", "jexmultiverse.command.teleport"),
    LOAD("commandLoad", "jexmultiverse.command.load");

    private final String internalName;
    private final String fallbackNode;

    EMultiversePermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
