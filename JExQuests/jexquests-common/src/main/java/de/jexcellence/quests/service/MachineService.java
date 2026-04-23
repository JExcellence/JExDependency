package de.jexcellence.quests.service;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.database.entity.Machine;
import de.jexcellence.quests.database.repository.MachineRepository;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Machine placement / dismantle / lookup. Storage, upgrades, and
 * trust lists are opaque JSON blobs the runtime parses on demand.
 */
public class MachineService {

    private final MachineRepository repo;
    private final JExLogger logger;

    public MachineService(@NotNull MachineRepository repo, @NotNull JExLogger logger) {
        this.repo = repo;
        this.logger = logger;
    }

    /**
     * Places a new machine at the given location.
     */
    public @NotNull CompletableFuture<Machine> placeAsync(
            @NotNull UUID ownerUuid,
            @NotNull String machineType,
            @NotNull Location location,
            @NotNull String facing
    ) {
        final Machine machine = new Machine(
                ownerUuid,
                machineType,
                location.getWorld() != null ? location.getWorld().getName() : "world",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                facing
        );
        machine.setLastActiveAt(LocalDateTime.now());
        return CompletableFuture.supplyAsync(() -> this.repo.create(machine)).exceptionally(ex -> {
            this.logger.error("machine place failed: {}", ex.getMessage());
            return null;
        });
    }

    /** Marks a machine dismantled rather than deleting the row — preserves audit history. */
    public @NotNull CompletableFuture<Boolean> dismantleAsync(@NotNull Machine machine) {
        return CompletableFuture.supplyAsync(() -> {
            machine.setDismantled(true);
            machine.setLastActiveAt(LocalDateTime.now());
            this.repo.update(machine);
            return true;
        }).exceptionally(ex -> {
            this.logger.error("machine dismantle failed: {}", ex.getMessage());
            return false;
        });
    }

    /**
     * Finds a machine at the given location.
     */
    public @NotNull CompletableFuture<Optional<Machine>> findAtAsync(@NotNull Location location) {
        final String world = location.getWorld() != null ? location.getWorld().getName() : "world";
        return this.repo.findByLocationAsync(world, location.getBlockX(), location.getBlockY(), location.getBlockZ())
                .thenApply(list -> list == null ? Optional.<Machine>empty() : list);
    }

    /** Direct primary-key lookup — used by the storage-close listener. */
    public @NotNull CompletableFuture<Optional<Machine>> findByIdAsync(long machineId) {
        return this.repo.findByIdAsync(machineId).exceptionally(ex -> {
            this.logger.error("machine findById failed: {}", ex.getMessage());
            return Optional.empty();
        });
    }

    /**
     * Finds machines owned by the given player.
     */
    public @NotNull CompletableFuture<List<Machine>> ownedByAsync(@NotNull UUID ownerUuid) {
        return this.repo.findByOwnerAsync(ownerUuid).exceptionally(ex -> {
            this.logger.error("machine owned query failed: {}", ex.getMessage());
            return List.of();
        });
    }

    /**
     * Finds machines of the given type.
     */
    public @NotNull CompletableFuture<List<Machine>> ofTypeAsync(@NotNull String machineType) {
        return this.repo.findByTypeAsync(machineType).exceptionally(ex -> {
            this.logger.error("machine type query failed: {}", ex.getMessage());
            return List.of();
        });
    }

    /** Updates the storage blob (service-layer responsibility for serde). */
    public @NotNull CompletableFuture<Machine> writeStorageAsync(@NotNull Machine machine, @Nullable String storageJson) {
        return CompletableFuture.supplyAsync(() -> {
            machine.setStorageData(storageJson);
            machine.setLastActiveAt(LocalDateTime.now());
            return this.repo.update(machine);
        }).exceptionally(ex -> {
            this.logger.error("machine write failed: {}", ex.getMessage());
            return machine;
        });
    }
}
