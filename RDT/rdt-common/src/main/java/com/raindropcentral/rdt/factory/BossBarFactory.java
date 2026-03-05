package com.raindropcentral.rdt.factory;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.utils.CordMessage;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory responsible for creating and updating the per-player BossBar
 * that displays town and chunk information.
 * <p>
 * Behavior:
 * <ul>
 *   <li>When a player is inside a claimed chunk, the bar shows the town name
 *   and the current chunk coordinates with a purple progress bar that reflects
 *   the percentage of used claims versus the configured claim limit.</li>
 *   <li>When a player is in an unclaimed chunk, the bar shows "Unincorporated"
 *   with a yellow, full progress bar.</li>
 * </ul>
 *
 * Implementation notes:
 * <ul>
 *   <li>Bars are cached per player and updated in-place to avoid flicker and
 *   unnecessary object churn.</li>
 *   <li>Title and structural changes (coordinates, town name, color) trigger a
 *   full update; otherwise only the progress is adjusted.</li>
 * </ul>
 */
@SuppressWarnings("StringTemplateMigration")
public class BossBarFactory {

    private final RDT plugin;

    /** Cache of last known boss bars and their coordinates per player. */
    private final ConcurrentHashMap<UUID, CordMessage> messages = new ConcurrentHashMap<>();

    /**
     * Create a new factory bound to the given plugin instance.
     *
     * @param plugin RDT plugin instance (required)
     */
    public BossBarFactory(@NonNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Compute and present the correct boss bar for the given player and destination chunk.
     * If the chunk belongs to the player's town, the bar will reflect claim usage progress;
     * otherwise it will show an "Unincorporated" bar.
     * Note: This method is designed to be lightweight and is intentionally quiet in logs
     * to avoid spam on frequent movement updates.
     *
     * @param player   player to show the bar to (non-null)
     * @param toChunkX target chunk X coordinate
     * @param toChunkZ target chunk Z coordinate
     */
    public void run(@NonNull Player player, int toChunkX, int toChunkZ) {
        RDTPlayer rPlayer = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (rPlayer != null && rPlayer.getTownUUID() != null) {
            RTown town = this.plugin.getTownRepository().findByTownUUID(rPlayer.getTownUUID());
            if (town != null) {
                for (RChunk chunk : town.getChunks()) {
                    if (toChunkX == chunk.getX_loc() && toChunkZ == chunk.getZ_loc()) {
                        double percent = (double) town.getChunks().size() / (double) this.plugin.getDefaultConfig().getClaimLimit();
                        float progress = (float) Math.max(0.0, Math.min(1.0, percent));
                        updateBar(player, toChunkX, toChunkZ, town.getTownName(), progress);
                        return;
                    }
                }
            }
        }
        updateBar(player, toChunkX, toChunkZ, "Unincorporated", 0);
    }

    /**
     * Create or update a player's boss bar with the given state.
     *
     * @param player    player to show the bar to (non-null)
     * @param x         chunk X coordinate
     * @param z         chunk Z coordinate
     * @param townName  town display name (or "Unincorporated")
     * @param progress  progress value in range [0, 1]; ignored for Unincorporated
     */
    private void updateBar(@NonNull Player player, int x, int z, String townName, float progress) {
        boolean unincorporated = townName.equalsIgnoreCase("Unincorporated");
        String title = townName + " - (" + x + ", " + z + ")";
        BossBar.Color color = unincorporated ? BossBar.Color.YELLOW : BossBar.Color.PURPLE;
        float barProgress = unincorporated ? 1.0f : progress;
        updateBarCustom(player, x, z, title, color, barProgress);
    }

    /**
     * Create or update the boss bar with fully specified parameters.
     * Avoids any string templates or formatting features that require preview flags.
     */
    private void updateBarCustom(@NonNull Player player, int x, int z, String title, BossBar.Color color, float barProgress) {
        messages.compute(player.getUniqueId(), (uuid, message) -> {
            // Create a new boss bar if none exists
            if (message == null) {
                BossBar bossBar = BossBar.bossBar(
                        Component.text(title),
                        barProgress,
                        color,
                        BossBar.Overlay.PROGRESS
                );
                player.showBossBar(bossBar);
                return new CordMessage(uuid, x, z, bossBar);
            }

            BossBar bossBar = message.bossBar();

            // Check if nothing structural changed
            String currentTitle = PlainTextComponentSerializer.plainText().serialize(bossBar.name());
            if (message.x() == x && message.z() == z && currentTitle.equals(title)) {
                bossBar.progress(barProgress);
                player.showBossBar(bossBar);
                return message;
            }

            // Update the existing boss bar
            bossBar.name(Component.text(title));
            bossBar.color(color);
            bossBar.progress(barProgress);
            player.showBossBar(bossBar);
            return new CordMessage(uuid, x, z, bossBar);
        });
    }
}