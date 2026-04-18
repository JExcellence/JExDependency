package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Teleports the player to a specific location.
 *
 * <p>Uses {@code Player.teleportAsync()} for Paper/Folia-safe teleportation.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class TeleportReward extends AbstractReward {

    @JsonProperty("world") private final String world;
    @JsonProperty("x") private final double x;
    @JsonProperty("y") private final double y;
    @JsonProperty("z") private final double z;
    @JsonProperty("yaw") private final float yaw;
    @JsonProperty("pitch") private final float pitch;

    /**
     * Creates a teleport reward.
     *
     * @param world the target world name
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param z     the z coordinate
     * @param yaw   the yaw rotation
     * @param pitch the pitch rotation
     */
    public TeleportReward(@JsonProperty("world") @Nullable String world,
                          @JsonProperty("x") double x,
                          @JsonProperty("y") double y,
                          @JsonProperty("z") double z,
                          @JsonProperty("yaw") float yaw,
                          @JsonProperty("pitch") float pitch) {
        super("TELEPORT");
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        var targetWorld = world != null ? Bukkit.getWorld(world) : player.getWorld();
        if (targetWorld == null) {
            return CompletableFuture.completedFuture(false);
        }

        var location = new Location(targetWorld, x, y, z, yaw, pitch);
        return player.teleportAsync(location).thenApply(success -> success);
    }

    @Override
    public @NotNull String descriptionKey() {
        return "reward.teleport";
    }
}
