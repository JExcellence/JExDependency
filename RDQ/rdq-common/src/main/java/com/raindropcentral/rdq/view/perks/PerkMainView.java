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

/**
 * Main perk menu view providing navigation to perk management features.
 * Similar to RankMainView for consistency.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class PerkMainView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkMainView.class.getName());

    // States
    private final State<RDQ> rdq = initialState("plugin");

    @Override
    protected String getKey() {
        return "perk_main_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
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
            final RDQ plugin = this.rdq.get(render);
            final PerkRegistry registry = plugin.getPerkRegistry();
            final PerkManager perkManager = plugin.getPerkInitializationManager().getPerkManager();
            final RDQPlayer rdqPlayer =
                    plugin.getPlayerRepository().findByAttributes(Map.of("uniqueId", player.getUniqueId()));

            // Calculate statistics
            final List<LoadedPerk> allPerks = registry.getAll();
            final List<RPerk> ownedPerks =
                    rdqPlayer != null ? perkManager.getPerkStateService().getOwnedPerks(rdqPlayer) : List.of();
            final long ownedCount = ownedPerks.size();
            final long lockedCount = allPerks.size() - ownedCount;
            final long activeCount = allPerks.stream()
                    .filter(perk -> perkManager.isActive(player, perk.getId()))
                    .count();

            // Render decorations
            this.renderDecorations(render, player);

            // View All Perks button
            render.layoutSlot('v')
                    .withItem(this.createViewAllPerksButton(player, allPerks.size()))
                    .onClick(click -> {
                        click.closeForPlayer();
                        plugin.getViewFrame().open(
                                PerkListViewFrame.class,
                                player,
                                Map.of("plugin", plugin, "player", rdqPlayer)
                        );
                    });

            // My Active Perks button
            render.layoutSlot('a')
                    .withItem(this.createActivePerksButton(player, (int) activeCount, 10)) // Assuming max 10
                    .onClick(click -> {
                        click.closeForPlayer();
                        plugin.getViewFrame().open(
                                PerkListViewFrame.class,
                                player,
                                Map.of("plugin", plugin, "player", rdqPlayer)
                        );
                    });

            // Perk Statistics button
            render.layoutSlot('s')
                    .withItem(this.createStatisticsButton(player, allPerks.size(), (int) ownedCount, (int) lockedCount, (int) activeCount));

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error rendering perk main view", e);
        }
    }

    /**
     * Renders decorative glass pane borders for visual enhancement.
     *
     * @param render the render context used to populate slots
     * @param player the player viewing the menu
     */
    private void renderDecorations(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.layoutSlot(
                'G',
                UnifiedBuilderFactory
                        .item(Material.CYAN_STAINED_GLASS_PANE)
                        .setName(this.i18n("decoration.name", player).build().component())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        );
    }

    private ItemStack createViewAllPerksButton(@NotNull Player player, int totalPerks) {
        return UnifiedBuilderFactory.item(Material.ENCHANTED_BOOK)
                .setName(this.i18n("view_all_perks.name", player).build().component())
                .setLore(
                        this.i18n("view_all_perks.lore", player)
                                .with("total_perks", totalPerks)
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .setGlowing(true)
                .build();
    }

    private ItemStack createActivePerksButton(@NotNull Player player, int activeCount, int maxCount) {
        return UnifiedBuilderFactory.item(Material.GLOWSTONE_DUST)
                .setName(this.i18n("my_active_perks.name", player).build().component())
                .setLore(
                        this.i18n("my_active_perks.lore", player)
                                .with("active_count", activeCount)
                                .with("max_count", maxCount)
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(activeCount > 0)
                .build();
    }

    private ItemStack createStatisticsButton(@NotNull Player player, int total, int owned, int locked, int active) {
        return UnifiedBuilderFactory.item(Material.KNOWLEDGE_BOOK)
                .setName(this.i18n("perk_statistics.name", player).build().component())
                .setLore(
                        this.i18n("perk_statistics.lore", player)
                                .withAll(
                                        Map.of(
                                                "total_perks", total,
                                                "owned_perks", owned,
                                                "locked_perks", locked,
                                                "active_perks", active
                                        )
                                )
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }
}
