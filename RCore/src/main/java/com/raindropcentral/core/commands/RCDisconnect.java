package com.raindropcentral.core.commands;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.core.RCore;
import com.raindropcentral.core.service.central.RCentralService;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Command handler for /rcdisconnect to unlink this server from RaindropCentral platform.
 */
@Command
public final class RCDisconnect extends PlayerCommand {

    private final RCentralService centralService;

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
            TranslationService.create(TranslationKey.of("rcdisconnect.error.not_connected"), player)
                    .withPrefix()
                    .send();
            return;
        }

        TranslationService.create(TranslationKey.of("rcdisconnect.disconnecting"), player)
                .withPrefix()
                .send();

        centralService.disconnect().thenAccept(success -> {
            if (success) {
                TranslationService.create(TranslationKey.of("rcdisconnect.success"), player)
                        .withPrefix()
                        .send();
            } else {
                TranslationService.create(TranslationKey.of("rcdisconnect.error.failed"), player)
                        .withPrefix()
                        .send();
            }
        }).exceptionally(throwable -> {
            TranslationService.create(TranslationKey.of("rcdisconnect.error.network"), player)
                    .withPrefix()
                    .with("error", throwable.getMessage())
                    .send();
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
