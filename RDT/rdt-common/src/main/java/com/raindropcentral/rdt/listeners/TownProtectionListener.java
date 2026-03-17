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

package com.raindropcentral.rdt.listeners;

import java.util.List;
import java.util.Map;

import de.jexcellence.jextranslate.i18n.I18n;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.utils.TownProtections;

/**
 * Enforces configured town and chunk protection rules in live world events.
 *
 * <p>This listener applies role-gated checks for block actions and switch interactions,
 * environmental flow/spread controls, and hostile/neutral mob cleanup in protected chunks.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.3
 */
@SuppressWarnings("unused")
public final class TownProtectionListener implements Listener {

    private final RDT plugin;

    /**
     * Creates the town-protection enforcement listener.
     *
     * @param plugin active RDT runtime
     */
    public TownProtectionListener(final @NotNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Cleans entities in newly loaded chunks based on town protection settings.
     *
     * @param event chunk load event
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(final @NotNull ChunkLoadEvent event) {
        this.cleanupEntitiesInChunk(event.getChunk());
    }

    /**
     * Cleans entities when players cross chunk boundaries so protected chunks stay sanitized.
     *
     * @param event player movement event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final @NotNull PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        this.cleanupEntitiesInChunk(event.getTo().getChunk());
    }

    /**
     * Removes hostile and neutral entities when they move into protected chunks.
     *
     * @param event living-entity movement event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityMove(final @NotNull EntityMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        final Location from = event.getFrom();
        final Location to = event.getTo();
        if (from.getWorld() == null || to.getWorld() == null) {
            return;
        }
        if (from.getWorld().equals(to.getWorld())
                && from.getChunk().getX() == to.getChunk().getX()
                && from.getChunk().getZ() == to.getChunk().getZ()) {
            return;
        }

        final LivingEntity entity = event.getEntity();
        final TownChunkContext destinationContext = this.resolveTownChunkContext(
                to.getChunk().getX(),
                to.getChunk().getZ()
        );
        if (destinationContext == null) {
            return;
        }

        if (this.isHostileEntity(entity)
                && this.isProtectionActive(destinationContext, TownProtections.TOWN_HOSTILE_ENTITIES)) {
            event.setCancelled(true);
            entity.remove();
            return;
        }

        if (this.isNeutralEntity(entity)
                && this.isProtectionActive(destinationContext, TownProtections.TOWN_NEUTRAL_ENTITIES)) {
            event.setCancelled(true);
            entity.remove();
        }
    }

    /**
     * Blocks hostile and neutral creature spawns in protected chunks when configured.
     *
     * @param event creature spawn event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(final @NotNull CreatureSpawnEvent event) {
        final Entity entity = event.getEntity();
        final TownChunkContext context = this.resolveTownChunkContext(
                entity.getLocation().getChunk().getX(),
                entity.getLocation().getChunk().getZ()
        );
        if (context == null) {
            return;
        }

        if (this.isHostileEntity(entity)
                && this.isProtectionActive(context, TownProtections.TOWN_HOSTILE_ENTITIES)) {
            event.setCancelled(true);
            entity.remove();
            return;
        }

        if (this.isNeutralEntity(entity)
                && this.isProtectionActive(context, TownProtections.TOWN_NEUTRAL_ENTITIES)) {
            event.setCancelled(true);
            entity.remove();
        }
    }

    /**
     * Prevents fire spread in protected chunks when town fire protection is active.
     *
     * @param event block spread event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(final @NotNull BlockSpreadEvent event) {
        final Material sourceType = event.getSource().getType();
        if (sourceType != Material.FIRE && sourceType != Material.SOUL_FIRE) {
            return;
        }

        final TownChunkContext destinationContext = this.resolveTownChunkContext(
                event.getBlock().getChunk().getX(),
                event.getBlock().getChunk().getZ()
        );
        if (destinationContext == null) {
            return;
        }
        if (this.isProtectionActive(destinationContext, TownProtections.TOWN_FIRE)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents water and lava flow in or into protected chunks when configured.
     *
     * @param event fluid flow event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(final @NotNull BlockFromToEvent event) {
        final Material sourceType = event.getBlock().getType();
        final TownProtections protection;
        if (sourceType == Material.WATER) {
            protection = TownProtections.TOWN_WATER;
        } else if (sourceType == Material.LAVA) {
            protection = TownProtections.TOWN_LAVA;
        } else {
            return;
        }

        final TownChunkContext sourceContext = this.resolveTownChunkContext(
                event.getBlock().getChunk().getX(),
                event.getBlock().getChunk().getZ()
        );
        final TownChunkContext destinationContext = this.resolveTownChunkContext(
                event.getToBlock().getChunk().getX(),
                event.getToBlock().getChunk().getZ()
        );

        if (this.isProtectionActive(sourceContext, protection) || this.isProtectionActive(destinationContext, protection)) {
            event.setCancelled(true);
        }
    }

    /**
     * Enforces block-break role requirements in protected chunks.
     *
     * @param event block break event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        this.enforcePlayerActionProtection(
                event,
                event.getPlayer(),
                event.getBlock().getLocation(),
                TownProtections.BREAK_BLOCK
        );
    }

    /**
     * Enforces block-place role requirements in protected chunks.
     *
     * @param event block place event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        this.enforcePlayerActionProtection(
                event,
                event.getPlayer(),
                event.getBlockPlaced().getLocation(),
                TownProtections.PLACE_BLOCK
        );
    }

    /**
     * Enforces friendly/public PvP role requirements inside protected town chunks.
     *
     * @param event entity damage-by-entity event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(final @NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        final @Nullable Player attacker = this.resolveAttackingPlayer(event.getDamager());
        if (attacker == null) {
            return;
        }

        this.enforceTownPvpProtection(event, attacker, victim);
    }

    /**
     * Enforces role requirements for right-click block switches and interaction items.
     *
     * @param event player interact event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Action action = event.getAction();
        final boolean rightClickBlock = action == Action.RIGHT_CLICK_BLOCK;
        final boolean rightClickAction = rightClickBlock || action == Action.RIGHT_CLICK_AIR;

        if (rightClickBlock && event.getClickedBlock() != null) {
            final TownProtections blockProtection = this.resolveBlockInteractionProtection(
                    event.getClickedBlock().getType()
            );
            if (blockProtection != null) {
                if (this.enforcePlayerActionProtection(
                        event,
                        player,
                        event.getClickedBlock().getLocation(),
                        blockProtection
                )) {
                    return;
                }
            }
        }

        if (!rightClickAction) {
            return;
        }

        final ItemStack usedItem = event.getItem();
        if (usedItem == null) {
            return;
        }
        final TownProtections itemProtection = this.resolveItemInteractionProtection(usedItem.getType());
        if (itemProtection == null) {
            return;
        }
        this.enforcePlayerActionProtection(
                event,
                player,
                player.getLocation(),
                itemProtection
        );
    }

    /**
     * Enforces role requirements for right-click entity interactions such as boats and minecarts.
     *
     * @param event player interact entity event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(final @NotNull PlayerInteractEntityEvent event) {
        final Entity target = event.getRightClicked();
        final Player player = event.getPlayer();

        final TownProtections protection;
        if (target instanceof Minecart) {
            protection = TownProtections.MINECARTS;
        } else if (target instanceof Boat) {
            protection = TownProtections.BOATS;
        } else if (this.isHoldingLead(player) && target instanceof LivingEntity) {
            protection = TownProtections.LEAD;
        } else {
            return;
        }

        this.enforcePlayerActionProtection(
                event,
                player,
                target.getLocation(),
                protection
        );
    }

    private void cleanupEntitiesInChunk(final @NotNull Chunk chunk) {
        final TownChunkContext context = this.resolveTownChunkContext(chunk.getX(), chunk.getZ());
        if (context == null) {
            return;
        }

        final boolean removeHostiles = this.isProtectionActive(context, TownProtections.TOWN_HOSTILE_ENTITIES);
        final boolean removeNeutrals = this.isProtectionActive(context, TownProtections.TOWN_NEUTRAL_ENTITIES);
        if (!removeHostiles && !removeNeutrals) {
            return;
        }

        for (final Entity entity : chunk.getEntities()) {
            if (removeHostiles && this.isHostileEntity(entity)) {
                entity.remove();
                continue;
            }
            if (removeNeutrals && this.isNeutralEntity(entity)) {
                entity.remove();
            }
        }
    }

    private boolean enforcePlayerActionProtection(
            final @NotNull Cancellable event,
            final @NotNull Player player,
            final @NotNull Location actionLocation,
            final @NotNull TownProtections protection
    ) {
        final TownChunkContext context = this.resolveTownChunkContext(
                actionLocation.getChunk().getX(),
                actionLocation.getChunk().getZ()
        );
        if (context == null) {
            return false;
        }

        final String requiredRoleId = this.resolveRequiredRoleId(context, protection);
        final RDTPlayer playerRecord = this.resolveTownPlayer(player);
        if (this.hasRoleAccess(playerRecord, context.town(), requiredRoleId)) {
            return false;
        }

        event.setCancelled(true);
        this.sendDeniedMessage(player, protection, requiredRoleId);
        return true;
    }

    private void enforceTownPvpProtection(
            final @NotNull Cancellable event,
            final @NotNull Player attacker,
            final @NotNull Player victim
    ) {
        final TownChunkContext context = this.resolveTownChunkContext(
                victim.getLocation().getChunk().getX(),
                victim.getLocation().getChunk().getZ()
        );
        if (context == null) {
            return;
        }

        final RDTPlayer attackerRecord = this.resolveTownPlayer(attacker);
        final RDTPlayer victimRecord = this.resolveTownPlayer(victim);
        final boolean attackerInTown = this.isPlayerInTown(attackerRecord, context.town());
        final boolean victimInTown = this.isPlayerInTown(victimRecord, context.town());

        final TownProtections protection = attackerInTown && victimInTown
                ? TownProtections.TOWN_FRIENDLY_PVP
                : TownProtections.TOWN_PUBLIC_PVP;
        final String requiredRoleId = this.resolveRequiredRoleId(context, protection);
        if (this.hasRoleAccess(attackerRecord, context.town(), requiredRoleId)) {
            return;
        }

        event.setCancelled(true);
        this.sendDeniedMessage(attacker, protection, requiredRoleId);
    }

    private boolean hasRoleAccess(
            final @Nullable RDTPlayer playerRecord,
            final @NotNull RTown town,
            final @NotNull String requiredRoleId
    ) {
        final String normalizedRequiredRoleId = RTown.normalizeRoleId(requiredRoleId);
        if (RTown.RESTRICTED_ROLE_ID.equals(normalizedRequiredRoleId)) {
            return false;
        }
        if (RTown.PUBLIC_ROLE_ID.equals(normalizedRequiredRoleId)) {
            return true;
        }
        if (playerRecord == null) {
            return false;
        }
        if (playerRecord.getTownUUID() == null || !town.getIdentifier().equals(playerRecord.getTownUUID())) {
            return false;
        }

        final String playerRoleId = playerRecord.getTownRoleId() == null
                ? RTown.MEMBER_ROLE_ID
                : RTown.normalizeRoleId(playerRecord.getTownRoleId());

        if (RTown.MAYOR_ROLE_ID.equals(playerRoleId)) {
            return true;
        }
        if (RTown.MEMBER_ROLE_ID.equals(normalizedRequiredRoleId)) {
            return true;
        }
        if (RTown.MAYOR_ROLE_ID.equals(normalizedRequiredRoleId)) {
            return false;
        }
        return playerRoleId.equals(normalizedRequiredRoleId);
    }

    private @NotNull String resolveRequiredRoleId(
            final @NotNull TownChunkContext context,
            final @NotNull TownProtections protection
    ) {
        final RChunk chunk = context.chunk();
        if (chunk != null) {
            final String chunkRoleId = chunk.getProtectionOverrideRoleId(protection);
            if (chunkRoleId != null) {
                return chunkRoleId;
            }
        }
        return context.town().getProtectionRoleId(protection);
    }

    private boolean isProtectionActive(
            final @Nullable TownChunkContext context,
            final @NotNull TownProtections protection
    ) {
        if (context == null) {
            return false;
        }
        final String requiredRoleId = this.resolveRequiredRoleId(context, protection);
        return !RTown.PUBLIC_ROLE_ID.equals(requiredRoleId);
    }

    private @Nullable TownChunkContext resolveTownChunkContext(
            final int chunkX,
            final int chunkZ
    ) {
        final RRTown townRepository = this.plugin.getTownRepository();
        if (townRepository == null) {
            return null;
        }

        final List<RTown> activeTowns = townRepository.findAllByAttributes(Map.of("active", true));
        for (final RTown town : activeTowns) {
            final @Nullable RChunk claimedChunk = this.findTownChunk(town, chunkX, chunkZ);
            if (claimedChunk != null) {
                return new TownChunkContext(town, claimedChunk);
            }
            if (this.isNexusChunk(town, chunkX, chunkZ)) {
                return new TownChunkContext(town, null);
            }
        }
        return null;
    }

    private boolean isNexusChunk(
            final @NotNull RTown town,
            final int chunkX,
            final int chunkZ
    ) {
        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            return false;
        }
        return nexusLocation.getChunk().getX() == chunkX && nexusLocation.getChunk().getZ() == chunkZ;
    }

    private @Nullable RChunk findTownChunk(
            final @NotNull RTown town,
            final int chunkX,
            final int chunkZ
    ) {
        for (final RChunk chunk : town.getChunks()) {
            if (chunk.getX_loc() == chunkX && chunk.getZ_loc() == chunkZ) {
                return chunk;
            }
        }
        return null;
    }

    private @Nullable RDTPlayer resolveTownPlayer(final @NotNull Player player) {
        final RRDTPlayer playerRepository = this.plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }
        return playerRepository.findByPlayer(player.getUniqueId());
    }

    private boolean isPlayerInTown(
            final @Nullable RDTPlayer playerRecord,
            final @NotNull RTown town
    ) {
        return playerRecord != null
                && playerRecord.getTownUUID() != null
                && town.getIdentifier().equals(playerRecord.getTownUUID());
    }

    private @Nullable Player resolveAttackingPlayer(final @NotNull Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            final ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private @Nullable TownProtections resolveBlockInteractionProtection(final @NotNull Material material) {
        return switch (material) {
            case CHEST -> TownProtections.CHEST;
            case TRAPPED_CHEST -> TownProtections.TRAPPED_CHEST;
            case FURNACE -> TownProtections.FURNACE;
            case BLAST_FURNACE -> TownProtections.BLAST_FURNACE;
            case DISPENSER -> TownProtections.DISPENSER;
            case HOPPER -> TownProtections.HOPPER;
            case DROPPER -> TownProtections.DROPPER;
            case JUKEBOX -> TownProtections.JUKEBOX;
            case STONECUTTER -> TownProtections.STONECUTTER;
            case SMITHING_TABLE -> TownProtections.SMITHING_TABLE;
            case FLETCHING_TABLE -> TownProtections.FLETCHING_TABLE;
            case SMOKER -> TownProtections.SMOKER;
            case LOOM -> TownProtections.LOOM;
            case GRINDSTONE -> TownProtections.GRINDSTONE;
            case COMPOSTER -> TownProtections.COMPOSTER;
            case CARTOGRAPHY_TABLE -> TownProtections.CARTOGRAPHY_TABLE;
            case BELL -> TownProtections.BELL;
            case BARREL -> TownProtections.BARREL;
            case BREWING_STAND -> TownProtections.BREWING_STAND;
            case LEVER -> TownProtections.LEVER;
            case LODESTONE -> TownProtections.LODESTONE;
            case RESPAWN_ANCHOR -> TownProtections.RESPAWN_ANCHOR;
            default -> this.resolveGroupedBlockProtection(material);
        };
    }

    private @Nullable TownProtections resolveGroupedBlockProtection(final @NotNull Material material) {
        final String materialName = material.name();
        if (materialName.endsWith("_SHULKER_BOX")) {
            return TownProtections.SHULKER_BOXES;
        }
        if (materialName.endsWith("_PRESSURE_PLATE")) {
            return TownProtections.PRESSURE_PLATES;
        }
        if (materialName.endsWith("_BUTTON")) {
            return TownProtections.BUTTONS;
        }
        if (materialName.endsWith("_FENCE_GATE")) {
            return TownProtections.FENCE_GATES;
        }
        if (materialName.endsWith("_TRAPDOOR")) {
            return TownProtections.TRAPDOORS;
        }
        if (materialName.endsWith("_DOOR") && !materialName.startsWith("IRON_")) {
            return TownProtections.WOOD_DOORS;
        }
        return null;
    }

    private @Nullable TownProtections resolveItemInteractionProtection(final @NotNull Material material) {
        return switch (material) {
            case ENDER_PEARL -> TownProtections.ENDER_PEARL;
            case FIRE_CHARGE -> TownProtections.FIREBALL;
            case CHORUS_FRUIT -> TownProtections.CHORUS_FRUIT;
            default -> null;
        };
    }

    private boolean isHostileEntity(final @NotNull Entity entity) {
        return entity instanceof Monster;
    }

    private boolean isNeutralEntity(final @NotNull Entity entity) {
        return entity instanceof Animals
                || entity instanceof Villager
                || entity instanceof WanderingTrader;
    }

    private boolean isHoldingLead(final @NotNull Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.LEAD
                || player.getInventory().getItemInOffHand().getType() == Material.LEAD;
    }

    private void sendDeniedMessage(
            final @NotNull Player player,
            final @NotNull TownProtections protection,
            final @NotNull String requiredRoleId
    ) {
        new I18n.Builder("town_protection_listener.error.denied", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "action", protection.getProtectionKey(),
                        "required_role", requiredRoleId
                ))
                .build()
                .sendMessage();
    }

    private record TownChunkContext(@NotNull RTown town, @Nullable RChunk chunk) {
    }
}
