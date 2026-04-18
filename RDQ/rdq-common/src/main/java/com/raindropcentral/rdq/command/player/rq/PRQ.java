/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.command.player.rq;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.command.player.rq.machine.EMachineAction;
import com.raindropcentral.rdq.command.player.rq.machine.EMachinePermission;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.machine.type.EMachineType;
import com.raindropcentral.rdq.view.admin.AdminOverviewView;
import com.raindropcentral.rdq.view.bounty.BountyMainView;
import com.raindropcentral.rdq.view.main.MainOverviewView;
import com.raindropcentral.rdq.view.ranks.RankMainView;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents the PRQ API type.
 */
@Command
@SuppressWarnings("unused")
public class PRQ extends PlayerCommand {

    private static final String PERKS_SCOREBOARD_TYPE = "perks";
    
    /**
     * The main plugin instance.
     */
    private final RDQ rdq;
    
    /**
     * Constructs a new {@code PAdmin} command handler.
     *
     * @param commandSection the command section configuration
     * @param rdq            the main plugin instance
     */
    public PRQ(
        final @NotNull PRQSection commandSection,
        final @NotNull RDQ rdq
    ) {
        
        super(commandSection);
        this.rdq = rdq;
    }
    
    /**
     * Handles the command execution when a player invokes it.
 *
 * <p>Checks for the required permission and opens the admin overview view for the player.
     *
     * @param player the player who executed the command
     * @param label  the command label used
     * @param args   the command arguments
     */
    @Override
    protected void onPlayerInvocation(
        final @NotNull Player player,
        final @NotNull String label,
        final @NotNull String[] args
    ) {

        EPRQAction action = enumParameterOrElse(
            args,
            0,
            EPRQAction.class,
            EPRQAction.HELP
        );
        
        switch (action) {
            case ADMIN -> {
                if (
                    this.hasNoPermission(
                        player,
                        EPRQPermission.ADMIN
                    )
                ) {
                    return;
                }
                
                if (
                    this.rdq.getLuckPermsService() == null
                ) {
                    new I18n.Builder("rq.no_luckperms_installed", player).includePrefix().build().sendMessage();
                    return;
                }
                
                this.rdq.getViewFrame().open(
                    AdminOverviewView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq,
                        "pluginName",
                        args.length >= 1 ?
                        stringParameter(
                            args,
                            0
                        ) :
                        ""
                    )
                );
            }
            case BOUNTY -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.BOUNTY
                )) {
                    return;
                }
                this.rdq.getViewFrame().open(
                    BountyMainView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );
            }
            case MACHINE -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.MACHINE
                )) {
                    return;
                }
                
                if (this.rdq.getMachineManager() == null) {
                    new I18n.Builder("error.machine_system_disabled", player).includePrefix().build().sendMessage();
                    return;
                }
                
                // Handle machine subcommands
                if (args.length >= 2) {
                    this.handleMachineSubcommand(player, args);
                } else {
                    // Send message to interact with a machine or show help
                    new I18n.Builder("machine.command.interact_prompt", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                }
            }
            case MAIN -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.MAIN
                )) {
                    return;
                }
                this.rdq.getViewFrame().open(
                    MainOverviewView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );
            }
            case QUESTS -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.QUESTS
                )) {
                    return;
                }
                this.rdq.getViewFrame().open(
                    com.raindropcentral.rdq.view.quest.QuestCategoryView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );
            }
            case RANKS -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.RANKS
                )) {
                    return;
                }
                this.rdq.getViewFrame().open(
                    RankMainView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );
            }
            case SCOREBOARD -> this.handleScoreboardCommand(player, args);
            case PERKS -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.PERKS
                )) {
                    return;
                }
                
                // Load player data synchronously
                final var rdqPlayerOpt = this.rdq.getPlayerRepository().findByAttributes(
                    Map.of("uniqueId", player.getUniqueId())
                );
                
                if (rdqPlayerOpt.isEmpty()) {
                    new I18n.Builder("error.player_not_found", player).includePrefix().build().sendMessage();
                    return;
                }
                
                this.rdq.getViewFrame().open(
                    com.raindropcentral.rdq.view.perks.PerkOverviewView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq,
                        "player",
                        rdqPlayerOpt.get()
                    )
                );
            }
            default -> {
                if (! this.canAccessAnyAction(player)) {
                    this.hasNoPermission(
                        player,
                        EPRQPermission.COMMAND
                    );
                    return;
                }
                new I18n.Builder("rq.help", player).includePrefix().build().sendMessage();
            }
        }
    }
    
    /**
     * Provides tab completion suggestions for the command.
 *
 * <p>Currently returns an empty list, as there are no suggestions for this command.
     *
     * @param player the player requesting tab completion
     * @param label  the command label used
     * @param args   the current command arguments
     *
     * @return a list of tab completion suggestions (currently empty)
     */
    @Override
    protected List<String> onPlayerTabCompletion(
        final @NotNull Player player,
        final @NotNull String label,
        final @NotNull String[] args
    ) {

        if (
            args.length == 1
        ) {
            List<String> suggestions = new ArrayList<>(
                Arrays.stream(EPRQAction.values())
                    .filter(action -> action != EPRQAction.HELP)
                    .filter(action -> this.canAccessAction(
                        player,
                        action
                    ))
                    .map(Enum::name)
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .toList()
            );
            return StringUtil.copyPartialMatches(
                args[0].toLowerCase(),
                suggestions,
                new ArrayList<>()
            );
        }
        if (
            args.length == 2
                && "scoreboard".equalsIgnoreCase(args[0])
                && this.hasPermission(player, EPRQPermission.SCOREBOARD)
        ) {
            return StringUtil.copyPartialMatches(
                args[1].toLowerCase(),
                List.of(PERKS_SCOREBOARD_TYPE),
                new ArrayList<>()
            );
        }
        if (
            args.length == 2
                && "machine".equalsIgnoreCase(args[0])
                && this.hasPermission(player, EPRQPermission.MACHINE)
        ) {
            List<String> machineActions = Arrays.stream(EMachineAction.values())
                .map(Enum::name)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
            return StringUtil.copyPartialMatches(
                args[1].toLowerCase(),
                machineActions,
                new ArrayList<>()
            );
        }
        if (
            args.length == 3
                && "machine".equalsIgnoreCase(args[0])
                && "give".equalsIgnoreCase(args[1])
                && this.hasPermission(player, EMachinePermission.GIVE)
        ) {
            return null; // Return null for player name completion
        }
        if (
            args.length == 4
                && "machine".equalsIgnoreCase(args[0])
                && "give".equalsIgnoreCase(args[1])
                && this.hasPermission(player, EMachinePermission.GIVE)
        ) {
            List<String> machineTypes = Arrays.stream(EMachineType.values())
                .map(Enum::name)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
            return StringUtil.copyPartialMatches(
                args[3].toLowerCase(),
                machineTypes,
                new ArrayList<>()
            );
        }
        return new ArrayList<>();
    }

    private boolean canAccessAnyAction(
        final @NotNull Player player
    ) {

        return Arrays.stream(EPRQAction.values())
            .filter(action -> action != EPRQAction.HELP)
            .anyMatch(action -> this.canAccessAction(
                player,
                action
            ));
    }

    private boolean canAccessAction(
        final @NotNull Player player,
        final @NotNull EPRQAction action
    ) {

        return switch (action) {
            case ADMIN -> this.hasPermission(
                player,
                EPRQPermission.ADMIN
            );
            case BOUNTY -> this.hasPermission(
                player,
                EPRQPermission.BOUNTY
            );
            case MACHINE -> this.hasPermission(
                player,
                EPRQPermission.MACHINE
            );
            case MAIN -> this.hasPermission(
                player,
                EPRQPermission.MAIN
            );
            case QUESTS -> this.hasPermission(
                player,
                EPRQPermission.QUESTS
            );
            case RANKS -> this.hasPermission(
                player,
                EPRQPermission.RANKS
            );
            case SCOREBOARD -> this.hasPermission(
                player,
                EPRQPermission.SCOREBOARD
            );
            case PERKS -> this.hasPermission(
                player,
                EPRQPermission.PERKS
            );
            case HELP -> this.canAccessAnyAction(player);
        };
    }

    private void handleScoreboardCommand(
        final @NotNull Player player,
        final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, EPRQPermission.SCOREBOARD)) {
            return;
        }

        if (args.length != 2 || !PERKS_SCOREBOARD_TYPE.equalsIgnoreCase(args[1])) {
            new I18n.Builder("rq.scoreboard.syntax", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (
            this.rdq.getPerkSidebarScoreboardService() == null
                || this.rdq.getPlayerRepository() == null
        ) {
            return;
        }

        final RDQPlayer playerData = this.getOrCreatePlayerData(player);
        final String messageKey;
        if (this.rdq.getPerkSidebarScoreboardService().isActive(player)) {
            this.rdq.getPerkSidebarScoreboardService().disable(player);
            playerData.setPerkSidebarScoreboardEnabled(false);
            messageKey = "rq.scoreboard.disabled";
        } else {
            this.rdq.getPerkSidebarScoreboardService().enable(player);
            playerData.setPerkSidebarScoreboardEnabled(true);
            messageKey = "rq.scoreboard.enabled";
        }

        this.rdq.getPlayerRepository().update(playerData);
        new I18n.Builder(messageKey, player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @NotNull RDQPlayer getOrCreatePlayerData(
        final @NotNull Player player
    ) {
        final var existingPlayer = this.rdq.getPlayerRepository().findByAttributes(
            Map.of("uniqueId", player.getUniqueId())
        );
        if (existingPlayer.isPresent()) {
            return existingPlayer.get();
        }

        final RDQPlayer newPlayer = new RDQPlayer(player);
        this.rdq.getPlayerRepository().create(newPlayer);
        return newPlayer;
    }

    /**
     * Handles machine subcommands.
     *
     * @param player the player executing the command
     * @param args   the command arguments (args[0] is "machine", args[1] is the subcommand)
     */
    private void handleMachineSubcommand(
        final @NotNull Player player,
        final @NotNull String[] args
    ) {
        final EMachineAction action = enumParameterOrElse(
            args,
            1,
            EMachineAction.class,
            EMachineAction.HELP
        );

        switch (action) {
            case GIVE -> {
                if (this.hasNoPermission(player, EMachinePermission.GIVE)) {
                    return;
                }

                if (args.length < 4) {
                    new I18n.Builder("machine.command.give.syntax", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                    return;
                }

                final String targetName = args[2];
                final Player target = Bukkit.getPlayer(targetName);

                if (target == null) {
                    new I18n.Builder("error.player_not_found", player)
                        .includePrefix()
                        .withPlaceholder("player", targetName)
                        .build()
                        .sendMessage();
                    return;
                }

                final EMachineType machineType;
                try {
                    machineType = EMachineType.valueOf(args[3].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    new I18n.Builder("machine.command.give.invalid_type", player)
                        .includePrefix()
                        .withPlaceholder("type", args[3])
                        .build()
                        .sendMessage();
                    return;
                }

                // Create machine item manually
                final ItemStack machineItem = createMachineItem(machineType, target);

                target.getInventory().addItem(machineItem);

                new I18n.Builder("machine.command.give.success", player)
                    .includePrefix()
                    .withPlaceholder("player", target.getName())
                    .withPlaceholder("machine", machineType.name().toLowerCase(Locale.ROOT))
                    .build()
                    .sendMessage();

                if (!target.equals(player)) {
                    new I18n.Builder("machine.command.give.received", target)
                        .includePrefix()
                        .withPlaceholder("machine", machineType.name().toLowerCase(Locale.ROOT))
                        .build()
                        .sendMessage();
                }
            }
            case LIST -> {
                if (this.hasNoPermission(player, EMachinePermission.LIST)) {
                    return;
                }

                // Get target player (self if not specified)
                final Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        new I18n.Builder("error.player_not_found", player)
                            .includePrefix()
                            .withPlaceholder("player", args[2])
                            .build()
                            .sendMessage();
                        return;
                    }
                } else {
                    target = player;
                }

                // Get machines owned by target
                final var machines = this.rdq.getMachineRepository().findByOwnerAsync(
                    target.getUniqueId()
                ).join();

                if (machines.isEmpty()) {
                    new I18n.Builder("machine.command.list.empty", player)
                        .includePrefix()
                        .withPlaceholder("player", target.getName())
                        .build()
                        .sendMessage();
                    return;
                }

                // Send header
                new I18n.Builder("machine.command.list.header", player)
                    .includePrefix()
                    .withPlaceholder("player", target.getName())
                    .withPlaceholder("count", machines.size())
                    .build()
                    .sendMessage();

                // List each machine
                for (final var machine : machines) {
                    new I18n.Builder("machine.command.list.entry", player)
                        .withPlaceholder("id", machine.getId())
                        .withPlaceholder("type", machine.getMachineType().name().toLowerCase(Locale.ROOT))
                        .withPlaceholder("location", String.format("%d, %d, %d",
                            machine.getX(), machine.getY(), machine.getZ()))
                        .withPlaceholder("state", machine.getState().name().toLowerCase(Locale.ROOT))
                        .build()
                        .sendMessage();
                }
            }
            case TELEPORT -> {
                if (this.hasNoPermission(player, EMachinePermission.TELEPORT)) {
                    return;
                }

                if (args.length < 3) {
                    new I18n.Builder("machine.command.teleport.syntax", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                    return;
                }

                final Long machineId;
                try {
                    machineId = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    new I18n.Builder("machine.command.teleport.invalid_id", player)
                        .includePrefix()
                        .withPlaceholder("id", args[2])
                        .build()
                        .sendMessage();
                    return;
                }

                // Find machine
                final var machineOpt = this.rdq.getMachineRepository().findByIdAsync(machineId).join();
                if (machineOpt.isEmpty()) {
                    new I18n.Builder("machine.command.teleport.not_found", player)
                        .includePrefix()
                        .withPlaceholder("id", machineId)
                        .build()
                        .sendMessage();
                    return;
                }

                final var machine = machineOpt.get();

                // Check if player can access this machine
                if (!this.rdq.getMachineService().canInteract(player, machine)) {
                    new I18n.Builder("machine.command.teleport.no_permission", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                    return;
                }

                // Teleport player
                final var world = Bukkit.getWorld(machine.getWorld());
                if (world == null) {
                    new I18n.Builder("machine.command.teleport.world_not_found", player)
                        .includePrefix()
                        .withPlaceholder("world", machine.getWorld())
                        .build()
                        .sendMessage();
                    return;
                }

                final var location = new org.bukkit.Location(
                    world,
                    machine.getX() + 0.5,
                    machine.getY() + 1,
                    machine.getZ() + 0.5
                );

                player.teleport(location);

                new I18n.Builder("machine.command.teleport.success", player)
                    .includePrefix()
                    .withPlaceholder("id", machineId)
                    .withPlaceholder("type", machine.getMachineType().name().toLowerCase(Locale.ROOT))
                    .build()
                    .sendMessage();
            }
            case RELOAD -> {
                if (this.hasNoPermission(player, EMachinePermission.RELOAD)) {
                    return;
                }

                // Reload machine configurations
                try {
                    this.rdq.getMachineManager().reloadConfigurations();
                    new I18n.Builder("machine.command.reload.success", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                } catch (Exception e) {
                    new I18n.Builder("machine.command.reload.error", player)
                        .includePrefix()
                        .withPlaceholder("error", e.getMessage())
                        .build()
                        .sendMessage();
                    this.rdq.getPlugin().getLogger().severe("Failed to reload machine configurations: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            case REMOVE, INFO -> {
                new I18n.Builder("error.not_implemented", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            }
            case HELP -> {
                new I18n.Builder("machine.command.help", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            }
        }
    }

    /**
     * Creates a machine item for the specified machine type.
     *
     * @param machineType the type of machine
     * @param player      the player (for locale)
     * @return the machine item
     */
    private @NotNull ItemStack createMachineItem(
        final @NotNull EMachineType machineType,
        final @NotNull Player player
    ) {
        final ItemStack item = new ItemStack(machineType.getCoreMaterial());
        final ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set display name
            final Component displayName = new I18n.Builder(
                "machine.item." + machineType.getIdentifier() + ".name",
                player
            ).build().component();
            meta.displayName(displayName);

            // Set lore
            final List<Component> lore = new I18n.Builder(
                "machine.item." + machineType.getIdentifier() + ".lore",
                player
            ).build().children();
            meta.lore(lore);

            // Store machine type in NBT
            final PersistentDataContainer container = meta.getPersistentDataContainer();
            final NamespacedKey machineTypeKey = new NamespacedKey(this.rdq.getPlugin(), "machine_type");
            final NamespacedKey machineItemKey = new NamespacedKey(this.rdq.getPlugin(), "machine_item");
            container.set(machineTypeKey, PersistentDataType.STRING, machineType.name());
            container.set(machineItemKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }

        return item;
    }
}
