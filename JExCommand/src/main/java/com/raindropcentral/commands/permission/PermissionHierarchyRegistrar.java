package com.raindropcentral.commands.permission;

import de.jexcellence.evaluable.section.PermissionsSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers configured parent-child permission relationships with Bukkit's permission manager.
 */
public final class PermissionHierarchyRegistrar {

    private static final Logger LOGGER = Logger.getLogger(PermissionHierarchyRegistrar.class.getName());
    private static final String CONFIGURED_NODES_FIELD = "nodes";

    private PermissionHierarchyRegistrar() {
    }

    /**
     * Registers the supplied permission hierarchy so parent permissions grant their configured children.
     *
     * @param plugin             plugin owning the command configuration
     * @param permissionsSection section containing configured permission nodes
     * @param permissionParents  mapping of parent keys to child keys
     */
    public static void register(
        final @NotNull JavaPlugin plugin,
        final PermissionsSection permissionsSection,
        final @NotNull Map<String, List<String>> permissionParents
    ) {

        if (
            permissionsSection == null ||
            permissionParents.isEmpty()
        ) {
            return;
        }

        final Map<String, String> configuredNodes = extractConfiguredNodes(permissionsSection);

        final PluginManager pluginManager = plugin.getServer().getPluginManager();

        for (Map.Entry<String, List<String>> entry : permissionParents.entrySet()) {
            final String parentNode = resolveNode(configuredNodes, entry.getKey());
            if (parentNode == null) {
                LOGGER.warning("Skipping permission parent '" + entry.getKey() + "' because it could not be resolved");
                continue;
            }

            final List<String> childKeys = entry.getValue();
            if (
                childKeys == null ||
                childKeys.isEmpty()
            ) {
                continue;
            }

            final Permission parentPermission = getOrRegisterPermission(pluginManager, parentNode);

            for (String childKey : childKeys) {
                final String childNode = resolveNode(configuredNodes, childKey);
                if (childNode == null) {
                    LOGGER.warning(
                        "Skipping permission child '" + childKey + "' for parent '" + parentNode + "' because it could not be resolved"
                    );
                    continue;
                }

                final Permission childPermission = getOrRegisterPermission(pluginManager, childNode);
                childPermission.addParent(parentPermission, true);
                childPermission.recalculatePermissibles();
            }

            parentPermission.recalculatePermissibles();
        }
    }

    private static @NotNull Permission getOrRegisterPermission(
        final @NotNull PluginManager pluginManager,
        final @NotNull String permissionNode
    ) {

        final Permission existingPermission = pluginManager.getPermission(permissionNode);
        if (existingPermission != null) {
            return existingPermission;
        }

        final Permission registeredPermission = new Permission(
            permissionNode,
            PermissionDefault.FALSE
        );

        try {
            pluginManager.addPermission(registeredPermission);
        } catch (final IllegalArgumentException exception) {
            final Permission concurrentPermission = pluginManager.getPermission(permissionNode);
            if (concurrentPermission != null) {
                return concurrentPermission;
            }
            throw exception;
        }

        return registeredPermission;
    }

    private static Map<String, String> extractConfiguredNodes(
        final @NotNull PermissionsSection permissionsSection
    ) {

        final Map<String, String> configuredNodes = new HashMap<>();

        try {
            final Field nodesField = PermissionsSection.class.getDeclaredField(CONFIGURED_NODES_FIELD);
            nodesField.setAccessible(true);

            final Object rawNodes = nodesField.get(permissionsSection);
            if (! (rawNodes instanceof Map<?, ?> nodesMap)) {
                return Map.of();
            }

            for (Map.Entry<?, ?> entry : nodesMap.entrySet()) {
                if (
                    entry.getKey() instanceof String internalName &&
                    entry.getValue() instanceof String permissionNode
                ) {
                    configuredNodes.put(
                        internalName,
                        permissionNode
                    );
                }
            }
        } catch (final ReflectiveOperationException exception) {
            LOGGER.log(
                Level.WARNING,
                "Unable to read configured permission nodes for hierarchy registration",
                exception
            );
            return Map.of();
        }

        return configuredNodes;
    }

    private static String resolveNode(
        final @NotNull Map<String, String> configuredNodes,
        final String permissionReference
    ) {

        if (permissionReference == null) {
            return null;
        }

        final String trimmedReference = permissionReference.trim();
        if (trimmedReference.isEmpty()) {
            return null;
        }

        final String configuredNode = configuredNodes.get(trimmedReference);
        if (configuredNode != null && ! configuredNode.isBlank()) {
            return configuredNode;
        }

        return trimmedReference.contains(".") ?
               trimmedReference :
               null;
    }
}
