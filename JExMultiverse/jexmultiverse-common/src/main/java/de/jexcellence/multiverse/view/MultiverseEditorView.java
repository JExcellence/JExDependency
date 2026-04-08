package de.jexcellence.multiverse.view;

import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.multiverse.database.entity.MVWorld;
import de.jexcellence.multiverse.database.repository.MVWorldRepository;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Interactive view for editing world settings in JExMultiverse.
 * <p>
 * This view provides a 6-row GUI for editing world properties including:
 * <ul>
 *   <li>Spawn location setting</li>
 *   <li>Global spawn toggle</li>
 *   <li>PvP toggle</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MultiverseEditorView extends BaseView {

    /**
     * State holder for the JavaPlugin instance.
     */
    private final State<JavaPlugin> plugin = initialState("plugin");

    /**
     * State holder for the MVWorldRepository.
     */
    private final State<MVWorldRepository> repository = initialState("repository");

    /**
     * State holder for the Executor.
     */
    private final State<Executor> executor = initialState("executor");

    /**
     * Mutable state holder for the world being edited.
     */
    private final MutableState<MVWorld> targetWorld = mutableState(null);

    /**
     * Constructs a new MultiverseEditorView with no parent view.
     */
    public MultiverseEditorView() {
        super(null);
    }

    @Override
    protected @NotNull String getKey() {
        return "multiverse_editor_ui";
    }

    @Override
    protected @NotNull String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X       X",
            "X S G P X",
            "X       X",
            "XXXXXXXXX",
            "        v"
        };
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull me.devnatan.inventoryframework.context.OpenContext open
    ) {
        final MVWorld world = (MVWorld) ((Map<String, Object>) open.getInitialData()).get("world");
        return Map.of("world_name", world != null ? world.getIdentifier() : "Unknown");
    }

    @Override
    public void onResume(
            final @NotNull Context originContext,
            final @NotNull Context targetContext
    ) {
        targetContext.update();
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext renderContext,
            final @NotNull Player contextPlayer
    ) {
        this.targetWorld.set(
            (MVWorld) ((Map<String, Object>) renderContext.getInitialData()).get("world"),
            renderContext
        );

        this.renderSpawnLocationSlot(renderContext, contextPlayer);
        this.renderGlobalSpawnToggle(renderContext, contextPlayer);
        this.renderPvpToggle(renderContext, contextPlayer);
        this.renderSaveButton(renderContext, contextPlayer);
    }


    /**
     * Renders the spawn location setting slot.
     * <p>
     * Clicking this slot sets the world's spawn to the player's current location.
     * </p>
     *
     * @param renderContext the current rendering context
     * @param contextPlayer the player viewing the interface
     */
    private void renderSpawnLocationSlot(
            final @NotNull RenderContext renderContext,
            final @NotNull Player contextPlayer
    ) {
        renderContext
            .layoutSlot('S')
            .watch(this.targetWorld)
            .renderWith(() -> {
                final MVWorld currentWorld = this.targetWorld.get(renderContext);
                final String formattedLocation = currentWorld.getFormattedSpawnLocation();

                return UnifiedBuilderFactory.item(Material.COMPASS)
                    .setName(
                        this.i18n("spawn_location.name", contextPlayer)
                            .build().component()
                    )
                    .setLore(
                        this.i18n("spawn_location.lore", contextPlayer)
                            .withPlaceholder("spawn_location", formattedLocation)
                            .build().children()
                    )
                    .build();
            })
            .onClick(clickContext -> {
                this.handleSetSpawnLocation(clickContext, contextPlayer);
            });
    }

    /**
     * Handles setting the spawn location to the player's current position.
     *
     * @param clickContext the click context
     * @param requestingPlayer the player requesting the change
     */
    private void handleSetSpawnLocation(
            final @NotNull Context clickContext,
            final @NotNull Player requestingPlayer
    ) {
        final MVWorld world = this.targetWorld.get(clickContext);
        final var newLocation = requestingPlayer.getLocation();

        // Update the world's spawn location
        world.setSpawnLocation(newLocation);
        this.targetWorld.set(world, clickContext);

        this.i18n("spawn_location.updated", requestingPlayer)
            .includePrefix()
            .withPlaceholder("spawn_location", world.getFormattedSpawnLocation())
            .build()
            .sendMessage();

        clickContext.update();
    }

    /**
     * Renders the global spawn toggle slot.
     * <p>
     * Clicking this slot toggles whether this world is the global spawn location.
     * </p>
     *
     * @param renderContext the current rendering context
     * @param contextPlayer the player viewing the interface
     */
    private void renderGlobalSpawnToggle(
            final @NotNull RenderContext renderContext,
            final @NotNull Player contextPlayer
    ) {
        renderContext
            .layoutSlot('G')
            .watch(this.targetWorld)
            .renderWith(() -> {
                final MVWorld currentWorld = this.targetWorld.get(renderContext);
                final boolean isGlobalSpawn = currentWorld.isGlobalizedSpawn();

                return UnifiedBuilderFactory.item(isGlobalSpawn ? Material.ENDER_EYE : Material.ENDER_PEARL)
                    .setName(
                        this.i18n("global_spawn.name", contextPlayer)
                            .build().component()
                    )
                    .setLore(
                        this.i18n("global_spawn.lore", contextPlayer)
                            .withPlaceholder("status", isGlobalSpawn ? "Enabled" : "Disabled")
                            .build().children()
                    )
                    .setGlowing(isGlobalSpawn)
                    .build();
            })
            .onClick(clickContext -> {
                this.handleToggleGlobalSpawn(clickContext, contextPlayer);
            });
    }

    /**
     * Handles toggling the global spawn status.
     *
     * @param clickContext the click context
     * @param requestingPlayer the player requesting the change
     */
    private void handleToggleGlobalSpawn(
            final @NotNull Context clickContext,
            final @NotNull Player requestingPlayer
    ) {
        final MVWorld world = this.targetWorld.get(clickContext);
        final MVWorldRepository worldRepository = this.repository.get(clickContext);
        final boolean currentStatus = world.isGlobalizedSpawn();

        // If trying to enable global spawn, check if another world already has it
        if (!currentStatus) {
            worldRepository.findByGlobalSpawnAsync().thenAccept(existingGlobalSpawn -> {
                Bukkit.getScheduler().runTask(this.plugin.get(clickContext), () -> {
                    if (existingGlobalSpawn.isPresent() && !existingGlobalSpawn.get().getIdentifier().equals(world.getIdentifier())) {
                        // Another world is already global spawn - warn user but still allow toggle
                        this.i18n("global_spawn.warning_other_world", requestingPlayer)
                            .includePrefix()
                            .withPlaceholder("other_world", existingGlobalSpawn.get().getIdentifier())
                            .build()
                            .sendMessage();
                    }

                    // Toggle the status
                    world.setGlobalizedSpawn(true);
                    this.targetWorld.set(world, clickContext);

                    this.i18n("global_spawn.enabled", requestingPlayer)
                        .includePrefix()
                        .withPlaceholder("world_name", world.getIdentifier())
                        .build()
                        .sendMessage();
                });
            });
        } else {
            // Disabling global spawn - no check needed
            world.setGlobalizedSpawn(false);
            this.targetWorld.set(world, clickContext);

            this.i18n("global_spawn.disabled", requestingPlayer)
                .includePrefix()
                .withPlaceholder("world_name", world.getIdentifier())
                .build()
                .sendMessage();
        }
    }

    /**
     * Renders the PvP toggle slot.
     * <p>
     * Clicking this slot toggles whether PvP is enabled in this world.
     * </p>
     *
     * @param renderContext the current rendering context
     * @param contextPlayer the player viewing the interface
     */
    private void renderPvpToggle(
            final @NotNull RenderContext renderContext,
            final @NotNull Player contextPlayer
    ) {
        renderContext
            .layoutSlot('P')
            .watch(this.targetWorld)
            .renderWith(() -> {
                final MVWorld currentWorld = this.targetWorld.get(renderContext);
                final boolean isPvpEnabled = currentWorld.isPvpEnabled();

                return UnifiedBuilderFactory.item(isPvpEnabled ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD)
                    .setName(
                        this.i18n("pvp_toggle.name", contextPlayer)
                            .build().component()
                    )
                    .setLore(
                        this.i18n("pvp_toggle.lore", contextPlayer)
                            .withPlaceholder("status", isPvpEnabled ? "Enabled" : "Disabled")
                            .build().children()
                    )
                    .setGlowing(isPvpEnabled)
                    .build();
            })
            .onClick(clickContext -> {
                this.handleTogglePvp(clickContext, contextPlayer);
            });
    }

    /**
     * Handles toggling the PvP status.
     *
     * @param clickContext the click context
     * @param requestingPlayer the player requesting the change
     */
    private void handleTogglePvp(
            final @NotNull Context clickContext,
            final @NotNull Player requestingPlayer
    ) {
        final MVWorld world = this.targetWorld.get(clickContext);
        final boolean newStatus = !world.isPvpEnabled();

        world.setPvpEnabled(newStatus);
        this.targetWorld.set(world, clickContext);

        // Also update the Bukkit world's PvP setting
        final var bukkitWorld = Bukkit.getWorld(world.getIdentifier());
        if (bukkitWorld != null) {
            bukkitWorld.setPVP(newStatus);
        }

        final String messageKey = newStatus ? "pvp_toggle.enabled" : "pvp_toggle.disabled";
        this.i18n(messageKey, requestingPlayer)
            .includePrefix()
            .withPlaceholder("world_name", world.getIdentifier())
            .build()
            .sendMessage();

        clickContext.update();
    }

    /**
     * Renders the save changes button.
     * <p>
     * Clicking this button saves all changes to the database.
     * </p>
     *
     * @param renderContext the current rendering context
     * @param contextPlayer the player viewing the interface
     */
    private void renderSaveButton(
            final @NotNull RenderContext renderContext,
            final @NotNull Player contextPlayer
    ) {
        renderContext
            .layoutSlot(
                'v',
                UnifiedBuilderFactory.item(new Proceed().getHead(contextPlayer))
                    .setName(
                        this.i18n("save_changes.name", contextPlayer)
                            .build().component()
                    )
                    .setLore(
                        this.i18n("save_changes.lore", contextPlayer)
                            .build().children()
                    )
                    .build()
            )
            .onClick(clickContext -> {
                this.handleSaveChanges(clickContext, contextPlayer);
            });
    }

    /**
     * Handles saving all changes to the database.
     *
     * @param clickContext the click context
     * @param requestingPlayer the player requesting the save
     */
    private void handleSaveChanges(
            final @NotNull Context clickContext,
            final @NotNull Player requestingPlayer
    ) {
        final MVWorld worldToSave = this.targetWorld.get(clickContext);
        final MVWorldRepository worldRepository = this.repository.get(clickContext);
        final JavaPlugin pluginInstance = this.plugin.get(clickContext);
        final Executor executorInstance = this.executor.get(clickContext);

        this.i18n("save.processing", requestingPlayer)
            .includePrefix()
            .withPlaceholder("world_name", worldToSave.getIdentifier())
            .build()
            .sendMessage();

        // If setting as global spawn, clear other global spawns first
        if (worldToSave.isGlobalizedSpawn()) {
            worldRepository.clearGlobalSpawnExcept(worldToSave.getIdentifier())
                .thenCompose(v -> worldRepository.saveWorld(worldToSave))
                .thenAcceptAsync(savedWorld -> {
                    Bukkit.getScheduler().runTask(pluginInstance, () -> {
                        this.i18n("save.success", requestingPlayer)
                            .includePrefix()
                            .withPlaceholder("world_name", worldToSave.getIdentifier())
                            .build()
                            .sendMessage();
                    });
                }, executorInstance)
                .exceptionally(exception -> {
                    Bukkit.getScheduler().runTask(pluginInstance, () -> {
                        this.i18n("save.failed", requestingPlayer)
                            .includePrefix()
                            .withPlaceholder("world_name", worldToSave.getIdentifier())
                            .withPlaceholder("error", exception.getMessage())
                            .build()
                            .sendMessage();
                    });
                    return null;
                });
        } else {
            worldRepository.saveWorld(worldToSave)
                .thenAcceptAsync(savedWorld -> {
                    Bukkit.getScheduler().runTask(pluginInstance, () -> {
                        this.i18n("save.success", requestingPlayer)
                            .includePrefix()
                            .withPlaceholder("world_name", worldToSave.getIdentifier())
                            .build()
                            .sendMessage();
                    });
                }, executorInstance)
                .exceptionally(exception -> {
                    Bukkit.getScheduler().runTask(pluginInstance, () -> {
                        this.i18n("save.failed", requestingPlayer)
                            .includePrefix()
                            .withPlaceholder("world_name", worldToSave.getIdentifier())
                            .withPlaceholder("error", exception.getMessage())
                            .build()
                            .sendMessage();
                    });
                    return null;
                });
        }

        clickContext.closeForPlayer();
    }
}
