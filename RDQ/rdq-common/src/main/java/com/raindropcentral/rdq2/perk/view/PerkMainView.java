/*
package com.raindropcentral.rdq2.perk.view;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

*/
/**
 * Main perk menu view with navigation to list, categories, and admin.
 *
 * @author JExcellence
 * @since 1.0.0
 *//*

public final class PerkMainView extends BaseView {

    private final State<RDQ> core = initialState("core");

    public PerkMainView() {
        super(null);
    }

    @Override
    protected String getKey() {
        return "perk_main_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "  A L C  ",
            "         ",
            "         ",
            "         ",
            "        x"
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        return Map.of();
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDQ RDQ = this.core.get(render);

        renderAllPerksButton(render, player, RDQ);
        renderCategoriesButton(render, player, RDQ);
        renderCloseButton(render, player);
    }

    private void renderAllPerksButton(RenderContext render, Player player, RDQ RDQ) {
        render.layoutSlot('A', UnifiedBuilderFactory
            .item(Material.NETHER_STAR)
            .setName(this.i18n("all_perks.name", player).build().component())
            .setLore(this.i18n("all_perks.lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(true)
            .build()
        ).onClick(ctx -> {
            RDQ.getViewFrame().open(
                PerkListView.class,
                player,
                Map.of("core", RDQ)
            );
        });
    }

    private void renderCategoriesButton(RenderContext render, Player player, RDQ RDQ) {
        final com.raindropcentral.rdq2.database.repository.RPerkRepository perkRepository = RDQ.getPerkRepository();
        // TODO: Implement getCategories() method in RPerkRepository
        final List<String> categories = List.of();

        render.layoutSlot('L', UnifiedBuilderFactory
            .item(Material.BOOKSHELF)
            .setName(this.i18n("categories.name", player).build().component())
            .setLore(this.i18n("categories.lore", player)
                .with("count", categories.size())
                .build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            RDQ.getViewFrame().open(
                PerkListView.class,
                player,
                Map.of("core", RDQ)
            );
        });
    }

    private void renderCloseButton(RenderContext render, Player player) {
        render.layoutSlot('x', UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(this.i18n("close.name", player).build().component())
            .setLore(this.i18n("close.lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> ctx.closeForPlayer());
    }
}
*/
