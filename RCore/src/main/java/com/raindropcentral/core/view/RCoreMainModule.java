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

package com.raindropcentral.core.view;

import com.raindropcentral.core.config.RCoreMainMenuConfig;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Internal module descriptor used by the {@code /rc main} hub.
 */
enum RCoreMainModule {

    RDA("RDA", "RDA", "pra", "main", "module.rda.name", "module.rda.description"),
    RDQ("RDQ", "RDQ", "prq", "main", "module.rdq.name", "module.rdq.description"),
    RDR("RDR", "RDR", "prr", "storage", "module.rdr.name", "module.rdr.description"),
    RDS("RDS", "RDS", "prs", "search", "module.rds.name", "module.rds.description"),
    RDT("RDT", "RDT", "prt", "main", "module.rdt.name", "module.rdt.description");

    private final String moduleId;
    private final String pluginName;
    private final String commandLabel;
    private final String commandArguments;
    private final String nameTranslationKey;
    private final String descriptionTranslationKey;

    RCoreMainModule(
        final @NotNull String moduleId,
        final @NotNull String pluginName,
        final @NotNull String commandLabel,
        final @NotNull String commandArguments,
        final @NotNull String nameTranslationKey,
        final @NotNull String descriptionTranslationKey
    ) {
        this.moduleId = moduleId;
        this.pluginName = pluginName;
        this.commandLabel = commandLabel;
        this.commandArguments = commandArguments;
        this.nameTranslationKey = nameTranslationKey;
        this.descriptionTranslationKey = descriptionTranslationKey;
    }

    static @NotNull List<RCoreMainModule> orderedModules(final @NotNull RCoreMainMenuConfig config) {
        final ArrayList<RCoreMainModule> modules = new ArrayList<>();
        for (final String moduleId : config.getPlacementOrder()) {
            final RCoreMainModule module = fromId(moduleId);
            if (module != null) {
                modules.add(module);
            }
        }
        return List.copyOf(modules);
    }

    static @Nullable RCoreMainModule fromId(final String rawModuleId) {
        if (rawModuleId == null || rawModuleId.isBlank()) {
            return null;
        }

        final String normalizedModuleId = rawModuleId.trim().toUpperCase(Locale.ROOT);
        for (final RCoreMainModule module : values()) {
            if (module.moduleId.equals(normalizedModuleId)) {
                return module;
            }
        }
        return null;
    }

    boolean isAvailable(final @NotNull Server server) {
        final Plugin plugin = server.getPluginManager().getPlugin(this.pluginName);
        return plugin != null && plugin.isEnabled();
    }

    boolean openForPlayer(final @NotNull Player player) {
        return player.performCommand(this.commandLine());
    }

    @NotNull String moduleId() {
        return this.moduleId;
    }

    @NotNull String pluginName() {
        return this.pluginName;
    }

    @NotNull String commandLabel() {
        return this.commandLabel;
    }

    @NotNull String commandArguments() {
        return this.commandArguments;
    }

    @NotNull String commandLine() {
        return this.commandLabel + " " + this.commandArguments;
    }

    @NotNull String nameTranslationKey() {
        return this.nameTranslationKey;
    }

    @NotNull String descriptionTranslationKey() {
        return this.descriptionTranslationKey;
    }
}
