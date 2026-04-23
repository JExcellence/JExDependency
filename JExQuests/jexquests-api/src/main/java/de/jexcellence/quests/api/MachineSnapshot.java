package de.jexcellence.quests.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Immutable identity + location snapshot for a placed machine.
 * Carried by {@link de.jexcellence.quests.api.event.MachineTickEvent}
 * so subscribers can look up the live entity via
 * {@code MachineService.findByIdAsync(snapshot.id())}.
 *
 * @param id database row id
 * @param ownerUuid player who placed the machine
 * @param machineType identifier from the machine registry
 * @param world Bukkit world name
 * @param x block X
 * @param y block Y
 * @param z block Z
 * @param facing cardinal facing ({@code NORTH} / {@code EAST} / {@code SOUTH} / {@code WEST})
 */
public record MachineSnapshot(
        long id,
        @NotNull UUID ownerUuid,
        @NotNull String machineType,
        @NotNull String world,
        int x, int y, int z,
        @NotNull String facing
) {
}
