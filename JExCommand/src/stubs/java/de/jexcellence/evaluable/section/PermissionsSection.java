package de.jexcellence.evaluable.section;

/**
 * Placeholder permissions section used to satisfy linkage during tests.
 */
public class PermissionsSection {

    public boolean hasPermission(Object player, IPermissionNode permissionNode) {
        return true;
    }

    public void sendMissingMessage(Object player, IPermissionNode permissionNode) {
        // No-op for tests
    }
}
