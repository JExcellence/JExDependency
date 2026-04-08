package de.jexcellence.multiverse.command.spawn;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Permission nodes for the spawn command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Getter
public enum ESpawnPermission implements IPermissionNode {

    SPAWN("spawn", "jexmultiverse.spawn"),
    SPAWN_OTHER("spawnOther", "jexmultiverse.spawn.other");

    private final String internalName;
    private final String fallbackNode;

    ESpawnPermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
