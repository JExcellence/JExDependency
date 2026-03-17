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
import com.raindropcentral.core.service.central.RCentralService;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Command handler for /rcconnect to link this server to RaindropCentral platform.
 */
@Command
public final class RCConnect extends PlayerCommand {

    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{32,128}$");
    private final RCentralService centralService;

    /**
     * Executes RCConnect.
     */
    public RCConnect(
            final @NotNull RCConnectSection section,
            final @NotNull RCore rCore
            ) {
        super(section);
        this.centralService = rCore.getImpl().getRCentralService();
    }

    @Override
    protected void onPlayerInvocation(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (hasNoPermission(player, ERCentralPermission.CONNECT)) {
            return;
        }

        if (args.length != 1) {
            new I18n.Builder("rcconnect.usage", player)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        var apiKey = stringParameter(args, 0);

        if (!API_KEY_PATTERN.matcher(apiKey).matches()) {
            new I18n.Builder("rcconnect.error.invalid_key", player)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        if (centralService.isConnected()) {
            new I18n.Builder("rcconnect.error.already_connected", player)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        new I18n.Builder("rcconnect.connecting", player)
                .includePrefix()
                .build().sendMessage();

        var playerUuid = player.getUniqueId().toString();
        var playerName = player.getName();

        centralService.connect(apiKey, playerUuid, playerName).thenAccept(result -> {
            if (result.success()) {
                new I18n.Builder("rcconnect.success", player)
                        .includePrefix()
                        .build().sendMessage();
            } else {
                var errorKey = switch (result.errorCode()) {
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

    @Override
    protected List<String> onPlayerTabCompletion(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        return List.of();
    }
}
