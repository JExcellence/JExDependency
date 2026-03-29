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

import com.raindropcentral.rplatform.skill.SkillBridge;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * mcMMO implementation of {@link com.raindropcentral.rplatform.skill.SkillBridge}.
 *
 * <p>This bridge uses reflection against mcMMO's public static API classes to read primary skill
 * levels when available.</p>
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
public final class McMMOSkillBridge extends AbstractReflectionSkillBridge {

    private static final Logger LOGGER = Logger.getLogger(McMMOSkillBridge.class.getName());
    private static final String INTEGRATION_ID = "mcmmo";
    private static final String PLUGIN_NAME = "mcMMO";

    private static final String[] EXPERIENCE_API_CLASS_NAMES = {
        "com.gmail.nossr50.api.ExperienceAPI"
    };
    private static final String[] SKILL_TYPE_CLASS_NAMES = {
        "com.gmail.nossr50.datatypes.skills.PrimarySkillType",
        "com.gmail.nossr50.datatypes.skills.SkillType"
    };
    private static final String[] USER_MANAGER_CLASS_NAMES = {
        "com.gmail.nossr50.util.player.UserManager"
    };

    private @Nullable Plugin plugin;
    private @Nullable Class<?> experienceApiClass;
    private @Nullable Class<?> skillTypeClass;
    private @Nullable Class<?> userManagerClass;

    /**
     * Creates an mcMMO bridge.
     */
    public McMMOSkillBridge() {
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull String getIntegrationId() {
        return INTEGRATION_ID;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull String getPluginName() {
        return PLUGIN_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailable() {
        final Plugin installedPlugin = resolvePlugin(PLUGIN_NAME, "mcMMO");
        if (installedPlugin == null || !installedPlugin.isEnabled()) {
            this.plugin = null;
            this.experienceApiClass = null;
            this.skillTypeClass = null;
            this.userManagerClass = null;
            return false;
        }

        this.plugin = installedPlugin;
        if (this.experienceApiClass != null && this.skillTypeClass != null) {
            return true;
        }

        this.experienceApiClass = resolveClass(installedPlugin, EXPERIENCE_API_CLASS_NAMES);
        this.skillTypeClass = resolveClass(installedPlugin, SKILL_TYPE_CLASS_NAMES);
        this.userManagerClass = resolveClass(installedPlugin, USER_MANAGER_CLASS_NAMES);

        return this.experienceApiClass != null && this.skillTypeClass != null;
    }

    /** {@inheritDoc} */
    @Override
    public double getSkillLevel(@NotNull Player player, @NotNull String skillId) {
        if (!isAvailable() || skillId.isBlank()) {
            return 0.0D;
        }

        try {
            final String trimmedSkillId = skillId.trim();
            final Object resolvedSkillType = resolveEnumConstant(this.skillTypeClass, trimmedSkillId);

            Object level = null;
            if (resolvedSkillType != null) {
                level = firstNonNull(
                    invokeStaticOptional(this.experienceApiClass, "getLevel", player, resolvedSkillType),
                    invokeStaticOptional(this.experienceApiClass, "getSkillLevel", player, resolvedSkillType),
                    invokeStaticOptional(this.experienceApiClass, "getLevel", player.getUniqueId(), resolvedSkillType),
                    invokeStaticOptional(this.experienceApiClass, "getSkillLevel", player.getUniqueId(), resolvedSkillType)
                );
            }

            if (asDouble(level) != null) {
                return asDouble(level);
            }

            level = firstNonNull(
                invokeStaticOptional(this.experienceApiClass, "getLevel", player, trimmedSkillId),
                invokeStaticOptional(this.experienceApiClass, "getSkillLevel", player, trimmedSkillId),
                invokeStaticOptional(this.experienceApiClass, "getLevel", player.getUniqueId(), trimmedSkillId),
                invokeStaticOptional(this.experienceApiClass, "getSkillLevel", player.getUniqueId(), trimmedSkillId)
            );
            if (asDouble(level) != null) {
                return asDouble(level);
            }

            final Object user = resolveMcMMOUser(player);
            if (user == null) {
                return 0.0D;
            }

            if (resolvedSkillType != null) {
                level = firstNonNull(
                    invokeOptional(user, "getSkillLevel", resolvedSkillType),
                    invokeOptional(user, "getSkillXpLevel", resolvedSkillType),
                    invokeOptional(user, "getSkill", resolvedSkillType)
                );
            } else {
                level = firstNonNull(
                    invokeOptional(user, "getSkillLevel", trimmedSkillId),
                    invokeOptional(user, "getSkillXpLevel", trimmedSkillId),
                    invokeOptional(user, "getSkill", trimmedSkillId)
                );
            }

            final Double numericLevel = asDouble(level);
            return numericLevel == null ? 0.0D : numericLevel;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve mcMMO level for " + player.getName(), exception);
            return 0.0D;
        }
    }

    @Override
    public @NotNull List<SkillBridge.SkillDescriptor> getAvailableSkills(@NotNull Player player) {
        if (!isAvailable() || this.skillTypeClass == null) {
            return List.of();
        }

        final Object[] constants = this.skillTypeClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return List.of();
        }

        final List<SkillBridge.SkillDescriptor> skills = new ArrayList<>();
        for (final Object constant : constants) {
            if (constant == null) {
                continue;
            }

            final String skillId = constant.toString();
            skills.add(new SkillBridge.SkillDescriptor(
                    this.getIntegrationId(),
                    this.getPluginName(),
                    skillId,
                    titleCase(skillId)
            ));
        }

        return skills.stream()
                .sorted(Comparator.comparing(SkillBridge.SkillDescriptor::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public boolean addSkillLevels(@NotNull Player player, @NotNull String skillId, int amount) {
        if (!isAvailable() || skillId.isBlank() || amount <= 0) {
            return false;
        }

        try {
            final String trimmedSkillId = skillId.trim();
            final double previousLevel = getSkillLevel(player, trimmedSkillId);
            final Object resolvedSkillType = resolveEnumConstant(this.skillTypeClass, trimmedSkillId);
            final Object user = resolveMcMMOUser(player);

            if (this.tryAddLevels(player, user, resolvedSkillType, trimmedSkillId, amount)) {
                return getSkillLevel(player, trimmedSkillId) >= previousLevel + amount;
            }
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to add mcMMO levels for " + player.getName(), exception);
        }

        return false;
    }

    private @Nullable Object resolveMcMMOUser(final @NotNull Player player) {
        if (this.userManagerClass == null) {
            return null;
        }

        return firstNonNull(
            invokeStaticOptional(this.userManagerClass, "getPlayer", player),
            invokeStaticOptional(this.userManagerClass, "getPlayer", player.getUniqueId()),
            invokeStaticOptional(this.userManagerClass, "getOfflinePlayer", player.getUniqueId())
        );
    }

    private boolean tryAddLevels(
            final @NotNull Player player,
            final @Nullable Object user,
            final @Nullable Object resolvedSkillType,
            final @NotNull String skillId,
            final int amount
    ) {
        final List<InvocationAttempt> attempts = new ArrayList<>();
        if (resolvedSkillType != null) {
            attempts.add(invokeStaticOptionalAttempt(this.experienceApiClass, "addLevels", player, resolvedSkillType, amount));
            attempts.add(invokeStaticOptionalAttempt(this.experienceApiClass, "addLevel", player, resolvedSkillType, amount));
            attempts.add(invokeStaticOptionalAttempt(this.experienceApiClass, "addLevels", player.getUniqueId(), resolvedSkillType, amount));
            attempts.add(invokeStaticOptionalAttempt(this.experienceApiClass, "addLevel", player.getUniqueId(), resolvedSkillType, amount));
            attempts.add(invokeOptionalAttempt(user, "addLevels", resolvedSkillType, amount));
            attempts.add(invokeOptionalAttempt(user, "addLevel", resolvedSkillType, amount));
        }

        attempts.add(invokeStaticOptionalAttempt(this.experienceApiClass, "addLevels", player, skillId, amount));
        attempts.add(invokeStaticOptionalAttempt(this.experienceApiClass, "addLevel", player, skillId, amount));
        attempts.add(invokeStaticOptionalAttempt(this.experienceApiClass, "addLevels", player.getUniqueId(), skillId, amount));
        attempts.add(invokeStaticOptionalAttempt(this.experienceApiClass, "addLevel", player.getUniqueId(), skillId, amount));
        attempts.add(invokeOptionalAttempt(user, "addLevels", skillId, amount));
        attempts.add(invokeOptionalAttempt(user, "addLevel", skillId, amount));

        for (final InvocationAttempt attempt : attempts) {
            if (!attempt.invoked()) {
                continue;
            }
            final Boolean booleanResult = asBoolean(attempt.value());
            if (booleanResult != null) {
                return booleanResult;
            }
            return true;
        }

        return false;
    }

    private @Nullable Class<?> resolveClass(final @NotNull Plugin installedPlugin, final @NotNull String[] candidates) {
        for (final String className : candidates) {
            final Class<?> resolvedClass = loadClass(installedPlugin, className);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return null;
    }
}
