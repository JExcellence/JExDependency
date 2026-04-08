package de.jexcellence.home.command.admin;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.home.JExHome;
import de.jexcellence.home.factory.HomeFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command handler for admin home management commands.
 * <p>
 * Allows administrators to create and delete homes for other players,
 * including offline players, and teleport to their homes.
 * </p>
 * <p>
 * Usage:
 * - /homeadmin sethome &lt;player&gt; &lt;name&gt; - Create a home for another player at admin's location
 * - /homeadmin delhome &lt;player&gt; &lt;name&gt; - Delete a home belonging to another player
 * - /homeadmin home &lt;player&gt; &lt;name&gt; - Teleport to a home belonging to another player
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Command
@SuppressWarnings("unused")
public class PAdminHome extends PlayerCommand {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("PAdminHome");
    private final JExHome jexHome;

    public PAdminHome(@NotNull PAdminHomeSection commandSection, @NotNull JExHome jexHome) {
        super(commandSection);
        this.jexHome = jexHome;
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sendUsage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "sethome" -> {
                if (hasNoPermission(player, EPAdminHomePermission.ADMIN_SETHOME)) return;
                if (args.length < 3) {
                    new I18n.Builder("admin.usage.sethome", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                    return;
                }
                handleSetHome(player, args[1], args[2]);
            }
            case "delhome" -> {
                if (hasNoPermission(player, EPAdminHomePermission.ADMIN_DELHOME)) return;
                if (args.length < 3) {
                    new I18n.Builder("admin.usage.delhome", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                    return;
                }
                handleDelHome(player, args[1], args[2]);
            }
            case "home" -> {
                if (hasNoPermission(player, EPAdminHomePermission.ADMIN_HOME)) return;
                if (args.length < 3) {
                    new I18n.Builder("admin.usage.home", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                    return;
                }
                handleHome(player, args[1], args[2]);
            }
            default -> sendUsage(player);
        }
    }

    private void sendUsage(Player player) {
        new I18n.Builder("admin.usage.general", player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private void handleSetHome(Player admin, String targetName, String homeName) {
        resolvePlayerUuid(targetName).thenAccept(optionalUuid -> {
            if (optionalUuid.isEmpty()) {
                jexHome.getPlatform().getScheduler().runSync(() -> {
                    new I18n.Builder("admin.player_not_found", admin)
                        .withPlaceholder("player_name", targetName)
                        .includePrefix()
                        .build()
                        .sendMessage();
                });
                return;
            }

            UUID targetUuid = optionalUuid.get();
            var location = admin.getLocation();

            try {
                var factory = HomeFactory.getInstance();
                factory.getHomeService().createHome(targetUuid, homeName, location)
                    .thenAccept(home -> {
                        factory.invalidateCache(targetUuid);
                        jexHome.getPlatform().getScheduler().runSync(() -> {
                            new I18n.Builder("admin.sethome.success", admin)
                                .withPlaceholder("home_name", homeName)
                                .withPlaceholder("player_name", targetName)
                                .includePrefix()
                                .build()
                                .sendMessage();
                        });
                    })
                    .exceptionally(throwable -> {
                        LOGGER.log(Level.SEVERE, "Failed to create home for " + targetName, throwable);
                        jexHome.getPlatform().getScheduler().runSync(() -> {
                            new I18n.Builder("home.error.internal", admin)
                                .includePrefix()
                                .build()
                                .sendMessage();
                        });
                        return null;
                    });
            } catch (IllegalStateException e) {
                new I18n.Builder("home.error.internal", admin).includePrefix().build().sendMessage();
                LOGGER.log(Level.SEVERE, "HomeFactory not initialized", e);
            }
        });
    }

    private void handleDelHome(Player admin, String targetName, String homeName) {
        resolvePlayerUuid(targetName).thenAccept(optionalUuid -> {
            if (optionalUuid.isEmpty()) {
                jexHome.getPlatform().getScheduler().runSync(() -> {
                    new I18n.Builder("admin.player_not_found", admin)
                        .withPlaceholder("player_name", targetName)
                        .includePrefix()
                        .build()
                        .sendMessage();
                });
                return;
            }

            UUID targetUuid = optionalUuid.get();

            try {
                var factory = HomeFactory.getInstance();
                factory.getHomeService().findHome(targetUuid, homeName)
                    .thenAccept(homeOpt -> {
                        if (homeOpt.isEmpty()) {
                            jexHome.getPlatform().getScheduler().runSync(() -> {
                                new I18n.Builder("admin.home_not_found", admin)
                                    .withPlaceholder("home_name", homeName)
                                    .withPlaceholder("player_name", targetName)
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            });
                            return;
                        }

                        factory.getHomeService().deleteHome(targetUuid, homeName)
                            .thenAccept(deleted -> {
                                factory.invalidateCache(targetUuid);
                                jexHome.getPlatform().getScheduler().runSync(() -> {
                                    new I18n.Builder("admin.delhome.success", admin)
                                        .withPlaceholder("home_name", homeName)
                                        .withPlaceholder("player_name", targetName)
                                        .includePrefix()
                                        .build()
                                        .sendMessage();
                                });
                            });
                    })
                    .exceptionally(throwable -> {
                        LOGGER.log(Level.SEVERE, "Failed to delete home for " + targetName, throwable);
                        jexHome.getPlatform().getScheduler().runSync(() -> {
                            new I18n.Builder("home.error.internal", admin)
                                .includePrefix()
                                .build()
                                .sendMessage();
                        });
                        return null;
                    });
            } catch (IllegalStateException e) {
                new I18n.Builder("home.error.internal", admin).includePrefix().build().sendMessage();
                LOGGER.log(Level.SEVERE, "HomeFactory not initialized", e);
            }
        });
    }

    private void handleHome(Player admin, String targetName, String homeName) {
        resolvePlayerUuid(targetName).thenAccept(optionalUuid -> {
            if (optionalUuid.isEmpty()) {
                jexHome.getPlatform().getScheduler().runSync(() -> {
                    new I18n.Builder("admin.player_not_found", admin)
                        .withPlaceholder("player_name", targetName)
                        .includePrefix()
                        .build()
                        .sendMessage();
                });
                return;
            }

            UUID targetUuid = optionalUuid.get();

            try {
                var factory = HomeFactory.getInstance();
                factory.getHomeService().findHome(targetUuid, homeName)
                    .thenAccept(homeOpt -> {
                        if (homeOpt.isEmpty()) {
                            jexHome.getPlatform().getScheduler().runSync(() -> {
                                new I18n.Builder("admin.home_not_found", admin)
                                    .withPlaceholder("home_name", homeName)
                                    .withPlaceholder("player_name", targetName)
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            });
                            return;
                        }

                        var home = homeOpt.get();
                        var location = home.toLocation();

                        if (location == null || location.getWorld() == null) {
                            jexHome.getPlatform().getScheduler().runSync(() -> {
                                new I18n.Builder("home.world_not_loaded", admin)
                                    .withPlaceholder("world", home.getWorldName())
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            });
                            return;
                        }

                        jexHome.getPlatform().getScheduler().runSync(() -> {
                            admin.teleport(location);
                            new I18n.Builder("admin.home.success", admin)
                                .withPlaceholder("home_name", homeName)
                                .withPlaceholder("player_name", targetName)
                                .includePrefix()
                                .build()
                                .sendMessage();
                        });
                    })
                    .exceptionally(throwable -> {
                        LOGGER.log(Level.SEVERE, "Failed to teleport to home for " + targetName, throwable);
                        jexHome.getPlatform().getScheduler().runSync(() -> {
                            new I18n.Builder("home.error.internal", admin)
                                .includePrefix()
                                .build()
                                .sendMessage();
                        });
                        return null;
                    });
            } catch (IllegalStateException e) {
                new I18n.Builder("home.error.internal", admin).includePrefix().build().sendMessage();
                LOGGER.log(Level.SEVERE, "HomeFactory not initialized", e);
            }
        });
    }

    /**
     * Resolves a player UUID from a player name, supporting both online and offline players.
     *
     * @param playerName the name of the player
     * @return a CompletableFuture containing an Optional with the UUID if found
     */
    private CompletableFuture<Optional<UUID>> resolvePlayerUuid(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            // Check online players first (case-insensitive)
            Player online = Bukkit.getPlayerExact(playerName);
            if (online != null) {
                return Optional.of(online.getUniqueId());
            }

            // Check offline player cache
            @SuppressWarnings("deprecation")
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
            if (offline.hasPlayedBefore() || offline.isOnline()) {
                return Optional.of(offline.getUniqueId());
            }

            return Optional.empty();
        });
    }

    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Subcommand completion
            List<String> subCommands = new ArrayList<>();
            if (!hasNoPermission(player, EPAdminHomePermission.ADMIN_SETHOME)) {
                subCommands.add("sethome");
            }
            if (!hasNoPermission(player, EPAdminHomePermission.ADMIN_DELHOME)) {
                subCommands.add("delhome");
            }
            if (!hasNoPermission(player, EPAdminHomePermission.ADMIN_HOME)) {
                subCommands.add("home");
            }
            return StringUtil.copyPartialMatches(args[0], subCommands, completions);
        }

        if (args.length == 2) {
            // Player name completion - suggest online players
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
            return StringUtil.copyPartialMatches(args[1], playerNames, completions);
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("delhome") || args[0].equalsIgnoreCase("home"))) {
            // Home name completion for delhome and home - suggest target player's homes
            String targetName = args[1];
            try {
                Optional<UUID> targetUuid = resolvePlayerUuid(targetName).join();
                if (targetUuid.isPresent()) {
                    var factory = HomeFactory.getInstance();
                    var homes = factory.getPlayerHomes(targetUuid.get()).join();
                    List<String> homeNames = homes.stream()
                        .map(h -> h.getHomeName())
                        .toList();
                    return StringUtil.copyPartialMatches(args[2], homeNames, completions);
                }
            } catch (Exception e) {
                // Ignore errors during tab completion
            }
        }

        return completions;
    }
}
