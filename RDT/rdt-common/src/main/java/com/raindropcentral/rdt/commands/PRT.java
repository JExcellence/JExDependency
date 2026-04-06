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

package com.raindropcentral.rdt.commands;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rdt.view.town.TownBankView;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Primary player command for the RDT town plugin.
 *
 * <p>The command intentionally remains small and only launches GUI entry points or town spawn
 * travel. Town creation and claiming are fully view and item driven.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PRT extends PlayerCommand {

    private final RDT rdt;

    /**
     * Creates the player command handler.
     *
     * @param commandSection configured command section for {@code /rt}
     * @param rdt active plugin runtime
     */
    public PRT(final @NotNull ACommandSection commandSection, final @NotNull RDT rdt) {
        super(commandSection);
        this.rdt = rdt;
    }

    /**
     * Routes player invocations to the view-first command actions.
     *
     * @param player invoking player
     * @param alias invoked alias
     * @param args supplied arguments
     */
    @Override
    protected void onPlayerInvocation(
        final @NotNull Player player,
        final @NotNull String alias,
        final @NonNull @NotNull String[] args
    ) {
        final EPRTAction action = this.resolveAction(args);
        switch (action) {
            case SPAWN -> this.handleSpawnCommand(player);
            case BANK -> this.handleBankCommand(player);
            case MAIN -> this.handleMainCommand(player);
        }
    }

    private void handleMainCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, EPRTPermission.MAIN)) {
            return;
        }

        this.rdt.getViewFrame().open(
            TownHubView.class,
            player,
            Map.of("plugin", this.rdt)
        );
    }

    private void handleSpawnCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, EPRTPermission.SPAWN)) {
            return;
        }

        if (this.rdt.getTownSpawnService() == null || !this.rdt.getTownSpawnService().teleportToTownSpawn(player)) {
            new I18n.Builder("prt.spawn.unavailable", player)
                .includePrefix()
                .build()
                .sendMessage();
        }
    }

    private void handleBankCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, EPRTPermission.BANK)) {
            return;
        }

        final RTown town = this.rdt.getTownRuntimeService() == null
            ? null
            : this.rdt.getTownRuntimeService().getTownFor(player.getUniqueId());
        if (town == null) {
            new I18n.Builder("prt.bank.no_town", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (!town.supportsRemoteBankAccess()) {
            new I18n.Builder("prt.bank.locked", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        this.rdt.getViewFrame().open(
            TownBankView.class,
            player,
            Map.of(
                "plugin", this.rdt,
                "town_uuid", town.getTownUUID(),
                "remote_bank", true
            )
        );
    }

    /**
     * Provides tab completion for supported command actions.
     *
     * @param player invoking player
     * @param alias used alias
     * @param args current arguments
     * @return matching action suggestions
     */
    @Override
    protected List<String> onPlayerTabCompletion(
        final @NotNull Player player,
        final @NotNull String alias,
        final @NonNull @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, EPRTPermission.COMMAND)) {
            return List.of();
        }

        if (args.length == 1) {
            final List<String> suggestions = new ArrayList<>();
            if (this.hasPermission(player, EPRTPermission.MAIN)) {
                suggestions.add(EPRTAction.MAIN.name().toLowerCase(Locale.ROOT));
            }
            if (this.hasPermission(player, EPRTPermission.SPAWN)) {
                suggestions.add(EPRTAction.SPAWN.name().toLowerCase(Locale.ROOT));
            }
            if (this.hasPermission(player, EPRTPermission.BANK)) {
                suggestions.add(EPRTAction.BANK.name().toLowerCase(Locale.ROOT));
            }
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());
        }

        return List.of();
    }

    private @NotNull EPRTAction resolveAction(final @NotNull String[] args) {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            return EPRTAction.MAIN;
        }

        try {
            return EPRTAction.valueOf(args[0].trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return EPRTAction.MAIN;
        }
    }
}
