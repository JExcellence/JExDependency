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

package com.raindropcentral.core.commands;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.core.RCore;
import com.raindropcentral.core.service.central.DropletClaimService;
import com.raindropcentral.core.service.central.RCentralService;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Command handler for {@code /rc}.
 *
 * <p>The command supports {@code connect}, {@code disconnect}, and {@code claim droplets}
 * subcommands for RaindropCentral connectivity and droplet reward redemption.</p>
 */
@Command
@SuppressWarnings("unused")
public final class PRC extends PlayerCommand {
    
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{32,128}$");
    private final RCentralService centralService;
    private final DropletClaimService dropletClaimService;

    /**
     * Creates the player command handler for central connection actions.
     *
     * @param section command metadata section bound to {@code /rc}
     * @param rCore active plugin instance providing central service access
     */
    public PRC(
            final @NotNull PRCSection section,
            final @NotNull RCore rCore
    ) {
        super(section);
        this.centralService = rCore.getImpl().getRCentralService();
        this.dropletClaimService = rCore.getImpl().getDropletClaimService();
    }

    @Override
    protected void onPlayerInvocation(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            this.sendUsage(player);
            return;
        }

        final RCAction action = RCAction.fromRawValue(stringParameter(args, 0));
        if (action == RCAction.INVALID) {
            this.sendUsage(player);
            return;
        }

        switch (action) {
            case CONNECT -> this.handleConnect(player, args);
            case DISCONNECT -> this.handleDisconnect(player, args);
            case CLAIM -> this.handleClaim(player, args);
            default -> this.sendUsage(player);
        }
    }

    @Override
    protected List<String> onPlayerTabCompletion(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (args.length == 1) {
            final List<String> availableSubcommands = new ArrayList<>();
            if (this.hasPermission(player, ERCentralPermission.CONNECT)) {
                availableSubcommands.add("connect");
            }
            if (this.hasPermission(player, ERCentralPermission.DISCONNECT)) {
                availableSubcommands.add("disconnect");
            }
            if (this.hasPermission(player, ERCentralPermission.CLAIM_DROPLETS) && this.dropletClaimService.isDropletStoreEnabled()) {
                availableSubcommands.add("claim");
            }
            return StringUtil.copyPartialMatches(args[0], availableSubcommands, new ArrayList<>());
        }
        if (args.length == 2
                && "claim".equalsIgnoreCase(args[0])
                && this.hasPermission(player, ERCentralPermission.CLAIM_DROPLETS)
                && this.dropletClaimService.isDropletStoreEnabled()) {
            return StringUtil.copyPartialMatches(args[1], List.of("droplets"), new ArrayList<>());
        }
        return List.of();
    }

    private void sendUsage(final @NotNull Player player) {
        final String usageKey = this.dropletClaimService.isDropletStoreEnabled()
                ? "rcconnect.usage"
                : "rcconnect.usage_no_claim";
        new I18n.Builder(usageKey, player)
                .includePrefix()
                .build().sendMessage();
    }

    private void handleConnect(
            final @NotNull Player player,
            final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, ERCentralPermission.CONNECT)) {
            return;
        }
        if (args.length != 2) {
            this.sendUsage(player);
            return;
        }

        final String apiKey = stringParameter(args, 1);
        if (!API_KEY_PATTERN.matcher(apiKey).matches()) {
            new I18n.Builder("rcconnect.error.invalid_key", player)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        if (this.centralService.isConnected()) {
            new I18n.Builder("rcconnect.error.already_connected", player)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        new I18n.Builder("rcconnect.connecting", player)
                .includePrefix()
                .build().sendMessage();

        final String playerUuid = player.getUniqueId().toString();
        final String playerName = player.getName();

        this.centralService.connect(apiKey, playerUuid, playerName).thenAccept(result -> {
            if (result.success()) {
                new I18n.Builder("rcconnect.success", player)
                        .includePrefix()
                        .build().sendMessage();
            } else {
                final String errorKey = switch (result.errorCode()) {
                    case "UUID_MISMATCH" -> "rcconnect.error.uuid_mismatch";
                    case "INVALID_KEY" -> "rcconnect.error.invalid_key";
                    default -> "rcconnect.error.network";
                };
                new I18n.Builder(errorKey, player)
                        .includePrefix()
                        .withPlaceholder("error", result.errorMessage())
                        .build().sendMessage();
            }
        }).exceptionally(throwable -> {
            new I18n.Builder("rcconnect.error.network", player)
                    .includePrefix()
                    .withPlaceholder("error", throwable.getMessage())
                    .build().sendMessage();
            return null;
        });
    }

    private void handleDisconnect(
            final @NotNull Player player,
            final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, ERCentralPermission.DISCONNECT)) {
            return;
        }
        if (args.length != 1) {
            this.sendUsage(player);
            return;
        }

        if (!this.centralService.isConnected()) {
            new I18n.Builder("rcdisconnect.error.not_connected", player)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        new I18n.Builder("rcdisconnect.disconnecting", player)
                .includePrefix()
                .build().sendMessage();

        this.centralService.disconnect().thenAccept(success -> {
            if (success) {
                new I18n.Builder("rcdisconnect.success", player)
                        .includePrefix()
                        .build().sendMessage();
            } else {
                new I18n.Builder("rcdisconnect.error.failed", player)
                        .includePrefix()
                        .build().sendMessage();
            }
        }).exceptionally(throwable -> {
            new I18n.Builder("rcdisconnect.error.network", player)
                    .includePrefix()
                    .withPlaceholder("error", throwable.getMessage())
                    .build().sendMessage();
            return null;
        });
    }

    private void handleClaim(
            final @NotNull Player player,
            final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, ERCentralPermission.CLAIM_DROPLETS)) {
            return;
        }
        if (args.length != 2 || !"droplets".equalsIgnoreCase(args[1])) {
            this.sendUsage(player);
            return;
        }

        this.dropletClaimService.openClaimsMenu(player);
    }

    private enum RCAction {
        CONNECT,
        DISCONNECT,
        CLAIM,
        INVALID;

        private static @NotNull RCAction fromRawValue(final @NotNull String rawValue) {
            final String normalizedValue = rawValue.trim().toLowerCase(Locale.ROOT);
            return switch (normalizedValue) {
                case "connect" -> CONNECT;
                case "disconnect" -> DISCONNECT;
                case "claim" -> CLAIM;
                default -> INVALID;
            };
        }
    }
}
