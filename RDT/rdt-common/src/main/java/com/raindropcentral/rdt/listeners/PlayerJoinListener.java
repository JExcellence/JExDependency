package com.raindropcentral.rdt.listeners;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NonNull;

/**
 * Bukkit listener that handles player join events for Raindrop Towns.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Create a new {@link com.raindropcentral.rdt.database.entity.RDTPlayer} record on first join</li>
 *   <li>Send an informative Adventure message about the player's town status</li>
 *   <li>Refresh the per-player boss bar via {@link com.raindropcentral.rdt.factory.BossBarFactory}</li>
 * </ul>
 * Messages use the Adventure API with consistent coloring:
 * primary green (0,200,0), details in gray, and tips in yellow.
 */
@SuppressWarnings("unused")
public class PlayerJoinListener implements Listener {

    private final RDT plugin;

    /**
     * Create a new join listener bound to the plugin instance.
     *
     * @param plugin main plugin instance
     */
    public PlayerJoinListener(RDT plugin) {
        this.plugin = plugin;
    }
    
    /// Handle the player join event:
    ///
    ///     - If the player has no record, create one asynchronously and send a quick start tip.
    ///     - If the player belongs to a town, show a welcome-back message with claim usage and chunk info.
    ///     - Otherwise, show a neutral welcome with command hints.
    ///     - Always updates the boss bar for the player's current location.
    ///
    ///
    /// @param event Bukkit player joins the event
    @EventHandler
    public void onPlayerJoin(@NonNull PlayerJoinEvent event) {
        var bukkitPlayer = event.getPlayer();
        final int chunkX = bukkitPlayer.getLocation().getChunk().getX();
        final int chunkZ = bukkitPlayer.getLocation().getChunk().getZ();

        var rPlayer = this.plugin.getPlayerRepository().findByPlayer(bukkitPlayer.getUniqueId());

        if (rPlayer == null) {
            // First join: create a record and inform the player how to get started
            var newPlayer = new RDTPlayer(bukkitPlayer.getUniqueId());
            this.plugin.getPlayerRepository().createAsync(newPlayer);
            this.plugin.getLogger().info("Created player entry for " + newPlayer.getIdentifier());

            var welcome = Component.text("Welcome! You're not in a town yet.")
                    .color(TextColor.color(0, 200, 0));
            var tip = Component.text("Use /prt create <name> or /prt join <name>")
                    .color(TextColor.color(255, 215, 0));
            bukkitPlayer.sendMessage(welcome.append(Component.newline()).append(tip));
        } else if (rPlayer.getTownUUID() != null) {
            var town = this.plugin.getTownRepository().findByTownUUID(rPlayer.getTownUUID());
            if (town != null) {
                int used = town.getChunks().size();
                int limit = this.plugin.getDefaultConfig().getClaimLimit();

                var title = Component.text("Welcome back to " + town.getTownName() + "!")
                        .color(TextColor.color(0, 200, 0));
                var details = Component.text(
                                String.format("Claims: %d/%d • Chunk: (%d, %d)", used, limit, chunkX, chunkZ))
                        .color(TextColor.color(160, 160, 160));
                bukkitPlayer.sendMessage(title.append(Component.newline()).append(details));
            } else {
                // Town reference missing; fall back to a generic message
                var msg = Component.text("Welcome! You're not in a town yet.")
                        .color(TextColor.color(0, 200, 0));
                bukkitPlayer.sendMessage(msg);
            }
        } else {
            // Has player record but no town
            var welcome = Component.text("Welcome! You're not in a town yet.")
                    .color(TextColor.color(0, 200, 0));
            var tip = Component.text("Use /prt create <name> or /prt join <name>")
                    .color(TextColor.color(255, 215, 0));
            bukkitPlayer.sendMessage(welcome.append(Component.newline()).append(tip));
        }

        // Always update/show the boss bar for the player's current location
        this.plugin.getBossBarFactory().run(bukkitPlayer, chunkX, chunkZ);
    }
}