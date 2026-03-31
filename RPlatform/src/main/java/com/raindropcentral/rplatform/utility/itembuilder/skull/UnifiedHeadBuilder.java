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

package com.raindropcentral.rplatform.utility.itembuilder.skull;

import com.raindropcentral.rplatform.api.PlatformAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unified head builder that works across Paper, Spigot, and Bukkit server variants.
 *
 * <p>The builder inspects whether {@link PlatformAPI} is available to drive head creation through
 * platform abstractions, otherwise falling back to the {@link SafeHeadBuilder} reflection bridge.
 * Callers can set textures, translations, and lore without worrying about runtime capabilities.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class UnifiedHeadBuilder {

    /**
     * Optional platform API used to delegate head mutations when available.
     */
    private final PlatformAPI platformAPI;

    /**
     * Mutable head instance that accumulates metadata updates prior to {@link #build()}.
     */
    private ItemStack head;
    
    /**
     * Creates a new UnifiedHeadBuilder with a default player head.
     *
     * @param platformAPI the platform API instance to use
     */
    public UnifiedHeadBuilder(@NotNull PlatformAPI platformAPI) {
        this.platformAPI = platformAPI;
        this.head = platformAPI.createPlayerHead((Player) null); // Creates empty head
    }
    
    /**
     * Creates a new UnifiedHeadBuilder starting with the specified player's head.
     *
     * @param platformAPI the platform API instance to use
     * @param player the player whose head to use
     */
    public UnifiedHeadBuilder(@NotNull PlatformAPI platformAPI, @Nullable Player player) {
        this.platformAPI = platformAPI;
        this.head = platformAPI.createPlayerHead(player);
    }
    
    /**
     * Creates a new UnifiedHeadBuilder starting with the specified offline player's head.
     *
     * @param platformAPI the platform API instance to use
     * @param offlinePlayer the offline player whose head to use
     */
    public UnifiedHeadBuilder(@NotNull PlatformAPI platformAPI, @Nullable OfflinePlayer offlinePlayer) {
        this.platformAPI = platformAPI;
        this.head = platformAPI.createPlayerHead(offlinePlayer);
    }
    
    /**
     * Creates a new UnifiedHeadBuilder starting with a custom textured head.
     *
     * @param platformAPI the platform API instance to use
     * @param uuid the UUID to associate with the head
     * @param textureData the base64 encoded texture data
     */
    public UnifiedHeadBuilder(@NotNull PlatformAPI platformAPI, @NotNull UUID uuid, @NotNull String textureData) {
        this.platformAPI = platformAPI;
        this.head = platformAPI.createCustomHead(uuid, textureData);
    }
    
    /**
     * Creates a new UnifiedHeadBuilder with a default player head using legacy builders.
     * This constructor is used when PlatformAPI is not available.
     */
    public UnifiedHeadBuilder() {
        this.platformAPI = null;
        this.head = createHeadUsingLegacyBuilders(null, null, null, null);
    }
    
    /**
     * Creates a new UnifiedHeadBuilder starting with the specified player's head using legacy builders.
     *
     * @param player the player whose head to use
     */
    public UnifiedHeadBuilder(@Nullable Player player) {
        this.platformAPI = null;
        this.head = createHeadUsingLegacyBuilders(player, null, null, null);
    }
    
    /**
     * Creates a new UnifiedHeadBuilder starting with the specified offline player's head using legacy builders.
     *
     * @param offlinePlayer the offline player whose head to use
     */
    public UnifiedHeadBuilder(@Nullable OfflinePlayer offlinePlayer) {
        this.platformAPI = null;
        this.head = createHeadUsingLegacyBuilders(null, offlinePlayer, null, null);
    }
    
    /**
     * Creates a new UnifiedHeadBuilder starting with a custom textured head using legacy builders.
     *
     * @param uuid the UUID to associate with the head
     * @param textureData the base64 encoded texture data
     */
    public UnifiedHeadBuilder(@NotNull UUID uuid, @NotNull String textureData) {
        this.platformAPI = null;
        this.head = createHeadUsingLegacyBuilders(null, null, uuid, textureData);
    }
    
    /**
     * Creates a head using the safe builder system when PlatformAPI is not available.
     *
     * @param player optional player supplying the texture
     * @param offlinePlayer optional offline player supplying the texture
     * @param uuid optional profile identifier for custom textures
     * @param textureData optional base64 texture payload
     * @return built item stack using the safe builder fallbacks
     */
    @NotNull
    private ItemStack createHeadUsingLegacyBuilders(@Nullable Player player, @Nullable OfflinePlayer offlinePlayer,
                                                   @Nullable UUID uuid, @Nullable String textureData) {
        // Use SafeHeadBuilder which automatically detects platform and uses appropriate methods

        SafeHeadBuilder builder = new SafeHeadBuilder();
        
        if (player != null) {
            builder.setPlayerHead(player);
        } else if (offlinePlayer != null) {
            builder.setPlayerHead(offlinePlayer);
        } else if (uuid != null && textureData != null) {
            builder.setCustomTexture(uuid, textureData);
        }
        
        return builder.build();
    }
    
    /**
     * Sets the head to use the specified player's skin.
     *
     * @param player the player whose head to use
     * @return this builder for chaining
     */
    @NotNull
    public UnifiedHeadBuilder setPlayerHead(@Nullable Player player) {
        if (platformAPI != null) {
            this.head = platformAPI.createPlayerHead(player);
        } else {
            this.head = createHeadUsingLegacyBuilders(player, null, null, null);
        }
        return this;
    }
    
    /**
     * Sets the head to use the specified offline player's skin.
     *
     * @param offlinePlayer the offline player whose head to use
     * @return this builder for chaining
     */
    @NotNull
    public UnifiedHeadBuilder setPlayerHead(@Nullable OfflinePlayer offlinePlayer) {
        if (platformAPI != null) {
            this.head = platformAPI.createPlayerHead(offlinePlayer);
        } else {
            this.head = createHeadUsingLegacyBuilders(null, offlinePlayer, null, null);
        }
        return this;
    }
    
    /**
     * Sets a custom texture for the head using base64 texture data.
     *
     * @param uuid the UUID to associate with the head (can be random)
     * @param textureData the base64 encoded texture data
     * @return this builder for chaining
     */
    @NotNull
    public UnifiedHeadBuilder setCustomTexture(@NotNull UUID uuid, @NotNull String textureData) {
        if (platformAPI != null) {
            this.head = platformAPI.applyCustomTexture(this.head, uuid, textureData);
        } else {
            // For legacy builders, we need to recreate the head with the texture
            this.head = createHeadUsingLegacyBuilders(null, null, uuid, textureData);
        }
        return this;
    }
    
    /**
     * Sets the display name of the head.
     *
     * @param displayName the display name component
     * @return this builder for chaining
     */
    @NotNull
    public UnifiedHeadBuilder setDisplayName(@Nullable Component displayName) {
        if (platformAPI != null) {
            this.head = platformAPI.setItemDisplayName(this.head, displayName);
        } else {
            // Use legacy method for setting display name
            setDisplayNameLegacy(displayName);
        }
        return this;
    }
    
    /**
     * Sets the display name of the head using a string.
     *
     * @param displayName the display name string
     * @return this builder for chaining
     */
    @NotNull
    public UnifiedHeadBuilder setDisplayName(@Nullable String displayName) {
        Component component = displayName != null ? Component.text(displayName) : null;
        return setDisplayName(component);
    }

    /**
     * Sets the display name using legacy methods when PlatformAPI is not available.
     *
     * @param displayName translated display name component or {@code null} to clear it
     */
    private void setDisplayNameLegacy(@Nullable Component displayName) {
        var meta = this.head.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(convertComponentToLegacy(displayName));
            } else {
                meta.setDisplayName(null);
            }
            this.head.setItemMeta(meta);
        }
    }
    
    /**
     * Sets the lore using legacy methods when PlatformAPI is not available.
     *
     * @param lore ordered lore components to serialize
     */
    private void setLoreLegacy(@NotNull List<Component> lore) {
        var meta = this.head.getItemMeta();
        if (meta != null) {
            List<String> legacyLore = new ArrayList<>();
            for (Component component : lore) {
                legacyLore.add(convertComponentToLegacy(component));
            }
            meta.setLore(legacyLore);
            this.head.setItemMeta(meta);
        }
    }
    
    /**
     * Converts a {@link Component} to legacy string format for compatibility.
     *
     * @param component Adventure component to serialize
     * @return serialized legacy text
     */
    private String convertComponentToLegacy(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Gets the current lore using legacy methods when PlatformAPI is not available.
     *
     * @return current lore components converted from legacy strings
     */
    @NotNull
    private List<Component> getLoreLegacy() {
        var meta = this.head.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return new ArrayList<>();
        }
        
        List<String> legacyLore = meta.getLore();
        if (legacyLore == null) {
            return new ArrayList<>();
        }
        
        List<Component> componentLore = new ArrayList<>();
        for (String line : legacyLore) {
            componentLore.add(Component.text(line));
        }
        return componentLore;
    }
    
    /**
     * Sets the lore of the head.
     *
     * @param lore the lore components
     * @return this builder for chaining
     */
    @NotNull
    public UnifiedHeadBuilder setLore(@NotNull List<Component> lore) {
        if (platformAPI != null) {
            this.head = platformAPI.setItemLore(this.head, lore);
        } else {
            setLoreLegacy(lore);
        }
        return this;
    }
    
    /**
     * Adds a single line to the lore.
     *
     * @param line the lore line component
     * @return this builder for chaining
     */
    @NotNull
    public UnifiedHeadBuilder addLoreLine(@NotNull Component line) {
        List<Component> currentLore = getLore();
        currentLore.add(line);
        return setLore(currentLore);
    }
    
    /**
     * Adds a single line to the lore using a string.
     *
     * @param line the lore line string
     * @return this builder for chaining
     */
    @NotNull
    public UnifiedHeadBuilder addLoreLine(@NotNull String line) {
        return addLoreLine(Component.text(line));
    }
    
    /**
     * Gets the current display name of the head.
     *
     * @return the display name component, or null if none
     */
    @Nullable
    public Component getDisplayName() {
        if (platformAPI != null) {
            return platformAPI.getItemDisplayName(this.head);
        } else {
            var meta = this.head.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return Component.text(meta.getDisplayName());
            }
            return null;
        }
    }
    
    /**
     * Gets the current lore of the head.
     *
     * @return the lore components
     */
    @NotNull
    public List<Component> getLore() {
        if (platformAPI != null) {
            return platformAPI.getItemLore(this.head);
        } else {
            return getLoreLegacy();
        }
    }
    
    /**
     * Builds and returns the final ItemStack.
     *
     * @return the completed head ItemStack
     */
    @NotNull
    public ItemStack build() {
        return this.head.clone();
    }
    
    /**
     * Creates a new UnifiedHeadBuilder for a player head.
     *
     * @param player the player whose head to create
     * @return a new UnifiedHeadBuilder
     */
    @NotNull
    public static UnifiedHeadBuilder player(@Nullable Player player) {
        return new UnifiedHeadBuilder(player);
    }
    
    /**
     * Creates a new UnifiedHeadBuilder for an offline player head.
     *
     * @param offlinePlayer the offline player whose head to create
     * @return a new UnifiedHeadBuilder
     */
    @NotNull
    public static UnifiedHeadBuilder player(@Nullable OfflinePlayer offlinePlayer) {
        return new UnifiedHeadBuilder(offlinePlayer);
    }
    
    /**
     * Creates a new UnifiedHeadBuilder for a custom textured head.
     *
     * @param uuid the UUID to associate with the head
     * @param textureData the base64 encoded texture data
     * @return a new UnifiedHeadBuilder
     */
    @NotNull
    public static UnifiedHeadBuilder custom(@NotNull UUID uuid, @NotNull String textureData) {
        return new UnifiedHeadBuilder(uuid, textureData);
    }
    
    /**
     * Creates a new UnifiedHeadBuilder for a custom textured head with a random UUID.
     *
     * @param textureData the base64 encoded texture data
     * @return a new UnifiedHeadBuilder
     */
    @NotNull
    public static UnifiedHeadBuilder custom(@NotNull String textureData) {
        return new UnifiedHeadBuilder(UUID.randomUUID(), textureData);
    }
}