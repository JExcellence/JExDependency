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

package com.raindropcentral.rdq.placeholders;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rplatform.placeholder.AbstractPlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RDQ internal PlaceholderAPI expansion.
 *
 * @author RaindropCentral
 * @version 6.0.0
 */
public final class RDQPlaceholderExpansion extends AbstractPlaceholderExpansion {

    private static final Logger LOGGER = Logger.getLogger(RDQPlaceholderExpansion.class.getName());
    private static final String FALSE = "false";

    private final RDQ rdq;

    /**
     * Creates a new RDQ placeholder expansion.
     *
     * @param rdq active RDQ runtime
     * @throws NullPointerException if {@code rdq} is {@code null}
     */
    public RDQPlaceholderExpansion(final @NotNull RDQ rdq) {
        super(rdq.getPlugin());
        this.rdq = rdq;
    }

    /**
     * Returns all placeholder keys handled by this expansion.
     *
     * @return supported placeholder keys
     */
    @Override
    protected @NotNull List<String> definePlaceholders() {
        return List.of(
            "bounty_active",
            "perks_active",
            "perks_unlocked",
            "ranks_total"
        );
    }

    /**
     * Resolves one RDQ placeholder value.
     *
     * @param player online player context
     * @param params placeholder key suffix
     * @return resolved value or empty string for unknown keys
     */
    @Override
    protected @NotNull String resolvePlaceholder(
        final @Nullable Player player,
        final @NotNull String params
    ) {
        final String normalized = params.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "bounty_active" -> this.resolveBountyActive(player);
            case "perks_active" -> this.resolvePlayerPerkCount(player, true);
            case "perks_unlocked" -> this.resolvePlayerPerkCount(player, false);
            case "ranks_total" -> this.resolveTotalRanks();
            default -> "";
        };
    }

    private @NotNull String resolveBountyActive(final @Nullable Player player) {
        if (player == null || this.rdq.getBountyService() == null) {
            return FALSE;
        }

        try {
            final Bounty bounty = this.rdq.getBountyService().findPlayerBounty(player.getUniqueId()).join();
            return String.valueOf(bounty != null && bounty.isActive());
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve rdq_bounty_active", exception);
            return FALSE;
        }
    }

    private @NotNull String resolvePlayerPerkCount(
        final @Nullable Player player,
        final boolean activeOnly
    ) {
        if (player == null || this.rdq.getPlayerPerkRepository() == null) {
            return "0";
        }

        try {
            final UUID playerId = player.getUniqueId();
            final List<PlayerPerk> perks = this.rdq.getPlayerPerkRepository().findAllByPlayerIdWithPerk(playerId).join();
            final long count = activeOnly
                ? perks.stream().filter(PlayerPerk::isUnlocked).filter(PlayerPerk::isEnabled).filter(PlayerPerk::isActive).count()
                : perks.stream().filter(PlayerPerk::isUnlocked).count();
            return Long.toString(count);
        } catch (Exception exception) {
            final String key = activeOnly ? "rdq_perks_active" : "rdq_perks_unlocked";
            LOGGER.log(Level.FINE, "Failed to resolve " + key, exception);
            return "0";
        }
    }

    private @NotNull String resolveTotalRanks() {
        try {
            if (this.rdq.getRankSystemFactory() != null && this.rdq.getRankSystemFactory().isInitialized()) {
                final int totalRanks = this.rdq.getRankSystemFactory()
                    .getRanks()
                    .values()
                    .stream()
                    .mapToInt(Map::size)
                    .sum();
                return Integer.toString(totalRanks);
            }

            if (this.rdq.getRankRepository() == null) {
                return "0";
            }

            return Integer.toString(this.rdq.getRankRepository().findAll().size());
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve rdq_ranks_total", exception);
            return "0";
        }
    }
}
