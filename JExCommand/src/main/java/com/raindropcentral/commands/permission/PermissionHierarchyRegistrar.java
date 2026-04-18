package com.raindropcentral.commands.permission;

import de.jexcellence.evaluable.section.PermissionsSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Registers parent-child permission relationships with Bukkit's permission manager.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
public final class PermissionHierarchyRegistrar {

    private static final Logger LOG = LoggerFactory.getLogger(PermissionHierarchyRegistrar.class);

    private PermissionHierarchyRegistrar() {}

    /**
     * Registers the hierarchy so parent permissions implicitly grant their children.
     */
    public static void register(
            @NotNull JavaPlugin plugin,
            PermissionsSection permissionsSection,
            @NotNull Map<String, List<String>> permissionParents
    ) {
        if (permissionsSection == null || permissionParents.isEmpty()) return;

        var nodes = PermissionNodeResolver.extractConfiguredNodes(permissionsSection);
        var pm = plugin.getServer().getPluginManager();

        for (var entry : permissionParents.entrySet()) {
            var parentNode = PermissionNodeResolver.resolveNode(nodes, entry.getKey());
            if (parentNode == null) {
                LOG.warn("Skipping unresolvable permission parent: '{}'", entry.getKey());
                continue;
            }

            var children = entry.getValue();
            if (children == null || children.isEmpty()) continue;

            var parentPerm = getOrRegister(pm, parentNode);

            for (var childKey : children) {
                var childNode = PermissionNodeResolver.resolveNode(nodes, childKey);
                if (childNode == null) {
                    LOG.warn("Skipping unresolvable child '{}' for parent '{}'", childKey, parentNode);
                    continue;
                }

                var childPerm = getOrRegister(pm, childNode);
                childPerm.addParent(parentPerm, true);
                childPerm.recalculatePermissibles();
            }

            parentPerm.recalculatePermissibles();
        }
    }

    private static Permission getOrRegister(PluginManager pm, String node) {
        var existing = pm.getPermission(node);
        if (existing != null) {
            if (existing.getDefault() != PermissionDefault.OP) {
                existing.setDefault(PermissionDefault.OP);
                existing.recalculatePermissibles();
            }
            return existing;
        }

        var perm = new Permission(node, PermissionDefault.OP);
        try {
            pm.addPermission(perm);
        } catch (IllegalArgumentException e) {
            var concurrent = pm.getPermission(node);
            if (concurrent != null) return concurrent;
            throw e;
        }
        return perm;
    }
}
