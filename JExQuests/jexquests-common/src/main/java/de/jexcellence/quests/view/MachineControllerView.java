package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.BaseView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.Machine;
import de.jexcellence.quests.machine.MachineType;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

/**
 * Per-placement machine controller — shown when a player right-clicks
 * a placed machine block. Surfaces the identity/location slot, the
 * registered type's metadata, and a dismantle button for owners /
 * admins. Subsystem-specific UIs (storage grids, upgrade slots,
 * recipe selection) hook in via additional views openable from this
 * hub.
 */
public class MachineControllerView extends BaseView {

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<Machine> machine = initialState("machine");

    public MachineControllerView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "machine_controller_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "    i    ",
                "         ",
                " l  s  d ",
                "         "
        };
    }

    @Override
    protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
        final Machine m = this.machine.get(render);
        final JExQuests quests = this.plugin.get(render);
        final MachineType type = quests.machineRegistry().get(m.getMachineType()).orElse(null);

        renderIcon(render, player, m, type);
        renderLocation(render, player, m);
        renderStorage(render, player, m, quests);
        renderDismantle(render, player, m, quests);
    }

    private void renderStorage(@NotNull RenderContext render, @NotNull Player player,
                               @NotNull Machine m, @NotNull JExQuests quests) {
        final MachineType type = quests.machineRegistry().get(m.getMachineType()).orElse(null);
        render.layoutSlot('s', createItem(
                Material.CHEST,
                i18n("storage.name", player).build().component(),
                i18n("storage.lore", player).build().children()
        )).onClick(click -> {
            if (type == null) {
                click.closeForPlayer();
                return;
            }
            click.closeForPlayer();
            // Scheduled so the current IF close settles before the
            // native inventory open takes over the viewport.
            quests.getPlugin().getServer().getScheduler().runTask(
                    quests.getPlugin(),
                    () -> de.jexcellence.quests.machine.MachineStorageInventory.open(player, m, type)
            );
        });
    }

    private void renderIcon(@NotNull RenderContext render, @NotNull Player player,
                            @NotNull Machine m, MachineType type) {
        final Material icon = type != null ? parseMaterial(type.iconMaterial()) : Material.BEACON;
        final String displayName = type != null ? type.displayName() : m.getMachineType();

        render.layoutSlot('i', createItem(
                icon,
                i18n("icon.name", player)
                        .withPlaceholder("display_name", displayName)
                        .build().component(),
                i18n("icon.lore", player)
                        .withPlaceholders(Map.of(
                                "identifier", m.getMachineType(),
                                "owner", m.getOwnerUuid().toString(),
                                "facing", m.getFacing()
                        ))
                        .build().children()
        ));
    }

    private void renderLocation(@NotNull RenderContext render, @NotNull Player player, @NotNull Machine m) {
        render.layoutSlot('l', createItem(
                Material.COMPASS,
                i18n("location.name", player).build().component(),
                i18n("location.lore", player)
                        .withPlaceholders(Map.of(
                                "world", m.getWorld(),
                                "x", String.valueOf(m.getX()),
                                "y", String.valueOf(m.getY()),
                                "z", String.valueOf(m.getZ())
                        ))
                        .build().children()
        ));
    }

    private void renderDismantle(@NotNull RenderContext render, @NotNull Player player,
                                 @NotNull Machine m, @NotNull JExQuests quests) {
        final boolean canDismantle = m.getOwnerUuid().equals(player.getUniqueId())
                || player.hasPermission("jexquests.machine.admin");
        if (!canDismantle) return;

        render.layoutSlot('d', createItem(
                Material.TNT,
                i18n("dismantle.name", player).build().component(),
                i18n("dismantle.lore", player).build().children()
        )).onClick(click -> {
            quests.machineService().dismantleAsync(m);
            click.closeForPlayer();
        });
    }

    private static @NotNull Material parseMaterial(@NotNull String key) {
        try {
            return Material.valueOf(key.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return Material.BEACON;
        }
    }
}
