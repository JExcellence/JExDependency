package de.jexcellence.jexplatform.server;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Platform-agnostic API for messaging, item names, and player heads.
 *
 * <p>Each server type receives its own implementation: Paper uses native
 * Adventure, Spigot bridges through BukkitAudiences, and Folia extends
 * the Paper implementation. Use the {@link #create(JavaPlugin, ServerType)}
 * factory to obtain the correct variant:
 *
 * <pre>{@code
 * var api = PlatformApi.create(plugin, serverType);
 * api.sendMessage(player, Component.text("Hello!"));
 * api.sendActionBar(player, Component.text("Status"));
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public sealed interface PlatformApi permits PaperPlatformApi, SpigotPlatformApi {

    /**
     * Sends a chat message to a player.
     *
     * @param player  the recipient
     * @param message the Adventure component to send
     */
    void sendMessage(@NotNull Player player, @NotNull Component message);

    /**
     * Sends an action bar message to a player.
     *
     * @param player  the recipient
     * @param message the Adventure component to display
     */
    void sendActionBar(@NotNull Player player, @NotNull Component message);

    /**
     * Sends a title and subtitle to a player.
     *
     * @param player       the recipient
     * @param title        the title component
     * @param subtitle     the subtitle component
     * @param fadeInTicks  fade-in duration in ticks
     * @param stayTicks    stay duration in ticks
     * @param fadeOutTicks fade-out duration in ticks
     */
    void sendTitle(@NotNull Player player, @NotNull Component title, @NotNull Component subtitle,
                   int fadeInTicks, int stayTicks, int fadeOutTicks);

    /**
     * Returns the player's display name as an Adventure component.
     *
     * @param player the player
     * @return the display name component
     */
    @NotNull Component playerDisplayName(@NotNull Player player);

    /**
     * Returns the display name of an item as an Adventure component.
     *
     * @param item the item stack
     * @return the display name, or a translatable component if unnamed
     */
    @NotNull Component itemDisplayName(@NotNull ItemStack item);

    /**
     * Sets the display name of an item.
     *
     * @param item the item stack to modify
     * @param name the Adventure component to set as display name
     */
    void setItemDisplayName(@NotNull ItemStack item, @NotNull Component name);

    /**
     * Returns the lore lines of an item as Adventure components.
     *
     * @param item the item stack
     * @return immutable list of lore components, empty if no lore
     */
    @NotNull List<Component> itemLore(@NotNull ItemStack item);

    /**
     * Sets the lore of an item.
     *
     * @param item the item stack to modify
     * @param lore the Adventure components to set as lore lines
     */
    void setItemLore(@NotNull ItemStack item, @NotNull List<Component> lore);

    /**
     * Creates a player head item for the given player.
     *
     * @param player the player whose head to create
     * @return an item stack with the player's skull texture
     */
    @NotNull ItemStack createPlayerHead(@NotNull OfflinePlayer player);

    /**
     * Creates a custom-textured head item.
     *
     * @param owner          UUID for the profile
     * @param base64Texture  base64-encoded skin texture value
     * @return an item stack with the specified skull texture
     */
    @NotNull ItemStack createTexturedHead(@NotNull UUID owner, @NotNull String base64Texture);

    /**
     * Creates the appropriate API implementation for the detected server type.
     *
     * @param plugin     owning plugin
     * @param serverType detected server type
     * @return platform-specific API implementation
     */
    static @NotNull PlatformApi create(@NotNull JavaPlugin plugin, @NotNull ServerType serverType) {
        return switch (serverType) {
            case ServerType.Folia ignored -> new FoliaPlatformApi(plugin);
            case ServerType.Paper ignored -> new PaperPlatformApi(plugin);
            case ServerType.Spigot ignored -> new SpigotPlatformApi(plugin);
        };
    }
}
