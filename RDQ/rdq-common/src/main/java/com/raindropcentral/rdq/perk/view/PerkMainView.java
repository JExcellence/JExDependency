package com.raindropcentral.rdq.perk.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.perk.repository.PerkRepository;
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

/**
 * Main perk menu view with navigation to list, categories, and admin.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class PerkMainView extends BaseView {

    private final State<RDQCore> core = initialState("core");

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
        final RDQCore rdqCore = this.core.get(render);

        renderAllPerksButton(render, player, rdqCore);
        renderCategoriesButton(render, player, rdqCore);
        renderCloseButton(render, player);
    }

    private void renderAllPerksButton(RenderContext render, Player player, RDQCore rdqCore) {
        render.layoutSlot('A', UnifiedBuilderFactory
            .item(Material.NETHER_STAR)
            .setName(this.i18n("all_perks.name", player).build().component())
            .setLore(this.i18n("all_perks.lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(true)
            .build()
        ).onClick(ctx -> {
            rdqCore.getViewFrame().open(
                PerkListView.class,
                player,
                Map.of("core", rdqCore)
            );
        });
    }

    private void renderCategoriesButton(RenderContext render, Player player, RDQCore rdqCore) {
        final PerkRepository perkRepository = rdqCore.getPerkRepository();
        final List<String> categories = perkRepository != null ? perkRepository.getCategories() : List.of();

        render.layoutSlot('L', UnifiedBuilderFactory
            .item(Material.BOOKSHELF)
            .setName(this.i18n("categories.name", player).build().component())
            .setLore(this.i18n("categories.lore", player)
                .with("count", categories.size())
                .build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            rdqCore.getViewFrame().open(
                PerkListView.class,
                player,
                Map.of("core", rdqCore)
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
