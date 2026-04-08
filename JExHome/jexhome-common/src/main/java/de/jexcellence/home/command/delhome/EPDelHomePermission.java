package de.jexcellence.home.command.delhome;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Permission nodes for the delhome command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Getter
public enum EPDelHomePermission implements IPermissionNode {

    DELHOME("delhome", "jexhome.delhome"),
    DELHOME_OTHER("delhomeOther", "jexhome.delhome.other"),
    DELHOME_ALL("delhomeAll", "jexhome.delhome.all");

    private final String internalName;
    private final String fallbackNode;

    EPDelHomePermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
