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

package com.raindropcentral.rplatform.requirement.plugin;

import com.raindropcentral.rplatform.job.JobBridge;
import com.raindropcentral.rplatform.skill.SkillBridge;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for {@link PluginIntegrationBridge} instances used by {@code PLUGIN} requirements.
 *
 * <p>The registry registers built-in skill and job adapters on first access and supports both
 * direct integration lookup and category-based auto-detection.</p>
 *
 * @author ItsRainingHP, JExcellence
 * @since 2.0.0
 * @version 1.0.0
 */
public final class PluginIntegrationRegistry {

    private static final String CATEGORY_SKILLS = "SKILLS";
    private static final String CATEGORY_JOBS = "JOBS";

    private static final PluginIntegrationRegistry INSTANCE = new PluginIntegrationRegistry();

    private final Map<String, PluginIntegrationBridge> bridges = new ConcurrentHashMap<>();

    private PluginIntegrationRegistry() {
        registerBuiltInBridges();
    }

    /**
     * Gets the singleton registry instance.
     *
     * @return registry instance
     */
    public static @NotNull PluginIntegrationRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a plugin integration bridge.
     *
     * @param bridge bridge to register
     * @throws NullPointerException if {@code bridge} is {@code null}
     */
    public void registerBridge(final @NotNull PluginIntegrationBridge bridge) {
        final String integrationId = normalizeIntegrationId(bridge.getIntegrationId());
        if (integrationId.isBlank() || "auto".equals(integrationId)) {
            throw new IllegalArgumentException("Bridge integration ID must not be blank or auto");
        }
        bridges.put(integrationId, bridge);
    }

    /**
     * Unregisters a bridge by integration identifier.
     *
     * @param integrationId integration identifier to unregister
     */
    public void unregisterBridge(final @NotNull String integrationId) {
        bridges.remove(normalizeIntegrationId(integrationId));
    }

    /**
     * Gets a bridge by integration identifier.
     *
     * @param integrationId integration identifier from config
     * @return available bridge, or {@code null} when not available
     */
    public @Nullable PluginIntegrationBridge getBridge(final @Nullable String integrationId) {
        if (integrationId == null || integrationId.isBlank()) {
            return null;
        }

        final String normalizedId = normalizeIntegrationId(integrationId);
        if ("auto".equals(normalizedId)) {
            return null;
        }

        final PluginIntegrationBridge bridge = bridges.get(normalizedId);
        if (bridge == null || !bridge.isAvailable()) {
            return null;
        }

        return bridge;
    }

    /**
     * Detects the first available bridge in a category.
     *
     * @param category category identifier
     * @return first available bridge for the category, or {@code null} when none are available
     */
    public @Nullable PluginIntegrationBridge detectBridge(final @Nullable String category) {
        if (category == null || category.isBlank()) {
            return detectAnyBridge();
        }

        final String normalizedCategory = category.trim().toUpperCase(Locale.ROOT);
        final List<String> priorities = switch (normalizedCategory) {
            case CATEGORY_SKILLS -> List.of("ecoskills", "auraskills", "mcmmo");
            case CATEGORY_JOBS -> List.of("ecojobs", "jobsreborn");
            default -> List.of();
        };

        for (final String integrationId : priorities) {
            final PluginIntegrationBridge bridge = getBridge(integrationId);
            if (bridge != null) {
                return bridge;
            }
        }

        for (final PluginIntegrationBridge bridge : bridges.values()) {
            if (!normalizedCategory.equalsIgnoreCase(bridge.getCategory())) {
                continue;
            }
            if (bridge.isAvailable()) {
                return bridge;
            }
        }

        return null;
    }

    /**
     * Gets a snapshot of all registered bridges.
     *
     * @return immutable map of integration ID to bridge
     */
    public @NotNull Map<String, PluginIntegrationBridge> getBridges() {
        return Map.copyOf(bridges);
    }

    private @Nullable PluginIntegrationBridge detectAnyBridge() {
        for (final PluginIntegrationBridge bridge : bridges.values()) {
            if (bridge.isAvailable()) {
                return bridge;
            }
        }
        return null;
    }

    private void registerBuiltInBridges() {
        for (final SkillBridge skillBridge : SkillBridge.getDefaultBridges()) {
            registerBridge(new SkillBridgeAdapter(skillBridge));
        }

        for (final JobBridge jobBridge : JobBridge.getDefaultBridges()) {
            registerBridge(new JobBridgeAdapter(jobBridge));
        }
    }

    private @NotNull String normalizeIntegrationId(final @NotNull String integrationId) {
        final String normalized = integrationId.trim().toLowerCase(Locale.ROOT)
            .replace("-", "")
            .replace("_", "");

        return switch (normalized) {
            case "jobs", "job", "jobsreborn" -> "jobsreborn";
            case "ecojob", "ecojobs" -> "ecojobs";
            case "ecoskill", "ecoskills" -> "ecoskills";
            case "auraskill", "auraskills" -> "auraskills";
            case "mcmmo" -> "mcmmo";
            default -> normalized;
        };
    }

    private static final class SkillBridgeAdapter implements PluginIntegrationBridge {

        private final SkillBridge delegate;

        private SkillBridgeAdapter(final @NotNull SkillBridge delegate) {
            this.delegate = delegate;
        }

        /**
         * Gets integrationId.
         */
        @Override
        public @NotNull String getIntegrationId() {
            return delegate.getIntegrationId();
        }

        /**
         * Gets pluginName.
         */
        @Override
        public @NotNull String getPluginName() {
            return delegate.getPluginName();
        }

        /**
         * Gets category.
         */
        @Override
        public @NotNull String getCategory() {
            return CATEGORY_SKILLS;
        }

        /**
         * Returns whether available.
         */
        @Override
        public boolean isAvailable() {
            return delegate.isAvailable();
        }

        /**
         * Gets value.
         */
        @Override
        public double getValue(@NotNull Player player, @NotNull String key) {
            return delegate.getSkillLevel(player, key);
        }

        /**
         * Executes consume.
         */
        @Override
        public boolean consume(@NotNull Player player, @NotNull String key, double amount) {
            return delegate.consumeSkillLevel(player, key, amount);
        }
    }

    private static final class JobBridgeAdapter implements PluginIntegrationBridge {

        private final JobBridge delegate;

        private JobBridgeAdapter(final @NotNull JobBridge delegate) {
            this.delegate = delegate;
        }

        /**
         * Gets integrationId.
         */
        @Override
        public @NotNull String getIntegrationId() {
            return delegate.getIntegrationId();
        }

        /**
         * Gets pluginName.
         */
        @Override
        public @NotNull String getPluginName() {
            return delegate.getPluginName();
        }

        /**
         * Gets category.
         */
        @Override
        public @NotNull String getCategory() {
            return CATEGORY_JOBS;
        }

        /**
         * Returns whether available.
         */
        @Override
        public boolean isAvailable() {
            return delegate.isAvailable();
        }

        /**
         * Gets value.
         */
        @Override
        public double getValue(@NotNull Player player, @NotNull String key) {
            return delegate.getJobLevel(player, key);
        }

        /**
         * Executes consume.
         */
        @Override
        public boolean consume(@NotNull Player player, @NotNull String key, double amount) {
            return delegate.consumeJobLevel(player, key, amount);
        }
    }
}
