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

import com.raindropcentral.rdq.machine.type.EMachineType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Detects multi-block structure formation during block placement events.
 *
 * <p>This class monitors block placement and checks if the placed block matches
 * any registered machine core block type. When a potential structure is detected,
 * it triggers validation to determine if a complete machine structure has been formed.
 *
 * <p>The detector maintains a registry of machine types and their structure definitions,
 * allowing it to identify which machine type (if any) is being constructed based on
 * the core block material.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class StructureDetector {

    private final Map<Material, EMachineType> coreBlockRegistry;
    private final Function<EMachineType, MultiBlockStructure> structureProvider;
    private final StructureValidator validator;

    /**
     * Creates a structure detector with a structure provider function.
     *
     * <p>The structure provider function is called when a structure needs to be
     * validated, allowing lazy loading of structure definitions from configuration.
     *
     * @param structureProvider function that provides structure definitions for machine types
     * @throws IllegalArgumentException when structureProvider is {@code null}
     */
    public StructureDetector(final @NotNull Function<EMachineType, MultiBlockStructure> structureProvider) {
        if (structureProvider == null) {
            throw new IllegalArgumentException("Structure provider cannot be null");
        }

        this.coreBlockRegistry = new HashMap<>();
        this.structureProvider = structureProvider;
        this.validator = new StructureValidator();

        // Register all machine types by their core materials
        for (final EMachineType type : EMachineType.values()) {
            this.coreBlockRegistry.put(type.getCoreMaterial(), type);
        }
    }

    /**
     * Detects if a block placement event represents a potential machine structure.
     *
     * <p>This method checks if the placed block's material matches any registered
     * core block type. If a match is found, it returns the corresponding machine type
     * for further validation.
     *
     * @param placedBlock the block that was placed
     * @return the machine type if the block is a core block, {@code null} otherwise
     * @throws IllegalArgumentException when placedBlock is {@code null}
     */
    public @Nullable EMachineType detectPotentialStructure(final @NotNull Block placedBlock) {
        if (placedBlock == null) {
            throw new IllegalArgumentException("Placed block cannot be null");
        }

        return this.coreBlockRegistry.get(placedBlock.getType());
    }

    /**
     * Detects and validates a complete machine structure at the specified location.
     *
     * <p>This method first checks if the block at the location is a core block for
     * any machine type. If so, it retrieves the structure definition and validates
     * that all required blocks are present in the correct positions.
     *
     * @param location location to check for a machine structure
     * @return detection result containing machine type and validation status
     * @throws IllegalArgumentException when location is {@code null}
     */
    public @NotNull DetectionResult detectAndValidate(final @NotNull Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }

        final Block block = location.getBlock();
        final EMachineType machineType = this.detectPotentialStructure(block);

        if (machineType == null) {
            return DetectionResult.notDetected();
        }

        // Get structure definition for this machine type
        final MultiBlockStructure structure = this.structureProvider.apply(machineType);
        if (structure == null) {
            return DetectionResult.error(machineType, "Structure definition not found for " + machineType);
        }

        // Validate the structure
        final StructureValidator.ValidationResult validationResult = this.validator.validate(location, structure);

        if (validationResult.isSuccess()) {
            return DetectionResult.valid(machineType, structure);
        } else {
            return DetectionResult.invalid(machineType, validationResult.getErrorMessage());
        }
    }

    /**
     * Checks if a material is a registered core block for any machine type.
     *
     * @param material material to check
     * @return {@code true} if the material is a core block, {@code false} otherwise
     * @throws IllegalArgumentException when material is {@code null}
     */
    public boolean isCoreBlock(final @NotNull Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        return this.coreBlockRegistry.containsKey(material);
    }

    /**
     * Gets the machine type associated with a core block material.
     *
     * @param material core block material
     * @return machine type, or {@code null} if material is not a core block
     * @throws IllegalArgumentException when material is {@code null}
     */
    public @Nullable EMachineType getMachineTypeForCore(final @NotNull Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        return this.coreBlockRegistry.get(material);
    }

    /**
     * Represents the result of structure detection and validation.
     */
    public static class DetectionResult {

        private final boolean detected;
        private final boolean valid;
        private final EMachineType machineType;
        private final MultiBlockStructure structure;
        private final String errorMessage;

        private DetectionResult(
                final boolean detected,
                final boolean valid,
                final EMachineType machineType,
                final MultiBlockStructure structure,
                final String errorMessage) {
            this.detected = detected;
            this.valid = valid;
            this.machineType = machineType;
            this.structure = structure;
            this.errorMessage = errorMessage;
        }

        /**
         * Creates a result indicating no machine structure was detected.
         *
         * @return not detected result
         */
        public static @NotNull DetectionResult notDetected() {
            return new DetectionResult(false, false, null, null, null);
        }

        /**
         * Creates a result indicating a valid machine structure was detected.
         *
         * @param machineType type of machine detected
         * @param structure structure definition
         * @return valid detection result
         * @throws IllegalArgumentException when machineType or structure is {@code null}
         */
        public static @NotNull DetectionResult valid(
                final @NotNull EMachineType machineType,
                final @NotNull MultiBlockStructure structure) {
            if (machineType == null) {
                throw new IllegalArgumentException("Machine type cannot be null");
            }
            if (structure == null) {
                throw new IllegalArgumentException("Structure cannot be null");
            }
            return new DetectionResult(true, true, machineType, structure, null);
        }

        /**
         * Creates a result indicating an invalid machine structure was detected.
         *
         * @param machineType type of machine detected
         * @param errorMessage validation error message
         * @return invalid detection result
         * @throws IllegalArgumentException when machineType or errorMessage is {@code null}
         */
        public static @NotNull DetectionResult invalid(
                final @NotNull EMachineType machineType,
                final @NotNull String errorMessage) {
            if (machineType == null) {
                throw new IllegalArgumentException("Machine type cannot be null");
            }
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                throw new IllegalArgumentException("Error message cannot be null or empty");
            }
            return new DetectionResult(true, false, machineType, null, errorMessage);
        }

        /**
         * Creates a result indicating an error occurred during detection.
         *
         * @param machineType type of machine being detected
         * @param errorMessage error description
         * @return error result
         * @throws IllegalArgumentException when machineType or errorMessage is {@code null}
         */
        public static @NotNull DetectionResult error(
                final @NotNull EMachineType machineType,
                final @NotNull String errorMessage) {
            if (machineType == null) {
                throw new IllegalArgumentException("Machine type cannot be null");
            }
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                throw new IllegalArgumentException("Error message cannot be null or empty");
            }
            return new DetectionResult(true, false, machineType, null, errorMessage);
        }

        /**
         * Checks if a machine structure was detected.
         *
         * @return {@code true} if a structure was detected, {@code false} otherwise
         */
        public boolean isDetected() {
            return this.detected;
        }

        /**
         * Checks if the detected structure is valid.
         *
         * @return {@code true} if structure is valid, {@code false} otherwise
         */
        public boolean isValid() {
            return this.valid;
        }

        /**
         * Gets the detected machine type.
         *
         * @return machine type, or {@code null} if no structure was detected
         */
        public @Nullable EMachineType getMachineType() {
            return this.machineType;
        }

        /**
         * Gets the validated structure definition.
         *
         * @return structure definition, or {@code null} if structure is invalid
         */
        public @Nullable MultiBlockStructure getStructure() {
            return this.structure;
        }

        /**
         * Gets the error message if validation failed.
         *
         * @return error message, or {@code null} if structure is valid
         */
        public @Nullable String getErrorMessage() {
            return this.errorMessage;
        }

        @Override
        public String toString() {
            if (!this.detected) {
                return "DetectionResult{detected=false}";
            }
            if (this.valid) {
                return "DetectionResult{detected=true, valid=true, type=" + this.machineType + "}";
            }
            return "DetectionResult{detected=true, valid=false, type=" + this.machineType +
                    ", error='" + this.errorMessage + "'}";
        }
    }
}
