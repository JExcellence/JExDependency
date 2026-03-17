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

package com.raindropcentral.rplatform.skill.impl;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuraSkills implementation of {@link com.raindropcentral.rplatform.skill.SkillBridge}.
 *
 * <p>The bridge reflects AuraSkills API classes at runtime to avoid linking against the plugin jar
 * while still supporting player skill-level requirements.</p>
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
public final class AuraSkillsSkillBridge extends AbstractReflectionSkillBridge {

    private static final Logger LOGGER = Logger.getLogger(AuraSkillsSkillBridge.class.getName());
    private static final String INTEGRATION_ID = "auraskills";
    private static final String PLUGIN_NAME = "AuraSkills";
    private static final String[] API_CLASS_NAMES = {
        "dev.aurelium.auraskills.api.AuraSkillsApi",
        "dev.aurelium.auraskills.api.AuraSkillsAPI"
    };
    private static final String[] SKILL_CLASS_NAMES = {
        "dev.aurelium.auraskills.api.skill.Skills",
        "dev.aurelium.auraskills.api.skill.Skill",
        "dev.aurelium.auraskills.api.registry.NamespacedId"
    };

    private @Nullable Plugin plugin;
    private @Nullable Object api;

    /**
     * Creates an AuraSkills bridge.
     */
    public AuraSkillsSkillBridge() {
    }

    /**
     * Gets integrationId.
     */
    @Override
    public @NotNull String getIntegrationId() {
        return INTEGRATION_ID;
    }

    /**
     * Gets pluginName.
     */
    @Override
    public @NotNull String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Returns whether available.
     */
    @Override
    public boolean isAvailable() {
        final Plugin installedPlugin = resolvePlugin(PLUGIN_NAME, "AuraSkills");
        if (installedPlugin == null || !installedPlugin.isEnabled()) {
            this.plugin = null;
            this.api = null;
            return false;
        }

        this.plugin = installedPlugin;
        if (this.api != null) {
            return true;
        }

        this.api = resolveApi(installedPlugin);
        return this.api != null;
    }

    /**
     * Gets skillLevel.
     */
    @Override
    public double getSkillLevel(@NotNull Player player, @NotNull String skillId) {
        if (!isAvailable() || skillId.isBlank()) {
            return 0.0D;
        }

        try {
            final Object rawLevel = resolveSkillLevel(player, skillId.trim());
            final Double numericLevel = asDouble(rawLevel);
            return numericLevel == null ? 0.0D : numericLevel;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve AuraSkills level for " + player.getName(), exception);
            return 0.0D;
        }
    }

    private @Nullable Object resolveApi(final @NotNull Plugin installedPlugin) {
        for (final String className : API_CLASS_NAMES) {
            final Class<?> apiClass = loadClass(installedPlugin, className);
            if (apiClass == null) {
                continue;
            }

            final Object resolvedApi = firstNonNull(
                invokeStaticOptional(apiClass, "get"),
                invokeStaticOptional(apiClass, "getInstance"),
                invokeStaticOptional(apiClass, "getApi"),
                invokeStaticOptional(apiClass, "getAPI")
            );
            if (resolvedApi != null) {
                return resolvedApi;
            }
        }

        return firstNonNull(
            invokeOptional(installedPlugin, "getApi"),
            invokeOptional(installedPlugin, "getAPI"),
            readFieldOptional(installedPlugin, "api")
        );
    }

    private @Nullable Object resolveSkillLevel(final @NotNull Player player, final @NotNull String skillId) {
        final Object resolvedApi = this.api;
        if (resolvedApi == null) {
            return null;
        }

        Object level = firstNonNull(
            invokeOptional(resolvedApi, "getSkillLevel", player, skillId),
            invokeOptional(resolvedApi, "getLevel", player, skillId),
            invokeOptional(resolvedApi, "getSkillLevel", player.getUniqueId(), skillId),
            invokeOptional(resolvedApi, "getLevel", player.getUniqueId(), skillId)
        );
        if (asDouble(level) != null) {
            return level;
        }

        final Object user = firstNonNull(
            invokeOptional(resolvedApi, "getUser", player.getUniqueId()),
            invokeOptional(resolvedApi, "getUser", player),
            invokeOptional(resolvedApi, "getProfile", player.getUniqueId()),
            invokeOptional(resolvedApi, "getProfile", player)
        );
        if (user == null) {
            return null;
        }

        level = firstNonNull(
            invokeOptional(user, "getSkillLevel", skillId),
            invokeOptional(user, "getLevel", skillId),
            invokeOptional(user, "getSkill", skillId)
        );
        if (asDouble(level) != null) {
            return level;
        }

        final Object skillObject = resolveSkillObject(skillId);
        if (skillObject != null) {
            level = firstNonNull(
                invokeOptional(user, "getSkillLevel", skillObject),
                invokeOptional(user, "getLevel", skillObject),
                invokeOptional(user, "getSkill", skillObject),
                invokeOptional(resolvedApi, "getSkillLevel", player, skillObject),
                invokeOptional(resolvedApi, "getLevel", player, skillObject)
            );
            if (asDouble(level) != null) {
                return level;
            }
        }

        final Object levelMap = firstNonNull(
            invokeOptional(user, "getSkillLevels"),
            invokeOptional(user, "getLevels"),
            readFieldOptional(user, "skillLevels"),
            readFieldOptional(user, "levels")
        );
        return resolveFromNamedMap(levelMap, skillId);
    }

    private @Nullable Object resolveSkillObject(final @NotNull String skillId) {
        final Object resolvedApi = this.api;
        if (resolvedApi == null) {
            return null;
        }

        Object skill = firstNonNull(
            invokeOptional(resolvedApi, "getSkill", skillId),
            invokeOptional(resolvedApi, "getSkillByName", skillId),
            invokeOptional(resolvedApi, "getSkillById", skillId),
            invokeOptional(resolvedApi, "getSkillByID", skillId)
        );
        if (skill != null) {
            return skill;
        }

        final Plugin installedPlugin = this.plugin;
        if (installedPlugin == null) {
            return null;
        }

        for (final String className : SKILL_CLASS_NAMES) {
            final Class<?> skillClass = loadClass(installedPlugin, className);
            if (skillClass == null) {
                continue;
            }

            skill = firstNonNull(
                resolveEnumConstant(skillClass, skillId),
                invokeStaticOptional(skillClass, "valueOf", skillId.toUpperCase(Locale.ROOT)),
                invokeStaticOptional(skillClass, "getByName", skillId),
                invokeStaticOptional(skillClass, "getById", skillId),
                invokeStaticOptional(skillClass, "of", skillId)
            );
            if (skill != null) {
                return skill;
            }

            final Object values = invokeStaticOptional(skillClass, "values");
            if (values instanceof Object[] arrayValues) {
                skill = resolveNamedEntry(Arrays.asList(arrayValues), skillId);
                if (skill != null) {
                    return skill;
                }
            }
        }

        return null;
    }
}
