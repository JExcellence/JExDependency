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
import java.util.regex.Pattern;

/**
 * Command handler for /rcconnect to link this server to RaindropCentral platform.
 */
@Command
public final class RCConnect extends PlayerCommand {

    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{32,128}$");
    private final RCentralService centralService;

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
            TranslationService.create(TranslationKey.of("rcconnect.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var apiKey = stringParameter(args, 0);

        if (!API_KEY_PATTERN.matcher(apiKey).matches()) {
            TranslationService.create(TranslationKey.of("rcconnect.error.invalid_key"), player)
                    .withPrefix()
                    .send();
            return;
        }

        if (centralService.isConnected()) {
            TranslationService.create(TranslationKey.of("rcconnect.error.already_connected"), player)
                    .withPrefix()
                    .send();
            return;
        }

        TranslationService.create(TranslationKey.of("rcconnect.connecting"), player)
                .withPrefix()
                .send();

        var playerUuid = player.getUniqueId().toString();
        var playerName = player.getName();

        centralService.connect(apiKey, playerUuid, playerName).thenAccept(result -> {
            if (result.success()) {
                TranslationService.create(TranslationKey.of("rcconnect.success"), player)
                        .withPrefix()
                        .send();
            } else {
                var errorKey = switch (result.errorCode()) {
                    case "UUID_MISMATCH" -> "rcconnect.error.uuid_mismatch";
                    case "INVALID_KEY" -> "rcconnect.error.invalid_key";
                    default -> "rcconnect.error.network";
                };
                TranslationService.create(TranslationKey.of(errorKey), player)
                        .withPrefix()
                        .with("error", result.errorMessage())
                        .send();
            }
        }).exceptionally(throwable -> {
            TranslationService.create(TranslationKey.of("rcconnect.error.network"), player)
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
