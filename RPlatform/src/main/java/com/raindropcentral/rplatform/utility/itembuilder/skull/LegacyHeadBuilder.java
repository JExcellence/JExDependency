package com.raindropcentral.rplatform.utility.itembuilder.skull;

import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Legacy skull builder that adapts reflective profile APIs to support texture manipulation across.
 * historical Bukkit versions.
 *
 * <p>Used when {@link com.raindropcentral.rplatform.version.ServerEnvironment#isPaper()} is false.
 * The builder handles legacy owner strings, reflective profile setters, and recent ResolvableProfile
 * mechanics.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@SuppressWarnings("deprecation")
public class LegacyHeadBuilder extends AItemBuilder<SkullMeta, LegacyHeadBuilder> implements IHeadBuilder<LegacyHeadBuilder> {

        /**
         * Creates a builder configured with the legacy player head material signature.
         */
        public LegacyHeadBuilder() {
                super(new ItemStack(Material.PLAYER_HEAD, 1, (short) 3));
        }

        /**
         * Applies a player texture sourced from the provided online player.
         *
         * @param player player supplying the head texture
         * @return fluent builder reference for chaining
         */
        @Override
        public LegacyHeadBuilder setPlayerHead(Player player) {
                if (
                        player == null
                ) {
                        return this;
                }

                meta.setOwner(player.getName());
                return this;
        }

        /**
         * Applies a player texture sourced from the provided offline player.
         *
         * @param offlinePlayer offline player supplying the head texture
         * @return fluent builder reference for chaining
         */
        @Override
        public LegacyHeadBuilder setPlayerHead(OfflinePlayer offlinePlayer) {
                if (
                        offlinePlayer == null
                ) {
                        return this;
                }

                meta.setOwner(offlinePlayer.getName());
                meta.setOwningPlayer(offlinePlayer);
                return this;
        }

        /**
         * Applies a custom texture to the skull by constructing a synthetic GameProfile.
         *
         * @param uuid profile identifier for the custom texture
         * @param textures base64 encoded texture payload
         * @return fluent builder reference for chaining
         */
        @Override
        public LegacyHeadBuilder setCustomTexture(
                @NotNull UUID uuid,
                @NotNull String textures
        ) {
                try {
                        String profileName = meta.getOwner();
                        if (profileName == null || profileName.trim().isEmpty()) {
                                profileName = "CustomHead";
                        }

                        Object profile = createGameProfile(uuid, profileName, textures);

                        // Try method-based approach first (older versions)
                        try {
                                meta.getClass()
                                        .getMethod("setProfile", profile.getClass())
                                        .invoke(meta, profile);
                        } catch (NoSuchMethodException e) {
                                // Fallback to field-based approach (newer versions like 1.21.8)
                                setProfileViaField(profile);
                        }
                } catch (Exception e) {
                        // Log the actual error for debugging
                        java.util.logging.Logger.getLogger("LegacyHeadBuilder")
                                .severe("Failed to set custom texture: " + e.getClass().getName() + ": " + e.getMessage());
                        if (e.getCause() != null) {
                                java.util.logging.Logger.getLogger("LegacyHeadBuilder")
                                        .severe("Caused by: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
                        }
                        throw new RuntimeException("Failed to set custom texture on legacy head", e);
                }
                return this;
        }

        /**
         * Sets the profile using direct field access (for newer Spigot versions).
         *
         * @param profile synthetic profile object to assign
         * @throws Exception when reflective access fails
         */
        private void setProfileViaField(Object profile) throws Exception {
                java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);

                // Check if the field expects ResolvableProfile (1.21.8+) or GameProfile (older)
                Class<?> fieldType = profileField.getType();

                if (fieldType.getSimpleName().equals("ResolvableProfile")) {
                        // Convert GameProfile to ResolvableProfile for 1.21.8+
                        Object resolvableProfile = createResolvableProfile(profile);
                        profileField.set(meta, resolvableProfile);
                } else {
                        // Use GameProfile directly for older versions
                        profileField.set(meta, profile);
                }
        }

        /**
         * Creates a ResolvableProfile from a GameProfile for Minecraft 1.21.8+.
         *
         * @param gameProfile game profile containing the texture data
         * @return resolvable profile compatible with newer Bukkit versions
         * @throws Exception when the reflective bridge cannot be established
         */
        private Object createResolvableProfile(Object gameProfile) throws Exception {
                // Try to create ResolvableProfile using reflection
                try {
                        Class<?> resolvableProfileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");

                        // Try constructor that takes GameProfile
                        try {
                                return resolvableProfileClass.getConstructor(gameProfile.getClass()).newInstance(gameProfile);
                        } catch (NoSuchMethodException e) {
                                // Try static factory method if constructor doesn't exist
                                try {
                                        return resolvableProfileClass.getMethod("of", gameProfile.getClass()).invoke(null, gameProfile);
                                } catch (NoSuchMethodException e2) {
                                        // Try other possible factory methods
                                        return resolvableProfileClass.getMethod("create", gameProfile.getClass()).invoke(null, gameProfile);
                                }
                        }
                } catch (Exception e) {
                        // If all else fails, try to use the GameProfile directly (might work in some cases)
                        throw new RuntimeException("Failed to create ResolvableProfile from GameProfile", e);
                }
        }

        /**
         * Creates a GameProfile populated with the provided base64 texture string.
         *
         * @param uuid profile identifier to associate with the texture
         * @param name profile name, defaulting to {@code CustomHead} when blank
         * @param texture base64 encoded texture payload
         * @return constructed GameProfile instance
         * @throws Exception when reflective GameProfile access fails
         */
        private Object createGameProfile(
                @NotNull UUID uuid,
                @NotNull String name,
                @NotNull String texture
        ) throws Exception {
                if (name.trim().isEmpty()) {
                        name = "CustomHead";
                }

                Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                Class<?> propertyClass    = Class.forName("com.mojang.authlib.properties.Property");
                Object   profile          = gameProfileClass
                                                    .getConstructor(UUID.class, String.class)
                                                    .newInstance(uuid, name);
                Object   property         = propertyClass
                                                    .getConstructor(String.class, String.class)
                                                    .newInstance("textures", texture);
                Object   properties       = gameProfileClass
                                                    .getMethod("getProperties")
                                                    .invoke(profile);
                properties
                        .getClass()
                        .getMethod("put", Object.class, Object.class)
                        .invoke(properties, "textures", property)
                ;
                return profile;
        }

        /**
         * Builds the legacy skull item with all applied metadata.
         *
         * @return fully configured player head item stack
         */
        @Override
        public ItemStack build() {
                item.setItemMeta(meta);
                return item;
        }

}
