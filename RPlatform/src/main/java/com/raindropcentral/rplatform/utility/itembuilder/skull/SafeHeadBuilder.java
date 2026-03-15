package com.raindropcentral.rplatform.utility.itembuilder.skull;

import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Head builder that inspects runtime capabilities to select safe Paper or legacy code paths.
 *
 * <p>The builder attempts Paper's profile APIs first and falls back to reflective legacy logic when
 * necessary, reducing the risk of {@link NoSuchMethodError} on mixed environments.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class SafeHeadBuilder extends AItemBuilder<SkullMeta, SafeHeadBuilder> implements IHeadBuilder<SafeHeadBuilder> {

    /**
     * Creates a builder with a platform-compatible player head material.
     */
    public SafeHeadBuilder() {
        super(createHeadMaterial());
    }

    /**
     * Determines the player head material identifier supported by the current runtime.
     *
     * @return compatible player head material constant
     */
    private static Material createHeadMaterial() {
        try {
            return Material.PLAYER_HEAD;
        } catch (Exception e) {
            try {
                return Material.valueOf("SKULL_ITEM");
            } catch (Exception ex) {
                return Material.valueOf("PLAYER_HEAD");
            }
        }
    }

    /**
     * Sets playerHead.
     */
    @Override
    public SafeHeadBuilder setPlayerHead(Player player) {
        if (player == null) {
            return this;
        }

        if (ServerEnvironment.getInstance().isPaper()) {
            try {
                meta.setPlayerProfile(player.getPlayerProfile());
            } catch (Exception e) {
                meta.setOwner(player.getName());
            }
        } else {
            meta.setOwner(player.getName());
        }
        return this;
    }

    /**
     * Sets playerHead.
     */
    @Override
    public SafeHeadBuilder setPlayerHead(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            return this;
        }

        if (ServerEnvironment.getInstance().isPaper()) {
            try {
                meta.setPlayerProfile(offlinePlayer.getPlayerProfile());
                meta.setOwningPlayer(offlinePlayer);
            } catch (Exception e) {
                meta.setOwner(offlinePlayer.getName());
                meta.setOwningPlayer(offlinePlayer);
            }
        } else {
            meta.setOwner(offlinePlayer.getName());
            meta.setOwningPlayer(offlinePlayer);
        }
        return this;
    }

    /**
     * Sets customTexture.
     */
    @Override
    public SafeHeadBuilder setCustomTexture(@NotNull UUID uuid, @NotNull String textures) {
        if (ServerEnvironment.getInstance().isPaper()) {
            try {
                setPaperCustomTexture(uuid, textures);
                return this;
            } catch (Exception e) {
                // Fall back to legacy approach below.
            }
        }

        setLegacyCustomTexture(uuid, textures);
        return this;
    }

    /**
     * Sets custom texture using Paper-specific APIs.
     *
     * @param uuid profile identifier for the synthetic head
     * @param textures base64 encoded texture payload
     * @throws Exception when Paper profile classes are not available or misbehave
     */
    private void setPaperCustomTexture(@NotNull UUID uuid, @NotNull String textures) throws Exception {
        Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
        Class<?> playerProfileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
        Class<?> profilePropertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");

        Object profile = bukkitClass.getMethod("createProfile", UUID.class, String.class)
                                   .invoke(null, uuid, null);

        Object property = profilePropertyClass.getConstructor(String.class, String.class, String.class)
                                            .newInstance("textures", textures, null);

        playerProfileClass.getMethod("setProperty", profilePropertyClass)
                         .invoke(profile, property);

        meta.getClass().getMethod("setPlayerProfile", playerProfileClass)
            .invoke(meta, profile);
    }

    /**
     * Sets custom texture using legacy reflection method.
     *
     * @param uuid profile identifier for the synthetic head
     * @param textures base64 encoded texture payload
     */
    private void setLegacyCustomTexture(@NotNull UUID uuid, @NotNull String textures) {
        try {
            String profileName = meta.getOwner();
            if (profileName == null || profileName.trim().isEmpty()) {
                profileName = "CustomHead";
            }

            Object profile = createGameProfile(uuid, profileName, textures);

            try {
                meta.getClass()
                    .getMethod("setProfile", profile.getClass())
                    .invoke(meta, profile);
            } catch (NoSuchMethodException e) {
                setProfileViaField(profile);
            }
        } catch (Exception e) {
            System.err.println("Failed to set custom texture: " + e.getMessage());
        }
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

        Class<?> fieldType = profileField.getType();

        if (fieldType.getSimpleName().equals("ResolvableProfile")) {
            Object resolvableProfile = createResolvableProfile(profile);
            profileField.set(meta, resolvableProfile);
        } else {
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
        try {
            Class<?> resolvableProfileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");

            try {
                return resolvableProfileClass.getConstructor(gameProfile.getClass()).newInstance(gameProfile);
            } catch (NoSuchMethodException e) {
                try {
                    return resolvableProfileClass.getMethod("of", gameProfile.getClass()).invoke(null, gameProfile);
                } catch (NoSuchMethodException e2) {
                    return resolvableProfileClass.getMethod("create", gameProfile.getClass()).invoke(null, gameProfile);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ResolvableProfile from GameProfile", e);
        }
    }

    /**
     * Creates a GameProfile with custom texture using reflection.
     *
     * @param uuid profile identifier to associate with the texture
     * @param name desired profile name, defaulting to {@code CustomHead} when blank
     * @param textureData base64 encoded texture payload
     * @return constructed GameProfile instance
     * @throws Exception when reflective GameProfile access fails
     */
    @NotNull
    private Object createGameProfile(@NotNull UUID uuid, String name, @NotNull String textureData) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            name = "CustomHead";
        }

        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

        Object profile = gameProfileClass
            .getConstructor(UUID.class, String.class)
            .newInstance(uuid, name);

        Object property = propertyClass
            .getConstructor(String.class, String.class)
            .newInstance("textures", textureData);

        Object properties = gameProfileClass
            .getMethod("getProperties")
            .invoke(profile);

        properties.getClass()
            .getMethod("put", Object.class, Object.class)
            .invoke(properties, "textures", property);

        return profile;
    }

    /**
     * Builds the head item with the applied platform-aware metadata.
     *
     * @return fully configured head item stack
     */
    @Override
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
