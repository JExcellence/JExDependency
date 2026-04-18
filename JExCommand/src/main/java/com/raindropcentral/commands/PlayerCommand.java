package com.raindropcentral.commands;

import com.raindropcentral.commands.permission.PermissionNodeResolver;
import com.raindropcentral.commands.permission.PermissionParentProvider;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.ErrorType;
import de.jexcellence.evaluable.section.CommandSection;
import de.jexcellence.evaluable.section.PermissionNode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base command restricted to player senders with built-in permission checks
 * and tab completion routing.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
public abstract class PlayerCommand extends BukkitCommand {

    protected PlayerCommand(final @NotNull CommandSection commandSection) {
        super(commandSection);
    }

    // ── Abstract hooks ────────────────────────────────────────────────────────

    protected abstract void onPlayerInvocation(
            @NotNull Player player, @NotNull String alias, @NotNull String[] args
    );

    protected abstract List<String> onPlayerTabCompletion(
            @NotNull Player player, @NotNull String alias, @NotNull String[] args
    );

    // ── Bukkit routing ────────────────────────────────────────────────────────

    @Override
    protected void onInvocation(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            throw new CommandError(null, ErrorType.NOT_A_PLAYER);
        }
        onPlayerInvocation(player, alias, args);
    }

    @Override
    protected List<String> onTabCompletion(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        return onPlayerTabCompletion(player, alias, args);
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the player <strong>lacks</strong> the permission
     * and sends a localized denial message.
     */
    protected boolean hasNoPermission(@NotNull Player player, @NotNull PermissionNode node) {
        var permissions = commandSection.getPermissions();
        if (permissions == null) return false;
        if (hasPermission(player, node)) return false;
        permissions.sendMissingMessage(player, node);
        return true;
    }

    /**
     * Returns {@code true} when the player <strong>has</strong> the permission.
     * Checks direct permission, then walks the inheritance hierarchy if the
     * section implements {@link PermissionParentProvider}.
     */
    protected boolean hasPermission(@NotNull Player player, @NotNull PermissionNode node) {
        if (player.isOp()) return true;

        var permissions = commandSection.getPermissions();
        if (permissions == null) return true;
        if (permissions.hasPermission(player, node)) return true;

        if (!(commandSection instanceof PermissionParentProvider provider)) return false;

        var parents = provider.getPermissionParents();
        if (parents.isEmpty()) return false;

        var configuredNodes = PermissionNodeResolver.extractConfiguredNodes(permissions);
        var targetNode = PermissionNodeResolver.resolveNode(configuredNodes, node.getInternalName(), node.getFallbackNode());
        if (targetNode == null) return false;

        return hasInheritedPermission(player, targetNode, configuredNodes, parents, new HashSet<>());
    }

    // ── Recursive inheritance walk ────────────────────────────────────────────

    private boolean hasInheritedPermission(
            Player player,
            String targetNode,
            Map<String, String> configuredNodes,
            Map<String, List<String>> parentMap,
            Set<String> visited
    ) {
        if (!visited.add(targetNode)) return false;

        for (var entry : parentMap.entrySet()) {
            var parentNode = PermissionNodeResolver.resolveNode(configuredNodes, entry.getKey());
            if (parentNode == null) continue;

            var children = entry.getValue();
            if (children == null || children.isEmpty()) continue;

            for (var childRef : children) {
                var childNode = PermissionNodeResolver.resolveNode(configuredNodes, childRef);
                if (!targetNode.equals(childNode)) continue;

                if (player.hasPermission(parentNode)
                        || hasInheritedPermission(player, parentNode, configuredNodes, parentMap, visited)) {
                    return true;
                }
            }
        }
        return false;
    }
}
