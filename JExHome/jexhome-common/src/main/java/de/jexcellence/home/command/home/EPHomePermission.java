package de.jexcellence.home.command.home;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Permission nodes for the home command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Getter
public enum EPHomePermission implements IPermissionNode {

    HOME("home", "jexhome.home"),
    HOME_OTHER("homeOther", "jexhome.home.other");

    private final String internalName;
    private final String fallbackNode;

    EPHomePermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
