/*
 * StorageViewLauncher.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.repository.RRStorage;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Shared launcher for lease-aware storage view opens.
 *
 * <p>This helper centralizes storage lease acquisition, delayed view transitions, and user feedback
 * so commands and inventory buttons open storages through the same cross-server-safe path.</p>
 *
 * @author RaindropCentral
 * @since 5.0.0
 * @version 5.0.0
 */
public final class StorageViewLauncher {

    private StorageViewLauncher() {}

    /**
     * Attempts to acquire a lease for the supplied storage and opens {@link StorageView} when the
     * lease succeeds.
     *
     * @param player player requesting access to the storage
     * @param plugin active plugin instance
     * @param storage storage to open
     * @throws NullPointerException if any argument is {@code null}
     */
    public static void openStorage(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage
    ) {
        if (!storage.canAccess(player.getUniqueId())) {
            sendAccessDeniedMessage(player, storage);
            return;
        }

        final RRStorage storageRepository = plugin.getStorageRepository();
        final Long storageId = storage.getId();
        if (storageRepository == null || storageId == null) {
            sendUnavailableMessage(player, storage);
            return;
        }

        final UUID leaseToken = UUID.randomUUID();
        storageRepository.tryAcquireLeaseAsync(
                storageId,
                plugin.getServerUuid(),
                player.getUniqueId(),
                leaseToken,
                StorageLeasePolicy.nextExpiry()
            )
            .thenAccept(result -> plugin.getScheduler().runSync(() -> handleLeaseAcquireResult(
                player,
                plugin,
                storage,
                storageId,
                leaseToken,
                result
            )))
            .exceptionally(throwable -> {
                plugin.getLogger().warning(
                    "Failed to acquire storage lease for " + storage.getStorageKey() + ": " + throwable.getMessage()
                );
                plugin.getScheduler().runSync(() -> sendUnavailableMessage(player, storage));
                return null;
            });
    }

    private static void handleLeaseAcquireResult(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage,
        final @NotNull Long storageId,
        final @NotNull UUID leaseToken,
        final @NotNull RRStorage.LeaseAcquireResult result
    ) {
        switch (result) {
            case ACQUIRED -> openStorageView(player, plugin, storage, storageId, leaseToken);
            case LOCKED -> sendLockedMessage(player, storage);
            case MISSING -> sendUnavailableMessage(player, storage);
        }
    }

    private static void openStorageView(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage,
        final @NotNull Long storageId,
        final @NotNull UUID leaseToken
    ) {
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("plugin", plugin);
        initialData.put("storage_id", storageId);
        initialData.put("storage_key", storage.getStorageKey());
        initialData.put("storage_size", storage.getInventorySize());
        initialData.put("storage_inventory", storage.getInventory());
        initialData.put("lease_token", leaseToken);
        initialData.put("storage_can_deposit", storage.canDeposit(player.getUniqueId()));
        initialData.put("storage_can_withdraw", storage.canWithdraw(player.getUniqueId()));

        player.closeInventory();
        plugin.getScheduler().runDelayed(() -> {
            if (!player.isOnline()) {
                releaseLease(plugin, storageId, player.getUniqueId(), leaseToken);
                return;
            }

            try {
                plugin.getViewFrame().open(StorageView.class, player, initialData);
            } catch (Exception exception) {
                plugin.getLogger().warning(
                    "Failed to open storage view for " + storage.getStorageKey() + ": " + exception.getMessage()
                );
                releaseLease(plugin, storageId, player.getUniqueId(), leaseToken);
                sendUnavailableMessage(player, storage);
            }
        }, 1L);
    }

    private static void sendLockedMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        new I18n.Builder("storage.message.locked", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();
    }

    private static void sendUnavailableMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        new I18n.Builder("storage.message.unavailable", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();
    }

    private static void sendAccessDeniedMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        new I18n.Builder("storage.message.access_denied", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();
    }

    private static void releaseLease(
        final @NotNull RDR plugin,
        final @NotNull Long storageId,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken
    ) {
        final RRStorage storageRepository = plugin.getStorageRepository();
        if (storageRepository == null) {
            return;
        }

        storageRepository.releaseLeaseAsync(
            storageId,
            plugin.getServerUuid(),
            playerUuid,
            leaseToken
        );
    }
}
