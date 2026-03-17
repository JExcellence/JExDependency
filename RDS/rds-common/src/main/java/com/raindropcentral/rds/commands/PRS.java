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
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.service.tax.ShopTaxSummarySupport;
import com.raindropcentral.rds.view.shop.ShopAdminView;
import com.raindropcentral.rds.view.shop.ShopSearchView;
import com.raindropcentral.rds.view.shop.ShopStoreView;
import com.raindropcentral.rds.view.shop.ShopTaxView;
import com.raindropcentral.rds.items.ShopBlock;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.protection.RProtectionBridge;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String alias, @NonNull @NotNull String[] args) {
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

        final boolean enabled = this.rds.getShopBossBarService().toggleFor(player);
        final String key = enabled
                ? "prs.bar.enabled"
                : "prs.bar.disabled";

        new I18n.Builder(key, player)
                .includePrefix()
                .build()
                .sendMessage();
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
            final List<String> suggestions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
            return StringUtil.copyPartialMatches(args[1], suggestions, new ArrayList<>());
        }
        if (args.length == 2 && "scoreboard".equalsIgnoreCase(args[0]) && this.hasPermission(player, EPRSPermission.SCOREBOARD)) {
            return StringUtil.copyPartialMatches(
                    args[1],
                    List.of(LEDGER_SCOREBOARD_TYPE, STOCK_SCOREBOARD_TYPE),
                    new ArrayList<>()
            );
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0]) && this.hasPermission(player, EPRSPermission.GIVE)) {
            return StringUtil.copyPartialMatches(args[2], List.of("1", "16", "64"), new ArrayList<>());
        }
        return List.of();
    }

    private boolean canTabCompleteAction(
            final @NotNull Player player,
            final @NotNull EPRSAction action
    ) {
        return switch (action) {
            case ADMIN -> this.hasPermission(player, EPRSPermission.ADMIN);
            case BAR -> this.hasPermission(player, EPRSPermission.BAR);
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
