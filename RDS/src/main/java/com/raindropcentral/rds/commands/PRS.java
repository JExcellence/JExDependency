package com.raindropcentral.rds.commands;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.view.shop.ShopSearchView;
import com.raindropcentral.rds.view.shop.ShopStoreView;
import com.raindropcentral.rds.items.ShopBlock;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
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


@Command
@SuppressWarnings("unused")
public class PRS extends PlayerCommand {

    private final RDS rds;

    public PRS(ACommandSection commandSection, RDS rds){
        super(commandSection);
        this.rds = rds;
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String alias, @NonNull @NotNull String[] args) {
        final EPRSAction action = this.resolveAction(args);
        switch (action) {
            case GIVE -> this.handleGiveCommand(player, args);
            case MAIN -> {
                this.rds.getLogger().info("Main Command");
            }
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
            default -> {
                final int ownedShops = this.countOwnedShops(player);
                final Map<String, Double> trackedCurrencies = this.collectTrackedCurrencies(player);
                new I18n.Builder("info.total", player)
                    .withPlaceholder("owned_shops", ownedShops)
                    .build()
                    .sendMessage();
                new I18n.Builder("info.bank", player)
                    .withPlaceholders(Map.of(
                            "currency_count", trackedCurrencies.size(),
                            "currencies", this.formatTrackedCurrencies(trackedCurrencies)
                    ))
                    .build()
                    .sendMessage();
            }
        }
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

    private int countOwnedShops(
            final @NotNull Player player
    ) {
        int ownedShops = 0;
        for (final Shop shop : this.rds.getShopRepository().findAllShops()) {
            if (shop.isOwner(player.getUniqueId())) {
                ownedShops++;
            }
        }
        return ownedShops;
    }

    private @NotNull Map<String, Double> collectTrackedCurrencies(
            final @NotNull Player player
    ) {
        final Map<String, Double> trackedCurrencies = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final Shop shop : this.rds.getShopRepository().findAllShops()) {
            if (!shop.isOwner(player.getUniqueId())) {
                continue;
            }

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

    private void giveShopBlocks(
            final @NotNull Player target,
            final int amount
    ) {
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
        if (args.length == 3 && "give".equalsIgnoreCase(args[0]) && this.hasPermission(player, EPRSPermission.GIVE)) {
            return StringUtil.copyPartialMatches(args[2], List.of("1", "16", "64"), new ArrayList<>());
        }
        return List.of();
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
}
