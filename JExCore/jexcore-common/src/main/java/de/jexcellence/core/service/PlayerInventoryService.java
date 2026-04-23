package de.jexcellence.core.service;

import de.jexcellence.core.database.entity.CentralServer;
import de.jexcellence.core.database.entity.CorePlayer;
import de.jexcellence.core.database.entity.PlayerInventory;
import de.jexcellence.core.database.repository.PlayerInventoryRepository;
import de.jexcellence.core.serialize.PlayerInventorySerializer;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service facade over {@link PlayerInventoryRepository}. Inventory
 * serialization itself is the caller's responsibility — the service only
 * persists opaque string blobs.
 */
public class PlayerInventoryService {

    private final PlayerInventoryRepository repo;
    private final JExLogger logger;

    public PlayerInventoryService(@NotNull PlayerInventoryRepository repo, @NotNull JExLogger logger) {
        this.repo = repo;
        this.logger = logger;
    }

    public @NotNull CompletableFuture<Optional<PlayerInventory>> latest(@NotNull CorePlayer player, @NotNull CentralServer server) {
        return this.repo.findLatestByPlayerAndServerAsync(player, server).exceptionally(ex -> {
            this.logger.error("latest inventory lookup failed: {}", ex.getMessage());
            return Optional.empty();
        });
    }

    public @NotNull CompletableFuture<List<PlayerInventory>> history(@NotNull CorePlayer player, @NotNull CentralServer server) {
        return this.repo.findByPlayerAndServerAsync(player, server).exceptionally(ex -> {
            this.logger.error("history lookup failed: {}", ex.getMessage());
            return List.of();
        });
    }

    public @NotNull CompletableFuture<PlayerInventory> snapshot(
            @NotNull CorePlayer player,
            @NotNull CentralServer server,
            @NotNull String inventoryBlob,
            @NotNull String armorBlob,
            @NotNull String enderchestBlob
    ) {
        final PlayerInventory row = new PlayerInventory(player, server);
        row.setInventory(inventoryBlob);
        row.setArmor(armorBlob);
        row.setEnderchest(enderchestBlob);
        return this.repo.createAsync(row).exceptionally(ex -> {
            this.logger.error("snapshot save failed: {}", ex.getMessage());
            return null;
        });
    }

    /**
     * Live-player overload. Must be called on the main server thread —
     * {@link Player#getInventory()} and {@link Player#getEnderChest()} are
     * read synchronously, then the encode + DB write happens asynchronously.
     *
     * @param player the tracked player record
     * @param server the server scope
     * @param bukkitPlayer live Bukkit player
     * @return future resolving to the persisted row
     */
    public @NotNull CompletableFuture<PlayerInventory> snapshot(
            @NotNull CorePlayer player,
            @NotNull CentralServer server,
            @NotNull Player bukkitPlayer
    ) {
        final String inv = PlayerInventorySerializer.encode(bukkitPlayer.getInventory().getContents());
        final String armor = PlayerInventorySerializer.encode(bukkitPlayer.getInventory().getArmorContents());
        final String ender = PlayerInventorySerializer.encode(bukkitPlayer.getEnderChest().getContents());
        return snapshot(player, server, inv, armor, ender);
    }

    /**
     * Restores a snapshot onto a live player. Must be called on the main
     * server thread.
     *
     * @param snapshot stored snapshot
     * @param bukkitPlayer target player
     */
    public void restore(@NotNull PlayerInventory snapshot, @NotNull Player bukkitPlayer) {
        if (snapshot.getInventory() != null) {
            bukkitPlayer.getInventory().setContents(PlayerInventorySerializer.decode(snapshot.getInventory()));
        }
        if (snapshot.getArmor() != null) {
            bukkitPlayer.getInventory().setArmorContents(PlayerInventorySerializer.decode(snapshot.getArmor()));
        }
        if (snapshot.getEnderchest() != null) {
            bukkitPlayer.getEnderChest().setContents(PlayerInventorySerializer.decode(snapshot.getEnderchest()));
        }
    }
}
