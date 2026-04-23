package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.machine.MachineType;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated catalogue of registered {@link MachineType}s. Icon comes
 * from each type's {@code iconMaterial} key; parsing falls back to a
 * beacon when the key is unknown.
 */
public class MachineTypeOverviewView extends PaginatedView<MachineType> {

    private final State<JExQuests> plugin = initialState("plugin");

    public MachineTypeOverviewView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "machine_type_overview_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "   <p>   "
        };
    }

    @Override
    protected @NotNull CompletableFuture<List<MachineType>> loadData(@NotNull Context ctx) {
        final List<MachineType> sorted = new ArrayList<>(this.plugin.get(ctx).machineRegistry().all());
        sorted.sort(Comparator.comparing(MachineType::identifier));
        return CompletableFuture.completedFuture(sorted);
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull MachineType entry
    ) {
        final var player = ctx.getPlayer();
        final Material icon = resolveMaterial(entry.iconMaterial());

        builder.withItem(createItem(
                icon,
                i18n("entry.name", player)
                        .withPlaceholder("display_name", entry.displayName())
                        .build().component(),
                i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "identifier", entry.identifier(),
                                "category", entry.category(),
                                "description", entry.description().isEmpty() ? "—" : entry.description(),
                                "size", entry.width() + "x" + entry.height() + "x" + entry.depth(),
                                "index", index + 1
                        ))
                        .build().children()
        )).onClick(click -> click.openForPlayer(
                MachineTypeDetailView.class,
                Map.of(
                        "plugin", this.plugin.get(click),
                        "type", entry,
                        "initialData", click.getInitialData()
                )
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
