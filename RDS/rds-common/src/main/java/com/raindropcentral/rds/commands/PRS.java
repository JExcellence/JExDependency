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

package com.raindropcentral.rds.commands;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopBlock;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.service.shop.TownShopService;
import com.raindropcentral.rds.service.tax.ShopTaxSummarySupport;
import com.raindropcentral.rds.view.shop.ShopAdminView;
import com.raindropcentral.rds.view.shop.ShopSearchView;
import com.raindropcentral.rds.view.shop.ShopStoreView;
import com.raindropcentral.rds.view.shop.ShopTaxView;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.protection.RProtectionBridge;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Primary player command for the RDS shop plugin.
 *
 * <p>The command exposes shop overview information, admin controls, shop-bar and scoreboard
 * toggles, shop-block grants, and player-facing shop search/store entry points.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PRS extends PlayerCommand {

    private static final String LEDGER_SCOREBOARD_TYPE = "ledger";
    private static final String STOCK_SCOREBOARD_TYPE = "stock";

    private final RDS rds;

    /**
     * Creates the player command handler.
     *
     * @param commandSection configured command section for {@code /prs}
     * @param rds active plugin instance
     */
    public PRS(ACommandSection commandSection, RDS rds){
        super(commandSection);
        this.rds = rds;
    }

    @Override
    protected void onInvocation(
        final @NotNull CommandSender sender,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        if (sender instanceof ConsoleCommandSender console) {
            if (!this.isInternalCommand(args)) {
                throw new CommandError(null, EErrorType.NOT_A_PLAYER);
            }
            this.handleInternalCommand(console, alias, args);
            return;
        }

        super.onInvocation(sender, alias, args);
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String alias, @NonNull @NotNull String[] args) {
        if (this.isInternalCommand(args)) {
            throw new CommandError(null, EErrorType.NOT_A_CONSOLE);
        }

        final EPRSAction action = this.resolveAction(args);
        switch (action) {
            case BAR -> this.handleBarCommand(player);
            case ADMIN -> this.handleAdminCommand(player);
            case GIVE -> this.handleGiveCommand(player, args);
            case SCOREBOARD -> this.handleScoreboardCommand(player, args);
            case SEARCH -> {
                this.rds.getViewFrame().open(
                    ShopSearchView.class,
                    player,
                    Map.of("plugin", this.rds)
                );
            }
            case STORE -> {
                this.rds.getViewFrame().open(
                    ShopStoreView.class,
                    player,
                    Map.of("plugin", this.rds)
                );
            }
            case TAXES -> {
                if (this.hasNoPermission(player, EPRSPermission.TAXES)) {
                    return;
                }
                this.rds.getViewFrame().open(
                    ShopTaxView.class,
                    player,
                    Map.of("plugin", this.rds)
                );
            }
            default -> {
                final List<Shop> ownedShops = this.getOwnedShops(player);
                final Map<String, Double> trackedCurrencies = this.collectTrackedCurrencies(ownedShops);
                final ShopTaxSummarySupport.ShopTaxSummary taxSummary = ShopTaxSummarySupport.summarize(
                        this.rds,
                        player.getUniqueId()
                );
                final String protectionPluginName = this.resolveProtectionPluginName();
                new I18n.Builder("info.total", player)
                    .withPlaceholder("owned_shops", ownedShops.size())
                    .build()
                    .sendMessage();
                new I18n.Builder("info.bank", player)
                    .withPlaceholders(Map.of(
                            "currency_count", trackedCurrencies.size(),
                            "currencies", this.formatTrackedCurrencies(trackedCurrencies)
                    ))
                    .build()
                    .sendMessage();
                new I18n.Builder("info.tax_amount", player)
                        .withPlaceholders(Map.of(
                                "taxed_shops", taxSummary.taxedShops(),
                                "taxes", taxSummary.amountSummary()
                        ))
                        .build()
                        .sendMessage();
                new I18n.Builder("info.protection_plugin", player)
                        .withPlaceholder("protection_plugin", protectionPluginName)
                        .build()
                        .sendMessage();
                if (taxSummary.hasProtectionTaxesConfigured()) {
                    new I18n.Builder("info.protection_tax_amount", player)
                            .withPlaceholders(Map.of(
                                    "taxed_shops", taxSummary.taxedShops(),
                                    "protection_tax_currency_count", taxSummary.protectionTaxCurrencyCount(),
                                    "protection_plugin", protectionPluginName,
                                    "protection_taxes", taxSummary.protectionAmountSummary()
                            ))
                            .build()
                            .sendMessage();
                }
                new I18n.Builder("info.tax_schedule", player)
                        .withPlaceholders(Map.of(
                                "next_tax_at", taxSummary.nextTaxDisplay(),
                                "time_until", taxSummary.timeUntilDisplay(),
                                "tax_time_zone", taxSummary.timeZone().getId()
                        ))
                        .build()
                        .sendMessage();
            }
        }
    }

    private void handleBarCommand(
            final @NotNull Player player
    ) {
        if (this.hasNoPermission(player, EPRSPermission.BAR)) {
            return;
        }

        if (this.rds.openShopBossBarSettings(player)) {
            new I18n.Builder("prs.bar.redirected", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final boolean enabled = this.rds.getShopBossBarService().toggleFor(player);
        final String key = enabled ? "prs.bar.enabled" : "prs.bar.disabled";
        new I18n.Builder(key, player).includePrefix().build().sendMessage();
    }

    private void handleAdminCommand(
            final @NotNull Player player
    ) {
        if (this.hasNoPermission(player, EPRSPermission.ADMIN)) {
            return;
        }

        new I18n.Builder("prs.admin.opened", player)
                .includePrefix()
                .build()
                .sendMessage();
        this.rds.getViewFrame().open(
                ShopAdminView.class,
                player,
                Map.of("plugin", this.rds)
        );
    }

    private void handleGiveCommand(
            final @NotNull Player sender,
            final @NotNull String[] args
    ) {
        if (this.hasNoPermission(sender, EPRSPermission.GIVE)) {
            return;
        }

        if (args.length == 4 && "town".equalsIgnoreCase(args[1])) {
            this.handleGiveTownCommand(sender, args);
            return;
        }

        if (args.length != 3) {
            this.sendGiveSyntax(sender);
            return;
        }

        final Player target = Bukkit.getPlayerExact(args[1]);
        final int amount = this.parsePositiveAmount(args[2]);
        if (target == null || amount < 1) {
            this.sendGiveSyntax(sender);
            return;
        }

        this.giveShopBlocks(target, amount);
    }

    private void handleGiveTownCommand(
        final @NotNull Player sender,
        final @NotNull String[] args
    ) {
        final Player target = Bukkit.getPlayerExact(args[2]);
        final int amount = this.parsePositiveAmount(args[3]);
        final TownShopService townShopService = this.rds.getTownShopService();
        if (target == null || amount < 1 || townShopService == null) {
            this.sendGiveSyntax(sender);
            return;
        }

        final TownShopService.LiveOutpostContext outpost = townShopService.resolveLiveOutpost(target);
        if (outpost == null) {
            new I18n.Builder("prs.give.town.outpost_required", sender)
                .includePrefix()
                .withPlaceholder("target_player", args[2])
                .build()
                .sendMessage();
            return;
        }

        final int granted = townShopService.grantBonusCapacity(
            target,
            outpost.protectionPlugin(),
            outpost.townIdentifier(),
            outpost.townDisplayName(),
            outpost.chunkUuid(),
            outpost.worldName(),
            outpost.chunkX(),
            outpost.chunkZ(),
            outpost.chunkLevel(),
            amount
        );
        new I18n.Builder("prs.give.town.success", sender)
            .includePrefix()
            .withPlaceholders(Map.of(
                "target_player", target.getName(),
                "granted_amount", granted,
                "town_name", outpost.townDisplayName(),
                "chunk_x", outpost.chunkX(),
                "chunk_z", outpost.chunkZ()
            ))
            .build()
            .sendMessage();
    }

    private void handleScoreboardCommand(
            final @NotNull Player player,
            final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, EPRSPermission.SCOREBOARD)) {
            return;
        }

        if (args.length < 2) {
            this.sendScoreboardSyntax(player);
            return;
        }

        final String scoreboardType = args[1].trim().toLowerCase(Locale.ROOT);
        if (!LEDGER_SCOREBOARD_TYPE.equals(scoreboardType) && !STOCK_SCOREBOARD_TYPE.equals(scoreboardType)) {
            this.sendScoreboardSyntax(player);
            return;
        }

        final String activeType = this.rds.getShopSidebarScoreboardService().getActiveTypeName(player);
        final String scoreboardTypeLabel = this.getScoreboardTypeLabel(player, scoreboardType);
        final RDSPlayer playerData = this.getOrCreatePlayer(player);

        if (scoreboardType.equalsIgnoreCase(activeType)) {
            this.rds.getShopSidebarScoreboardService().disable(player);
            playerData.clearShopSidebarScoreboardType();
            this.rds.getPlayerRepository().update(playerData);
            new I18n.Builder("prs.scoreboard.disabled", player)
                    .withPlaceholder("scoreboard_type", scoreboardTypeLabel)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (LEDGER_SCOREBOARD_TYPE.equals(scoreboardType)) {
            this.rds.getShopSidebarScoreboardService().enableLedger(player);
        } else {
            this.rds.getShopSidebarScoreboardService().enableStock(player);
        }
        playerData.setShopSidebarScoreboardType(scoreboardType);
        this.rds.getPlayerRepository().update(playerData);

        final String messageKey = activeType == null
                ? "prs.scoreboard.enabled"
                : "prs.scoreboard.switched";
        new I18n.Builder(messageKey, player)
                .withPlaceholder("scoreboard_type", scoreboardTypeLabel)
                .includePrefix()
                .build()
                .sendMessage();
    }

    private @NotNull List<Shop> getOwnedShops(
            final @NotNull Player player
    ) {
        final List<Shop> ownedShops = new ArrayList<>();
        for (final Shop shop : this.rds.getShopRepository().findAllShops()) {
            if (shop.isOwner(player.getUniqueId())) {
                ownedShops.add(shop);
            }
        }
        return ownedShops;
    }

    private @NotNull Map<String, Double> collectTrackedCurrencies(
            final @NotNull List<Shop> ownedShops
    ) {
        final Map<String, Double> trackedCurrencies = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final Shop shop : ownedShops) {
            for (final var bankEntry : shop.getBankEntries()) {
                trackedCurrencies.merge(
                        bankEntry.getCurrencyType(),
                        bankEntry.getAmount(),
                        Double::sum
                );
            }

            for (final AbstractItem item : shop.getItems()) {
                if (item instanceof ShopItem shopItem) {
                    trackedCurrencies.putIfAbsent(shopItem.getCurrencyType(), 0D);
                }
            }
        }

        return trackedCurrencies;
    }

    private @NotNull String formatTrackedCurrencies(
            final @NotNull Map<String, Double> trackedCurrencies
    ) {
        if (trackedCurrencies.isEmpty()) {
            return "None";
        }

        return trackedCurrencies.entrySet().stream()
                .map(entry -> this.getCurrencyDisplayName(entry.getKey())
                        + ": "
                        + this.formatCurrencyAmount(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private @NotNull String formatCurrencyAmount(
            final @NotNull String currencyType,
            final double amount
    ) {
        if (this.usesVaultCurrency(currencyType)) {
            return this.rds.formatVaultCurrency(amount);
        }

        return this.formatAmount(amount);
    }

    private boolean usesVaultCurrency(
            final @NotNull String currencyType
    ) {
        return "vault".equalsIgnoreCase(currencyType);
    }

    private @NotNull String getCurrencyDisplayName(
            final @NotNull String currencyType
    ) {
        if (this.usesVaultCurrency(currencyType)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge == null ? currencyType : bridge.getCurrencyDisplayName(currencyType);
    }

    private @NotNull String formatAmount(
            final double amount
    ) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private @NotNull String resolveProtectionPluginName() {
        final RProtectionBridge bridge = RProtectionBridge.getBridge();
        if (bridge == null || !bridge.isAvailable()) {
            return "None";
        }

        return bridge.getPluginName();
    }

    private int parsePositiveAmount(
            final @NotNull String rawAmount
    ) {
        try {
            final int amount = Integer.parseInt(rawAmount.trim());
            return amount > 0 ? amount : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void sendGiveSyntax(
            final @NotNull Player player
    ) {
        new I18n.Builder("prs.give.syntax", player)
                .includePrefix()
                .build()
                .sendMessage();
    }

    private void sendScoreboardSyntax(
            final @NotNull Player player
    ) {
        new I18n.Builder("prs.scoreboard.syntax", player)
                .includePrefix()
                .build()
                .sendMessage();
    }

    private void giveShopBlocks(
            final @NotNull Player target,
            final int amount
    ) {
        final RDSPlayer targetPlayer = this.getOrCreatePlayer(target);
        targetPlayer.addShop(amount);
        this.rds.getPlayerRepository().update(targetPlayer);

        int remaining = amount;
        final List<ItemStack> stacks = new ArrayList<>();

        while (remaining > 0) {
            final ItemStack stack = ShopBlock.getShopBlock(this.rds, target);
            final int stackAmount = Math.min(remaining, stack.getMaxStackSize());
            stack.setAmount(stackAmount);
            stacks.add(stack);
            remaining -= stackAmount;
        }

        target.getInventory().addItem(stacks.toArray(new ItemStack[0]))
                .forEach((slot, item) -> target.getWorld().dropItem(
                        target.getLocation().clone().add(0, 0.5, 0),
                        item
                ));
    }

    private @NotNull RDSPlayer getOrCreatePlayer(final @NotNull Player player) {
        final RDSPlayer existingPlayer = this.rds.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (existingPlayer != null) {
            return existingPlayer;
        }

        final RDSPlayer newPlayer = new RDSPlayer(player.getUniqueId());
        this.rds.getPlayerRepository().create(newPlayer);
        return newPlayer;
    }

    private boolean isInternalCommand(final @NotNull String[] args) {
        return args.length > 0 && "internal".equalsIgnoreCase(args[0]);
    }

    private void handleInternalCommand(
        final @NotNull ConsoleCommandSender console,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        if (args.length < 2) {
            this.sendInternalSyntax(console, alias);
            return;
        }

        final String route = args[1].trim().toLowerCase(Locale.ROOT);
        switch (route) {
            case "reward-town-shop" -> this.handleRewardTownShop(console, alias, args);
            case "sync-town-outpost" -> this.handleSyncTownOutpost(console, alias, args);
            case "remove-town-outpost" -> this.handleRemoveTownOutpost(console, alias, args);
            case "claim-town-shop" -> this.handleClaimTownShop(console, alias, args);
            default -> this.sendInternalSyntax(console, alias);
        }
    }

    private void handleRewardTownShop(
        final @NotNull ConsoleCommandSender console,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        if (args.length != 11 || this.rds.getTownShopService() == null) {
            this.sendInternalSyntax(console, alias);
            return;
        }

        final Player target = Bukkit.getPlayerExact(args[2]);
        final String protectionPlugin = args[3];
        final String townIdentifier = args[4];
        final String townDisplayName = this.decodeBase64(args[5]);
        final UUID chunkUuid = this.parseUuid(args[6]);
        final String worldName = args[7];
        final Integer chunkX = this.parseInteger(args[8]);
        final Integer chunkZ = this.parseInteger(args[9]);
        final int chunkLevel = this.parsePositiveAmount(args[10]);
        if (target == null
            || townDisplayName == null
            || chunkUuid == null
            || chunkX == null
            || chunkZ == null
            || chunkLevel < 1) {
            this.sendInternalSyntax(console, alias);
            return;
        }

        final int granted = this.rds.getTownShopService().grantLevelReward(
            target,
            protectionPlugin,
            townIdentifier,
            townDisplayName,
            chunkUuid,
            worldName,
            chunkX,
            chunkZ,
            chunkLevel
        );
        console.sendMessage("Granted " + granted + " town shop(s) to " + target.getName() + " for outpost " + chunkUuid + '.');
    }

    private void handleSyncTownOutpost(
        final @NotNull ConsoleCommandSender console,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        if (args.length != 10 || this.rds.getTownShopService() == null) {
            this.sendInternalSyntax(console, alias);
            return;
        }

        final String protectionPlugin = args[2];
        final String townIdentifier = args[3];
        final String townDisplayName = this.decodeBase64(args[4]);
        final UUID chunkUuid = this.parseUuid(args[5]);
        final String worldName = args[6];
        final Integer chunkX = this.parseInteger(args[7]);
        final Integer chunkZ = this.parseInteger(args[8]);
        final int chunkLevel = this.parsePositiveAmount(args[9]);
        if (townDisplayName == null
            || chunkUuid == null
            || chunkX == null
            || chunkZ == null
            || chunkLevel < 1) {
            this.sendInternalSyntax(console, alias);
            return;
        }

        this.rds.getTownShopService().syncOutpost(
            protectionPlugin,
            townIdentifier,
            townDisplayName,
            chunkUuid,
            worldName,
            chunkX,
            chunkZ,
            chunkLevel
        );
        console.sendMessage("Synced town outpost " + chunkUuid + '.');
    }

    private void handleRemoveTownOutpost(
        final @NotNull ConsoleCommandSender console,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        if (args.length != 3 || this.rds.getTownShopService() == null) {
            this.sendInternalSyntax(console, alias);
            return;
        }

        final UUID chunkUuid = this.parseUuid(args[2]);
        if (chunkUuid == null) {
            this.sendInternalSyntax(console, alias);
            return;
        }

        final int closed = this.rds.getTownShopService().removeOutpost(chunkUuid);
        console.sendMessage("Removed town outpost " + chunkUuid + " and force-closed " + closed + " town shop(s).");
    }

    private void handleClaimTownShop(
        final @NotNull ConsoleCommandSender console,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        if (args.length != 4 || this.rds.getTownShopService() == null) {
            this.sendInternalSyntax(console, alias);
            return;
        }

        final Player target = Bukkit.getPlayerExact(args[2]);
        final UUID chunkUuid = this.parseUuid(args[3]);
        if (target == null || chunkUuid == null) {
            this.sendInternalSyntax(console, alias);
            return;
        }

        final boolean claimed = this.rds.getTownShopService().claimTownShopToken(target, chunkUuid);
        console.sendMessage(
            (claimed ? "Reissued" : "Unable to reissue")
                + " town shop token for "
                + target.getName()
                + " in outpost "
                + chunkUuid
                + '.'
        );
    }

    private void sendInternalSyntax(
        final @NotNull ConsoleCommandSender console,
        final @NotNull String alias
    ) {
        console.sendMessage("Usage: /" + alias + " internal <reward-town-shop|sync-town-outpost|remove-town-outpost|claim-town-shop> ...");
    }

    private @Nullable UUID parseUuid(final @NotNull String rawUuid) {
        try {
            return UUID.fromString(rawUuid.trim());
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    private @Nullable Integer parseInteger(final @NotNull String rawInteger) {
        try {
            return Integer.parseInt(rawInteger.trim());
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private @Nullable String decodeBase64(final @NotNull String rawValue) {
        try {
            return new String(Base64.getUrlDecoder().decode(rawValue.trim()), StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Provide tab completion for the first argument with available {@link EPRSAction} values.
     * Requires {@link EPRSPermission#COMMAND}.
     *
     * @param player invoking player (non-null)
     * @param alias  command alias used
     * @param args   current input arguments
     * @return matching suggestions or an empty list when not permitted or out of scope
     */
    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String alias, @NonNull @NotNull String[] args) {
        if (
                this.hasNoPermission(
                        player,
                        EPRSPermission.COMMAND
                )
        ){
            return List.of();
        }
        if (args.length == 1){
            final List<String> suggestions = new ArrayList<>();
            for (final EPRSAction action : EPRSAction.values()) {
                if (!this.canTabCompleteAction(player, action)) {
                    continue;
                }
                suggestions.add(action.name());
            }
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());

        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0]) && this.hasPermission(player, EPRSPermission.GIVE)) {
            final List<String> suggestions = new ArrayList<>();
            suggestions.add("town");
            suggestions.addAll(
                Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList()
            );
            return StringUtil.copyPartialMatches(args[1], suggestions, new ArrayList<>());
        }
        if (args.length == 2 && "scoreboard".equalsIgnoreCase(args[0]) && this.hasPermission(player, EPRSPermission.SCOREBOARD)) {
            return StringUtil.copyPartialMatches(
                    args[1],
                    List.of(LEDGER_SCOREBOARD_TYPE, STOCK_SCOREBOARD_TYPE),
                    new ArrayList<>()
            );
        }
        if (args.length == 3
            && "give".equalsIgnoreCase(args[0])
            && "town".equalsIgnoreCase(args[1])
            && this.hasPermission(player, EPRSPermission.GIVE)) {
            final List<String> suggestions = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
            return StringUtil.copyPartialMatches(args[2], suggestions, new ArrayList<>());
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0]) && this.hasPermission(player, EPRSPermission.GIVE)) {
            return StringUtil.copyPartialMatches(args[2], List.of("1", "16", "64"), new ArrayList<>());
        }
        if (args.length == 4
            && "give".equalsIgnoreCase(args[0])
            && "town".equalsIgnoreCase(args[1])
            && this.hasPermission(player, EPRSPermission.GIVE)) {
            return StringUtil.copyPartialMatches(args[3], List.of("1", "2", "5"), new ArrayList<>());
        }
        return List.of();
    }

    private boolean canTabCompleteAction(
            final @NotNull Player player,
            final @NotNull EPRSAction action
    ) {
        return switch (action) {
            case ADMIN -> this.hasPermission(player, EPRSPermission.ADMIN);
            case BAR -> false;
            case GIVE -> this.hasPermission(player, EPRSPermission.GIVE);
            case SCOREBOARD -> this.hasPermission(player, EPRSPermission.SCOREBOARD);
            case SEARCH -> this.hasPermission(player, EPRSPermission.SEARCH);
            case STORE -> this.hasPermission(player, EPRSPermission.STORE);
            case TAXES -> this.hasPermission(player, EPRSPermission.TAXES);
            case INFO -> this.hasPermission(player, EPRSPermission.INFO);
        };
    }

    private @NotNull EPRSAction resolveAction(
            final @NotNull String[] args
    ) {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            return EPRSAction.INFO;
        }

        try {
            return EPRSAction.valueOf(args[0].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EPRSAction.INFO;
        }
    }

    private @NotNull String getScoreboardTypeLabel(
            final @NotNull Player player,
            final @NotNull String scoreboardType
    ) {
        final String key = LEDGER_SCOREBOARD_TYPE.equalsIgnoreCase(scoreboardType)
                ? "prs.scoreboard.type.ledger"
                : "prs.scoreboard.type.stock";
        return new I18n.Builder(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }
}
