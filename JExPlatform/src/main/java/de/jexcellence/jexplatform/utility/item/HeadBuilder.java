package de.jexcellence.jexplatform.utility.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Paper-first fluent builder for player head items with texture support.
 *
 * <p>Supports online player profiles, offline player profiles, and custom
 * Base64-encoded textures. Falls back to reflection-based {@code GameProfile}
 * injection when Paper profile APIs are unavailable.
 *
 * <pre>{@code
 * var playerHead = HeadBuilder.fromPlayer(player)
 *     .name(Component.text("Player's Head"))
 *     .build();
 *
 * var customHead = HeadBuilder.fromTexture(uuid, base64Texture)
 *     .name(Component.text("Custom Skull"))
 *     .build();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class HeadBuilder {

    private final ItemStack item;
    private final SkullMeta meta;

    private HeadBuilder() {
        this.item = new ItemStack(Material.PLAYER_HEAD);
        this.meta = (SkullMeta) item.getItemMeta();
    }

    /**
     * Creates a head builder with the texture of an online player.
     *
     * @param player the online player
     * @return a new builder instance
     */
    public static @NotNull HeadBuilder fromPlayer(@NotNull Player player) {
        var builder = new HeadBuilder();
        builder.meta.setPlayerProfile(player.getPlayerProfile());
        return builder;
    }

    /**
     * Creates a head builder with the texture of an offline player.
     *
     * @param player the offline player
     * @return a new builder instance
     */
    public static @NotNull HeadBuilder fromPlayer(@NotNull OfflinePlayer player) {
        var builder = new HeadBuilder();
        builder.meta.setPlayerProfile(player.getPlayerProfile());
        builder.meta.setOwningPlayer(player);
        return builder;
    }

    /**
     * Creates a head builder with a custom Base64-encoded texture.
     *
     * <p>Uses Paper's {@link PlayerProfile} API when available, falling back to
     * reflective {@code GameProfile} injection on unsupported platforms.
     *
     * @param uuid     synthetic profile UUID associated with the texture
     * @param textures Base64-encoded texture payload
     * @return a new builder instance
     */
    public static @NotNull HeadBuilder fromTexture(@NotNull UUID uuid, @NotNull String textures) {
        var builder = new HeadBuilder();
        try {
            applyPaperTexture(builder.meta, uuid, textures);
        } catch (Exception e) {
            applyLegacyTexture(builder.meta, uuid, textures);
        }
        return builder;
    }

    /**
     * Creates a blank head builder with no texture applied.
     *
     * @return a new builder instance
     */
    public static @NotNull HeadBuilder create() {
        return new HeadBuilder();
    }

    // ── Display ────────────────────────────────────────────────────────────────

    /**
     * Sets the display name of the head item.
     *
     * @param name the display name component
     * @return this builder
     */
    public @NotNull HeadBuilder name(@NotNull Component name) {
        meta.displayName(name);
        return this;
    }

    /**
     * Sets the lore of the head item.
     *
     * @param lore ordered lore lines
     * @return this builder
     */
    public @NotNull HeadBuilder lore(@NotNull List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    /**
     * Appends a lore line to the existing lore.
     *
     * @param line the lore component to append
     * @return this builder
     */
    public @NotNull HeadBuilder addLore(@NotNull Component line) {
        var current = currentLore();
        current.add(line);
        meta.lore(current);
        return this;
    }

    // ── Build ──────────────────────────────────────────────────────────────────

    /**
     * Commits metadata changes and returns the built head item.
     *
     * @return the fully configured player head item stack
     */
    public @NotNull ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private static void applyPaperTexture(@NotNull SkullMeta meta, @NotNull UUID uuid, @NotNull String textures) {
        PlayerProfile profile = Bukkit.createProfile(uuid, null);
        profile.setProperty(new ProfileProperty("textures", textures, null));
        meta.setPlayerProfile(profile);
    }

    private static void applyLegacyTexture(@NotNull SkullMeta meta, @NotNull UUID uuid, @NotNull String textures) {
        try {
            var gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            var propertyClass = Class.forName("com.mojang.authlib.properties.Property");

            var profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(uuid, "CustomHead");

            var property = propertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", textures);

            var properties = gameProfileClass
                    .getMethod("getProperties")
                    .invoke(profile);

            properties.getClass()
                    .getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", property);

            try {
                meta.getClass()
                        .getMethod("setProfile", gameProfileClass)
                        .invoke(meta, profile);
            } catch (NoSuchMethodException e) {
                var profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                var fieldType = profileField.getType();

                if (fieldType.getSimpleName().equals("ResolvableProfile")) {
                    var resolvableClass = Class.forName(
                            "net.minecraft.world.item.component.ResolvableProfile");
                    var resolvable = resolvableClass
                            .getConstructor(gameProfileClass).newInstance(profile);
                    profileField.set(meta, resolvable);
                } else {
                    profileField.set(meta, profile);
                }
            }
        } catch (Exception e) {
            // Texture application failed silently — head renders as Steve
        }
    }

    private @NotNull List<Component> currentLore() {
        @Nullable var existing = meta.lore();
        return existing != null ? new ArrayList<>(existing) : new ArrayList<>();
    }
}
