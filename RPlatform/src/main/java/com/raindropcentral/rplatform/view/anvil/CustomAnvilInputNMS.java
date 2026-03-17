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

package com.raindropcentral.rplatform.view.anvil;

import me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate;
import me.devnatan.inventoryframework.runtime.thirdparty.McVersion;
import me.devnatan.inventoryframework.runtime.thirdparty.ReflectionUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.logging.Logger;

import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.*;
import static me.devnatan.inventoryframework.runtime.thirdparty.ReflectionUtils.ENTITY_PLAYER;
import static me.devnatan.inventoryframework.runtime.thirdparty.ReflectionUtils.getNMSClass;

/**
 * Custom AnvilInput NMS implementation that fixes the InventoryView casting issue.
 * from the original AnvilInputNMS class in the external library.
 *
 * <p>This implementation provides a more robust and error-resistant approach to
 * opening anvil inventories with proper Component handling and better error reporting.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class CustomAnvilInputNMS {
    
    private static final Logger LOGGER = Logger.getLogger(CustomAnvilInputNMS.class.getName());
    
    // CONSTRUCTORS
    private static final MethodHandle ANVIL_CONSTRUCTOR;
    private static final Class<?> ANVIL;
    
    // METHODS
    private static final MethodHandle GET_PLAYER_NEXT_CONTAINER_COUNTER;
    private static final MethodHandle GET_PLAYER_INVENTORY;
    private static final MethodHandle SET_PLAYER_ACTIVE_CONTAINER;
    private static final MethodHandle ADD_CONTAINER_SLOT_LISTENER;
    private static final MethodHandle INIT_MENU;
    
    // FIELDS
    private static final MethodHandle CONTAINER_CHECK_REACHABLE;
    private static final MethodHandle PLAYER_DEFAULT_CONTAINER;
    private static final MethodHandle CONTAINER_WINDOW_ID;
    
    static {
        try {
            ANVIL = Objects.requireNonNull(
                getNMSClass("world.inventory", "ContainerAnvil"), "ContainerAnvil NMS class not found");
            
            final Class<?> playerInventoryClass = getNMSClass("world.entity.player", "PlayerInventory");
            
            ANVIL_CONSTRUCTOR = getConstructor(ANVIL, int.class, playerInventoryClass);
            CONTAINER_CHECK_REACHABLE = setFieldHandle(CONTAINER, boolean.class, "checkReachable");
            
            final Class<?> containerPlayer = getNMSClass("world.inventory", "ContainerPlayer");
            PLAYER_DEFAULT_CONTAINER = getField(ENTITY_PLAYER, containerPlayer, "inventoryMenu", "bQ", "bR");
            
            final String activeContainerObfuscatedName = ReflectionUtils.supportsMC1202() ? "bS" : "bR";
            SET_PLAYER_ACTIVE_CONTAINER = setField(
                ENTITY_PLAYER, containerPlayer, "activeContainer", "containerMenu", activeContainerObfuscatedName);
            
            GET_PLAYER_NEXT_CONTAINER_COUNTER =
                getMethod(ENTITY_PLAYER, "nextContainerCounter", MethodType.methodType(int.class));
            
            GET_PLAYER_INVENTORY = getMethod(
                ENTITY_PLAYER, "fN", MethodType.methodType(playerInventoryClass), false, "fR", "getInventory");
            
            CONTAINER_WINDOW_ID = setField(CONTAINER, int.class, "windowId", "containerId", "j");
            ADD_CONTAINER_SLOT_LISTENER = getMethod(
                CONTAINER, "a", MethodType.methodType(void.class, getNMSClass("world.inventory.ICrafting")));
            INIT_MENU = getMethod(ENTITY_PLAYER, "a", MethodType.methodType(void.class, CONTAINER));
        } catch (Exception exception) {
            throw new RuntimeException(
                "Unsupported version for Anvil Input feature: " + ReflectionUtils.getVersionInformation(),
                exception);
        }
    }
    
    private CustomAnvilInputNMS() {}
    
    /**
     * Opens an anvil inventory for the specified player with the given title and initial input.
     * This method fixes the InventoryView casting bug present in the original AnvilInputNMS.
     *
     * @param player the player to open the anvil for (must not be null)
     * @param title the title of the anvil inventory (can be null, will use default)
     * @param initialInput the initial input text to display (can be null, will use empty string)
     * @return the opened anvil inventory
     * @throws IllegalArgumentException if player is null
     * @throws IllegalStateException if CustomAnvilInputNMS was not properly initialized
     * @throws RuntimeException if something goes wrong while opening the inventory
     */
    public static Inventory open(Player player, Object title, String initialInput) {
        try {
            final Object entityPlayer = ReflectionUtils.getEntityPlayer(player);
            final Object defaultContainer = PLAYER_DEFAULT_CONTAINER.invoke(entityPlayer);
            SET_PLAYER_ACTIVE_CONTAINER.invoke(entityPlayer, defaultContainer);
            
            final int windowId = (int) GET_PLAYER_NEXT_CONTAINER_COUNTER.invoke(entityPlayer);
            final Object anvilContainer = ANVIL_CONSTRUCTOR.invoke(windowId, GET_PLAYER_INVENTORY.invoke(entityPlayer));
            CONTAINER_CHECK_REACHABLE.invoke(anvilContainer, false);
            
            // FIXED: Get the AnvilInventory without the problematic InventoryView cast
            final Object bukkitView = InventoryUpdate.getBukkitView.invoke(anvilContainer);
            if (bukkitView == null) {
                throw new IllegalStateException("Failed to get bukkit view from anvil container");
            }
            
            // Set anvil properties if it's an AnvilView
            if (bukkitView instanceof AnvilView anvilView) {
                anvilView.setRepairCost(0);
                anvilView.setMaximumRepairCost(0);
            }
            
            final Object topInventory = bukkitView.getClass()
                                                  .getMethod("getTopInventory")
                                                  .invoke(bukkitView);
            
            if (!(topInventory instanceof AnvilInventory)) {
                throw new IllegalStateException("Expected AnvilInventory but got: " +
                                                (topInventory != null ? topInventory.getClass().getSimpleName() : "null"));
            }
            
            final AnvilInventory inventory = (AnvilInventory) topInventory;
            inventory.setMaximumRepairCost(0);
            
            @SuppressWarnings("deprecation")
            final ItemStack item = new ItemStack(Material.PAPER, 1, (short) 0);
            final ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
            meta.setDisplayName(initialInput);
            item.setItemMeta(meta);
            inventory.setItem(0, item);
            
            Object nmsContainers = getContainerOrName(InventoryUpdate.Containers.ANVIL, InventoryType.ANVIL);
            Object updatedTitle = createTitleComponent(title == null ? "" : title);
            Object openWindowPacket = useContainers()
                                      ? packetPlayOutOpenWindow.invoke(windowId, nmsContainers, updatedTitle)
                                      : packetPlayOutOpenWindow.invoke(
                windowId, nmsContainers, updatedTitle, InventoryType.ANVIL.getDefaultSize());
            
            ReflectionUtils.sendPacketSync(player, openWindowPacket);
            SET_PLAYER_ACTIVE_CONTAINER.invoke(entityPlayer, anvilContainer);
            CONTAINER_WINDOW_ID.invoke(anvilContainer, windowId);
            
            if (McVersion.supports(19)) {
                INIT_MENU.invoke(entityPlayer, anvilContainer);
            } else {
                ADD_CONTAINER_SLOT_LISTENER.invoke(anvilContainer, player);
            }
            return inventory;
        } catch (Throwable throwable) {
            throw new RuntimeException("Something went wrong while opening Anvil Input NMS inventory.", throwable);
        }
    }
}
