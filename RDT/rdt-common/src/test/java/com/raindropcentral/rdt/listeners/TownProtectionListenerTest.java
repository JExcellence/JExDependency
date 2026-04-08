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

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.service.TownRuntimeService;
import com.raindropcentral.rdt.service.TownService;
import com.raindropcentral.rdt.utils.TownProtections;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TownProtectionListenerTest {

    @Mock
    private TownRuntimeService townRuntimeService;

    @Mock
    private World world;

    @Mock
    private Monster hostileMonster;

    @Mock
    private Animals passiveAnimal;

    @Mock
    private Villager villager;

    @Mock
    private JavaPlugin javaPlugin;

    @Mock
    private TownService townService;

    @Test
    void onEntityMoveRemovesHostileEntityThatEntersRestrictedChunk() throws ReflectiveOperationException {
        final TownProtectionListener listener = new TownProtectionListener(this.createPlugin(this.townRuntimeService));
        when(this.world.getUID()).thenReturn(UUID.randomUUID());
        final Location from = new Location(this.world, 15.5, 64.0, 0.5);
        final Location to = new Location(this.world, 16.5, 64.0, 0.5);
        when(this.townRuntimeService.isWorldActionAllowed(to, TownProtections.TOWN_HOSTILE_ENTITIES)).thenReturn(false);

        listener.onEntityMove(new EntityMoveEvent(this.hostileMonster, from, to));

        verify(this.townRuntimeService).isWorldActionAllowed(to, TownProtections.TOWN_HOSTILE_ENTITIES);
        verify(this.hostileMonster).remove();
    }

    @Test
    void onEntityMoveSkipsSameChunkPassiveMovement() {
        final TownProtectionListener listener = new TownProtectionListener(this.createPlugin(null));
        when(this.world.getUID()).thenReturn(UUID.randomUUID());
        final Location from = new Location(this.world, 17.5, 64.0, 0.5);
        final Location to = new Location(this.world, 31.5, 64.0, 0.5);

        listener.onEntityMove(new EntityMoveEvent(this.passiveAnimal, from, to));

        verifyNoInteractions(this.townRuntimeService);
        verify(this.passiveAnimal, never()).remove();
    }

    @Test
    void onEntityMoveIgnoresVillagersBecausePassiveProtectionTargetsAnimalsOnly() {
        final TownProtectionListener listener = new TownProtectionListener(this.createPlugin(null));
        final Location from = new Location(this.world, 15.5, 64.0, 0.5);
        final Location to = new Location(this.world, 16.5, 64.0, 0.5);

        listener.onEntityMove(new EntityMoveEvent(this.villager, from, to));

        verifyNoInteractions(this.townRuntimeService);
        verify(this.villager, never()).remove();
    }

    @Test
    void resolveInteractProtectionReturnsSpecificSwitchEntriesForListedBlocks() {
        assertEquals(TownProtections.CHEST, TownProtectionListener.resolveInteractProtection(this.blockWith(Material.CHEST)));
        assertEquals(
            TownProtections.SHULKER_BOXES,
            TownProtectionListener.resolveInteractProtection(this.blockWith(Material.SHULKER_BOX))
        );
        assertEquals(
            TownProtections.BLAST_FURNACE,
            TownProtectionListener.resolveInteractProtection(this.blockWith(Material.BLAST_FURNACE))
        );
        assertEquals(
            TownProtections.PRESSURE_PLATES,
            TownProtectionListener.resolveInteractProtection(this.blockWith(Material.STONE_PRESSURE_PLATE))
        );
        assertEquals(TownProtections.WOOD_DOORS, TownProtectionListener.resolveInteractProtection(this.blockWith(Material.OAK_DOOR)));
        assertEquals(TownProtections.TARGET, TownProtectionListener.resolveInteractProtection(this.blockWith(Material.TARGET)));
    }

    @Test
    void resolveInteractProtectionFallsBackToGenericSwitchAccessForUnlistedOpenables() {
        final Block block = Mockito.mock(Block.class);
        when(block.getType()).thenReturn(Material.IRON_DOOR);
        when(block.getBlockData()).thenReturn(Mockito.mock(Openable.class));

        assertEquals(TownProtections.SWITCH_ACCESS, TownProtectionListener.resolveInteractProtection(block));
    }

    @Test
    void resolveEntityInteractProtectionMatchesMinecartsOnly() {
        final Entity minecart = Mockito.mock(Entity.class);
        when(minecart.getType()).thenReturn(EntityType.MINECART);
        final Entity villagerEntity = Mockito.mock(Entity.class);
        when(villagerEntity.getType()).thenReturn(EntityType.VILLAGER);

        assertEquals(TownProtections.MINECARTS, TownProtectionListener.resolveEntityInteractProtection(minecart));
        assertNull(TownProtectionListener.resolveEntityInteractProtection(villagerEntity));
    }

    @Test
    void resolveItemUseProtectionMatchesEachConfiguredItemAction() {
        assertEquals(TownProtections.MINECARTS, TownProtectionListener.resolveItemUseProtection(Material.MINECART));
        assertEquals(TownProtections.BOATS, TownProtectionListener.resolveItemUseProtection(Material.OAK_BOAT));
        assertEquals(TownProtections.ENDER_PEARL, TownProtectionListener.resolveItemUseProtection(Material.ENDER_PEARL));
        assertEquals(TownProtections.FIREBALL, TownProtectionListener.resolveItemUseProtection(Material.FIRE_CHARGE));
        assertEquals(TownProtections.CHORUS_FRUIT, TownProtectionListener.resolveItemUseProtection(Material.CHORUS_FRUIT));
        assertEquals(TownProtections.LEAD, TownProtectionListener.resolveItemUseProtection(Material.LEAD));
        assertNull(TownProtectionListener.resolveItemUseProtection(Material.STONE));
    }

    @Test
    void resolveEntityItemUseProtectionMatchesLeadTargetsOnly() {
        final org.bukkit.entity.LivingEntity cow = Mockito.mock(org.bukkit.entity.LivingEntity.class);
        final Entity droppedItem = Mockito.mock(Entity.class);
        when(droppedItem.getType()).thenReturn(EntityType.ITEM);

        assertEquals(TownProtections.LEAD, TownProtectionListener.resolveEntityItemUseProtection(cow, Material.LEAD));
        assertNull(TownProtectionListener.resolveEntityItemUseProtection(droppedItem, Material.LEAD));
        assertNull(TownProtectionListener.resolveEntityItemUseProtection(cow, Material.STICK));
    }

    private RDT createPlugin(final TownRuntimeService runtimeService) {
        final RDT plugin = new RDT(this.javaPlugin, "test", this.townService);
        if (runtimeService == null) {
            return plugin;
        }

        try {
            setField(plugin, "townRuntimeService", runtimeService);
            return plugin;
        } catch (final ReflectiveOperationException exception) {
            throw new AssertionError("Failed to attach TownRuntimeService to test plugin.", exception);
        }
    }

    private static void setField(final RDT target, final String fieldName, final Object value) throws ReflectiveOperationException {
        final Field field = RDT.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Block blockWith(final Material material) {
        final Block block = Mockito.mock(Block.class);
        when(block.getType()).thenReturn(material);
        return block;
    }
}
