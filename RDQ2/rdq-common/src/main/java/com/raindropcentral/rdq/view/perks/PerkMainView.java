package com.raindropcentral.rdq.view.perks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.perk.runtime.LoadedPerk;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PerkMainView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkMainView.class.getName());

    private final State<RDQ> rdq = initialState("plugin");

    @Override
    protected String getKey() {
        return "perk_main_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(@NotNull OpenContext openContext) {
        return Map.of();
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "GGGGGGGGG",
                "G       G",
                "G v a s G",
                "G       G",
                "GGGGGGGGG"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        try {
            var plugin = rdq.get(render);
            var registry = plugin.getPerkRegistry();
            var perkManager = plugin.getPerkInitializationManager().getPerkManager();
            var rdqPlayer = plugin.getPlayerRepository().findByAttributes(Map.of("uniqueId", player.getUniqueId()));

            var allPerks = registry.getAll();
            var ownedPerks = rdqPlayer != null ? perkManager.getPerkStateService().getOwnedPerks(rdqPlayer) : List.<RPerk>of();
            var ownedCount = ownedPerks.size();
            var lockedCount = allPerks.size() - ownedCount;
            var activeCount = allPerks.stream()
                    .filter(perk -> perkManager.isActive(player, perk.getId()))
                    .count();

            renderDecorations(render, player);

            render.layoutSlot('v')
                    .withItem(createViewAllPerksButton(player, allPerks.size()))
                    .onClick(click -> {
                        click.closeForPlayer();
                        plugin.getViewFrame().open(PerkListViewFrame.class, player, Map.of("plugin", plugin, "player", rdqPlayer));
                    });

            render.layoutSlot('a')
                    .withItem(createActivePerksButton(player, (int) activeCount, 10))
                    .onClick(click -> {
                        click.closeForPlayer();
                        plugin.getViewFrame().open(PerkListViewFrame.class, player, Map.of("plugin", plugin, "player", rdqPlayer));
                    });

            render.layoutSlot('s')
                    .withItem(createStatisticsButton(player, allPerks.size(), (int) ownedCount, (int) lockedCount, (int) activeCount));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error rendering perk main view", e);
        }
    }

    private void renderDecorations(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('G', UnifiedBuilderFactory
                .item(Material.CYAN_STAINED_GLASS_PANE)
                .setName(i18n("decoration.name", player).build().component())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    private ItemStack createViewAllPerksButton(@NotNull Player player, int totalPerks) {
        return UnifiedBuilderFactory.item(Material.ENCHANTED_BOOK)
                .setName(i18n("view_all_perks.name", player).build().component())
                .setLore(i18n("view_all_perks.lore", player).with("total_perks", totalPerks).build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .setGlowing(true)
                .build();
    }

    private ItemStack createActivePerksButton(@NotNull Player player, int activeCount, int maxCount) {
        return UnifiedBuilderFactory.item(Material.GLOWSTONE_DUST)
                .setName(i18n("my_active_perks.name", player).build().component())
                .setLore(i18n("my_active_perks.lore", player)
                        .with("active_count", activeCount)
                        .with("max_count", maxCount)
                        .build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(activeCount > 0)
                .build();
    }

    private ItemStack createStatisticsButton(@NotNull Player player, int total, int owned, int locked, int active) {
        return UnifiedBuilderFactory.item(Material.KNOWLEDGE_BOOK)
                .setName(i18n("perk_statistics.name", player).build().component())
                .setLore(i18n("perk_statistics.lore", player)
                        .withAll(Map.of(
                                "total_perks", total,
                                "owned_perks", owned,
                                "locked_perks", locked,
                                "active_perks", active
                        ))
                        .build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }
}
