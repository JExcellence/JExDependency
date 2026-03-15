package com.raindropcentral.rdt.factory;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;

import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

/**
 * Builds and updates player town-status boss bars.
 *
 * <p>The bar communicates whether a player currently stands inside one of their own town chunks
 * or in an unincorporated area. Text is localized through i18n keys.</p>
 *
 * @author RaindropCentral
 * @since 1.0.0
 * @version 1.0.1
 */
@SuppressWarnings("StringTemplateMigration")
public class BossBarFactory {

    private final RDT plugin;
    private final ConcurrentHashMap<UUID, BossBarState> states = new ConcurrentHashMap<>();

    /**
     * Creates a boss bar factory for an RDT runtime.
     *
     * @param plugin active runtime
     */
    public BossBarFactory(final @NonNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Updates the player boss bar for the given chunk position.
     *
     * @param player target player
     * @param chunkX current chunk x
     * @param chunkZ current chunk z
     */
    public void run(final @NonNull Player player, final int chunkX, final int chunkZ) {
        final TownAreaState areaState = this.resolveTownAreaState(player, chunkX, chunkZ);
        final Component title = this.buildTitle(player, areaState, chunkX, chunkZ);
        final BossBar.Color color = areaState.color();
        final float progress = areaState.progress();

        this.states.compute(player.getUniqueId(), (playerId, currentState) -> {
            if (currentState == null) {
                final BossBar createdBar = BossBar.bossBar(title, progress, color, BossBar.Overlay.PROGRESS);
                player.showBossBar(createdBar);
                return new BossBarState(chunkX, chunkZ, areaState.inTownArea(), areaState.townName(), createdBar);
            }

            final boolean unchanged = currentState.chunkX() == chunkX
                    && currentState.chunkZ() == chunkZ
                    && currentState.inTownArea() == areaState.inTownArea()
                    && Objects.equals(currentState.townName(), areaState.townName());
            if (unchanged) {
                currentState.bossBar().name(title);
                currentState.bossBar().color(color);
                currentState.bossBar().progress(progress);
                player.showBossBar(currentState.bossBar());
                return currentState;
            }

            currentState.bossBar().name(title);
            currentState.bossBar().color(color);
            currentState.bossBar().progress(progress);
            player.showBossBar(currentState.bossBar());
            return new BossBarState(chunkX, chunkZ, areaState.inTownArea(), areaState.townName(), currentState.bossBar());
        });
    }

    /**
     * Hides and removes the active boss bar state for a player.
     *
     * @param player player whose boss bar should be removed
     */
    public void clear(final @NotNull Player player) {
        final BossBarState removedState = this.states.remove(player.getUniqueId());
        if (removedState == null) {
            return;
        }
        player.hideBossBar(removedState.bossBar());
    }

    private @NotNull Component buildTitle(
            final @NotNull Player player,
            final @NotNull TownAreaState areaState,
            final int chunkX,
            final int chunkZ
    ) {
        final String key = areaState.inTownArea()
                ? "boss_bar.title.town"
                : "boss_bar.title.unincorporated";
        return new I18n.Builder(key, player)
                .withPlaceholders(Map.of(
                        "town_name", areaState.townName() == null ? "" : areaState.townName(),
                        "chunk_x", chunkX,
                        "chunk_z", chunkZ
                ))
                .build()
                .component();
    }

    private @NotNull TownAreaState resolveTownAreaState(
            final @NotNull Player player,
            final int chunkX,
            final int chunkZ
    ) {
        final RRDTPlayer playerRepository = this.plugin.getPlayerRepository();
        final RRTown townRepository = this.plugin.getTownRepository();
        if (playerRepository == null || townRepository == null) {
            return TownAreaState.unincorporated();
        }

        final RDTPlayer rdtPlayer = playerRepository.findByPlayer(player.getUniqueId());
        if (rdtPlayer == null || rdtPlayer.getTownUUID() == null) {
            return TownAreaState.unincorporated();
        }

        final RTown town = townRepository.findByTownUUID(rdtPlayer.getTownUUID());
        if (town == null) {
            return TownAreaState.unincorporated();
        }

        if (this.isInsideTownArea(town, chunkX, chunkZ)) {
            return TownAreaState.town(
                    town.getTownName(),
                    this.computeTownProgress(town)
            );
        }

        return TownAreaState.unincorporated();
    }

    private boolean isInsideTownArea(
            final @NotNull RTown town,
            final int chunkX,
            final int chunkZ
    ) {
        for (final RChunk chunk : town.getChunks()) {
            if (chunk.getX_loc() == chunkX && chunk.getZ_loc() == chunkZ) {
                return true;
            }
        }

        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            return false;
        }

        return nexusLocation.getChunk().getX() == chunkX && nexusLocation.getChunk().getZ() == chunkZ;
    }

    private float computeTownProgress(final @NotNull RTown town) {
        final int maxChunkLimit = Math.max(1, this.plugin.getDefaultConfig().getGlobalMaxChunkLimit());
        final int claimedChunks = Math.max(0, town.getChunks().size());
        final double ratio = (double) claimedChunks / (double) maxChunkLimit;
        return (float) Math.max(0.0D, Math.min(1.0D, ratio));
    }

    private record BossBarState(
            int chunkX,
            int chunkZ,
            boolean inTownArea,
            @Nullable String townName,
            @NotNull BossBar bossBar
    ) {}

    private record TownAreaState(
            boolean inTownArea,
            @Nullable String townName,
            @NotNull BossBar.Color color,
            float progress
    ) {
        private static @NotNull TownAreaState town(
                final @Nullable String townName,
                final float progress
        ) {
            return new TownAreaState(
                    true,
                    townName == null ? "" : townName,
                    BossBar.Color.PURPLE,
                    progress
            );
        }

        private static @NotNull TownAreaState unincorporated() {
            return new TownAreaState(false, null, BossBar.Color.YELLOW, 1.0F);
        }
    }
}
