package de.jexcellence.home.command.admin;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Permission nodes for the admin home commands.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Getter
public enum EPAdminHomePermission implements IPermissionNode {

    ADMIN_SETHOME("adminSethome", "jexhome.admin.sethome"),
    ADMIN_DELHOME("adminDelhome", "jexhome.admin.delhome"),
    ADMIN_HOME("adminHome", "jexhome.admin.home");

    private final String internalName;
    private final String fallbackNode;

    EPAdminHomePermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
