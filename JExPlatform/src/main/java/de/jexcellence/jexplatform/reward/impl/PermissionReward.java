package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Grants a permission node to the player via LuckPerms reflection.
 *
 * <p>Supports permanent and temporary permissions. If LuckPerms is not
 * available, falls back to Bukkit's attachment system (non-persistent).
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class PermissionReward extends AbstractReward {

    @JsonProperty("permission") private final String permission;
    @JsonProperty("duration") private final long durationSeconds;

    /**
     * Creates a permission reward.
     *
     * @param permission      the permission node to grant
     * @param durationSeconds the duration in seconds ({@code 0} for permanent)
     */
    public PermissionReward(@JsonProperty("permission") @NotNull String permission,
                            @JsonProperty("duration") long durationSeconds) {
        super("PERMISSION");
        this.permission = permission;
        this.durationSeconds = Math.max(0, durationSeconds);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        if (tryLuckPerms(player)) {
            return CompletableFuture.completedFuture(true);
        }
        // Fallback: Bukkit attachment (session-only)
        var attachment = player.addAttachment(
                Bukkit.getPluginManager().getPlugins()[0], permission, true);
        if (attachment == null) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(true);
    }

    @SuppressWarnings("unchecked")
    private boolean tryLuckPerms(@NotNull Player player) {
        try {
            var providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            var luckPerms = providerClass.getMethod("get").invoke(null);

            var userManager = luckPerms.getClass().getMethod("getUserManager")
                    .invoke(luckPerms);
            var user = userManager.getClass()
                    .getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUniqueId());
            if (user == null) {
                return false;
            }

            var nodeBuilderClass = Class.forName("net.luckperms.api.node.Node");
            var builder = nodeBuilderClass.getMethod("builder", String.class)
                    .invoke(null, permission);

            if (durationSeconds > 0) {
                builder.getClass().getMethod("expiry", Duration.class)
                        .invoke(builder, Duration.ofSeconds(durationSeconds));
            }

            var node = builder.getClass().getMethod("build").invoke(builder);
            var dataMethod = user.getClass().getMethod("data");
            var data = dataMethod.invoke(user);
            data.getClass().getMethod("add", nodeBuilderClass).invoke(data, node);

            userManager.getClass().getMethod("saveUser", user.getClass())
                    .invoke(userManager, user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public @NotNull String descriptionKey() {
        return "reward.permission";
    }
}
