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
import com.raindropcentral.rdt.service.ServerBankService;
import com.raindropcentral.rdt.service.TaxRuntimeService;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rdt.view.town.ServerBankRootView;
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
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Primary {@code /rt} command root for the RDT town plugin.
 *
 * <p>Players can open town, nation, and admin bank views, while console/admin routes expose tax
 * runtime status and server-bank maintenance commands.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PRT extends PlayerCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final DateTimeFormatter COMMAND_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss z",
        Locale.US
    );

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
     * Routes console senders to the supported admin/internal command actions and keeps other
     * actions player-only.
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
            switch (this.resolveAction(args)) {
                case BROADCAST -> this.handleBroadcastCommand(console, alias, args);
                case TAX -> this.handleTaxConsoleCommand(console, alias, args);
                case SERVERBANK -> this.handleServerBankConsoleCommand(console, alias, args);
                default -> throw new CommandError(null, EErrorType.NOT_A_PLAYER);
            }
            return;
        }

        super.onInvocation(sender, alias, args);
    }

    /**
     * Routes player invocations to the view-first command actions and admin utilities.
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
            case FOB -> this.handleFobCommand(player);
            case BANK -> this.handleBankCommand(player);
            case SERVERBANK -> this.handleServerBankPlayerCommand(player, args);
            case TAX -> this.handleTaxPlayerCommand(player, args);
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

    private void handleFobCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, EPRTPermission.FOB)) {
            return;
        }

        final var runtimeService = this.rdt.getTownRuntimeService();
        final RTown town = runtimeService == null ? null : runtimeService.getTownFor(player.getUniqueId());
        if (town == null) {
            new I18n.Builder("prt.fob.no_town", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!runtimeService.hasTownPermission(player, town, TownPermissions.USE_FOB)) {
            new I18n.Builder("prt.fob.no_permission", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (this.rdt.getTownFobService() == null || !this.rdt.getTownFobService().teleportToTownFob(player)) {
            new I18n.Builder("prt.fob.unavailable", player)
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

    private void handleServerBankPlayerCommand(final @NotNull Player player, final @NotNull String[] args) {
        if (this.hasNoPermission(player, EPRTPermission.SERVERBANK)) {
            return;
        }

        final ServerBankService serverBankService = this.rdt.getServerBankService();
        if (serverBankService == null) {
            new I18n.Builder("prt.serverbank.unavailable", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (args.length <= 1) {
            this.rdt.getViewFrame().open(
                ServerBankRootView.class,
                player,
                Map.of("plugin", this.rdt)
            );
            return;
        }

        final String route = args[1].trim().toLowerCase(Locale.ROOT);
        switch (route) {
            case "balance" -> this.handleServerBankBalancePlayer(player, args, serverBankService);
            case "set" -> this.handleServerBankSetPlayer(player, args, serverBankService);
            case "add" -> this.handleServerBankAddPlayer(player, args, serverBankService);
            case "take" -> this.handleServerBankTakePlayer(player, args, serverBankService);
            default -> this.sendPlayerSyntax(player, "prt.serverbank.syntax");
        }
    }

    private void handleTaxPlayerCommand(final @NotNull Player player, final @NotNull String[] args) {
        if (this.hasNoPermission(player, EPRTPermission.TAX)) {
            return;
        }

        final TaxRuntimeService taxRuntimeService = this.rdt.getTaxRuntimeService();
        if (taxRuntimeService == null) {
            new I18n.Builder("prt.tax.unavailable", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final String route = args.length <= 1 ? "status" : args[1].trim().toLowerCase(Locale.ROOT);
        switch (route) {
            case "status" -> this.sendTaxStatus(player, taxRuntimeService.getStatusSnapshot());
            case "run", "run-now" -> {
                final boolean success = taxRuntimeService.collectTaxesNow();
                new I18n.Builder(success ? "prt.tax.run.success" : "prt.tax.run.failed", player)
                    .includePrefix()
                    .withPlaceholder("completed_at", this.formatInstant(Instant.now()))
                    .build()
                    .sendMessage();
            }
            default -> this.sendPlayerSyntax(player, "prt.tax.syntax");
        }
    }

    /**
     * Provides tab completion for supported command actions and admin subcommands.
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
            if (this.hasPermission(player, EPRTPermission.FOB)) {
                suggestions.add(EPRTAction.FOB.name().toLowerCase(Locale.ROOT));
            }
            if (this.hasPermission(player, EPRTPermission.BANK)
                && this.rdt.getTownBankService() != null
                && this.rdt.getTownBankService().isBankCommandVisible(player)) {
                suggestions.add(EPRTAction.BANK.name().toLowerCase(Locale.ROOT));
            }
            if (this.hasPermission(player, EPRTPermission.SERVERBANK) && this.rdt.getServerBankService() != null) {
                suggestions.add(EPRTAction.SERVERBANK.name().toLowerCase(Locale.ROOT));
            }
            if (this.hasPermission(player, EPRTPermission.TAX) && this.rdt.getTaxRuntimeService() != null) {
                suggestions.add(EPRTAction.TAX.name().toLowerCase(Locale.ROOT));
            }
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());
        }

        if (args.length == 2 && "tax".equalsIgnoreCase(args[0]) && this.hasPermission(player, EPRTPermission.TAX)) {
            return StringUtil.copyPartialMatches(args[1], List.of("status", "run"), new ArrayList<>());
        }

        if (args.length == 2
            && "serverbank".equalsIgnoreCase(args[0])
            && this.hasPermission(player, EPRTPermission.SERVERBANK)) {
            return StringUtil.copyPartialMatches(args[1], List.of("balance", "set", "add", "take"), new ArrayList<>());
        }

        if (args.length == 3
            && "serverbank".equalsIgnoreCase(args[0])
            && this.hasPermission(player, EPRTPermission.SERVERBANK)
            && this.rdt.getServerBankService() != null) {
            return StringUtil.copyPartialMatches(
                args[2],
                this.rdt.getServerBankService().getConfiguredCurrencies(),
                new ArrayList<>()
            );
        }

        if (args.length == 4
            && "serverbank".equalsIgnoreCase(args[0])
            && List.of("set", "add", "take").contains(args[1].trim().toLowerCase(Locale.ROOT))
            && this.hasPermission(player, EPRTPermission.SERVERBANK)) {
            return StringUtil.copyPartialMatches(args[3], List.of("0", "100", "1000"), new ArrayList<>());
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

    private void handleTaxConsoleCommand(
        final @NotNull ConsoleCommandSender console,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        final TaxRuntimeService taxRuntimeService = this.rdt.getTaxRuntimeService();
        if (taxRuntimeService == null) {
            console.sendMessage("RDT tax runtime is unavailable.");
            return;
        }

        final String route = args.length <= 1 ? "status" : args[1].trim().toLowerCase(Locale.ROOT);
        switch (route) {
            case "status" -> {
                final TaxRuntimeService.TaxStatusSnapshot snapshot = taxRuntimeService.getStatusSnapshot();
                console.sendMessage("RDT tax runtime: " + (snapshot.running() ? "running" : "stopped"));
                console.sendMessage("Last collection: " + this.formatInstant(snapshot.lastCollectionAt()));
                console.sendMessage("Next collection: " + this.formatInstant(snapshot.nextScheduledRunAt()));
                console.sendMessage(
                    "Active towns: " + snapshot.activeTownCount()
                        + ", active nations: " + snapshot.activeNationCount()
                        + ", towns in debt: " + snapshot.townsInDebt()
                        + ", nations in debt: " + snapshot.nationsInDebt()
                );
            }
            case "run", "run-now" -> {
                final boolean success = taxRuntimeService.collectTaxesNow();
                console.sendMessage(
                    success
                        ? "RDT tax collection completed at " + this.formatInstant(Instant.now()) + '.'
                        : "RDT tax collection could not be completed."
                );
            }
            default -> console.sendMessage("Usage: /" + alias + " tax <status|run>");
        }
    }

    private void handleServerBankConsoleCommand(
        final @NotNull ConsoleCommandSender console,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        final ServerBankService serverBankService = this.rdt.getServerBankService();
        if (serverBankService == null) {
            console.sendMessage("RDT server bank is unavailable.");
            return;
        }
        if (args.length <= 1) {
            console.sendMessage("Usage: /" + alias + " serverbank <balance|set|add|take> <currency> [amount]");
            return;
        }

        final String route = args[1].trim().toLowerCase(Locale.ROOT);
        switch (route) {
            case "balance" -> {
                if (args.length != 3) {
                    console.sendMessage("Usage: /" + alias + " serverbank balance <currency>");
                    return;
                }
                console.sendMessage(
                    "Server bank balance for "
                        + args[2]
                        + ": "
                        + serverBankService.getCurrencyBalance(args[2])
                );
            }
            case "set" -> {
                final Double amount = this.parseNonNegativeAmount(args, 3);
                if (args.length != 4 || amount == null) {
                    console.sendMessage("Usage: /" + alias + " serverbank set <currency> <amount>");
                    return;
                }
                final double newBalance = serverBankService.setCurrencyBalance(args[2], amount);
                console.sendMessage("Server bank balance for " + args[2] + " set to " + newBalance + '.');
            }
            case "add" -> {
                final Double amount = this.parsePositiveAmount(args, 3);
                if (args.length != 4 || amount == null) {
                    console.sendMessage("Usage: /" + alias + " serverbank add <currency> <amount>");
                    return;
                }
                final double newBalance = serverBankService.addCurrency(args[2], amount);
                console.sendMessage("Added " + amount + ' ' + args[2] + ". New balance: " + newBalance + '.');
            }
            case "take" -> {
                final Double amount = this.parsePositiveAmount(args, 3);
                if (args.length != 4 || amount == null) {
                    console.sendMessage("Usage: /" + alias + " serverbank take <currency> <amount>");
                    return;
                }
                if (!serverBankService.takeCurrency(args[2], amount)) {
                    console.sendMessage("Server bank does not have enough " + args[2] + " to take " + amount + '.');
                    return;
                }
                console.sendMessage(
                    "Removed " + amount + ' ' + args[2] + ". New balance: " + serverBankService.getCurrencyBalance(args[2]) + '.'
                );
            }
            default -> console.sendMessage("Usage: /" + alias + " serverbank <balance|set|add|take> <currency> [amount]");
        }
    }

    private void handleServerBankBalancePlayer(
        final @NotNull Player player,
        final @NotNull String[] args,
        final @NotNull ServerBankService serverBankService
    ) {
        if (args.length != 3) {
            this.sendPlayerSyntax(player, "prt.serverbank.syntax");
            return;
        }

        new I18n.Builder("prt.serverbank.balance", player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "currency", serverBankService.resolveCurrencyDisplayName(args[2]),
                "server_balance", serverBankService.getCurrencyBalance(args[2])
            ))
            .build()
            .sendMessage();
    }

    private void handleServerBankSetPlayer(
        final @NotNull Player player,
        final @NotNull String[] args,
        final @NotNull ServerBankService serverBankService
    ) {
        final Double amount = this.parseNonNegativeAmount(args, 3);
        if (args.length != 4 || amount == null) {
            this.sendPlayerSyntax(player, "prt.serverbank.syntax");
            return;
        }

        final double newBalance = serverBankService.setCurrencyBalance(args[2], amount);
        new I18n.Builder("prt.serverbank.set", player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "currency", serverBankService.resolveCurrencyDisplayName(args[2]),
                "amount", amount,
                "server_balance", newBalance
            ))
            .build()
            .sendMessage();
    }

    private void handleServerBankAddPlayer(
        final @NotNull Player player,
        final @NotNull String[] args,
        final @NotNull ServerBankService serverBankService
    ) {
        final Double amount = this.parsePositiveAmount(args, 3);
        if (args.length != 4 || amount == null) {
            this.sendPlayerSyntax(player, "prt.serverbank.syntax");
            return;
        }

        final double newBalance = serverBankService.addCurrency(args[2], amount);
        new I18n.Builder("prt.serverbank.add", player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "currency", serverBankService.resolveCurrencyDisplayName(args[2]),
                "amount", amount,
                "server_balance", newBalance
            ))
            .build()
            .sendMessage();
    }

    private void handleServerBankTakePlayer(
        final @NotNull Player player,
        final @NotNull String[] args,
        final @NotNull ServerBankService serverBankService
    ) {
        final Double amount = this.parsePositiveAmount(args, 3);
        if (args.length != 4 || amount == null) {
            this.sendPlayerSyntax(player, "prt.serverbank.syntax");
            return;
        }
        if (!serverBankService.takeCurrency(args[2], amount)) {
            new I18n.Builder("prt.serverbank.not_enough", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                    "currency", serverBankService.resolveCurrencyDisplayName(args[2]),
                    "amount", amount
                ))
                .build()
                .sendMessage();
            return;
        }

        new I18n.Builder("prt.serverbank.take", player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "currency", serverBankService.resolveCurrencyDisplayName(args[2]),
                "amount", amount,
                "server_balance", serverBankService.getCurrencyBalance(args[2])
            ))
            .build()
            .sendMessage();
    }

    private void sendTaxStatus(
        final @NotNull Player player,
        final @NotNull TaxRuntimeService.TaxStatusSnapshot snapshot
    ) {
        new I18n.Builder("prt.tax.status.runtime", player)
            .includePrefix()
            .withPlaceholder("running_state", snapshot.running() ? "Running" : "Stopped")
            .build()
            .sendMessage();
        new I18n.Builder("prt.tax.status.last_collection", player)
            .includePrefix()
            .withPlaceholder("last_collection_at", this.formatInstant(snapshot.lastCollectionAt()))
            .build()
            .sendMessage();
        new I18n.Builder("prt.tax.status.next_collection", player)
            .includePrefix()
            .withPlaceholder("next_collection_at", this.formatInstant(snapshot.nextScheduledRunAt()))
            .build()
            .sendMessage();
        new I18n.Builder("prt.tax.status.counts", player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "active_towns", snapshot.activeTownCount(),
                "active_nations", snapshot.activeNationCount(),
                "towns_in_debt", snapshot.townsInDebt(),
                "nations_in_debt", snapshot.nationsInDebt()
            ))
            .build()
            .sendMessage();
    }

    private void sendPlayerSyntax(final @NotNull Player player, final @NotNull String translationKey) {
        new I18n.Builder(translationKey, player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @Nullable Double parsePositiveAmount(final @NotNull String[] args, final int index) {
        if (index < 0 || index >= args.length) {
            return null;
        }
        try {
            final double parsed = Double.parseDouble(args[index].trim());
            return Double.isFinite(parsed) && parsed > 0.0D ? parsed : null;
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private @Nullable Double parseNonNegativeAmount(final @NotNull String[] args, final int index) {
        if (index < 0 || index >= args.length) {
            return null;
        }
        try {
            final double parsed = Double.parseDouble(args[index].trim());
            return Double.isFinite(parsed) && parsed >= 0.0D ? parsed : null;
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private @NotNull String formatInstant(final @Nullable Instant instant) {
        if (instant == null) {
            return "Never";
        }
        return COMMAND_TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
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
