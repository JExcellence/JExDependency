package de.jexcellence.multiverse.command.multiverse;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.multiverse.JExMultiverse;
import de.jexcellence.multiverse.database.entity.MVWorld;
import de.jexcellence.multiverse.factory.WorldFactory;
import de.jexcellence.multiverse.service.IMultiverseService;
import de.jexcellence.multiverse.type.MVWorldType;
import de.jexcellence.multiverse.view.MultiverseEditorView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command handler for the /multiverse command.
 * <p>
 * Provides subcommands for world management:
 * <ul>
 *   <li>create - Create a new world</li>
 *   <li>delete - Delete an existing world</li>
 *   <li>edit - Open the world editor GUI</li>
 *   <li>teleport - Teleport to a world</li>
 *   <li>load - Load a world from database</li>
 *   <li>help - Show help message</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PMultiverse extends PlayerCommand {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("PMultiverse");

    private final JExMultiverse multiverse;

    /**
     * Constructs a new PMultiverse command handler.
     *
     * @param commandSection the command section configuration
     * @param multiverse  the main plugin context
     */
    public PMultiverse(
            @NotNull PMultiverseSection commandSection,
            @NotNull JExMultiverse multiverse
    ) {
        super(commandSection);
        this.multiverse = multiverse;
    }

    private JavaPlugin getPlugin() {
        return multiverse.getPlugin();
    }

    private IMultiverseService getMultiverseService() {
        return multiverse.getMultiverseService();
    }

    private WorldFactory getWorldFactory() {
        return multiverse.getWorldFactory();
    }

    private ViewFrame getViewFrame() {
        return multiverse.getViewFrame();
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, EMultiversePermission.COMMAND)) return;

        var action = enumParameterOrElse(args, 0, EMultiverseAction.class, EMultiverseAction.HELP);

        switch (action) {
            case CREATE -> handleCreate(player, args);
            case DELETE -> handleDelete(player, args);
            case EDIT -> handleEdit(player, args);
            case TELEPORT -> handleTeleport(player, args);
            case LOAD -> handleLoad(player, args);
            default -> help(player);
        }
    }

    /**
     * Handles the create subcommand.
     * Usage: /mv create <name> [environment] [type]
     */
    private void handleCreate(@NotNull Player player, @NotNull String[] args) {
        if (hasNoPermission(player, EMultiversePermission.CREATE)) return;

        if (args.length < 2) {
            new I18n.Builder("multiverse.usage.create", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        var worldName = stringParameter(args, 1);
        var environment = enumParameterOrElse(args, 2, World.Environment.class, World.Environment.NORMAL);
        var worldType = enumParameterOrElse(args, 3, MVWorldType.class, MVWorldType.DEFAULT);

        // Check if world type is available in this edition
        if (!getMultiverseService().isWorldTypeAvailable(worldType)) {
            new I18n.Builder("multiverse.world_type_not_available", player)
                    .withPlaceholder("type", worldType.name())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        // Check world limit
        getMultiverseService().isAtWorldLimit().thenAccept(atLimit -> {
            if (atLimit) {
                Bukkit.getScheduler().runTask(getPlugin(), () ->
                        new I18n.Builder("multiverse.world_limit_reached", player)
                                .withPlaceholder("limit", String.valueOf(getMultiverseService().getMaxWorlds()))
                                .includePrefix()
                                .build()
                                .sendMessage()
                );
                return;
            }

            // Check if world already exists
            getMultiverseService().worldExists(worldName).thenAccept(exists -> {
                if (exists) {
                    Bukkit.getScheduler().runTask(getPlugin(), () ->
                            new I18n.Builder("multiverse.world_already_exists", player)
                                    .withPlaceholder("world_name", worldName)
                                    .includePrefix()
                                    .build()
                                    .sendMessage()
                    );
                    return;
                }

                // Send preparing message
                Bukkit.getScheduler().runTask(getPlugin(), () ->
                        new I18n.Builder("multiverse.preparing_world", player)
                                .withPlaceholder("world_name", worldName)
                                .includePrefix()
                                .build()
                                .sendMessage()
                );

                // Create the world
                getMultiverseService().createWorld(worldName, environment, worldType, player)
                        .thenAccept(mvWorld -> Bukkit.getScheduler().runTask(getPlugin(), () ->
                                new I18n.Builder("multiverse.world_created", player)
                                        .withPlaceholder("world_name", worldName)
                                        .withPlaceholder("type", worldType.name())
                                        .withPlaceholder("environment", environment.name())
                                        .includePrefix()
                                        .build()
                                        .sendMessage()
                        ))
                        .exceptionally(throwable -> {
                            LOGGER.log(Level.SEVERE, "Failed to create world: " + worldName, throwable);
                            Bukkit.getScheduler().runTask(getPlugin(), () ->
                                    new I18n.Builder("multiverse.world_creation_failed", player)
                                            .withPlaceholder("world_name", worldName)
                                            .withPlaceholder("error", throwable.getMessage())
                                            .includePrefix()
                                            .build()
                                            .sendMessage()
                            );
                            return null;
                        });
            });
        });
    }

    /**
     * Handles the delete subcommand.
     * Usage: /mv delete <name>
     */
    private void handleDelete(@NotNull Player player, @NotNull String[] args) {
        if (hasNoPermission(player, EMultiversePermission.DELETE)) return;

        if (args.length < 2) {
            new I18n.Builder("multiverse.usage.delete", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        var worldName = stringParameter(args, 1);

        // Check if world exists
        getMultiverseService().getWorld(worldName).thenAccept(worldOpt -> {
            if (worldOpt.isEmpty()) {
                Bukkit.getScheduler().runTask(getPlugin(), () ->
                        new I18n.Builder("multiverse.world_does_not_exist", player)
                                .withPlaceholder("world_name", worldName)
                                .includePrefix()
                                .build()
                                .sendMessage()
                );
                return;
            }

            // Check if world has players
            World bukkitWorld = Bukkit.getWorld(worldName);
            if (bukkitWorld != null && !bukkitWorld.getPlayers().isEmpty()) {
                Bukkit.getScheduler().runTask(getPlugin(), () ->
                        new I18n.Builder("multiverse.world_contains_players", player)
                                .withPlaceholder("world_name", worldName)
                                .includePrefix()
                                .build()
                                .sendMessage()
                );
                return;
            }

            // Delete the world
            getMultiverseService().deleteWorld(worldName, player)
                    .thenAccept(deleted -> Bukkit.getScheduler().runTask(getPlugin(), () -> {
                        if (deleted) {
                            new I18n.Builder("multiverse.world_deleted", player)
                                    .withPlaceholder("world_name", worldName)
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                        } else {
                            new I18n.Builder("multiverse.world_deletion_failed", player)
                                    .withPlaceholder("world_name", worldName)
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                        }
                    }))
                    .exceptionally(throwable -> {
                        LOGGER.log(Level.SEVERE, "Failed to delete world: " + worldName, throwable);
                        Bukkit.getScheduler().runTask(getPlugin(), () ->
                                new I18n.Builder("multiverse.world_deletion_failed", player)
                                        .withPlaceholder("world_name", worldName)
                                        .includePrefix()
                                        .build()
                                        .sendMessage()
                        );
                        return null;
                    });
        });
    }

    /**
     * Handles the edit subcommand.
     * Usage: /mv edit <name>
     */
    private void handleEdit(@NotNull Player player, @NotNull String[] args) {
        if (hasNoPermission(player, EMultiversePermission.EDIT)) return;

        if (args.length < 2) {
            new I18n.Builder("multiverse.usage.edit", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        var worldName = stringParameter(args, 1);

        getMultiverseService().getWorld(worldName).thenAccept(worldOpt -> {
            if (worldOpt.isEmpty()) {
                Bukkit.getScheduler().runTask(getPlugin(), () ->
                        new I18n.Builder("multiverse.world_does_not_exist", player)
                                .withPlaceholder("world_name", worldName)
                                .includePrefix()
                                .build()
                                .sendMessage()
                );
                return;
            }

            // Open the editor view on the main thread
            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                getViewFrame().open(MultiverseEditorView.class, player, Map.of(
                        "plugin", getPlugin(),
                        "repository", getWorldFactory().getWorldRepository(),
                        "executor", getPlugin().getServer().getScheduler().getMainThreadExecutor(getPlugin()),
                        "world", worldOpt.get()
                ));
            });
        });
    }

    /**
     * Handles the teleport subcommand.
     * Usage: /mv teleport <name>
     */
    private void handleTeleport(@NotNull Player player, @NotNull String[] args) {
        if (hasNoPermission(player, EMultiversePermission.TELEPORT)) return;

        if (args.length < 2) {
            new I18n.Builder("multiverse.usage.teleport", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        var worldName = stringParameter(args, 1);

        multiverse.getMultiverseService().getWorld(worldName).thenAccept(worldOpt -> {
            if (worldOpt.isEmpty()) {
                Bukkit.getScheduler().runTask(multiverse.getPlugin(), () ->
                        new I18n.Builder("multiverse.world_does_not_exist", player)
                                .withPlaceholder("world_name", worldName)
                                .includePrefix()
                                .build()
                                .sendMessage()
                );
                return;
            }

            MVWorld mvWorld = worldOpt.get();
            World bukkitWorld = Bukkit.getWorld(worldName);

            if (bukkitWorld == null) {
                Bukkit.getScheduler().runTask(multiverse.getPlugin(), () ->
                        new I18n.Builder("multiverse.world_not_loaded", player)
                                .withPlaceholder("world_name", worldName)
                                .includePrefix()
                                .build()
                                .sendMessage()
                );
                return;
            }

            // Teleport on main thread
            Bukkit.getScheduler().runTask(multiverse.getPlugin(), () -> {
                var spawnLocation = mvWorld.getSpawnLocation();
                if (spawnLocation.getWorld() == null) {
                    spawnLocation.setWorld(bukkitWorld);
                }

                // Ensure safe landing - place glass if air below
                Block blockBelow = spawnLocation.clone().subtract(0, 1, 0).getBlock();
                if (blockBelow.getType().isAir()) {
                    blockBelow.setType(Material.GLASS);
                }

                player.teleport(spawnLocation);
                new I18n.Builder("multiverse.teleported", player)
                        .withPlaceholder("world_name", worldName)
                        .includePrefix()
                        .build()
                        .sendMessage();
            });
        });
    }

    /**
     * Handles the load subcommand.
     * Usage: /mv load <name>
     */
    private void handleLoad(@NotNull Player player, @NotNull String[] args) {
        if (hasNoPermission(player, EMultiversePermission.LOAD)) return;

        if (args.length < 2) {
            new I18n.Builder("multiverse.usage.load", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        var worldName = stringParameter(args, 1);

        // Check if already loaded
        if (Bukkit.getWorld(worldName) != null) {
            new I18n.Builder("multiverse.world_already_loaded", player)
                    .withPlaceholder("world_name", worldName)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        multiverse.getMultiverseService().getWorld(worldName).thenAccept(worldOpt -> {
            if (worldOpt.isEmpty()) {
                Bukkit.getScheduler().runTask(multiverse.getPlugin(), () ->
                        new I18n.Builder("multiverse.world_does_not_exist", player)
                                .withPlaceholder("world_name", worldName)
                                .includePrefix()
                                .build()
                                .sendMessage()
                );
                return;
            }

            MVWorld mvWorld = worldOpt.get();

            // Send loading message
            Bukkit.getScheduler().runTask(multiverse.getPlugin(), () ->
                    new I18n.Builder("multiverse.loading_world", player)
                            .withPlaceholder("world_name", worldName)
                            .includePrefix()
                            .build()
                            .sendMessage()
            );

            // Load the world
            multiverse.getWorldFactory().loadWorld(mvWorld)
                    .thenRun(() -> Bukkit.getScheduler().runTask(multiverse.getPlugin(), () ->
                            new I18n.Builder("multiverse.world_loaded", player)
                                    .withPlaceholder("world_name", worldName)
                                    .includePrefix()
                                    .build()
                                    .sendMessage()
                    ))
                    .exceptionally(throwable -> {
                        LOGGER.log(Level.SEVERE, "Failed to load world: " + worldName, throwable);
                        Bukkit.getScheduler().runTask(multiverse.getPlugin(), () ->
                                new I18n.Builder("multiverse.world_load_failed", player)
                                        .withPlaceholder("world_name", worldName)
                                        .includePrefix()
                                        .build()
                                        .sendMessage()
                        );
                        return null;
                    });
        });
    }

    /**
     * Shows the help message.
     */
    private void help(@NotNull Player player) {
        new I18n.Builder("multiverse.help", player)
                .includePrefix()
                .build()
                .sendMessage();
    }

    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, EMultiversePermission.COMMAND)) return new ArrayList<>();

        if (args.length == 1) {
            // Tab complete actions
            List<String> actions = Arrays.stream(EMultiverseAction.values())
                    .map(a -> a.name().toLowerCase())
                    .toList();
            return StringUtil.copyPartialMatches(args[0].toLowerCase(), actions, new ArrayList<>());
        }

        if (args.length == 2) {
            var action = enumParameterOrElse(args, 0, EMultiverseAction.class, null);
            if (action == null) return new ArrayList<>();

            return switch (action) {
                case CREATE -> new ArrayList<>(List.of("<world_name>"));
                case DELETE, EDIT, TELEPORT, LOAD -> {
                    // Tab complete world names
                    List<String> worldNames = multiverse.getWorldFactory().getAllCachedWorlds().stream()
                            .map(MVWorld::getIdentifier)
                            .toList();
                    yield StringUtil.copyPartialMatches(args[1].toLowerCase(), worldNames, new ArrayList<>());
                }
                default -> new ArrayList<>();
            };
        }

        if (args.length == 3) {
            var action = enumParameterOrElse(args, 0, EMultiverseAction.class, null);
            if (action == EMultiverseAction.CREATE) {
                // Tab complete environments
                List<String> environments = Arrays.stream(World.Environment.values())
                        .map(e -> e.name().toLowerCase())
                        .toList();
                return StringUtil.copyPartialMatches(args[2].toLowerCase(), environments, new ArrayList<>());
            }
        }

        if (args.length == 4) {
            var action = enumParameterOrElse(args, 0, EMultiverseAction.class, null);
            if (action == EMultiverseAction.CREATE) {
                // Tab complete world types (only available ones)
                List<String> types = multiverse.getMultiverseService().getAvailableWorldTypes().stream()
                        .map(t -> t.name().toLowerCase())
                        .toList();
                return StringUtil.copyPartialMatches(args[3].toLowerCase(), types, new ArrayList<>());
            }
        }

        return new ArrayList<>();
    }
}
