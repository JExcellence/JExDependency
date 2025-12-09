package com.raindropcentral.rdq.utility.forge;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * Utility class for visualizing a custom structure in the Minecraft world for a player.
 * <p>
 * The {@code ForgeStructureVisualizer} provides a static method to display particle effects
 * at each block position of a structure blueprint, helping players see which blocks are
 * correctly placed and which are not. Correct blocks are highlighted with lime particles,
 * while incorrect blocks are highlighted with red particles.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Call {@link #visualizeStructure(Player, Location, Map)} to show the structure visualization to a player.</li>
 *   <li>Typically used in conjunction with structure verification logic to provide feedback.</li>
 * </ul>
 *
 * <h2>Visualization Details</h2>
 * <ul>
 *   <li>Correct blocks: Lime-colored DUST particles.</li>
 *   <li>Incorrect blocks: Red-colored DUST particles.</li>
 *   <li>Particles are spawned at the center of each relevant block.</li>
 * </ul>
 *
 * @author ItsRainingHP, JExcellence
 * @version 3.0.0
 * @since 5.0.0
 */
public class ForgeStructureVisualizer {

    /**
     * Visualizes the structure at the given origin location for the player.
     * <p>
     * For each block in the blueprint, spawns colored particles at the block's location:
     * <ul>
     *   <li>Lime particles for blocks that match the expected material.</li>
     *   <li>Red particles for blocks that do not match the expected material.</li>
     * </ul>
     *
     * @param player    The player to show particles to.
     * @param origin    The origin (reference) location for the structure.
     * @param blueprint The structure blueprint mapping relative positions to required materials.
     */
    public static void visualizeStructure(Player player, Location origin, Map<Vector, Material> blueprint) {
        World world = origin.getWorld();
        if (world == null) return;

        for (Map.Entry<Vector, Material> entry : blueprint.entrySet()) {
            Vector rel = entry.getKey();
            Material expected = entry.getValue();

            Location blockLoc = origin.clone().add(rel);
            Block block = blockLoc.getBlock();
            boolean correct = block.getType() == expected;

            Particle.DustOptions dust = new Particle.DustOptions(
                    correct ? Color.LIME : Color.RED, 1.5f
            );
            Location particleLoc = blockLoc.clone().add(0.5, 0.5, 0.5);

            player.spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    10,
                    0.2, 0.2, 0.2,
                    0,
                    dust
            );
        }
    }
}
