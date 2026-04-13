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

package com.raindropcentral.rda;

import com.raindropcentral.rda.database.entity.RDAPlayerBuild;
import com.raindropcentral.rda.database.repository.RRDAPlayerBuild;
import com.raindropcentral.rplatform.service.RCoreBossBarBridge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optional bridge that registers the RDA mana HUD with the shared RCore boss-bar settings
 * service.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public final class ManaBossBarIntegration {

    private static final String DISPLAY_MODE_OPTION_KEY = "display-mode";

    private final RDA rda;
    private final RRDAPlayerBuild playerBuildRepository;
    private final PlayerBuildService playerBuildService;
    private final StatsConfig statsConfig;
    private final @Nullable RCoreBossBarBridge bossBarBridge;

    /**
     * Creates the optional integration bridge.
     *
     * @param rda active RDA runtime
     * @param playerBuildRepository build repository for lazy migration
     * @param playerBuildService runtime build service used for live HUD refreshes
     * @param statsConfig loaded stats configuration
     */
    public ManaBossBarIntegration(
        final @NotNull RDA rda,
        final @NotNull RRDAPlayerBuild playerBuildRepository,
        final @NotNull PlayerBuildService playerBuildService,
        final @NotNull StatsConfig statsConfig
    ) {
        this.rda = rda;
        this.playerBuildRepository = playerBuildRepository;
        this.playerBuildService = playerBuildService;
        this.statsConfig = statsConfig;
        this.bossBarBridge = RCoreBossBarBridge.create();
    }

    /**
     * Registers the RDA mana HUD as an RCore-managed provider.
     *
     * @return {@code true} when the provider was registered
     */
    public boolean register() {
        if (this.bossBarBridge == null) {
            return false;
        }

        this.bossBarBridge.registerProvider(new RCoreBossBarBridge.ProviderRegistration(
            RDA.MANA_BOSS_BAR_PROVIDER_KEY,
            Material.HEART_OF_THE_SEA,
            "rda_boss_bar_provider.mana.name",
            "rda_boss_bar_provider.mana.description",
            true,
            List.of(new RCoreBossBarBridge.ProviderOption(
                DISPLAY_MODE_OPTION_KEY,
                "rda_boss_bar_provider.mana.option.display_mode.name",
                "rda_boss_bar_provider.mana.option.display_mode.description",
                this.statsConfig.getManaSettings().defaultDisplayMode().name(),
                List.of(
                    new RCoreBossBarBridge.ProviderOptionChoice(
                        ManaDisplayMode.ACTION_BAR.name(),
                        ManaDisplayMode.ACTION_BAR.getTranslationKey(),
                        null
                    ),
                    new RCoreBossBarBridge.ProviderOptionChoice(
                        ManaDisplayMode.BOSS_BAR.name(),
                        ManaDisplayMode.BOSS_BAR.getTranslationKey(),
                        null
                    ),
                    new RCoreBossBarBridge.ProviderOptionChoice(
                        ManaDisplayMode.ALWAYS_VISIBLE.name(),
                        ManaDisplayMode.ALWAYS_VISIBLE.getTranslationKey(),
                        null
                    ),
                    new RCoreBossBarBridge.ProviderOptionChoice(
                        ManaDisplayMode.MENUS_ONLY.name(),
                        ManaDisplayMode.MENUS_ONLY.getTranslationKey(),
                        null
                    )
                )
            )),
            (playerUuid, providerKey) -> this.resolveLegacyPreference(playerUuid),
            (playerUuid, preferenceSnapshot) -> this.handlePreferenceChange(playerUuid, preferenceSnapshot.enabled())
        ));
        return true;
    }

    /**
     * Unregisters the RDA provider from RCore.
     */
    public void unregister() {
        if (this.bossBarBridge != null) {
            this.bossBarBridge.unregisterProvider(RDA.MANA_BOSS_BAR_PROVIDER_KEY);
        }
    }

    /**
     * Returns whether the mana HUD is enabled for one player.
     *
     * @param playerUuid player UUID to inspect
     * @return {@code true} when enabled
     */
    public boolean isEnabled(final @NotNull UUID playerUuid) {
        return this.bossBarBridge != null
            && this.bossBarBridge.resolvePreferences(playerUuid, RDA.MANA_BOSS_BAR_PROVIDER_KEY).enabled();
    }

    /**
     * Resolves the centralized mana display mode for one player.
     *
     * @param playerUuid player UUID to inspect
     * @param playerBuild optional already-loaded player build row used as a legacy fallback
     * @return resolved mana display mode
     */
    public @NotNull ManaDisplayMode resolveDisplayMode(
        final @NotNull UUID playerUuid,
        final @Nullable RDAPlayerBuild playerBuild
    ) {
        if (this.bossBarBridge == null) {
            return this.resolveLegacyDisplayMode(playerBuild);
        }

        final RCoreBossBarBridge.PreferenceSnapshot snapshot = this.bossBarBridge.resolvePreferences(
            playerUuid,
            RDA.MANA_BOSS_BAR_PROVIDER_KEY
        );
        try {
            return ManaDisplayMode.valueOf(
                snapshot.options().getOrDefault(DISPLAY_MODE_OPTION_KEY, this.statsConfig.getManaSettings().defaultDisplayMode().name())
            );
        } catch (final IllegalArgumentException exception) {
            return this.statsConfig.getManaSettings().defaultDisplayMode();
        }
    }

    /**
     * Cycles the centralized mana display mode for one player.
     *
     * @param player target player
     * @param currentMode player's current resolved display mode
     * @return resulting display mode
     */
    public @NotNull ManaDisplayMode cycleDisplayMode(
        final @NotNull Player player,
        final @NotNull ManaDisplayMode currentMode
    ) {
        if (this.bossBarBridge == null) {
            return currentMode;
        }

        final ManaDisplayMode[] values = ManaDisplayMode.values();
        final ManaDisplayMode nextMode = values[(currentMode.ordinal() + 1) % values.length];
        this.bossBarBridge.setOption(
            player.getUniqueId(),
            RDA.MANA_BOSS_BAR_PROVIDER_KEY,
            DISPLAY_MODE_OPTION_KEY,
            nextMode.name()
        );
        return nextMode;
    }

    /**
     * Opens the RCore-managed provider settings view for one player.
     *
     * @param player target player
     */
    public void openSettings(final @NotNull Player player) {
        if (this.bossBarBridge != null) {
            this.bossBarBridge.openSettingsView(player, RDA.MANA_BOSS_BAR_PROVIDER_KEY);
        }
    }

    private @Nullable RCoreBossBarBridge.PreferenceSeed resolveLegacyPreference(final @NotNull UUID playerUuid) {
        final RDAPlayerBuild playerBuild = this.playerBuildRepository.findByPlayer(playerUuid);
        if (playerBuild == null) {
            return null;
        }

        return new RCoreBossBarBridge.PreferenceSeed(
            true,
            Map.of(DISPLAY_MODE_OPTION_KEY, this.resolveLegacyDisplayMode(playerBuild).name())
        );
    }

    private @NotNull ManaDisplayMode resolveLegacyDisplayMode(final @Nullable RDAPlayerBuild playerBuild) {
        if (playerBuild == null) {
            return this.statsConfig.getManaSettings().defaultDisplayMode();
        }

        try {
            return ManaDisplayMode.valueOf(playerBuild.getManaDisplayMode().toUpperCase(java.util.Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return this.statsConfig.getManaSettings().defaultDisplayMode();
        }
    }

    private void handlePreferenceChange(final @NotNull UUID playerUuid, final boolean enabled) {
        final Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return;
        }

        if (enabled) {
            this.playerBuildService.refreshHud(player);
            return;
        }
        this.playerBuildService.clearHud(player);
    }
}
