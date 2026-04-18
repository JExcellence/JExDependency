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

package com.raindropcentral.rdq.machine.structure;

import com.raindropcentral.rdq.machine.config.MachineStructureSection;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a multi-block structure definition for a machine.
 *
 * <p>This class stores the structure definition loaded from configuration and provides
 * methods to access required blocks and their relative positions. It serves as an
 * immutable representation of a machine's structural requirements.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class MultiBlockStructure {

    private final Material coreBlock;
    private final Map<Material, List<MachineStructureSection.RelativePosition>> requiredBlocks;

    /**
     * Creates a multi-block structure from a configuration section.
     *
     * @param structureSection configuration section defining the structure
     * @throws IllegalArgumentException when structure section is {@code null}
     */
    public MultiBlockStructure(final @NotNull MachineStructureSection structureSection) {
        if (structureSection == null) {
            throw new IllegalArgumentException("Structure section cannot be null");
        }

        this.coreBlock = structureSection.getCoreBlock();
        this.requiredBlocks = new HashMap<>();

        // Build map of required blocks by material type
        for (final MachineStructureSection.RequiredBlockSection blockSection : structureSection.getRequiredBlocks()) {
            final Material material = blockSection.getType();
            final List<MachineStructureSection.RelativePosition> positions = blockSection.getRelativePositions();

            this.requiredBlocks.computeIfAbsent(material, k -> new ArrayList<>()).addAll(positions);
        }
    }

    /**
     * Returns the core block material type.
     *
     * <p>The core block is the primary block that serves as the anchor point for the structure.
     * All relative positions are calculated from this block's location.
     *
     * @return core block material, never {@code null}
     */
    public @NotNull Material getCoreBlock() {
        return this.coreBlock;
    }

    /**
     * Returns all required block types in this structure.
     *
     * <p>This method returns a list of unique material types that are required
     * to form a valid structure, excluding the core block itself.
     *
     * @return list of required block materials, never {@code null}
     */
    public @NotNull List<Material> getRequiredBlockTypes() {
        return new ArrayList<>(this.requiredBlocks.keySet());
    }

    /**
     * Returns the relative positions for a specific block type.
     *
     * <p>Each position represents an offset from the core block location where
     * the specified material must be present.
     *
     * @param material block material type
     * @return list of relative positions for the material, empty if material is not required
     * @throws IllegalArgumentException when material is {@code null}
     */
    public @NotNull List<MachineStructureSection.RelativePosition> getPositionsForBlock(final @NotNull Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }

        final List<MachineStructureSection.RelativePosition> positions = this.requiredBlocks.get(material);
        return positions == null ? new ArrayList<>() : new ArrayList<>(positions);
    }

    /**
     * Returns all required blocks with their positions.
     *
     * <p>This method returns a map where each key is a required material type
     * and the value is a list of relative positions where that material must be present.
     *
     * @return map of materials to their required positions, never {@code null}
     */
    public @NotNull Map<Material, List<MachineStructureSection.RelativePosition>> getAllRequiredBlocks() {
        final Map<Material, List<MachineStructureSection.RelativePosition>> copy = new HashMap<>();
        for (final Map.Entry<Material, List<MachineStructureSection.RelativePosition>> entry : this.requiredBlocks.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Returns the total number of blocks required for this structure.
     *
     * <p>This count includes the core block plus all required blocks at all positions.
     *
     * @return total block count
     */
    public int getTotalBlockCount() {
        int count = 1; // Core block
        for (final List<MachineStructureSection.RelativePosition> positions : this.requiredBlocks.values()) {
            count += positions.size();
        }
        return count;
    }

    /**
     * Checks if a material is required by this structure.
     *
     * @param material material to check
     * @return {@code true} if the material is required, {@code false} otherwise
     * @throws IllegalArgumentException when material is {@code null}
     */
    public boolean isRequiredBlock(final @NotNull Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        return this.requiredBlocks.containsKey(material);
    }

    @Override
    public String toString() {
        return "MultiBlockStructure{" +
                "coreBlock=" + this.coreBlock +
                ", requiredBlockTypes=" + this.requiredBlocks.keySet() +
                ", totalBlocks=" + this.getTotalBlockCount() +
                '}';
    }
}
