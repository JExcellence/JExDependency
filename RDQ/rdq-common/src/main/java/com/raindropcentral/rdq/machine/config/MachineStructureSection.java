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

package com.raindropcentral.rdq.machine.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration section defining a multi-block structure pattern for machines.
 *
 * <p>This section specifies the core block type and required blocks with their relative positions
 * to form a valid machine structure. The structure is validated during machine construction to
 * ensure all required blocks are present in the correct positions.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
public class MachineStructureSection extends AConfigSection {

    /**
     * Core block type that serves as the machine's primary block.
     */
    private String coreBlock;

    /**
     * List of required block configurations with their relative positions.
     */
    private List<RequiredBlockSection> requiredBlocks;

    /**
     * Creates a machine structure configuration section.
     *
     * @param baseEnvironment evaluation environment builder for expression resolution
     */
    public MachineStructureSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Returns the core block material type.
     *
     * @return core block material
     * @throws IllegalStateException when core block is not configured or invalid
     */
    public @NotNull Material getCoreBlock() {
        if (this.coreBlock == null || this.coreBlock.trim().isEmpty()) {
            Bukkit.getLogger().warning("Core block has not been set");
            return Material.DROPPER;
        }

        try {
            return Material.valueOf(this.coreBlock.trim().toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new IllegalStateException("Invalid core block material: " + this.coreBlock, e);
        }
    }

    /**
     * Returns the list of required block configurations.
     *
     * @return list of required blocks, never {@code null}
     */
    public @NotNull List<RequiredBlockSection> getRequiredBlocks() {
        return this.requiredBlocks == null ? new ArrayList<>() : new ArrayList<>(this.requiredBlocks);
    }

    /**
     * Validates the structure configuration.
     *
     * @throws IllegalStateException when configuration is invalid
     */
    /**
     * Performs post-parsing validation and initialization.
     *
     * @param fields the parsed fields
     * @throws Exception if validation fails
     */
    public void afterParsing(final @NotNull List<Field> fields) throws Exception {
        super.afterParsing(fields);

        // Validate core block
        this.getCoreBlock();

        // Validate required blocks
        final List<RequiredBlockSection> blocks = this.getRequiredBlocks();
        for (final RequiredBlockSection block : blocks) {
            block.validate();
        }
    }

    /**
     * Configuration section for a required block with relative positions.
     */
    @CSAlways
    public static class RequiredBlockSection extends AConfigSection {

        /**
         * Block material type.
         */
        private String type;

        /**
         * List of relative positions where this block type is required.
         */
        private List<Map<String, Integer>> relativePositions;

        /**
         * Creates a required block configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public RequiredBlockSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Creates a required block configuration section with default environment.
         * Used by ConfigMapper for automatic instantiation.
         */
        public RequiredBlockSection() {
            this(new EvaluationEnvironmentBuilder());
        }

        /**
         * Returns the block material type.
         *
         * @return block material
         * @throws IllegalStateException when type is not configured or invalid
         */
        public @NotNull Material getType() {
            if (this.type == null || this.type.trim().isEmpty()) {
                throw new IllegalStateException("Block type must be configured");
            }

            try {
                return Material.valueOf(this.type.trim().toUpperCase());
            } catch (final IllegalArgumentException e) {
                throw new IllegalStateException("Invalid block material: " + this.type, e);
            }
        }

        /**
         * Returns the list of relative positions for this block type.
         *
         * @return list of relative positions, never {@code null}
         */
        public @NotNull List<RelativePosition> getRelativePositions() {
            if (this.relativePositions == null || this.relativePositions.isEmpty()) {
                return new ArrayList<>();
            }

            final List<RelativePosition> positions = new ArrayList<>();
            for (final Map<String, Integer> posMap : this.relativePositions) {
                final Integer x = posMap.get("x");
                final Integer y = posMap.get("y");
                final Integer z = posMap.get("z");

                if (x == null || y == null || z == null) {
                    throw new IllegalStateException("Relative position must have x, y, and z coordinates");
                }

                positions.add(new RelativePosition(x, y, z));
            }

            return positions;
        }

        /**
         * Validates the required block configuration.
         *
         * @throws IllegalStateException when configuration is invalid
         */
        public void validate() {
            this.getType();

            final List<RelativePosition> positions = this.getRelativePositions();
            if (positions.isEmpty()) {
                throw new IllegalStateException("Required block must have at least one relative position");
            }
        }
    }

    /**
     * Represents a relative position offset from the core block.
     */
    public static class RelativePosition {

        private final int x;
        private final int y;
        private final int z;

        /**
         * Creates a relative position.
         *
         * @param x x-coordinate offset
         * @param y y-coordinate offset
         * @param z z-coordinate offset
         */
        public RelativePosition(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Returns the x-coordinate offset.
         *
         * @return x offset
         */
        public int getX() {
            return this.x;
        }

        /**
         * Returns the y-coordinate offset.
         *
         * @return y offset
         */
        public int getY() {
            return this.y;
        }

        /**
         * Returns the z-coordinate offset.
         *
         * @return z offset
         */
        public int getZ() {
            return this.z;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RelativePosition other)) {
                return false;
            }
            return this.x == other.x && this.y == other.y && this.z == other.z;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(this.x);
            result = 31 * result + Integer.hashCode(this.y);
            result = 31 * result + Integer.hashCode(this.z);
            return result;
        }

        @Override
        public String toString() {
            return "RelativePosition{x=" + this.x + ", y=" + this.y + ", z=" + this.z + "}";
        }
    }
}
