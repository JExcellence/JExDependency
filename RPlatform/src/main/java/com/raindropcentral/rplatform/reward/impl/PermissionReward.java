package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@JsonTypeName("PERMISSION")
public final class PermissionReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(PermissionReward.class.getName());

    private final List<String> permissions;
    private final Long durationSeconds;
    private final boolean temporary;

    @JsonCreator
    public PermissionReward(
        @JsonProperty("permissions") @NotNull List<String> permissions,
        @JsonProperty("durationSeconds") Long durationSeconds,
        @JsonProperty("temporary") boolean temporary
    ) {
        this.permissions = new ArrayList<>(permissions);
        this.durationSeconds = durationSeconds;
        this.temporary = temporary || durationSeconds != null;
    }

    @Override
    public @NotNull String getTypeId() {
        return "PERMISSION";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
                var luckPerms = Bukkit.getServicesManager().getRegistration(luckPermsClass);
                
                if (luckPerms != null) {
                    return grantViaLuckPerms(player, luckPerms.getProvider());
                }
            } catch (ClassNotFoundException e) {
                LOGGER.warning("LuckPerms not found, cannot grant permissions");
            }
            return false;
        });
    }

    private boolean grantViaLuckPerms(@NotNull Player player, @NotNull Object luckPerms) {
        try {
            var userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            var userFuture = userManager.getClass()
                .getMethod("loadUser", java.util.UUID.class)
                .invoke(userManager, player.getUniqueId());
            
            var user = ((CompletableFuture<?>) userFuture).join();
            
            for (String permission : permissions) {
                var nodeBuilder = Class.forName("net.luckperms.api.node.Node")
                    .getMethod("builder", String.class)
                    .invoke(null, permission);
                
                if (temporary && durationSeconds != null) {
                    var duration = Class.forName("java.time.Duration")
                        .getMethod("ofSeconds", long.class)
                        .invoke(null, durationSeconds);
                    nodeBuilder.getClass().getMethod("expiry", Class.forName("java.time.Duration"))
                        .invoke(nodeBuilder, duration);
                }
                
                var node = nodeBuilder.getClass().getMethod("build").invoke(nodeBuilder);
                user.getClass().getMethod("data").invoke(user).getClass()
                    .getMethod("add", Class.forName("net.luckperms.api.node.Node"))
                    .invoke(user.getClass().getMethod("data").invoke(user), node);
            }
            
            userManager.getClass().getMethod("saveUser", user.getClass()).invoke(userManager, user);
            return true;
            
        } catch (Exception e) {
            LOGGER.warning("Failed to grant permissions via LuckPerms: " + e.getMessage());
            return false;
        }
    }

    @Override
    public double getEstimatedValue() {
        return permissions.size() * (temporary ? 50.0 : 100.0);
    }

    public List<String> getPermissions() {
        return List.copyOf(permissions);
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public void validate() {
        if (permissions.isEmpty()) {
            throw new IllegalArgumentException("Permission reward must have at least one permission");
        }
        if (temporary && durationSeconds != null && durationSeconds <= 0) {
            throw new IllegalArgumentException("Duration must be positive for temporary permissions");
        }
    }
}
