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
import com.raindropcentral.core.RCoreImpl;
import com.raindropcentral.core.service.RCoreBossBarService;
import com.raindropcentral.core.service.central.DropletClaimService;
import com.raindropcentral.core.service.central.RCentralService;
import com.raindropcentral.core.view.RCoreMainOverviewView;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Command handler for {@code /rc}.
 *
 * <p>The command supports {@code main}, {@code connect}, {@code disconnect}, {@code claim droplets},
 * {@code store update}, and {@code bossbar} subcommands for RaindropCentral connectivity,
 * module navigation, and droplet reward management.</p>
 */
@Command
@SuppressWarnings("unused")
public final class PRC extends PlayerCommand {
    
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{32,128}$");
    private final RCoreImpl rCoreImpl;
    private final RCentralService centralService;
    private final DropletClaimService dropletClaimService;
    private final RCoreBossBarService bossBarService;

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
        this.rCoreImpl = rCore.getImpl();
        this.centralService = this.rCoreImpl.getRCentralService();
        this.dropletClaimService = this.rCoreImpl.getDropletClaimService();
        this.bossBarService = this.rCoreImpl.getBossBarService();
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
            case BOSSBAR -> this.handleBossBar(player, args);
            case MAIN -> this.handleMain(player, args);
            case CONNECT -> this.handleConnect(player, args);
            case DISCONNECT -> this.handleDisconnect(player, args);
            case CLAIM -> this.handleClaim(player, args);
            case STORE -> this.handleStore(player, args);
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
            if (this.hasPermission(player, ERCentralPermission.MAIN)) {
                availableSubcommands.add("main");
            }
            if (this.hasPermission(player, ERCentralPermission.CONNECT)) {
                availableSubcommands.add("connect");
            }
            if (this.hasPermission(player, ERCentralPermission.DISCONNECT)) {
                availableSubcommands.add("disconnect");
            }
            if (this.hasPermission(player, ERCentralPermission.CLAIM_DROPLETS)) {
                availableSubcommands.add("claim");
            }
            if (this.hasPermission(player, ERCentralPermission.STORE_UPDATE)) {
                availableSubcommands.add("store");
            }
            if (this.hasPermission(player, ERCentralPermission.BOSS_BAR)) {
                availableSubcommands.add("bossbar");
            }
            return StringUtil.copyPartialMatches(args[0], availableSubcommands, new ArrayList<>());
        }
        if (this.isBossBarRootAccessible(player, args)) {
            return StringUtil.copyPartialMatches(
                args[1],
                List.of("toggle", "enable", "disable", "set"),
                new ArrayList<>()
            );
        }
        if (this.isBossBarProviderArgument(player, args, 2)) {
            return StringUtil.copyPartialMatches(args[2], this.getBossBarProviderKeys(), new ArrayList<>());
        }
        if (this.isBossBarSetOptionArgument(player, args)) {
            return this.getBossBarOptionKeys(args[2], args[3]);
        }
        if (this.isBossBarSetValueArgument(player, args)) {
            return this.getBossBarOptionValues(args[2], args[3], args[4]);
        }
        if (args.length == 2
                && "claim".equalsIgnoreCase(args[0])
                && this.hasPermission(player, ERCentralPermission.CLAIM_DROPLETS)) {
            return StringUtil.copyPartialMatches(args[1], List.of("droplets"), new ArrayList<>());
        }
        if (args.length == 2
                && "store".equalsIgnoreCase(args[0])
                && this.hasPermission(player, ERCentralPermission.STORE_UPDATE)) {
            return StringUtil.copyPartialMatches(args[1], List.of("update"), new ArrayList<>());
        }
        return List.of();
    }

    private void sendUsage(final @NotNull Player player) {
        new I18n.Builder("rcconnect.usage", player)
                .includePrefix()
                .build().sendMessage();
    }

    private void handleMain(
        final @NotNull Player player,
        final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, ERCentralPermission.MAIN)) {
            return;
        }
        if (args.length != 1) {
            this.sendUsage(player);
            return;
        }
        if (this.rCoreImpl.getViewFrame() == null) {
            new I18n.Builder("rcmain.error.menu_unavailable", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        this.rCoreImpl.getViewFrame().open(RCoreMainOverviewView.class, player, Map.of("plugin", this.rCoreImpl));
    }

    private void handleBossBar(
        final @NotNull Player player,
        final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, ERCentralPermission.BOSS_BAR)) {
            return;
        }
        if (this.bossBarService == null) {
            new I18n.Builder("rcbossbar.error.unavailable", player)
                .includePrefix()
                .build().sendMessage();
            return;
        }
        if (args.length == 1) {
            this.bossBarService.openSettingsView(player);
            return;
        }
        if (args.length < 3) {
            this.sendUsage(player);
            return;
        }

        final String subAction = args[1].trim().toLowerCase(Locale.ROOT);
        final String providerKey = args[2];
        try {
            switch (subAction) {
                case "toggle" -> this.sendBossBarStateMessage(
                    player,
                    providerKey,
                    this.bossBarService.toggleEnabled(player.getUniqueId(), providerKey)
                );
                case "enable" -> this.sendBossBarStateMessage(
                    player,
                    providerKey,
                    this.bossBarService.setEnabled(player.getUniqueId(), providerKey, true)
                );
                case "disable" -> this.sendBossBarStateMessage(
                    player,
                    providerKey,
                    this.bossBarService.setEnabled(player.getUniqueId(), providerKey, false)
                );
                case "set" -> this.handleBossBarSet(player, args);
                default -> this.sendUsage(player);
            }
        } catch (final IllegalArgumentException exception) {
            this.sendBossBarValidationError(player, providerKey, args, exception);
        }
    }

    private void handleBossBarSet(final @NotNull Player player, final @NotNull String[] args) {
        if (args.length != 5) {
            this.sendUsage(player);
            return;
        }

        final String providerKey = args[2];
        final String optionKey = args[3];
        final String value = args[4];
        final RCoreBossBarService.PreferenceSnapshot preferenceSnapshot = this.bossBarService.setOption(
            player.getUniqueId(),
            providerKey,
            optionKey,
            value
        );
        final RCoreBossBarService.ProviderDefinition providerDefinition = this.bossBarService.findProvider(providerKey).orElseThrow();
        final RCoreBossBarService.ProviderOption providerOption = providerDefinition.findOption(optionKey).orElseThrow();
        final String choiceLabelKey = providerOption.choices().stream()
            .filter(choice -> choice.value().equals(preferenceSnapshot.options().get(providerOption.key())))
            .findFirst()
            .map(RCoreBossBarService.ProviderOptionChoice::labelTranslationKey)
            .orElse(providerOption.choices().getFirst().labelTranslationKey());

        new I18n.Builder("rcbossbar.option.updated", player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "provider_name", this.getBossBarProviderName(player, providerDefinition),
                "option_name", this.translatePlaceholder(player, providerOption.nameTranslationKey()),
                "option_value", this.translatePlaceholder(player, choiceLabelKey)
            ))
            .build()
            .sendMessage();
    }

    private void sendBossBarStateMessage(
        final @NotNull Player player,
        final @NotNull String providerKey,
        final @NotNull RCoreBossBarService.PreferenceSnapshot preferenceSnapshot
    ) {
        final RCoreBossBarService.ProviderDefinition providerDefinition = this.bossBarService.findProvider(providerKey).orElseThrow();
        final String translationKey = preferenceSnapshot.enabled() ? "rcbossbar.enabled" : "rcbossbar.disabled";
        new I18n.Builder(translationKey, player)
            .includePrefix()
            .withPlaceholder("provider_name", this.getBossBarProviderName(player, providerDefinition))
            .build()
            .sendMessage();
    }

    private void sendBossBarValidationError(
        final @NotNull Player player,
        final @NotNull String providerKey,
        final @NotNull String[] args,
        final @NotNull IllegalArgumentException exception
    ) {
        final String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("Unknown boss-bar provider")) {
            new I18n.Builder("rcbossbar.error.provider_missing", player)
                .includePrefix()
                .withPlaceholder("provider_key", providerKey)
                .build()
                .sendMessage();
            return;
        }
        if (message.contains("Unknown option")) {
            new I18n.Builder("rcbossbar.error.option_missing", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                    "provider_key", providerKey,
                    "option_key", args.length > 3 ? args[3] : "unknown"
                ))
                .build()
                .sendMessage();
            return;
        }
        if (message.contains("Invalid value")) {
            new I18n.Builder("rcbossbar.error.invalid_value", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                    "provider_key", providerKey,
                    "option_key", args.length > 3 ? args[3] : "unknown",
                    "value", args.length > 4 ? args[4] : "unknown"
                ))
                .build()
                .sendMessage();
            return;
        }
        this.sendUsage(player);
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

    private void handleStore(
            final @NotNull Player player,
            final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, ERCentralPermission.STORE_UPDATE)) {
            return;
        }
        if (args.length != 2 || !"update".equalsIgnoreCase(args[1])) {
            this.sendUsage(player);
            return;
        }
        if (!this.centralService.isConnected()) {
            new I18n.Builder("rcstore.error.not_connected", player)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        new I18n.Builder("rcstore.updating", player)
                .includePrefix()
                .build().sendMessage();

        this.centralService.forceRefreshDropletStoreAllowlist()
                .thenAccept(result -> {
                    if (result.success()) {
                        new I18n.Builder("rcstore.success", player)
                                .includePrefix()
                                .withPlaceholder("count", result.allowedItemCodes().size())
                                .build().sendMessage();
                        return;
                    }

                    final String errorKey = result.usedCachedValue()
                            ? "rcstore.error.using_cache"
                            : "rcstore.error.failed";
                    new I18n.Builder(errorKey, player)
                            .includePrefix()
                            .withPlaceholder("error", result.errorMessage())
                            .build().sendMessage();
                })
                .exceptionally(throwable -> {
                    new I18n.Builder("rcstore.error.failed", player)
                            .includePrefix()
                            .withPlaceholder("error", throwable.getMessage())
                            .build().sendMessage();
                    return null;
                });
    }

    private boolean isBossBarRootAccessible(final @NotNull Player player, final @NotNull String[] args) {
        return args.length == 2
            && "bossbar".equalsIgnoreCase(args[0])
            && this.hasPermission(player, ERCentralPermission.BOSS_BAR);
    }

    private boolean isBossBarProviderArgument(
        final @NotNull Player player,
        final @NotNull String[] args,
        final int targetIndex
    ) {
        return args.length == targetIndex + 1
            && "bossbar".equalsIgnoreCase(args[0])
            && List.of("toggle", "enable", "disable", "set").contains(args[1].toLowerCase(Locale.ROOT))
            && this.hasPermission(player, ERCentralPermission.BOSS_BAR);
    }

    private boolean isBossBarSetOptionArgument(final @NotNull Player player, final @NotNull String[] args) {
        return args.length == 4
            && "bossbar".equalsIgnoreCase(args[0])
            && "set".equalsIgnoreCase(args[1])
            && this.hasPermission(player, ERCentralPermission.BOSS_BAR);
    }

    private boolean isBossBarSetValueArgument(final @NotNull Player player, final @NotNull String[] args) {
        return args.length == 5
            && "bossbar".equalsIgnoreCase(args[0])
            && "set".equalsIgnoreCase(args[1])
            && this.hasPermission(player, ERCentralPermission.BOSS_BAR);
    }

    private @NotNull List<String> getBossBarProviderKeys() {
        if (this.bossBarService == null) {
            return List.of();
        }
        return this.bossBarService.getRegisteredProviders().stream()
            .map(RCoreBossBarService.ProviderDefinition::key)
            .toList();
    }

    private @NotNull List<String> getBossBarOptionKeys(
        final @NotNull String providerKey,
        final @NotNull String partial
    ) {
        if (this.bossBarService == null) {
            return List.of();
        }
        final RCoreBossBarService.ProviderDefinition providerDefinition = this.bossBarService.findProvider(providerKey).orElse(null);
        if (providerDefinition == null) {
            return List.of();
        }
        final List<String> options = providerDefinition.options().stream()
            .map(RCoreBossBarService.ProviderOption::key)
            .toList();
        return StringUtil.copyPartialMatches(partial, options, new ArrayList<>());
    }

    private @NotNull List<String> getBossBarOptionValues(
        final @NotNull String providerKey,
        final @NotNull String optionKey,
        final @NotNull String partial
    ) {
        if (this.bossBarService == null) {
            return List.of();
        }
        final RCoreBossBarService.ProviderDefinition providerDefinition = this.bossBarService.findProvider(providerKey).orElse(null);
        if (providerDefinition == null) {
            return List.of();
        }
        final RCoreBossBarService.ProviderOption providerOption = providerDefinition.findOption(optionKey).orElse(null);
        if (providerOption == null) {
            return List.of();
        }
        final List<String> values = providerOption.choices().stream()
            .map(RCoreBossBarService.ProviderOptionChoice::value)
            .toList();
        return StringUtil.copyPartialMatches(partial, values, new ArrayList<>());
    }

    private @NotNull String getBossBarProviderName(
        final @NotNull Player player,
        final @NotNull RCoreBossBarService.ProviderDefinition providerDefinition
    ) {
        return this.translatePlaceholder(player, providerDefinition.nameTranslationKey());
    }

    private @NotNull String translatePlaceholder(
        final @NotNull Player player,
        final @NotNull String translationKey
    ) {
        return new I18n.Builder(translationKey, player)
            .build()
            .getI18nVersionWrapper()
            .asPlaceholder();
    }

    private enum RCAction {
        BOSSBAR,
        MAIN,
        CONNECT,
        DISCONNECT,
        CLAIM,
        STORE,
        INVALID;

        private static @NotNull RCAction fromRawValue(final @NotNull String rawValue) {
            final String normalizedValue = rawValue.trim().toLowerCase(Locale.ROOT);
            return switch (normalizedValue) {
                case "bossbar" -> BOSSBAR;
                case "main" -> MAIN;
                case "connect" -> CONNECT;
                case "disconnect" -> DISCONNECT;
                case "claim" -> CLAIM;
                case "store" -> STORE;
                default -> INVALID;
            };
        }
    }
}
