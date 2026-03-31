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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Reward that teleports the player to a specific location.
 */
@JsonTypeName("TELEPORT")
public final class TeleportReward extends AbstractReward {

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    /**
     * Executes TeleportReward.
     */
    @JsonCreator
    public TeleportReward(
        @JsonProperty("worldName") @NotNull String worldName,
        @JsonProperty("x") double x,
        @JsonProperty("y") double y,
        @JsonProperty("z") double z,
        @JsonProperty("yaw") float yaw,
        @JsonProperty("pitch") float pitch
    ) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Gets typeId.
     */
    @Override
    public @NotNull String getTypeId() {
        return "TELEPORT";
    }

    /**
     * Executes grant.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    return false;
                }
                
                Location location = new Location(world, x, y, z, yaw, pitch);
                
                // Teleport must be done on main thread
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugins()[0],
                    () -> player.teleport(location)
                );
                
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Gets estimatedValue.
     */
    @Override
    public double getEstimatedValue() {
        return 0.0;
    }

    /**
     * Gets worldName.
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Gets x.
     */
    public double getX() {
        return x;
    }

    /**
     * Gets y.
     */
    public double getY() {
        return y;
    }

    /**
     * Gets z.
     */
    public double getZ() {
        return z;
    }

    /**
     * Gets yaw.
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Gets pitch.
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Executes validate.
     */
    @Override
    public void validate() {
        if (worldName == null || worldName.trim().isEmpty()) {
            throw new IllegalArgumentException("World name cannot be empty");
        }
    }
}
