package de.jexcellence.oneblock.view;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.service.WorldManagementService;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class WorldSelectorView extends BaseView {

    private final State<WorldManagementService> worldManagementService = initialState("worldManagementService");
    private final State<Consumer<World>> onWorldSelected = initialState("onWorldSelected");
    private final State<List<World>> availableWorlds = initialState("availableWorlds");

    @Override
    protected String getKey() {
        return "world_selector";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X       X",
            "X  WWW  X",
            "X  WWW  X",
            "XBXRXIXXX"
        };
    }

    @Override
    protected int getSize() {
        return 5;
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderBorder(render);

        if (availableWorlds.get(render) == null) {
            loadAvailableWorlds(render);
        } else {
            renderWorldOptions(render, player);
        }
    }
    
    public void onRender(@NotNull RenderContext render) {
        var player = render.getPlayer();

        renderBorder(render);

        if (availableWorlds.get(render) == null) {
            loadAvailableWorlds(render);
        } else {
            renderWorldOptions(render, player);
        }

        renderNavigationButtons(render, player);
    }

    private void loadAvailableWorlds(@NotNull RenderContext render) {
        var player = render.getPlayer();
        var service = worldManagementService.get(render);

        render.slot(22, UnifiedBuilderFactory
            .item(Material.CLOCK)
            .setName(i18n("world_selector.loading", player).build().component())
            .build()
        );

        service.getAvailableOneBlockWorlds()
            .thenAccept(worlds -> {
                org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.Bukkit.getPluginManager().getPlugin("JExOneblock"), () -> {
                    renderWorldOptions(render, player);
                });
            })
            .exceptionally(throwable -> {
                org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.Bukkit.getPluginManager().getPlugin("JExOneblock"), () -> {
                    renderErrorState(render, player, throwable);
                });
                return null;
            });
    }

    private void renderWorldOptions(@NotNull RenderContext render, @NotNull Player player) {
        var worlds = availableWorlds.get(render);

        if (worlds == null || worlds.isEmpty()) {
            renderNoWorldsState(render, player);
            return;
        }

        render.slot(22, UnifiedBuilderFactory
            .item(Material.AIR)
            .build()
        );

        var worldSlots = new int[]{11, 12, 13, 20, 21, 22};
        
        for (int i = 0; i < Math.min(worlds.size(), worldSlots.length); i++) {
            var world = worlds.get(i);
            var slot = worldSlots[i];
            
            render.slot(slot, createWorldButton(world, player))
                .onClick(click -> handleWorldSelection(click, world));
        }
    }

    private void renderNoWorldsState(@NotNull RenderContext render, @NotNull Player player) {
        render.slot(22, UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("world_selector.no_worlds.title", player).build().component())
            .setLore(i18n("world_selector.no_worlds.description", player).build().children())
            .build()
        );
    }

    private void renderErrorState(@NotNull RenderContext render, @NotNull Player player, @NotNull Throwable error) {
        render.slot(22, UnifiedBuilderFactory
            .item(Material.REDSTONE_BLOCK)
            .setName(i18n("world_selector.error.title", player).build().component())
            .setLore(i18n("world_selector.error.description", player)
                .withPlaceholder("error", error.getMessage())
                .build().children())
            .build()
        );
    }

    private @NotNull ItemStack createWorldButton(@NotNull World world, @NotNull Player player) {
        var iconMaterial = getWorldIconMaterial(world);
        
        return UnifiedBuilderFactory.item(iconMaterial)
            .setName(i18n("world_selector.world.name", player)
                .withPlaceholder("world", world.getName())
                .build().component())
            .setLore(i18n("world_selector.world.description", player)
                .withPlaceholder("world", world.getName())
                .withPlaceholder("environment", world.getEnvironment().name())
                .withPlaceholder("players", String.valueOf(world.getPlayers().size()))
                .build().children())
            .build();
    }

    private @NotNull Material getWorldIconMaterial(@NotNull World world) {
        return switch (world.getEnvironment()) {
            case NORMAL -> Material.GRASS_BLOCK;
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.STONE;
        };
    }

    private void handleWorldSelection(@NotNull me.devnatan.inventoryframework.context.SlotClickContext click, @NotNull World world) {
        var player = click.getPlayer();
        var callback = onWorldSelected.get(click);
        
        if (callback != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
            
            callback.accept(world);
        }
    }

    public void renderNavigationButtons(@NotNull RenderContext render, @NotNull Player player) {
        render.slot(36, UnifiedBuilderFactory
            .item(Material.ARROW)
            .setName(i18n("general.back", player).build().component())
            .build()
        ).onClick(click -> {
            click.getPlayer().playSound(click.getPlayer().getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            handleBackButtonClick(click);
        });

        render.slot(38, UnifiedBuilderFactory
            .item(Material.EMERALD)
            .setName(i18n("world_selector.refresh", player).build().component())
            .build()
        ).onClick(click -> {
            click.getPlayer().playSound(click.getPlayer().getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            loadAvailableWorlds(render);
        });

        render.slot(40, UnifiedBuilderFactory
            .item(Material.BOOK)
            .setName(i18n("world_selector.info.title", player).build().component())
            .setLore(i18n("world_selector.info.description", player).build().children())
            .build()
        );
    }
    
    private void renderBorder(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(net.kyori.adventure.text.Component.text(""))
            .build()
        );
    }
    
    protected void handleBackButtonClick(@NotNull me.devnatan.inventoryframework.context.SlotClickContext click) {
        click.getPlayer().closeInventory();
    }
}
