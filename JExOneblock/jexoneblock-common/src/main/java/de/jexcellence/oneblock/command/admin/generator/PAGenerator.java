package de.jexcellence.oneblock.command.admin.generator;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.PlayerGeneratorStructure;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import de.jexcellence.oneblock.database.repository.OneblockPlayerRepository;
import de.jexcellence.oneblock.service.GeneratorStructureManager;
import de.jexcellence.oneblock.view.generator.GeneratorBrowserView;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Admin command for managing generator structures.
 * Provides commands for listing, building, removing, and managing generator designs.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Command
@SuppressWarnings("unused")
public class PAGenerator extends PlayerCommand {

    private final JExOneblock plugin;
    private final GeneratorStructureManager structureManager;
    private final OneblockPlayerRepository playerRepository;
    private final OneblockIslandRepository islandRepository;

    public PAGenerator(
            @NotNull PAGeneratorSection commandSection,
            @NotNull Object pluginObject
    ) {
        super(commandSection);
        if (!(pluginObject instanceof JExOneblock)) {
            throw new IllegalArgumentException("Plugin must be an instance of JExOneblock");
        }
        this.plugin = (JExOneblock) pluginObject;
        this.structureManager = plugin.getGeneratorStructureManager();
        this.playerRepository = plugin.getOneblockPlayerRepository();
        this.islandRepository = plugin.getOneblockIslandRepository();
    }

    @Override
    protected void onPlayerInvocation(
            @NotNull Player player,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (hasNoPermission(player, EAdminGeneratorPermission.COMMAND)) {
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        var action = enumParameterOrElse(args, 0, EAdminGeneratorAction.class, EAdminGeneratorAction.HELP);

        switch (action) {
            case HELP -> sendHelp(player);
            case LIST -> handleList(player);
            case INFO -> handleInfo(player, args);
            case BUILD -> handleBuild(player, args);
            case REMOVE -> handleRemove(player, args);
            case RELOAD -> handleReload(player);
            case STATUS -> handleStatus(player, args);
            case ENABLE -> handleEnable(player, args);
            case DISABLE -> handleDisable(player, args);
            case GIVE -> handleGive(player, args);
            case GUI -> handleGui(player, args);
        }
    }

    private void sendHelp(@NotNull Player player) {
        new I18n.Builder("admin.generator.help.header", player).includePrefix().build().sendMessage();
        new I18n.Builder("admin.generator.help.list", player).build().sendMessage();
        new I18n.Builder("admin.generator.help.info", player).build().sendMessage();
        new I18n.Builder("admin.generator.help.build", player).build().sendMessage();
        new I18n.Builder("admin.generator.help.remove", player).build().sendMessage();
        new I18n.Builder("admin.generator.help.reload", player).build().sendMessage();
        new I18n.Builder("admin.generator.help.status", player).build().sendMessage();
        new I18n.Builder("admin.generator.help.enable", player).build().sendMessage();
        new I18n.Builder("admin.generator.help.disable", player).build().sendMessage();
        new I18n.Builder("admin.generator.help.give", player).build().sendMessage();
        new I18n.Builder("admin.generator.help.gui", player).build().sendMessage();
    }

    private void handleList(@NotNull Player player) {
        if (!checkInitialized(player)) return;

        List<GeneratorDesign> designs = structureManager.getDesignService().getAllDesigns();

        new I18n.Builder("admin.generator.list.header", player)
                .withPlaceholder("count", String.valueOf(designs.size()))
                .includePrefix()
                .build().sendMessage();

        for (GeneratorDesign design : designs) {
            String enabled = Boolean.TRUE.equals(design.getEnabled()) ? "<green>✓</green>" : "<red>✗</red>";
            new I18n.Builder("admin.generator.list.entry", player)
                    .withPlaceholder("enabled", enabled)
                    .withPlaceholder("key", design.getDesignKey())
                    .withPlaceholder("name", design.getNameKey())
                    .withPlaceholder("tier", String.valueOf(design.getTier()))
                    .withPlaceholder("type", design.getDesignType().getNameKey())
                    .build().sendMessage();
        }
    }

    private void handleInfo(@NotNull Player player, @NotNull String[] args) {
        if (!checkInitialized(player)) return;

        if (args.length < 2) {
            new I18n.Builder("admin.generator.info.usage", player).includePrefix().build().sendMessage();
            return;
        }

        String designKey = args[1].toLowerCase();
        Optional<GeneratorDesign> designOpt = structureManager.getDesign(designKey);

        if (designOpt.isEmpty()) {
            new I18n.Builder("admin.generator.info.not_found", player)
                    .withPlaceholder("design", designKey)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        GeneratorDesign design = designOpt.get();

        new I18n.Builder("admin.generator.info.header", player)
                .withPlaceholder("name", design.getNameKey())
                .includePrefix()
                .build().sendMessage();

        new I18n.Builder("admin.generator.info.key", player)
                .withPlaceholder("key", design.getDesignKey())
                .build().sendMessage();
        new I18n.Builder("admin.generator.info.type", player)
                .withPlaceholder("type", design.getDesignType().getDescriptionKey())
                .build().sendMessage();
        new I18n.Builder("admin.generator.info.tier", player)
                .withPlaceholder("tier", String.valueOf(design.getTier()))
                .build().sendMessage();
        new I18n.Builder("admin.generator.info.enabled", player)
                .withPlaceholder("enabled", Boolean.TRUE.equals(design.getEnabled()) ? "Yes" : "No")
                .build().sendMessage();
        new I18n.Builder("admin.generator.info.speed", player)
                .withPlaceholder("speed", String.format("%.2fx", design.getSpeedMultiplier()))
                .build().sendMessage();
        new I18n.Builder("admin.generator.info.xp", player)
                .withPlaceholder("xp", String.format("%.2fx", design.getXpMultiplier()))
                .build().sendMessage();
        new I18n.Builder("admin.generator.info.fortune", player)
                .withPlaceholder("fortune", String.format("%.2fx", design.getFortuneBonus()))
                .build().sendMessage();
        new I18n.Builder("admin.generator.info.layers", player)
                .withPlaceholder("layers", String.valueOf(design.getLayers().size()))
                .build().sendMessage();
    }

    private void handleBuild(@NotNull Player player, @NotNull String[] args) {
        if (!checkInitialized(player)) return;

        if (args.length < 3) {
            new I18n.Builder("admin.generator.build.usage", player).includePrefix().build().sendMessage();
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            new I18n.Builder("admin.generator.build.player_not_found", player)
                    .withPlaceholder("player", args[1])
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        String designKey = args[2].toLowerCase();
        Optional<GeneratorDesign> designOpt = structureManager.getDesign(designKey);

        if (designOpt.isEmpty()) {
            new I18n.Builder("admin.generator.build.design_not_found", player)
                    .withPlaceholder("design", designKey)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        GeneratorDesign design = designOpt.get();
        Location location = parseLocationOrDefault(args, 3, target.getLocation());

        if (location == null) {
            new I18n.Builder("admin.generator.build.invalid_coordinates", player)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        structureManager.getBuildService().startAutoBuild(target, design, location)
                .thenAccept(result -> {
                    if (result.success()) {
                        new I18n.Builder("admin.generator.build.started", player)
                                .withPlaceholder("player", target.getName())
                                .withPlaceholder("design", design.getNameKey())
                                .withPlaceholder("location", formatLocation(location))
                                .includePrefix()
                                .build().sendMessage();

                        new I18n.Builder("admin.generator.build.started_player", target)
                                .withPlaceholder("design", design.getNameKey())
                                .includePrefix()
                                .build().sendMessage();
                    } else {
                        new I18n.Builder("admin.generator.build.failed", player)
                                .withPlaceholder("reason", result.message())
                                .includePrefix()
                                .build().sendMessage();
                    }
                })
                .exceptionally(ex -> {
                    new I18n.Builder("admin.generator.build.error", player)
                            .withPlaceholder("error", ex.getMessage())
                            .includePrefix()
                            .build().sendMessage();
                    return null;
                });
    }

    private void handleRemove(@NotNull Player player, @NotNull String[] args) {
        if (!checkInitialized(player)) return;

        if (args.length < 2) {
            new I18n.Builder("admin.generator.remove.usage", player).includePrefix().build().sendMessage();
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            new I18n.Builder("admin.generator.remove.player_not_found", player)
                    .withPlaceholder("player", args[1])
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        playerRepository.findByUuidAsync(target.getUniqueId())
                .thenCompose(playerOpt -> {
                    if (playerOpt.isEmpty() || !playerOpt.get().hasIsland()) {
                        new I18n.Builder("admin.generator.remove.no_island", player)
                                .withPlaceholder("player", target.getName())
                                .includePrefix()
                                .build().sendMessage();
                        return CompletableFuture.completedFuture(null);
                    }

                    return islandRepository.findByOwnerAsync(playerOpt.get().getUniqueId())
                            .thenCompose(islandOpt -> {
                                if (islandOpt.isEmpty()) {
                                    return CompletableFuture.completedFuture(null);
                                }

                                OneblockIsland island = islandOpt.get();

                                if (args.length >= 3) {
                                    return removeSpecificDesign(player, target, island, args[2]);
                                } else {
                                    return removeAllStructures(player, target, island);
                                }
                            });
                })
                .exceptionally(ex -> {
                    new I18n.Builder("admin.generator.remove.error", player)
                            .withPlaceholder("error", ex.getMessage())
                            .includePrefix()
                            .build().sendMessage();
                    return null;
                });
    }

    private CompletableFuture<Void> removeSpecificDesign(Player player, Player target, OneblockIsland island, String designKey) {
        return structureManager.getStructures(island.getId())
                .thenCompose(structures -> {
                    Optional<PlayerGeneratorStructure> structureOpt = structures.stream()
                            .filter(s -> s.getDesign().getDesignKey().equalsIgnoreCase(designKey))
                            .findFirst();

                    if (structureOpt.isEmpty()) {
                        new I18n.Builder("admin.generator.remove.not_found", player)
                                .withPlaceholder("player", target.getName())
                                .withPlaceholder("design", designKey)
                                .includePrefix()
                                .build().sendMessage();
                        return CompletableFuture.completedFuture(null);
                    }

                    return structureManager.destroyStructure(structureOpt.get())
                            .thenRun(() -> new I18n.Builder("admin.generator.remove.success", player)
                                    .withPlaceholder("player", target.getName())
                                    .withPlaceholder("design", designKey)
                                    .includePrefix()
                                    .build().sendMessage());
                });
    }

    private CompletableFuture<Void> removeAllStructures(Player player, Player target, OneblockIsland island) {
        return structureManager.getStructures(island.getId())
                .thenCompose(structures -> {
                    if (structures.isEmpty()) {
                        new I18n.Builder("admin.generator.remove.none_found", player)
                                .withPlaceholder("player", target.getName())
                                .includePrefix()
                                .build().sendMessage();
                        return CompletableFuture.completedFuture(null);
                    }

                    int count = structures.size();
                    CompletableFuture<?>[] futures = structures.stream()
                            .map(structureManager::destroyStructure)
                            .toArray(CompletableFuture[]::new);

                    return CompletableFuture.allOf(futures)
                            .thenRun(() -> new I18n.Builder("admin.generator.remove.all_success", player)
                                    .withPlaceholder("player", target.getName())
                                    .withPlaceholder("count", String.valueOf(count))
                                    .includePrefix()
                                    .build().sendMessage());
                });
    }

    private void handleReload(@NotNull Player player) {
        if (!checkInitialized(player)) return;

        new I18n.Builder("admin.generator.reload.reloading", player).includePrefix().build().sendMessage();

        structureManager.reload()
                .thenRun(() -> new I18n.Builder("admin.generator.reload.success", player)
                        .includePrefix()
                        .build().sendMessage())
                .exceptionally(ex -> {
                    new I18n.Builder("admin.generator.reload.failed", player)
                            .withPlaceholder("reason", ex.getMessage())
                            .includePrefix()
                            .build().sendMessage();
                    return null;
                });
    }

    private void handleStatus(@NotNull Player player, @NotNull String[] args) {
        if (!checkInitialized(player)) return;

        if (args.length < 2) {
            new I18n.Builder("admin.generator.status.usage", player).includePrefix().build().sendMessage();
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            new I18n.Builder("admin.generator.status.player_not_found", player)
                    .withPlaceholder("player", args[1])
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        playerRepository.findByUuidAsync(target.getUniqueId())
                .thenCompose(playerOpt -> {
                    if (playerOpt.isEmpty() || !playerOpt.get().hasIsland()) {
                        new I18n.Builder("admin.generator.status.no_island", player)
                                .withPlaceholder("player", target.getName())
                                .includePrefix()
                                .build().sendMessage();
                        return CompletableFuture.completedFuture(null);
                    }

                    return islandRepository.findByOwnerAsync(playerOpt.get().getUniqueId())
                            .thenCompose(islandOpt -> {
                                if (islandOpt.isEmpty()) {
                                    return CompletableFuture.completedFuture(null);
                                }

                                return structureManager.getStructures(islandOpt.get().getId())
                                        .thenAccept(structures -> displayStatus(player, target, structures));
                            });
                })
                .exceptionally(ex -> {
                    new I18n.Builder("admin.generator.status.error", player)
                            .withPlaceholder("error", ex.getMessage())
                            .includePrefix()
                            .build().sendMessage();
                    return null;
                });
    }

    private void displayStatus(Player player, Player target, List<PlayerGeneratorStructure> structures) {
        new I18n.Builder("admin.generator.status.header", player)
                .withPlaceholder("player", target.getName())
                .withPlaceholder("count", String.valueOf(structures.size()))
                .includePrefix()
                .build().sendMessage();

        if (structures.isEmpty()) {
            new I18n.Builder("admin.generator.status.none", player).build().sendMessage();
            return;
        }

        for (PlayerGeneratorStructure structure : structures) {
            GeneratorDesign design = structure.getDesign();
            String active = Boolean.TRUE.equals(structure.getIsActive()) ? "<green>✓</green>" : "<red>✗</red>";

            new I18n.Builder("admin.generator.status.entry", player)
                    .withPlaceholder("active", active)
                    .withPlaceholder("design", design.getNameKey())
                    .withPlaceholder("tier", String.valueOf(design.getTier()))
                    .withPlaceholder("location", structure.getCoreLocation().toString())
                    .build().sendMessage();
        }
    }

    private void handleEnable(@NotNull Player player, @NotNull String[] args) {
        if (!checkInitialized(player)) return;

        if (args.length < 2) {
            new I18n.Builder("admin.generator.enable.usage", player).includePrefix().build().sendMessage();
            return;
        }

        String designKey = args[1].toLowerCase();
        Optional<GeneratorDesign> designOpt = structureManager.getDesign(designKey);

        if (designOpt.isEmpty()) {
            new I18n.Builder("admin.generator.enable.not_found", player)
                    .withPlaceholder("design", designKey)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        GeneratorDesign design = designOpt.get();
        design.setEnabled(true);

        new I18n.Builder("admin.generator.enable.success", player)
                .withPlaceholder("design", design.getNameKey())
                .includePrefix()
                .build().sendMessage();
    }

    private void handleDisable(@NotNull Player player, @NotNull String[] args) {
        if (!checkInitialized(player)) return;

        if (args.length < 2) {
            new I18n.Builder("admin.generator.disable.usage", player).includePrefix().build().sendMessage();
            return;
        }

        String designKey = args[1].toLowerCase();
        Optional<GeneratorDesign> designOpt = structureManager.getDesign(designKey);

        if (designOpt.isEmpty()) {
            new I18n.Builder("admin.generator.disable.not_found", player)
                    .withPlaceholder("design", designKey)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        GeneratorDesign design = designOpt.get();
        design.setEnabled(false);

        new I18n.Builder("admin.generator.disable.success", player)
                .withPlaceholder("design", design.getNameKey())
                .includePrefix()
                .build().sendMessage();
    }

    private void handleGive(@NotNull Player player, @NotNull String[] args) {
        if (!checkInitialized(player)) return;

        if (args.length < 3) {
            new I18n.Builder("admin.generator.give.usage", player).includePrefix().build().sendMessage();
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            new I18n.Builder("admin.generator.give.player_not_found", player)
                    .withPlaceholder("player", args[1])
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        String designKey = args[2].toLowerCase();
        Optional<GeneratorDesign> designOpt = structureManager.getDesign(designKey);

        if (designOpt.isEmpty()) {
            new I18n.Builder("admin.generator.give.design_not_found", player)
                    .withPlaceholder("design", designKey)
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        GeneratorDesign design = designOpt.get();

        structureManager.getBuildService().startAutoBuild(target, design, target.getLocation())
                .thenAccept(result -> {
                    if (result.success()) {
                        new I18n.Builder("admin.generator.give.success", player)
                                .withPlaceholder("player", target.getName())
                                .withPlaceholder("design", design.getNameKey())
                                .includePrefix()
                                .build().sendMessage();

                        new I18n.Builder("admin.generator.give.received", target)
                                .withPlaceholder("design", design.getNameKey())
                                .includePrefix()
                                .build().sendMessage();
                    } else {
                        new I18n.Builder("admin.generator.give.failed", player)
                                .withPlaceholder("reason", result.message())
                                .includePrefix()
                                .build().sendMessage();
                    }
                })
                .exceptionally(ex -> {
                    new I18n.Builder("admin.generator.give.error", player)
                            .withPlaceholder("error", ex.getMessage())
                            .includePrefix()
                            .build().sendMessage();
                    return null;
                });
    }

    private void handleGui(@NotNull Player player, @NotNull String[] args) {
        if (!checkInitialized(player)) return;

        if (args.length < 2) {
            new I18n.Builder("admin.generator.gui.usage", player).includePrefix().build().sendMessage();
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            new I18n.Builder("admin.generator.gui.player_not_found", player)
                    .withPlaceholder("player", args[1])
                    .includePrefix()
                    .build().sendMessage();
            return;
        }

        plugin.getViewFrame().open(GeneratorBrowserView.class, target, Map.of(
                "plugin", plugin,
                "structureManager", structureManager,
                "designService", structureManager.getDesignService(),
                "requirementService", structureManager.getRequirementService()
        ));

        new I18n.Builder("admin.generator.gui.opened", player)
                .withPlaceholder("player", target.getName())
                .includePrefix()
                .build().sendMessage();
    }

    private boolean checkInitialized(@NotNull Player player) {
        if (structureManager == null || !structureManager.isInitialized()) {
            new I18n.Builder("admin.generator.not_initialized", player).includePrefix().build().sendMessage();
            return false;
        }
        return true;
    }

    private Location parseLocationOrDefault(String[] args, int startIndex, Location defaultLocation) {
        if (args.length >= startIndex + 3) {
            try {
                double x = Double.parseDouble(args[startIndex]);
                double y = Double.parseDouble(args[startIndex + 1]);
                double z = Double.parseDouble(args[startIndex + 2]);
                return new Location(defaultLocation.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return defaultLocation;
    }

    @NotNull
    private String formatLocation(@NotNull Location location) {
        return String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
    }

    @Override
    protected List<String> onPlayerTabCompletion(
            @NotNull Player player,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (hasNoPermission(player, EAdminGeneratorPermission.COMMAND)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            var suggestions = java.util.Arrays.stream(EAdminGeneratorAction.values())
                    .map(a -> a.name().toLowerCase())
                    .toList();
            return StringUtil.copyPartialMatches(args[0].toLowerCase(), suggestions, new ArrayList<>());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            return switch (subCommand) {
                case "info", "enable", "disable" -> getDesignKeySuggestions(args[1]);
                case "build", "remove", "status", "give", "gui" -> getPlayerSuggestions(args[1]);
                default -> new ArrayList<>();
            };
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if ("build".equals(subCommand) || "give".equals(subCommand) || "remove".equals(subCommand)) {
                return getDesignKeySuggestions(args[2]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> getDesignKeySuggestions(String partial) {
        if (structureManager == null || !structureManager.isInitialized()) {
            return new ArrayList<>();
        }
        var suggestions = structureManager.getDesignService().getAllDesigns().stream()
                .map(GeneratorDesign::getDesignKey)
                .toList();
        return StringUtil.copyPartialMatches(partial.toLowerCase(), suggestions, new ArrayList<>());
    }

    private List<String> getPlayerSuggestions(String partial) {
        var suggestions = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
        return StringUtil.copyPartialMatches(partial, suggestions, new ArrayList<>());
    }
}
