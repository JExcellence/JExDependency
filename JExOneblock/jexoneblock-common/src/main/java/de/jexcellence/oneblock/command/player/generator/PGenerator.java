package de.jexcellence.oneblock.command.player.generator;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.PlayerGeneratorStructure;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import de.jexcellence.oneblock.database.repository.OneblockPlayerRepository;
import de.jexcellence.oneblock.service.GeneratorDesignService;
import de.jexcellence.oneblock.service.GeneratorRequirementService;
import de.jexcellence.oneblock.service.GeneratorStructureManager;
import de.jexcellence.oneblock.service.GeneratorStructureDetectionService;
import de.jexcellence.oneblock.view.generator.GeneratorBrowserView;
import de.jexcellence.oneblock.view.generator.GeneratorDesignDetailView;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main command for generator structure management.
 * Handles structure viewing, detection, and activation.
 * Updated to support the new generator design system.
 */
@Command
@SuppressWarnings("unused")
public class PGenerator extends PlayerCommand {

    private final JExOneblock plugin;
    private final OneblockPlayerRepository playerRepository;
    private final OneblockIslandRepository islandRepository;
    private final GeneratorStructureDetectionService detectionService;
    private final GeneratorStructureManager structureManager;

    public PGenerator(
            @NotNull PGeneratorSection commandSection,
            @NotNull JExOneblock plugin
    ) {
        super(commandSection);
        this.plugin = plugin;
        this.playerRepository = this.plugin.getOneblockPlayerRepository();
        this.islandRepository = this.plugin.getOneblockIslandRepository();
        this.detectionService = this.plugin.getGeneratorStructureManager() != null ? 
            this.plugin.getGeneratorStructureManager().getDetectionService() : null;
        this.structureManager = this.plugin.getGeneratorStructureManager();
    }

    @Override
    protected void onPlayerInvocation(
        @NotNull Player player,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (args.length == 0) {
            // Default: open the generator browser GUI
            handleBrowse(player);
            return;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help" -> sendHelp(player);
            case "list" -> handleList(player);
            case "browse", "gui", "menu" -> handleBrowse(player);
            case "view" -> handleView(player, args);
            case "animate" -> handleAnimate(player, args);
            case "build" -> handleBuild(player, args);
            case "scan" -> handleScan(player);
            case "activate" -> handleActivate(player, args);
            case "validate" -> handleValidate(player);
            case "info" -> handleInfo(player, args);
            case "cancel" -> handleCancel(player);
            case "status" -> handleStatus(player);
            default -> sendUsage(player);
        }
    }

    /**
     * Sends command usage information.
     */
    private void sendUsage(@NotNull Player player) {
        new I18n.Builder("generator.commands.title", player).includePrefix().build().sendMessage();
        new I18n.Builder("generator.commands.help", player).build().sendMessage();
        new I18n.Builder("generator.commands.browse", player).build().sendMessage();
        new I18n.Builder("generator.commands.list", player).build().sendMessage();
        new I18n.Builder("generator.commands.view", player).build().sendMessage();
        new I18n.Builder("generator.commands.animate", player).build().sendMessage();
        new I18n.Builder("generator.commands.build", player).build().sendMessage();
        new I18n.Builder("generator.commands.scan", player).build().sendMessage();
        new I18n.Builder("generator.commands.activate", player).build().sendMessage();
        new I18n.Builder("generator.commands.validate", player).build().sendMessage();
        new I18n.Builder("generator.commands.info", player).build().sendMessage();
        new I18n.Builder("generator.commands.cancel", player).build().sendMessage();
        new I18n.Builder("generator.commands.status", player).build().sendMessage();
    }

    /**
     * Sends detailed help information.
     */
    private void sendHelp(@NotNull Player player) {
        new I18n.Builder("generator.help.title", player).includePrefix().build().sendMessage();
        
        player.sendMessage(""); // Empty line
        
        new I18n.Builder("generator.help.how_to_build", player).build().sendMessage();
        new I18n.Builder("generator.help.step1", player).build().sendMessage();
        new I18n.Builder("generator.help.step2", player).build().sendMessage();
        new I18n.Builder("generator.help.step3", player).build().sendMessage();
        new I18n.Builder("generator.help.step4", player).build().sendMessage();
        
        player.sendMessage(""); // Empty line
        
        new I18n.Builder("generator.help.available_types", player).build().sendMessage();
        if (structureManager != null && structureManager.isInitialized()) {
            List<GeneratorDesign> designs = structureManager.getDesignService().getAllDesigns();
            for (GeneratorDesign design : designs) {
                new I18n.Builder("generator.help.type_entry", player)
                    .withPlaceholder("type", design.getDesignKey())
                    .withPlaceholder("description", design.getDescriptionKey())
                    .build().sendMessage();
            }
        }
        
        player.sendMessage(""); // Empty line
        
        new I18n.Builder("generator.help.tip", player).build().sendMessage();
    }

    /**
     * Handles the list subcommand.
     */
    private void handleList(@NotNull Player player) {
        new I18n.Builder("generator.list.title", player).includePrefix().build().sendMessage();
        
        player.sendMessage("");
        
        // List new generator designs
        if (structureManager != null && structureManager.isInitialized()) {
            List<GeneratorDesign> designs = structureManager.getDesignService().getAllDesigns();
            
            for (GeneratorDesign design : designs) {
                boolean canUnlock = structureManager.canUnlock(player, design);
                String status = canUnlock ? "<green>✓</green>" : "<red>✗</red>";
                
                new I18n.Builder("generator.list.design_entry", player)
                    .withPlaceholder("status", status)
                    .withPlaceholder("name", design.getNameKey())
                    .withPlaceholder("tier", String.valueOf(design.getTier()))
                    .withPlaceholder("type", design.getDesignType().name())
                    .build().sendMessage();
            }
        } else {
            new I18n.Builder("generator.list.no_designs", player).build().sendMessage();
        }
        
        player.sendMessage("");
        
        new I18n.Builder("generator.list.footer", player).build().sendMessage();
    }

    /**
     * Handles the browse subcommand - opens the generator browser GUI.
     */
    private void handleBrowse(@NotNull Player player) {
        if (structureManager == null || !structureManager.isInitialized()) {
            new I18n.Builder("generator.error.not_initialized", player).includePrefix().build().sendMessage();
            return;
        }
        
        plugin.getViewFrame().open(GeneratorBrowserView.class, player, Map.of(
            "plugin", plugin,
            "structureManager", structureManager,
            "designService", structureManager.getDesignService(),
            "requirementService", structureManager.getRequirementService()
        ));
        
        new I18n.Builder("generator.browse.opening", player).includePrefix().build().sendMessage();
    }

    /**
     * Handles the status subcommand - shows player's generator status.
     */
    private void handleStatus(@NotNull Player player) {
        if (structureManager == null || !structureManager.isInitialized()) {
            new I18n.Builder("generator.error.not_initialized", player).includePrefix().build().sendMessage();
            return;
        }
        
        playerRepository.findByUuidAsync(player.getUniqueId())
            .thenCompose(playerOpt -> {
                if (playerOpt.isEmpty()) {
                    new I18n.Builder("generator.status.player_not_found", player).includePrefix().build().sendMessage();
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }
                
                OneblockPlayer oneblockPlayer = playerOpt.get();
                if (!oneblockPlayer.hasIsland()) {
                    new I18n.Builder("generator.status.no_island", player).includePrefix().build().sendMessage();
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }
                
                return islandRepository.findByOwnerAsync(oneblockPlayer.getUniqueId())
                    .thenCompose(islandOpt -> {
                        if (islandOpt.isEmpty()) {
                            new I18n.Builder("generator.status.island_not_found", player).includePrefix().build().sendMessage();
                            return java.util.concurrent.CompletableFuture.completedFuture(null);
                        }
                        
                        OneblockIsland island = islandOpt.get();
                        return structureManager.getStructures(island.getId())
                            .thenAccept(structures -> {
                                new I18n.Builder("generator.status.header", player)
                                    .withPlaceholder("count", String.valueOf(structures.size()))
                                    .includePrefix()
                                    .build().sendMessage();
                                
                                if (structures.isEmpty()) {
                                    new I18n.Builder("generator.status.none", player).build().sendMessage();
                                    new I18n.Builder("generator.status.help", player).build().sendMessage();
                                    return;
                                }
                                
                                for (PlayerGeneratorStructure structure : structures) {
                                    GeneratorDesign design = structure.getDesign();
                                    String activeStatus = Boolean.TRUE.equals(structure.getIsActive()) ? "<green>✓</green>" : "<red>✗</red>";
                                    
                                    new I18n.Builder("generator.status.entry", player)
                                        .withPlaceholder("active", activeStatus)
                                        .withPlaceholder("name", design.getNameKey())
                                        .withPlaceholder("tier", String.valueOf(design.getTier()))
                                        .withPlaceholder("bonuses", formatBonuses(design))
                                        .build().sendMessage();
                                }
                                
                                new I18n.Builder("generator.status.footer", player).build().sendMessage();
                            });
                    });
            })
            .exceptionally(throwable -> {
                new I18n.Builder("generator.status.error", player)
                    .withPlaceholder("error", throwable.getMessage())
                    .includePrefix()
                    .build().sendMessage();
                return null;
            });
    }

    /**
     * Formats bonuses for display.
     */
    @NotNull
    private String formatBonuses(@NotNull GeneratorDesign design) {
        StringBuilder bonuses = new StringBuilder();
        
        if (design.getSpeedMultiplier() > 1.0) {
            bonuses.append(String.format("Speed: %.1fx", design.getSpeedMultiplier()));
        }
        
        if (design.getXpMultiplier() > 1.0) {
            if (bonuses.length() > 0) bonuses.append(", ");
            bonuses.append(String.format("XP: %.1fx", design.getXpMultiplier()));
        }
        
        if (design.getFortuneBonus() > 1.0) {
            if (bonuses.length() > 0) bonuses.append(", ");
            bonuses.append(String.format("Fortune: %.1fx", design.getFortuneBonus()));
        }
        
        return bonuses.length() > 0 ? bonuses.toString() : "None";
    }
    /**
     * Handles the animate subcommand.
     */
    private void handleAnimate(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            new I18n.Builder("generator.animate.usage", player).includePrefix().build().sendMessage();
            new I18n.Builder("generator.animate.available_types", player)
                .withPlaceholder("types", String.join(", ", getAvailableDesignKeys()))
                .build().sendMessage();
            return;
        }

        String designKey = args[1].toLowerCase();
        
        if (structureManager == null || !structureManager.isInitialized()) {
            new I18n.Builder("generator.error.not_initialized", player).includePrefix().build().sendMessage();
            return;
        }
        
        GeneratorDesign design = structureManager.getDesignService().getDesign(designKey);
        if (design == null) {
            new I18n.Builder("generator.error.invalid_design", player)
                .withPlaceholder("design", args[1])
                .includePrefix()
                .build().sendMessage();
            new I18n.Builder("generator.animate.available_types", player)
                .withPlaceholder("types", String.join(", ", getAvailableDesignKeys()))
                .build().sendMessage();
            return;
        }

        // Animated view not yet implemented in new system
        new I18n.Builder("generator.animate.not_implemented", player)
            .withPlaceholder("name", design.getNameKey())
            .includePrefix()
            .build().sendMessage();
        player.sendMessage("Use /generator view " + designKey + " to see the design details.");
    }

    /**
     * Handles the build subcommand.
     */
    private void handleBuild(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            new I18n.Builder("generator.build.usage", player).includePrefix().build().sendMessage();
            new I18n.Builder("generator.build.description", player).build().sendMessage();
            return;
        }

        String designKey = args[1].toLowerCase();
        
        if (structureManager == null || !structureManager.isInitialized()) {
            new I18n.Builder("generator.error.not_initialized", player).includePrefix().build().sendMessage();
            return;
        }
        
        GeneratorDesign design = structureManager.getDesignService().getDesign(designKey);
        if (design == null) {
            new I18n.Builder("generator.error.invalid_design", player)
                .withPlaceholder("design", args[1])
                .includePrefix()
                .build().sendMessage();
            return;
        }

        // Build view not yet implemented in new system
        new I18n.Builder("generator.build.not_implemented", player)
            .withPlaceholder("name", design.getNameKey())
            .includePrefix()
            .build().sendMessage();
        player.sendMessage("Use /generator activate " + designKey + " to activate a built structure.");
    }

    /**
     * Handles the cancel subcommand.
     */
    private void handleCancel(@NotNull Player player) {
        new I18n.Builder("generator.cancel.not_implemented", player).includePrefix().build().sendMessage();
    }

    /**
     * Handles the view subcommand.
     */
    private void handleView(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            new I18n.Builder("generator.view.usage", player).includePrefix().build().sendMessage();
            new I18n.Builder("generator.view.available_types", player)
                .withPlaceholder("types", String.join(", ", getAvailableDesignKeys()))
                .build().sendMessage();
            return;
        }

        String designKey = args[1].toLowerCase();
        
        if (structureManager == null || !structureManager.isInitialized()) {
            new I18n.Builder("generator.error.not_initialized", player).includePrefix().build().sendMessage();
            return;
        }
        
        GeneratorDesign design = structureManager.getDesignService().getDesign(designKey);
        if (design == null) {
            new I18n.Builder("generator.error.invalid_design", player)
                .withPlaceholder("design", args[1])
                .includePrefix()
                .build().sendMessage();
            new I18n.Builder("generator.view.available_types", player)
                .withPlaceholder("types", String.join(", ", getAvailableDesignKeys()))
                .build().sendMessage();
            return;
        }

        plugin.getViewFrame().open(GeneratorDesignDetailView.class, player, Map.of(
            "plugin", plugin,
            "design", design,
            "structureManager", structureManager
        ));
        new I18n.Builder("generator.view.opening", player)
            .withPlaceholder("name", design.getNameKey())
            .includePrefix()
            .build().sendMessage();
    }

    /**
     * Handles the scan subcommand.
     */
    private void handleScan(@NotNull Player player) {
        new I18n.Builder("generator.scan.scanning", player).includePrefix().build().sendMessage();
        
        if (detectionService == null) {
            new I18n.Builder("generator.error.not_initialized", player).includePrefix().build().sendMessage();
            return;
        }
        
        detectionService.scanForStructures(player.getLocation(), 50)
            .thenAccept(results -> {
                if (results.isEmpty()) {
                    new I18n.Builder("generator.scan.no_structures", player).includePrefix().build().sendMessage();
                    new I18n.Builder("generator.scan.build_correctly", player).build().sendMessage();
                    return;
                }
                
                new I18n.Builder("generator.scan.found", player)
                    .withPlaceholder("count", String.valueOf(results.size()))
                    .includePrefix()
                    .build().sendMessage();
                for (var result : results) {
                    new I18n.Builder("generator.scan.result_entry", player)
                        .withPlaceholder("type", result.design().getDesignKey())
                        .withPlaceholder("location", formatLocation(result.structureLocation()))
                        .build().sendMessage();
                }
                
                new I18n.Builder("generator.scan.activate_hint", player).build().sendMessage();
            })
            .exceptionally(throwable -> {
                new I18n.Builder("generator.scan.error", player)
                    .withPlaceholder("error", throwable.getMessage())
                    .includePrefix()
                    .build().sendMessage();
                return null;
            });
    }

    /**
     * Handles the activate subcommand.
     */
    private void handleActivate(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            new I18n.Builder("generator.activate.usage", player).includePrefix().build().sendMessage();
            return;
        }

        String designKey = args[1].toLowerCase();
        
        if (structureManager == null || !structureManager.isInitialized()) {
            new I18n.Builder("generator.error.not_initialized", player).includePrefix().build().sendMessage();
            return;
        }
        
        GeneratorDesign design = structureManager.getDesignService().getDesign(designKey);
        if (design == null) {
            new I18n.Builder("generator.error.invalid_design", player)
                .withPlaceholder("design", args[1])
                .includePrefix()
                .build().sendMessage();
            return;
        }

        new I18n.Builder("generator.activate.attempting", player)
            .withPlaceholder("design", designKey)
            .includePrefix()
            .build().sendMessage();

        playerRepository.findByUuidAsync(player.getUniqueId())
            .thenCompose(playerOpt -> {
                if (playerOpt.isEmpty()) {
                    new I18n.Builder("generator.activate.player_not_found", player).includePrefix().build().sendMessage();
                    return null;
                }

                OneblockPlayer oneblockPlayer = playerOpt.get();
                if (!oneblockPlayer.hasIsland()) {
                    new I18n.Builder("generator.activate.no_island", player).includePrefix().build().sendMessage();
                    return null;
                }

                return islandRepository.findByOwnerAsync(oneblockPlayer.getUniqueId())
                    .thenCompose(islandOpt -> {
                        if (islandOpt.isEmpty()) {
                            new I18n.Builder("generator.activate.island_not_found", player).includePrefix().build().sendMessage();
                            return null;
                        }

                        OneblockIsland island = islandOpt.get();
                        
                        return detectionService.activateGenerator(player, island, design, player.getLocation())
                            .thenAccept(result -> {
                                if (result.success()) {
                                    new I18n.Builder("generator.activate.success", player)
                                        .withPlaceholder("message", result.message())
                                        .includePrefix()
                                        .build().sendMessage();
                                    new I18n.Builder("generator.activate.active", player)
                                        .withPlaceholder("design", designKey)
                                        .build().sendMessage();
                                } else {
                                    new I18n.Builder("generator.activate.failed", player)
                                        .withPlaceholder("message", result.message())
                                        .includePrefix()
                                        .build().sendMessage();
                                }
                            });
                    });
            })
            .exceptionally(throwable -> {
                new I18n.Builder("generator.activate.error", player)
                    .withPlaceholder("error", throwable.getMessage())
                    .includePrefix()
                    .build().sendMessage();
                return null;
            });
    }

    /**
     * Handles the validate subcommand.
     */
    private void handleValidate(@NotNull Player player) {
        new I18n.Builder("generator.validate.validating", player).includePrefix().build().sendMessage();

        if (structureManager == null || !structureManager.isInitialized()) {
            new I18n.Builder("generator.error.not_initialized", player).includePrefix().build().sendMessage();
            return;
        }

        playerRepository.findByUuidAsync(player.getUniqueId())
            .thenCompose(playerOpt -> {
                if (playerOpt.isEmpty()) {
                    new I18n.Builder("generator.validate.player_not_found", player).includePrefix().build().sendMessage();
                    return null;
                }

                OneblockPlayer oneblockPlayer = playerOpt.get();
                if (!oneblockPlayer.hasIsland()) {
                    new I18n.Builder("generator.validate.no_island", player).includePrefix().build().sendMessage();
                    return null;
                }

                return islandRepository.findByOwnerAsync(oneblockPlayer.getUniqueId())
                    .thenCompose(islandOpt -> {
                        if (islandOpt.isEmpty()) {
                            new I18n.Builder("generator.validate.island_not_found", player).includePrefix().build().sendMessage();
                            return null;
                        }

                        OneblockIsland island = islandOpt.get();
                        
                        return structureManager.getDetectionService().validateAllGenerators(island.getId())
                            .thenAccept(summary -> {
                                new I18n.Builder("generator.validate.results", player).includePrefix().build().sendMessage();
                                new I18n.Builder("generator.validate.valid_count", player)
                                    .withPlaceholder("count", String.valueOf(summary.validCount()))
                                    .build().sendMessage();
                                new I18n.Builder("generator.validate.invalid_count", player)
                                    .withPlaceholder("count", String.valueOf(summary.invalidCount()))
                                    .build().sendMessage();
                                new I18n.Builder("generator.validate.total_count", player)
                                    .withPlaceholder("count", String.valueOf(summary.getTotalCount()))
                                    .build().sendMessage();
                                
                                if (summary.hasInvalidGenerators()) {
                                    new I18n.Builder("generator.validate.invalid_warning", player).includePrefix().build().sendMessage();
                                    new I18n.Builder("generator.validate.check_structures", player).build().sendMessage();
                                }
                            });
                    });
            })
            .exceptionally(throwable -> {
                new I18n.Builder("generator.validate.error", player)
                    .withPlaceholder("error", throwable.getMessage())
                    .includePrefix()
                    .build().sendMessage();
                return null;
            });
    }

    /**
     * Handles the info subcommand.
     */
    private void handleInfo(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            new I18n.Builder("generator.info.usage", player).includePrefix().build().sendMessage();
            return;
        }

        String designKey = args[1].toLowerCase();
        
        if (structureManager == null || !structureManager.isInitialized()) {
            new I18n.Builder("generator.error.not_initialized", player).includePrefix().build().sendMessage();
            return;
        }
        
        GeneratorDesign design = structureManager.getDesignService().getDesign(designKey);
        if (design == null) {
            new I18n.Builder("generator.error.invalid_design", player)
                .withPlaceholder("design", args[1])
                .includePrefix()
                .build().sendMessage();
            return;
        }

        new I18n.Builder("generator.info.header", player)
            .withPlaceholder("name", design.getNameKey())
            .includePrefix()
            .build().sendMessage();
        new I18n.Builder("generator.info.description", player)
            .withPlaceholder("description", design.getDescriptionKey())
            .build().sendMessage();
        player.sendMessage("");
        new I18n.Builder("generator.info.details_header", player).build().sendMessage();
        new I18n.Builder("generator.info.type", player)
            .withPlaceholder("type", design.getDesignType().name())
            .build().sendMessage();
        new I18n.Builder("generator.info.tier", player)
            .withPlaceholder("tier", String.valueOf(design.getTier()))
            .build().sendMessage();
        new I18n.Builder("generator.info.bonuses", player)
            .withPlaceholder("bonuses", formatBonuses(design))
            .build().sendMessage();
        player.sendMessage("");
        
        new I18n.Builder("generator.info.requirements_header", player).build().sendMessage();
        var requirements = design.getRequirements();
        if (requirements.isEmpty()) {
            new I18n.Builder("generator.info.no_requirements", player).build().sendMessage();
        } else {
            for (var requirement : requirements) {
                new I18n.Builder("generator.info.requirement", player)
                    .withPlaceholder("requirement", requirement.getDescriptionKey())
                    .build().sendMessage();
            }
        }
        
        player.sendMessage("");
        new I18n.Builder("generator.info.view_hint", player)
            .withPlaceholder("design", designKey)
            .build().sendMessage();
    }

    /**
     * Gets available design keys for display.
     */
    @NotNull
    private String[] getAvailableDesignKeys() {
        if (structureManager == null || !structureManager.isInitialized()) {
            return new String[0];
        }
        
        List<GeneratorDesign> designs = structureManager.getDesignService().getAllDesigns();
        return designs.stream()
            .map(GeneratorDesign::getDesignKey)
            .toArray(String[]::new);
    }

    /**
     * Formats a location for display.
     */
    @NotNull
    private String formatLocation(@NotNull org.bukkit.Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    @Override
    protected java.util.List<String> onPlayerTabCompletion(
        @NotNull Player player,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (args.length == 1) {
            // Suggest subcommands
            return java.util.Arrays.asList("help", "browse", "list", "view", "animate", "build", "scan", "activate", "validate", "info", "cancel", "status");
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if ("view".equals(subCommand) || "animate".equals(subCommand) || "build".equals(subCommand) || "info".equals(subCommand) || "activate".equals(subCommand)) {
                // Suggest design keys
                java.util.List<String> suggestions = new java.util.ArrayList<>();
                
                // Add design keys
                if (structureManager != null && structureManager.isInitialized()) {
                    for (GeneratorDesign design : structureManager.getDesignService().getAllDesigns()) {
                        suggestions.add(design.getDesignKey());
                    }
                }
                
                return suggestions;
            }
        }
        return new java.util.ArrayList<>();
    }
}