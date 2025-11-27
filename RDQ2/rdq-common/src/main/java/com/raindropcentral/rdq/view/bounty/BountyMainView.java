package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.utility.map.Maps;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class BountyMainView extends BaseView {

    private final State<RDQ> rdq = initialState("plugin");

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
                "XXcblsmXX",
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
                .build())
                .onClick(ctx -> ctx.openForPlayer(BountyCreationView.class, Maps.merge(render.getInitialData()).with(Map.of(
                        "target", Optional.empty(),
                        "reward_items", new HashSet<>(),
                        "reward_currencies", new HashMap<>(),
                        "bounty", Optional.empty(),
                        "inserted_items", new HashMap<>()
                )).immutable()));
    }

    private void renderBountyListButton(
            @NotNull RenderContext render,
            @NotNull Player player
    ) {
        render.layoutSlot('l', UnifiedBuilderFactory
                .item(Material.BOOK)
                .setName(this.i18n("list_button.name", player).build().component())
                .setLore(this.i18n("list_button.lore", player).build().splitLines())
                .build())
                .onClick(ctx -> ctx.openForPlayer(BountyListView.class, render.getInitialData()));
    }

    private void renderLeaderboardButton(
            @NotNull RenderContext render,
            @NotNull Player player
    ) {
        render.layoutSlot('s', UnifiedBuilderFactory
                .item(Material.DIAMOND)
                .setName(this.i18n("leaderboard_button.name", player).build().component())
                .setLore(this.i18n("leaderboard_button.lore", player).build().splitLines())
                .build())
                .onClick(ctx -> ctx.openForPlayer(BountyLeaderboardView.class, render.getInitialData()));
    }

    private void renderMyBountiesButton(
            @NotNull RenderContext render,
            @NotNull Player player
    ) {
        render.layoutSlot('m', UnifiedBuilderFactory
                .item(Material.PAPER)
                .setName(this.i18n("my_bounties_button.name", player).build().component())
                .setLore(this.i18n("my_bounties_button.lore", player).build().splitLines())
                .build())
                .onClick(ctx -> ctx.openForPlayer(MyBountiesView.class, render.getInitialData()));
    }
}
