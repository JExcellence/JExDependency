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

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Paper-native head builder that uses {@link PlayerProfile} APIs for texture management.
 *
 * <p>Constructed by {@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory#head()} when
 * Paper is available, this builder avoids reflection and uses first-party profile types.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class ModernHeadBuilder extends AItemBuilder<SkullMeta, ModernHeadBuilder> implements IHeadBuilder<ModernHeadBuilder> {

        /**
         * Creates a modern head builder for the standard player head material.
         */
        public ModernHeadBuilder() {
                super(Material.PLAYER_HEAD);
        }

        /**
         * Applies player metadata sourced from a live {@link Player} instance.
         *
         * @param player player supplying the profile
         * @return fluent builder reference for chaining
         */
        @Override
        public ModernHeadBuilder setPlayerHead(Player player) {
                if (
                        player == null
                ) {
                        return this;
                }

                meta.setPlayerProfile(player.getPlayerProfile());
                return this;
        }

        /**
         * Applies player metadata sourced from an {@link OfflinePlayer} snapshot.
         *
         * @param offlinePlayer offline player supplying the profile
         * @return fluent builder reference for chaining
         */
        @Override
        public ModernHeadBuilder setPlayerHead(OfflinePlayer offlinePlayer) {
                if (
                        offlinePlayer == null
                ) {
                        return this;
                }

                meta.setPlayerProfile(offlinePlayer.getPlayerProfile());
                meta.setOwningPlayer(offlinePlayer);
                return this;
        }

        /**
         * Applies a custom texture using Paper's {@link PlayerProfile} support.
         *
         * @param uuid synthetic profile identifier associated with the texture
         * @param textures base64 encoded texture payload
         * @return fluent builder reference for chaining
         */
        @Override
        public ModernHeadBuilder setCustomTexture(
                @NotNull UUID uuid,
                @NotNull String textures
        ) {
                PlayerProfile profile = Bukkit.createProfile(uuid, null);
                profile.setProperty(new ProfileProperty("textures", textures, null));
                meta.setPlayerProfile(profile);
                return this;
        }

        /**
         * Builds the modern skull item with the applied metadata.
         *
         * @return fully configured player head item stack
         */
        @Override
        public ItemStack build() {
                item.setItemMeta(meta);
                return item;
        }

}