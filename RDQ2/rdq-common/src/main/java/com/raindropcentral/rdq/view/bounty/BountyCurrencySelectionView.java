package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.utility.map.Maps;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * View for selecting currencies to add as bounty rewards.
 * Integrates with JExEconomy to display available currencies.
 * 
 * Requirements: 5.1, 5.3, 5.4, 5.5
 */
public class BountyCurrencySelectionView extends BaseView {

    // Immutable state
    private final State<RDQ> rdq = initialState("plugin");
    
    // Mutable states shared with BountyCreationView
    private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
    private final MutableState<Map<String, Double>> rewardCurrencies = initialState("reward_currencies");
    
    private static final int CURRENCIES_PER_PAGE = 28; // 4 rows of 7

    public BountyCurrencySelectionView() {
        super(BountyCreationView.class);
    }

    @Override
    protected String getKey() {
        return "bounty_currency_selection_ui";
    }

    @Override
    protected int getSize() {
        return 6;
    }

    @Override
    protected String[] getLayout() {
        return new String[] {
                "XXXXXXXXX",
                "ccccccccc",
                "ccccccccc",
                "ccccccccc",
                "ccccccccc",
                "XXXXbXXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        loadAndRenderCurrencies(render, player);
        renderBackButton(render, player);
    }

    /**
     * Renders decorative glass panes.
     */
    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
                .item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(Component.empty())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    /**
     * Loads currencies from JExEconomy and renders them.
     */
    private void loadAndRenderCurrencies(@NotNull RenderContext render, @NotNull Player player) {
        // Get JExEconomy instance from Bukkit plugin manager
        var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("JExEconomy");
        
        if (!(plugin instanceof de.jexcellence.economy.JExEconomy jexEconomy)) {
            renderError(render, player, "JExEconomy not available");
            return;
        }
        
        // Get all currencies - JExEconomy returns Map<Long, Currency>
        var currencyMap = jexEconomy.getImpl().getCurrencies();
        var currencies = new ArrayList<de.jexcellence.economy.database.entity.Currency>(currencyMap != null ? currencyMap.values() : java.util.Collections.emptyList());
        
        if (currencies.isEmpty()) {
            renderEmpty(render, player);
            return;
        }
        
        // Render currencies in slots
        renderCurrencies(render, player, currencies);
    }

    /**
     * Renders currency entries.
     */
    private void renderCurrencies(@NotNull RenderContext render, @NotNull Player player, @NotNull List<Currency> currencies) {
        int slotIndex = 0;
        
        for (int row = 1; row <= 4 && slotIndex < currencies.size(); row++) {
            for (int col = 0; col < 9 && slotIndex < currencies.size(); col++) {
                var currency = currencies.get(slotIndex);
                int actualSlot = (row * 9) + col;
                
                render.slot(actualSlot, createCurrencyItem(currency, player, render))
                        .onClick(ctx -> handleCurrencyClick(ctx, currency, player));
                
                slotIndex++;
            }
        }
    }

    /**
     * Creates an ItemStack representing a currency.
     */
    private org.bukkit.inventory.ItemStack createCurrencyItem(@NotNull Currency currency, @NotNull Player player, @NotNull RenderContext render) {
        var currentRewards = this.rewardCurrencies.get(render);
        boolean isSelected = currentRewards.containsKey(currency.getIdentifier());
        
        var builder = UnifiedBuilderFactory
                .item(currency.getIcon())
                .setName(this.i18n("currency_entry.name", player)
                        .with(Placeholder.of("currency_name", currency.getSymbol()))
                        .with(Placeholder.of("currency_id", currency.getIdentifier()))
                        .build().component());
        
        // Build lore
        var loreBuilder = this.i18n("currency_entry.lore", player)
                .with(Placeholder.of("currency_prefix", currency.getPrefix()))
                .with(Placeholder.of("currency_suffix", currency.getSuffix()));
        
        if (isSelected) {
            double amount = currentRewards.get(currency.getIdentifier());
            loreBuilder.with(Placeholder.of("selected_amount", amount));
            builder.setLore(this.i18n("currency_entry.selected_lore", player)
                    .with(Placeholder.of("amount", amount))
                    .build().splitLines());
        } else {
            builder.setLore(loreBuilder.build().splitLines());
        }
        
        return builder.build();
    }

    /**
     * Handles currency selection click.
     * Adds a fixed amount of 100 for now (TODO: implement amount input)
     */
    private void handleCurrencyClick(@NotNull me.devnatan.inventoryframework.context.Context ctx, 
                                    @NotNull Currency currency, 
                                    @NotNull Player player) {
        var rewards = this.rewardCurrencies.get(ctx);
        
        // Toggle currency - if already selected, remove it; otherwise add with amount 100
        if (rewards.containsKey(currency.getIdentifier())) {
            rewards.remove(currency.getIdentifier());
            this.i18n("currency_removed.message", player)
                    .withPrefix()
                    .with(Placeholder.of("currency_name", currency.getSymbol()))
                    .send();
        } else {
            rewards.put(currency.getIdentifier(), 100.0);
            this.i18n("currency_added.message", player)
                    .withPrefix()
                    .with(Placeholder.of("currency_name", currency.getSymbol()))
                    .with(Placeholder.of("amount", 100.0))
                    .send();
        }
        
        // Refresh view
        ctx.update();
    }

    /**
     * Renders empty state.
     */
    private void renderEmpty(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('c', UnifiedBuilderFactory
                .item(Material.BARRIER)
                .setName(this.i18n("empty.name", player).build().component())
                .setLore(this.i18n("empty.lore", player).build().splitLines())
                .build());
    }

    /**
     * Renders error state.
     */
    private void renderError(@NotNull RenderContext render, @NotNull Player player, @NotNull String error) {
        render.layoutSlot('c', UnifiedBuilderFactory
                .item(Material.REDSTONE_BLOCK)
                .setName(this.i18n("error.name", player).build().component())
                .setLore(this.i18n("error.lore", player)
                        .with(Placeholder.of("error", error))
                        .build().splitLines())
                .build());
    }

    /**
     * Renders back button.
     */
    private void renderBackButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('b', UnifiedBuilderFactory
                .item(Material.ARROW)
                .setName(this.i18n("back_button.name", player).build().component())
                .setLore(this.i18n("back_button.lore", player).build().splitLines())
                .build())
                .onClick(ctx -> ctx.openForPlayer(BountyCreationView.class, ctx.getInitialData()));
    }
}
