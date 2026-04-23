package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.machine.MachineType;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

/**
 * Single-type detail view — icon, size, category, and
 * plugin-specific property summary. Back-navigates to
 * {@link MachineTypeOverviewView}.
 */
public class MachineTypeDetailView extends BaseView {

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<MachineType> type = initialState("type");

    public MachineTypeDetailView() {
        super(MachineTypeOverviewView.class);
    }

    @Override
    protected String translationKey() {
        return "machine_type_detail_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                " i   s   ",
                "         ",
                " p     c ",
                "        r"
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        final MachineType t = this.type.get(render);
        renderIcon(render, player, t);
        renderSize(render, player, t);
        renderProperties(render, player, t);
        renderCategory(render, player, t);
    }

    private void renderIcon(@NotNull RenderContext render, @NotNull Player player, @NotNull MachineType t) {
        render.layoutSlot('i', createItem(
                resolveMaterial(t.iconMaterial()),
                i18n("icon.name", player)
                        .withPlaceholder("display_name", t.displayName())
                        .build().component(),
                i18n("icon.lore", player)
                        .withPlaceholder("identifier", t.identifier())
                        .withPlaceholder("description", t.description().isEmpty() ? "—" : t.description())
                        .build().children()
        ));
    }

    private void renderSize(@NotNull RenderContext render, @NotNull Player player, @NotNull MachineType t) {
        render.layoutSlot('s', createItem(
                Material.COMPASS,
                i18n("size.name", player).build().component(),
                i18n("size.lore", player)
                        .withPlaceholders(Map.of(
                                "width", String.valueOf(t.width()),
                                "height", String.valueOf(t.height()),
                                "depth", String.valueOf(t.depth())
                        ))
                        .build().children()
        ));
    }

    private void renderProperties(@NotNull RenderContext render, @NotNull Player player, @NotNull MachineType t) {
        render.layoutSlot('p', createItem(
                Material.WRITABLE_BOOK,
                i18n("properties.name", player).build().component(),
                i18n("properties.lore", player)
                        .withPlaceholder("count", String.valueOf(t.properties().size()))
                        .build().children()
        ));
    }

    private void renderCategory(@NotNull RenderContext render, @NotNull Player player, @NotNull MachineType t) {
        render.layoutSlot('c', createItem(
                Material.PAPER,
                i18n("category.name", player)
                        .withPlaceholder("category", t.category())
                        .build().component(),
                i18n("category.lore", player)
                        .withPlaceholder("category", t.category())
                        .build().children()
        ));
    }

    private static @NotNull Material resolveMaterial(@NotNull String key) {
        try {
            return Material.valueOf(key.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return Material.BEACON;
        }
    }
}
