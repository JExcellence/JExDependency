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

/**
 * Command handler for /rcdisconnect to unlink this server from RaindropCentral platform.
 */
@Command
public final class RCDisconnect extends PlayerCommand {

    private final RCentralService centralService;

    /**
     * Executes RCDisconnect.
     */
    public RCDisconnect(
            final @NotNull RCDisconnectSection section,
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
        if (hasNoPermission(player, ERCentralPermission.DISCONNECT)) {
            return;
        }

        if (!centralService.isConnected()) {
            new I18n.Builder("rcdisconnect.error.not_connected", player)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        new I18n.Builder("rcdisconnect.disconnecting", player)
                .includePrefix()
                .build().sendMessage();

        centralService.disconnect().thenAccept(success -> {
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

    @Override
    protected List<String> onPlayerTabCompletion(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        return List.of();
    }
}
