package de.jexcellence.home.command.sethome;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Permission nodes for the sethome command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Getter
public enum EPSetHomePermission implements IPermissionNode {

    SETHOME("sethome", "jexhome.sethome"),
    SETHOME_UNLIMITED("sethomeUnlimited", "jexhome.sethome.unlimited");

    private final String internalName;
    private final String fallbackNode;

    EPSetHomePermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
