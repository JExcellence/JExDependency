package com.raindropcentral.rdt.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.view.main.MainOverviewView;
import com.raindropcentral.rdt.view.town.TownOverviewView;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.jextranslate.i18n.I18n;

/**
 * Primary player command entry point for Raindrop Towns (alias: {@code /prt} and {@code /rt}).
 *
 * <p>This command currently exposes player-facing help and town overview routes while preserving
 * the action parser for future command expansion.</p>
 *
 * @author RaindropCentral
 * @since 1.0.0
 * @version 1.0.3
 */
@Command
@SuppressWarnings("unused")
public class PRT extends PlayerCommand {

    private static final int TICKS_PER_SECOND = 20;
    private static final int MAX_SPAWN_SEARCH_RADIUS = 48;
    private static final Set<Material> UNSAFE_STAND_BLOCKS = Set.of(
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.CACTUS,
            Material.POWDER_SNOW
    );

    private final RDT plugin;

    /**
     * Creates a new command handler.
     *
     * @param commandSection mapped command section
     * @param plugin active runtime
     */
    public PRT(final ACommandSection commandSection, final RDT plugin){
        super(commandSection);
        this.plugin = plugin;
    }

    /**
     * Handles player invocation for the town command.
     *
     * @param player invoking player
     * @param alias alias used by player
     * @param args raw command arguments
     */
    @Override
    protected void onPlayerInvocation(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NonNull @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, EPRTPermission.COMMAND)) {
            return;
        }

        final EPRTAction action = this.resolveAction(args);
        switch (action) {
            case MAIN -> this.handleMainOverview(player);
            case TOWN, INFO -> this.handleTownOverview(player);
            case SPAWN -> this.handleTownSpawn(player);
            case HELP -> this.sendHelp(player, alias);
            default -> this.sendHelp(player, alias);
        }
    }

    /**
     * Provides first-argument tab completions for available actions.
     *
     * @param player invoking player
     * @param alias command alias
     * @param args current argument set
     * @return matching suggestions
     */
    @Override
    protected List<String> onPlayerTabCompletion(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NonNull @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, EPRTPermission.COMMAND)) {
            return List.of();
        }

        if (args.length == 1) {
            final List<String> suggestions = new ArrayList<>(
                    Arrays.asList(
                            EPRTAction.HELP.name().toLowerCase(Locale.ROOT),
                            EPRTAction.MAIN.name().toLowerCase(Locale.ROOT),
                            EPRTAction.TOWN.name().toLowerCase(Locale.ROOT),
                            EPRTAction.SPAWN.name().toLowerCase(Locale.ROOT)
                    )
            );
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());
        }

        return List.of();
    }

    private @NotNull EPRTAction resolveAction(final @NotNull String[] args) {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            return EPRTAction.HELP;
        }

        try {
            return EPRTAction.valueOf(args[0].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EPRTAction.HELP;
        }
    }

    private void handleMainOverview(final @NotNull Player player) {
        if (this.hasNoPermission(player, EPRTPermission.MAIN)) {
            return;
        }

        this.plugin.getViewFrame().open(
                MainOverviewView.class,
                player,
                Map.of("plugin", this.plugin)
        );
    }

    private void handleTownOverview(final @NotNull Player player) {
        if (this.hasNoPermission(player, EPRTPermission.TOWN)) {
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(player);
        if (townPlayer == null || townPlayer.getTownUUID() == null) {
            new I18n.Builder("command.prt.town.not_in_town", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final UUID playerTownUuid = townPlayer.getTownUUID();
        final RTown town = this.plugin.getTownRepository() == null
                ? null
                : this.plugin.getTownRepository().findByTownUUID(playerTownUuid);
        if (town == null) {
            new I18n.Builder("command.prt.town.not_in_town", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (!town.hasTownPermission(townPlayer, TownPermissions.VIEW_TOWN)) {
            new I18n.Builder("command.prt.town.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.VIEW_TOWN.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        this.plugin.getViewFrame().open(
                TownOverviewView.class,
                player,
                Map.of(
                        "plugin", this.plugin,
                        "town_uuid", playerTownUuid
                )
        );
    }

    private void handleTownSpawn(final @NotNull Player player) {
        if (this.hasNoPermission(player, EPRTPermission.SPAWN)) {
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(player);
        if (townPlayer == null || townPlayer.getTownUUID() == null) {
            new I18n.Builder("command.prt.town.not_in_town", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final UUID playerTownUuid = townPlayer.getTownUUID();
        final RTown town = this.resolveTown(playerTownUuid);
        if (town == null) {
            new I18n.Builder("command.prt.town.not_in_town", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        Location townSpawnLocation = town.getTownSpawnLocation();
        if (townSpawnLocation == null) {
            final Location nexusLocation = town.getNexusLocation();
            if (nexusLocation != null && nexusLocation.getWorld() != null) {
                townSpawnLocation = nexusLocation.toBlockLocation();
                town.setTownSpawnLocation(townSpawnLocation);
                if (this.plugin.getTownRepository() != null) {
                    this.plugin.getTownRepository().update(town);
                }
            }
        }

        if (townSpawnLocation == null) {
            new I18n.Builder("command.prt.spawn.spawn_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }
        if (townSpawnLocation.getWorld() == null) {
            new I18n.Builder("command.prt.spawn.world_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final int delaySeconds = this.plugin.getDefaultConfig().getTownSpawnTeleportDelaySeconds();
        new I18n.Builder("command.prt.spawn.starting", player)
                .includePrefix()
                .withPlaceholder("seconds", delaySeconds)
                .build()
                .sendMessage();

        final UUID playerUuid = player.getUniqueId();
        final UUID townUuid = town.getIdentifier();
        final Location fallbackTownSpawnLocation = townSpawnLocation.toBlockLocation();
        final Runnable delayedTeleportTask = () -> this.executeTownSpawnTeleport(
                playerUuid,
                townUuid,
                fallbackTownSpawnLocation
        );

        final long delayTicks = Math.max(0, delaySeconds) * TICKS_PER_SECOND;
        if (this.plugin.getScheduler() != null) {
            this.plugin.getScheduler().runDelayed(delayedTeleportTask, delayTicks);
            return;
        }

        this.plugin.getServer().getScheduler().runTaskLater(
                this.plugin.getPlugin(),
                delayedTeleportTask,
                delayTicks
        );
    }

    private void executeTownSpawnTeleport(
            final @NotNull UUID playerUuid,
            final @NotNull UUID expectedTownUuid,
            final @NotNull Location fallbackTownSpawnLocation
    ) {
        final Player player = this.plugin.getServer().getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(player);
        if (townPlayer == null || townPlayer.getTownUUID() == null || !expectedTownUuid.equals(townPlayer.getTownUUID())) {
            return;
        }

        Location townSpawnLocation = fallbackTownSpawnLocation;
        final RTown latestTownState = this.resolveTown(expectedTownUuid);
        if (latestTownState != null
                && latestTownState.getTownSpawnLocation() != null
                && latestTownState.getTownSpawnLocation().getWorld() != null) {
            townSpawnLocation = latestTownState.getTownSpawnLocation().toBlockLocation();
        }

        if (townSpawnLocation.getWorld() == null) {
            new I18n.Builder("command.prt.spawn.world_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final Location safeLocation = this.findSafeTownSpawnLocation(townSpawnLocation);
        if (safeLocation == null) {
            new I18n.Builder("command.prt.spawn.no_safe_location", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        player.teleport(safeLocation);
        new I18n.Builder("command.prt.spawn.success", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "x", safeLocation.getBlockX(),
                        "y", safeLocation.getBlockY(),
                        "z", safeLocation.getBlockZ()
                ))
                .build()
                .sendMessage();
    }

    private @Nullable Location findSafeTownSpawnLocation(final @NotNull Location spawnLocation) {
        final World world = spawnLocation.getWorld();
        if (world == null) {
            return null;
        }

        final int centerX = spawnLocation.getBlockX();
        final int centerZ = spawnLocation.getBlockZ();
        for (int radius = 0; radius <= MAX_SPAWN_SEARCH_RADIUS; radius++) {
            final @Nullable Location location = this.findSafeLocationInRadiusRing(world, centerX, centerZ, radius);
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    private @Nullable Location findSafeLocationInRadiusRing(
            final @NotNull World world,
            final int centerX,
            final int centerZ,
            final int radius
    ) {
        if (radius == 0) {
            return this.findSafeLocationInColumn(world, centerX, centerZ);
        }

        final int minX = centerX - radius;
        final int maxX = centerX + radius;
        final int minZ = centerZ - radius;
        final int maxZ = centerZ + radius;

        for (int x = minX; x <= maxX; x++) {
            final @Nullable Location location = this.findSafeLocationInColumn(world, x, minZ);
            if (location != null) {
                return location;
            }
        }
        for (int z = minZ + 1; z <= maxZ; z++) {
            final @Nullable Location location = this.findSafeLocationInColumn(world, maxX, z);
            if (location != null) {
                return location;
            }
        }
        for (int x = maxX - 1; x >= minX; x--) {
            final @Nullable Location location = this.findSafeLocationInColumn(world, x, maxZ);
            if (location != null) {
                return location;
            }
        }
        for (int z = maxZ - 1; z > minZ; z--) {
            final @Nullable Location location = this.findSafeLocationInColumn(world, minX, z);
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    private @Nullable Location findSafeLocationInColumn(
            final @NotNull World world,
            final int x,
            final int z
    ) {
        final int minY = world.getMinHeight();
        final int maxStandY = world.getMaxHeight() - 3;
        final int startY = Math.min(maxStandY, world.getHighestBlockYAt(x, z));

        for (int standY = startY; standY >= minY; standY--) {
            final Material standBlock = world.getBlockAt(x, standY, z).getType();
            final Material feetBlock = world.getBlockAt(x, standY + 1, z).getType();
            final Material headBlock = world.getBlockAt(x, standY + 2, z).getType();

            if (!this.isSafeStandBlock(standBlock)) {
                continue;
            }
            if (!feetBlock.isAir() || !headBlock.isAir()) {
                continue;
            }

            return new Location(
                    world,
                    x + 0.5D,
                    standY + 1.0D,
                    z + 0.5D
            );
        }

        return null;
    }

    private boolean isSafeStandBlock(final @NotNull Material material) {
        if (!material.isSolid()) {
            return false;
        }
        return !UNSAFE_STAND_BLOCKS.contains(material);
    }

    private @Nullable RDTPlayer resolveTownPlayer(final @NotNull Player player) {
        final RRDTPlayer playerRepository = this.plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }
        return playerRepository.findByPlayer(player.getUniqueId());
    }

    private @Nullable RTown resolveTown(final @NotNull UUID townUuid) {
        if (this.plugin.getTownRepository() == null) {
            return null;
        }
        return this.plugin.getTownRepository().findByTownUUID(townUuid);
    }

    private void sendHelp(final @NotNull Player player, final @NotNull String alias) {
        if (this.hasNoPermission(player, EPRTPermission.HELP)) {
            return;
        }

        new I18n.Builder("command.prt.help.header", player)
                .includePrefix()
                .withPlaceholder("alias", alias.toLowerCase(Locale.ROOT))
                .build()
                .sendMessage();
        new I18n.Builder("command.prt.help.line_town", player)
                .withPlaceholder("alias", alias.toLowerCase(Locale.ROOT))
                .build()
                .sendMessage();
        new I18n.Builder("command.prt.help.line_main", player)
                .withPlaceholder("alias", alias.toLowerCase(Locale.ROOT))
                .build()
                .sendMessage();
        new I18n.Builder("command.prt.help.line_spawn", player)
                .withPlaceholder("alias", alias.toLowerCase(Locale.ROOT))
                .build()
                .sendMessage();
        new I18n.Builder("command.prt.help.line_create_hint", player)
                .withPlaceholder("alias", alias.toLowerCase(Locale.ROOT))
                .build()
                .sendMessage();
    }
}
