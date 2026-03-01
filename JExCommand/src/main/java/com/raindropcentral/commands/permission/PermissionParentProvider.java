package com.raindropcentral.commands.permission;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Exposes command-local permission parent relationships using configured permission keys.
 */
public interface PermissionParentProvider {

    /**
     * Returns a mapping of parent permission keys to their child permission keys.
     * Keys may reference configured permission aliases or raw permission nodes.
     *
     * @return immutable or mutable mapping of parent keys to child keys
     */
    @NotNull
    Map<String, List<String>> getPermissionParents();
}
