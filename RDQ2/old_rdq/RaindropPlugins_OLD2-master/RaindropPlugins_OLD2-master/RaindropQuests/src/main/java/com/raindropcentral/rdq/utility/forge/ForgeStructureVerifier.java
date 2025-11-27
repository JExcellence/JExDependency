package com.raindropcentral.rdq.utility.forge;

import com.raindropcentral.rdq.utility.CooldownManager;

import de.jexcellence.translate.api.I18n;
import de.jexcellence.translate.api.Message;
import de.jexcellence.translate.api.MessageKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Utility class for verifying and managing a custom "Forge" block structure in a Minecraft world.
 * <p>
 * The {@code ForgeStructureVerifier} provides methods to check whether a player-constructed structure
 * at a given location matches a predefined blueprint of blocks and materials. It also handles cooldowns
 * to prevent repeated checks, visualizes the structure for the player, and provides detailed feedback
 * on missing or incorrect blocks using localized messages.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Instantiate this class to manage structure verification for a specific blueprint.</li>
 *   <li>Call {@link #checkStructure(Player, Block)} when a player interacts with a block to verify the structure.</li>
 *   <li>Blueprint is immutable and defined in {@link #createBlueprint()}.</li>
 *   <li>Cooldowns are managed per structure location to prevent spam.</li>
 * </ul>
 *
 * <h2>Feedback</h2>
 * <ul>
 *   <li>Correct blocks are visualized with lime particles; incorrect blocks with red particles.</li>
 *   <li>Players receive detailed messages about missing or incorrect blocks, as well as cooldown status.</li>
 * </ul>
 *
 * @author ItsRainingHP
 * @version 3.0.0
 * @since 5.0.0
 */
public class ForgeStructureVerifier {

    /**
     * The cooldown duration (in milliseconds) for structure verification at a given location.
     */
    private static final long COOLDOWN_MILLIS = 10_000L; // 10 seconds

    /**
     * Immutable blueprint mapping relative block positions to required materials.
     */
    private final Map<Vector, Material> blueprint;

    /**
     * Manages cooldowns for structure verification per location.
     */
    private final CooldownManager cooldownManager;

    /**
     * Constructs a new {@code ForgeStructureVerifier} with the default forge blueprint and a new cooldown manager.
     */
    public ForgeStructureVerifier() {
        this.blueprint = createBlueprint();
        this.cooldownManager = new CooldownManager();
    }

    /**
     * Checks if the structure at the given {@link Block} matches the predefined blueprint.
     * <p>
     * If the structure matches, a cooldown is set for the location and a success message is sent.
     * If not, the player receives detailed feedback about each incorrect or missing block.
     * Visual feedback is provided using particles for both correct and incorrect blocks.
     * </p>
     *
     * @param player       The player to send feedback and visualization to.
     * @param clickedBlock The block that was clicked and serves as the reference point for the structure check.
     * @return {@code true} if the structure matches the blueprint; {@code false} otherwise.
     */
    public boolean checkStructure(@NotNull Player player, @NotNull Block clickedBlock) {
        Location clickedLocation = clickedBlock.getLocation().toBlockLocation();
        World world = clickedLocation.getWorld();
        if (world == null) return false;

        String structureLocationKey = getStructureLocationKey(clickedLocation);

        // Cooldown check
        if (cooldownManager.isOnCooldown(structureLocationKey, COOLDOWN_MILLIS)) {
            long remaining = cooldownManager.getRemainingCooldown(structureLocationKey, COOLDOWN_MILLIS) / 1000;
            I18n.create(
                    MessageKey.of("forge.cooldown_active"),
                    player
            ).includePrefix().withPlaceholder("cooldown", remaining).sendMessage();
            return false;
        }

        // Visualize the structure for the player
        ForgeStructureVisualizer.visualizeStructure(player, clickedLocation, blueprint);

        List<Message> incorrectBlockInfo = new ArrayList<>();
        boolean structureMatches = true;

        // Check each block in the blueprint
        for (Map.Entry<Vector, Material> entry : blueprint.entrySet()) {
            Vector relativePosition = entry.getKey();
            Material expectedMaterial = entry.getValue();

            Location blockLoc = clickedLocation.clone().add(relativePosition);
            Block blockAtRelativePosition = blockLoc.getBlock();
            boolean correct = blockAtRelativePosition.getType() == expectedMaterial;

            if (!correct) {
                structureMatches = false;
                incorrectBlockInfo.add(
                        I18n.create(
                                MessageKey.of("forge.incorrect_block_info"),
                                player
                        ).includePrefix().withPlaceholders(
                                Map.of(
                                        "vector_position", relativePosition.toString(),
                                        "expected", expectedMaterial.translationKey(),
                                        "found", blockAtRelativePosition.getType().translationKey()
                                )
                        ).build()
                );
            }

            // Feedback for incorrect blocks
            if (!structureMatches) {
                incorrectBlockInfo.forEach(incorrectBlock -> {
                    I18n.create(
                            MessageKey.of("forge.structure_incomplete"),
                            player
                    ).withPlaceholder("missing_block", incorrectBlock.getText()).sendMessage();
                });
            } else {
                cooldownManager.resetCooldown(structureLocationKey);
                I18n.create(
                        MessageKey.of("forge.structure_complete"),
                        player
                ).includePrefix().sendMessage();
            }
        }

        return structureMatches;
    }

    /**
     * Generates a unique string key for a block location, combining world name and block coordinates.
     *
     * @param loc The location to generate a key for.
     * @return A unique string key for the location (format: world:x,y,z).
     */
    private static String getStructureLocationKey(@NotNull Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    /**
     * Creates the immutable blueprint for the forge structure.
     * <p>
     * The blueprint maps relative {@link Vector} positions to required {@link Material}s.
     * This method defines the shape and composition of the forge structure.
     * </p>
     *
     * @return An unmodifiable map representing the forge blueprint.
     */
    private static Map<Vector, Material> createBlueprint() {
        Map<Vector, Material> blueprint = new HashMap<>();
        blueprint.put(new Vector(0, 0, 0), Material.DIAMOND_BLOCK);

        for (int i = -2; i < 3; i++) {
            blueprint.put(new Vector(i, -1, 2), Material.GILDED_BLACKSTONE);
            blueprint.put(new Vector(i, -1, 1), Material.GILDED_BLACKSTONE);
            blueprint.put(new Vector(i, -1, 0), Material.GILDED_BLACKSTONE);
            blueprint.put(new Vector(i, -1, -1), Material.GILDED_BLACKSTONE);
            blueprint.put(new Vector(i, -1, -2), Material.GILDED_BLACKSTONE);
        }

        for (int i = 0; i < 3; i++) {
            blueprint.put(new Vector(2, i, 2), Material.NETHER_BRICKS);
            blueprint.put(new Vector(2, i, 1), Material.FURNACE);
            blueprint.put(new Vector(2, i, 0), Material.FURNACE);
            blueprint.put(new Vector(2, i, -1), Material.FURNACE);
            blueprint.put(new Vector(2, i, -2), Material.NETHER_BRICKS);

            blueprint.put(new Vector(1, i, 2), Material.NETHER_BRICKS);
            blueprint.put(new Vector(0, i, 2), Material.NETHER_BRICKS);
            blueprint.put(new Vector(-1, i, 2), Material.NETHER_BRICKS);
            blueprint.put(new Vector(-2, i, 2), Material.NETHER_BRICKS);

            blueprint.put(new Vector(1, i, -2), Material.NETHER_BRICKS);
            blueprint.put(new Vector(0, i, -2), Material.NETHER_BRICKS);
            blueprint.put(new Vector(-1, i, -2), Material.NETHER_BRICKS);
            blueprint.put(new Vector(-2, i, -2), Material.NETHER_BRICKS);

            blueprint.put(new Vector(-2, i, 1), Material.NETHER_BRICKS);
            blueprint.put(new Vector(-2, i, -1), Material.NETHER_BRICKS);
        }
        return Collections.unmodifiableMap(blueprint);
    }
}
