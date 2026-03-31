/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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

/**
 * Represents the PermissionReward API type.
 */
@JsonTypeName("PERMISSION")
public final class PermissionReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(PermissionReward.class.getName());

    private final List<String> permissions;
    private final Long durationSeconds;
    private final boolean temporary;

    /**
     * Executes PermissionReward.
     */
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

    /**
     * Gets typeId.
     */
    @Override
    public @NotNull String getTypeId() {
        return "PERMISSION";
    }

    /**
     * Executes grant.
     */
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

    /**
     * Gets estimatedValue.
     */
    @Override
    public double getEstimatedValue() {
        return permissions.size() * (temporary ? 50.0 : 100.0);
    }

    /**
     * Gets permissions.
     */
    public List<String> getPermissions() {
        return List.copyOf(permissions);
    }

    /**
     * Gets durationSeconds.
     */
    public Long getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Returns whether temporary.
     */
    public boolean isTemporary() {
        return temporary;
    }

    /**
     * Executes validate.
     */
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
