package com.raindropcentral.rdq.bounty.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.bounty.RewardItem;
import com.raindropcentral.rplatform.utility.map.Maps;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public final class BountyMainView extends BaseView {

    private final State<RDQCore> rdqCore = initialState("rdqCore");

    @Override
    protected String getKey() {
        return "bounty_main_ui";
    }

    @Override
    protected int getSize() {
        return 3;
    }

    @Override
    protected String[] getLayout() {
        return new String[] {
            "XXXXXXXXX",
            "XXcblmXXX",
            "XXXXXXXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        renderCreateButton(render, player);
        renderBountyListButton(render, player);
        renderLeaderboardButton(render, player);
        renderMyBountiesButton(render, player);
    }

    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderCreateButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('c', UnifiedBuilderFactory
            .item(Material.EMERALD)
            .setName(this.i18n("create_button.name", player).build().component())
            .setLore(this.i18n("create_button.lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build())
            .onClick(ctx -> {
                var rdq = this.rdqCore.get(ctx);
                var config = rdq != null ? rdq.getBountyConfig() : null;
                
                // Create mutable settings map for amount/currency
                var bountySettings = new HashMap<String, Object>();
                bountySettings.put("amount", config != null ? config.minAmount() : BigDecimal.valueOf(100));
                bountySettings.put("currency", config != null ? config.defaultCurrency() : "coins");
                
                ctx.openForPlayer(BountyCreationView.class, Maps.merge(render.getInitialData()).with(Map.of(
                    "target", Optional.empty(),
                    "bounty_settings", bountySettings,
                    "reward_items", new HashSet<RewardItem>(),
                    "inserted_items", new HashMap<Integer, ItemStack>()
                )).immutable());
            });
    }

    private void renderBountyListButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('b', UnifiedBuilderFactory
            .item(Material.BOOK)
            .setName(this.i18n("list_button.name", player).build().component())
            .setLore(this.i18n("list_button.lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build())
            .onClick(ctx -> ctx.openForPlayer(BountyListView.class, render.getInitialData()));
    }

    private void renderLeaderboardButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('l', UnifiedBuilderFactory
            .item(Material.DIAMOND)
            .setName(this.i18n("leaderboard_button.name", player).build().component())
            .setLore(this.i18n("leaderboard_button.lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build())
            .onClick(ctx -> ctx.openForPlayer(BountyLeaderboardView.class, render.getInitialData()));
    }

    private void renderMyBountiesButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('m', UnifiedBuilderFactory
            .item(Material.PAPER)
            .setName(this.i18n("my_bounties_button.name", player).build().component())
            .setLore(this.i18n("my_bounties_button.lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build())
            .onClick(ctx -> ctx.openForPlayer(MyBountiesView.class, render.getInitialData()));
    }
}
