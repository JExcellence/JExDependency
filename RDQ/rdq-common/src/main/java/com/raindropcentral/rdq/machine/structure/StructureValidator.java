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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates multi-block structures for machine construction.
 *
 * <p>This class checks whether blocks are placed in the correct positions to form
 * a valid machine structure. It provides detailed validation results including
 * specific error messages for debugging and user feedback.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class StructureValidator {

    /**
     * Validates a multi-block structure at the specified core location.
     *
     * <p>This method checks that all required blocks are present at their expected
     * positions relative to the core block. If validation fails, the result contains
     * detailed error information about which blocks are missing or incorrect.
     *
     * @param coreLocation location of the core block
     * @param structure structure definition to validate against
     * @return validation result with success status and error details
     * @throws IllegalArgumentException when coreLocation or structure is {@code null}
     */
    public @NotNull ValidationResult validate(
            final @NotNull Location coreLocation,
            final @NotNull MultiBlockStructure structure) {

        if (coreLocation == null) {
            throw new IllegalArgumentException("Core location cannot be null");
        }
        if (structure == null) {
            throw new IllegalArgumentException("Structure cannot be null");
        }

        // Validate core block
        final Block coreBlock = coreLocation.getBlock();
        if (coreBlock.getType() != structure.getCoreBlock()) {
            return ValidationResult.failure(
                    "Invalid core block at " + formatLocation(coreLocation) +
                    ". Expected: " + structure.getCoreBlock() +
                    ", Found: " + coreBlock.getType()
            );
        }

        // Validate all required blocks
        final Map<Material, List<MachineStructureSection.RelativePosition>> requiredBlocks =
                structure.getAllRequiredBlocks();

        for (final Map.Entry<Material, List<MachineStructureSection.RelativePosition>> entry : requiredBlocks.entrySet()) {
            final Material requiredMaterial = entry.getKey();
            final List<MachineStructureSection.RelativePosition> positions = entry.getValue();

            for (final MachineStructureSection.RelativePosition offset : positions) {
                final Location checkLocation = coreLocation.clone().add(offset.getX(), offset.getY(), offset.getZ());
                final Block block = checkLocation.getBlock();

                if (block.getType() != requiredMaterial) {
                    return ValidationResult.failure(
                            "Invalid block at " + formatLocation(checkLocation) +
                            " (offset: " + offset + "). " +
                            "Expected: " + requiredMaterial +
                            ", Found: " + block.getType()
                    );
                }
            }
        }

        return ValidationResult.success();
    }

    /**
     * Validates a structure and returns a list of all validation errors.
     *
     * <p>Unlike {@link #validate(Location, MultiBlockStructure)}, this method continues
     * checking all blocks even after finding errors, returning a complete list of issues.
     *
     * @param coreLocation location of the core block
     * @param structure structure definition to validate against
     * @return list of validation errors, empty if structure is valid
     * @throws IllegalArgumentException when coreLocation or structure is {@code null}
     */
    public @NotNull List<String> validateAll(
            final @NotNull Location coreLocation,
            final @NotNull MultiBlockStructure structure) {

        if (coreLocation == null) {
            throw new IllegalArgumentException("Core location cannot be null");
        }
        if (structure == null) {
            throw new IllegalArgumentException("Structure cannot be null");
        }

        final List<String> errors = new ArrayList<>();

        // Check core block
        final Block coreBlock = coreLocation.getBlock();
        if (coreBlock.getType() != structure.getCoreBlock()) {
            errors.add(
                    "Invalid core block at " + formatLocation(coreLocation) +
                    ". Expected: " + structure.getCoreBlock() +
                    ", Found: " + coreBlock.getType()
            );
        }

        // Check all required blocks
        final Map<Material, List<MachineStructureSection.RelativePosition>> requiredBlocks =
                structure.getAllRequiredBlocks();

        for (final Map.Entry<Material, List<MachineStructureSection.RelativePosition>> entry : requiredBlocks.entrySet()) {
            final Material requiredMaterial = entry.getKey();
            final List<MachineStructureSection.RelativePosition> positions = entry.getValue();

            for (final MachineStructureSection.RelativePosition offset : positions) {
                final Location checkLocation = coreLocation.clone().add(offset.getX(), offset.getY(), offset.getZ());
                final Block block = checkLocation.getBlock();

                if (block.getType() != requiredMaterial) {
                    errors.add(
                            "Invalid block at " + formatLocation(checkLocation) +
                            " (offset: " + offset + "). " +
                            "Expected: " + requiredMaterial +
                            ", Found: " + block.getType()
                    );
                }
            }
        }

        return errors;
    }

    /**
     * Formats a location as a human-readable string.
     *
     * @param location location to format
     * @return formatted location string
     */
    private @NotNull String formatLocation(final @NotNull Location location) {
        return String.format("%s(%d, %d, %d)",
                location.getWorld() != null ? location.getWorld().getName() : "unknown",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    /**
     * Represents the result of a structure validation.
     */
    public static class ValidationResult {

        private final boolean success;
        private final String errorMessage;

        private ValidationResult(final boolean success, final String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        /**
         * Creates a successful validation result.
         *
         * @return success result
         */
        public static @NotNull ValidationResult success() {
            return new ValidationResult(true, null);
        }

        /**
         * Creates a failed validation result with an error message.
         *
         * @param errorMessage description of the validation failure
         * @return failure result
         * @throws IllegalArgumentException when errorMessage is {@code null} or empty
         */
        public static @NotNull ValidationResult failure(final @NotNull String errorMessage) {
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                throw new IllegalArgumentException("Error message cannot be null or empty");
            }
            return new ValidationResult(false, errorMessage);
        }

        /**
         * Checks if the validation was successful.
         *
         * @return {@code true} if validation succeeded, {@code false} otherwise
         */
        public boolean isSuccess() {
            return this.success;
        }

        /**
         * Checks if the validation failed.
         *
         * @return {@code true} if validation failed, {@code false} otherwise
         */
        public boolean isFailure() {
            return !this.success;
        }

        /**
         * Returns the error message if validation failed.
         *
         * @return error message, or {@code null} if validation succeeded
         */
        public String getErrorMessage() {
            return this.errorMessage;
        }

        @Override
        public String toString() {
            return this.success
                    ? "ValidationResult{success=true}"
                    : "ValidationResult{success=false, error='" + this.errorMessage + "'}";
        }
    }
}
