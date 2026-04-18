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
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Configuration section for system-wide machine settings.
 *
 * <p>This section controls global machine behavior including enablement, caching, permissions,
 * and breaking behavior. It provides validation to ensure configuration values are within
 * acceptable ranges and logically consistent.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
public class MachineSystemSection extends AConfigSection {

    /**
     * Master switch for the entire machine system.
     */
    private Boolean enabled;

    /**
     * Cache configuration section.
     */
    private CacheSection cache;

    /**
     * Permission configuration section.
     */
    private PermissionSection permissions;

    /**
     * Breaking behavior configuration section.
     */
    private BreakingSection breaking;

    /**
     * Creates a machine system configuration section.
     *
     * @param baseEnvironment evaluation environment builder for expression resolution
     */
    public MachineSystemSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Creates a machine system configuration section with default environment.
     */
    public MachineSystemSection() {
        this(new EvaluationEnvironmentBuilder());
    }

    /**
     * Indicates whether the machine system is enabled.
     *
     * @return {@code true} when machines are enabled, defaults to {@code true}
     */
    public boolean isEnabled() {
        return this.enabled == null || this.enabled;
    }

    /**
     * Returns the cache configuration section.
     *
     * @return cache configuration, never {@code null}
     */
    public @NotNull CacheSection getCache() {
        return this.cache == null ? new CacheSection(new EvaluationEnvironmentBuilder()) : this.cache;
    }

    /**
     * Returns the permission configuration section.
     *
     * @return permission configuration, never {@code null}
     */
    public @NotNull PermissionSection getPermissions() {
        return this.permissions == null ? new PermissionSection(new EvaluationEnvironmentBuilder()) : this.permissions;
    }

    /**
     * Returns the breaking behavior configuration section.
     *
     * @return breaking configuration, never {@code null}
     */
    public @NotNull BreakingSection getBreaking() {
        return this.breaking == null ? new BreakingSection(new EvaluationEnvironmentBuilder()) : this.breaking;
    }

    /**
     * Validates the machine system configuration.
     *
     * @throws IllegalStateException when configuration values are invalid
     */
    /**
     * Performs post-parsing validation and initialization.
     *
     * @param fields the parsed fields
     * @throws Exception if validation fails
     */
    public void afterParsing(final @NotNull List<Field> fields) throws Exception {
        super.afterParsing(fields);

        final CacheSection cacheSection = this.getCache();
        if (cacheSection.getAutoSaveInterval() < 0) {
            throw new IllegalStateException("Cache auto-save interval cannot be negative: " + cacheSection.getAutoSaveInterval());
        }

        if (cacheSection.getMaxMachinesPerPlayer() < 0) {
            throw new IllegalStateException("Max machines per player cannot be negative: " + cacheSection.getMaxMachinesPerPlayer());
        }

        final PermissionSection permissionSection = this.getPermissions();
        final String basePermission = permissionSection.getBasePermission();
        if (basePermission == null || basePermission.trim().isEmpty()) {
            throw new IllegalStateException("Base permission cannot be empty");
        }
    }

    /**
     * Cache configuration subsection.
     */
    @CSAlways
    public static class CacheSection extends AConfigSection {

        /**
         * Whether caching is enabled.
         */
        private Boolean enabled;

        /**
         * Auto-save interval in seconds.
         */
        private Integer autoSaveInterval;

        /**
         * Maximum machines per player.
         */
        private Integer maxMachinesPerPlayer;

        /**
         * Creates a cache configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public CacheSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Indicates whether caching is enabled.
         *
         * @return {@code true} when caching is enabled, defaults to {@code true}
         */
        public boolean isEnabled() {
            return this.enabled == null || this.enabled;
        }

        /**
         * Returns the auto-save interval in seconds.
         *
         * @return auto-save interval, defaults to 300 seconds (5 minutes)
         */
        public int getAutoSaveInterval() {
            return this.autoSaveInterval == null ? 300 : this.autoSaveInterval;
        }

        /**
         * Returns the maximum machines per player.
         *
         * @return max machines per player, defaults to 10
         */
        public int getMaxMachinesPerPlayer() {
            return this.maxMachinesPerPlayer == null ? 10 : this.maxMachinesPerPlayer;
        }
    }

    /**
     * Permission configuration subsection.
     */
    @CSAlways
    public static class PermissionSection extends AConfigSection {

        /**
         * Whether permission checks are required.
         */
        private Boolean requirePermission;

        /**
         * Base permission node for machines.
         */
        private String basePermission;

        /**
         * Creates a permission configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public PermissionSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Indicates whether permission checks are required.
         *
         * @return {@code true} when permissions are required, defaults to {@code true}
         */
        public boolean isRequirePermission() {
            return this.requirePermission == null || this.requirePermission;
        }

        /**
         * Returns the base permission node.
         *
         * @return base permission, defaults to "rdq.machine"
         */
        public @NotNull String getBasePermission() {
            return this.basePermission == null ? "rdq.machine" : this.basePermission;
        }
    }

    /**
     * Breaking behavior configuration subsection.
     */
    @CSAlways
    public static class BreakingSection extends AConfigSection {

        /**
         * Whether to drop items when machine is broken.
         */
        private Boolean dropItems;

        /**
         * Whether to drop the machine item when broken.
         */
        private Boolean dropMachineItem;

        /**
         * Whether only the owner can break the machine.
         */
        private Boolean requireOwner;

        /**
         * Creates a breaking configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public BreakingSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Indicates whether items should be dropped when machine is broken.
         *
         * @return {@code true} when items should drop, defaults to {@code false}
         */
        public boolean isDropItems() {
            return this.dropItems != null && this.dropItems;
        }

        /**
         * Indicates whether the machine item should be dropped when broken.
         *
         * @return {@code true} when machine item should drop, defaults to {@code true}
         */
        public boolean isDropMachineItem() {
            return this.dropMachineItem == null || this.dropMachineItem;
        }

        /**
         * Indicates whether only the owner can break the machine.
         *
         * @return {@code true} when only owner can break, defaults to {@code false}
         */
        public boolean isRequireOwner() {
            return this.requireOwner != null && this.requireOwner;
        }
    }
}
