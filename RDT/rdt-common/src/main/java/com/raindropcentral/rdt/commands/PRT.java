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
import com.raindropcentral.rdt.view.town.TownBankRootView;
import com.raindropcentral.rdt.view.town.TownBankStorageView;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Primary {@code /rt} command root for the RDT town plugin.
 *
 * <p>Players can open the main town views or trigger spawn travel, while the server console can
 * dispatch the internal {@code broadcast} route used by config-driven reward execution.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PRT extends PlayerCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

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
     * Routes console senders to the broadcast subcommand and keeps all other routes player-only.
     *
     * @param sender invoking command sender
     * @param alias invoked alias
     * @param args supplied arguments
     */
    @Override
    protected void onInvocation(
        final @NotNull CommandSender sender,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        if (sender instanceof ConsoleCommandSender console) {
            if (this.resolveAction(args) != EPRTAction.BROADCAST) {
                throw new CommandError(null, EErrorType.NOT_A_PLAYER);
            }
            this.handleBroadcastCommand(console, alias, args);
            return;
        }

        super.onInvocation(sender, alias, args);
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
            case BROADCAST -> throw new CommandError(null, EErrorType.NOT_A_CONSOLE);
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

        if (this.rdt.getTownBankService() == null) {
            new I18n.Builder("prt.bank.locked", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final var access = this.rdt.getTownBankService().resolveCommandAccess(player);
        switch (access.status()) {
            case NO_TOWN -> new I18n.Builder("prt.bank.no_town", player)
                .includePrefix()
                .build()
                .sendMessage();
            case NO_VIEW_PERMISSION, NO_REMOTE_PERMISSION -> new I18n.Builder("prt.bank.no_permission", player)
                .includePrefix()
                .build()
                .sendMessage();
            case LOCKED, INVALID_TARGET -> new I18n.Builder("prt.bank.locked", player)
                .includePrefix()
                .build()
                .sendMessage();
            case LOCATION_REQUIRED -> new I18n.Builder("prt.bank.location_required", player)
                .includePrefix()
                .build()
                .sendMessage();
            case CACHE_UNAVAILABLE -> new I18n.Builder("prt.bank.cache_unavailable", player)
                .includePrefix()
                .build()
                .sendMessage();
            case LOCAL_BANK -> this.rdt.getViewFrame().open(
                TownBankRootView.class,
                player,
                Map.of(
                    "plugin", this.rdt,
                    "town_uuid", access.town().getTownUUID(),
                    "world_name", access.localChunk() == null ? player.getWorld().getName() : access.localChunk().getWorldName(),
                    "chunk_x", access.localChunk() == null ? player.getLocation().getChunk().getX() : access.localChunk().getX(),
                    "chunk_z", access.localChunk() == null ? player.getLocation().getChunk().getZ() : access.localChunk().getZ()
                )
            );
            case REMOTE_CACHE_DEPOSIT -> this.rdt.getViewFrame().open(
                TownBankStorageView.class,
                player,
                Map.of(
                    "plugin", this.rdt,
                    "town_uuid", access.town().getTownUUID(),
                    "bank_storage_mode", "REMOTE_CACHE_DEPOSIT"
                )
            );
        }
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
            if (this.hasPermission(player, EPRTPermission.BANK)
                && this.rdt.getTownBankService() != null
                && this.rdt.getTownBankService().isBankCommandVisible(player)) {
                suggestions.add(EPRTAction.BANK.name().toLowerCase(Locale.ROOT));
            }
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());
        }

        return List.of();
    }

    private void handleBroadcastCommand(
        final @NotNull ConsoleCommandSender console,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        if (args.length < 3) {
            console.sendMessage("Usage: /" + alias + " broadcast <town_uuid> <mini_message>");
            return;
        }

        final UUID townUuid;
        try {
            townUuid = UUID.fromString(args[1]);
        } catch (final IllegalArgumentException exception) {
            console.sendMessage("Invalid town UUID: " + args[1]);
            return;
        }

        final RTown town = this.rdt.getTownRuntimeService() == null
            ? null
            : this.rdt.getTownRuntimeService().getTown(townUuid);
        if (town == null) {
            console.sendMessage("Town not found: " + townUuid);
            return;
        }

        final String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        final Component broadcastMessage = MINI_MESSAGE.deserialize(message);
        final Server server = this.rdt.getServer();
        int delivered = 0;
        for (final var member : town.getMembers()) {
            final Player onlinePlayer = server.getPlayer(member.getIdentifier());
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                continue;
            }
            onlinePlayer.sendMessage(broadcastMessage);
            delivered++;
        }

        console.sendMessage(
            "Sent town broadcast to " + delivered + " online member(s) of " + town.getTownName() + '.'
        );
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
