package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A world-placed machine instance. The legacy RDQ had four separate
 * tables (machine, machine_storage, machine_trust, machine_upgrade) —
 * merged here to a single row because none of those satellite tables
 * ever had a relational query against them. Inventory, upgrades, and
 * trust list live as JSON blobs; the runtime parses on demand.
 *
 * <p>Location stored as a denormalised {@code world + x + y + z + facing}
 * tuple for cheap spatial queries.
 */
@Entity
@Table(
        name = "jexquests_machine",
        indexes = {
                @Index(name = "idx_jexquests_machine_owner", columnList = "owner_uuid"),
                @Index(name = "idx_jexquests_machine_type", columnList = "machine_type"),
                @Index(name = "idx_jexquests_machine_world", columnList = "world")
        }
)
public class Machine extends LongIdEntity {

    @Column(name = "owner_uuid", nullable = false)
    private UUID ownerUuid;

    @Column(name = "machine_type", nullable = false, length = 64)
    private String machineType;

    @Column(name = "world", nullable = false, length = 64)
    private String world;

    @Column(name = "x", nullable = false)
    private int x;

    @Column(name = "y", nullable = false)
    private int y;

    @Column(name = "z", nullable = false)
    private int z;

    @Column(name = "facing", nullable = false, length = 16)
    private String facing;

    @Lob
    @Column(name = "storage_data")
    private String storageData;

    @Lob
    @Column(name = "upgrade_data")
    private String upgradeData;

    @Lob
    @Column(name = "trusted_uuids")
    private String trustedUuids;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "dismantled", nullable = false)
    private boolean dismantled;

    protected Machine() {
    }

    public Machine(
            @NotNull UUID ownerUuid,
            @NotNull String machineType,
            @NotNull String world,
            int x, int y, int z,
            @NotNull String facing
    ) {
        this.ownerUuid = ownerUuid;
        this.machineType = machineType;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
    }

    public @NotNull UUID getOwnerUuid() { return this.ownerUuid; }
    public @NotNull String getMachineType() { return this.machineType; }
    public @NotNull String getWorld() { return this.world; }
    public int getX() { return this.x; }
    public int getY() { return this.y; }
    public int getZ() { return this.z; }
    public @NotNull String getFacing() { return this.facing; }
    public void setFacing(@NotNull String facing) { this.facing = facing; }
    public @Nullable String getStorageData() { return this.storageData; }
    public void setStorageData(@Nullable String storageData) { this.storageData = storageData; }
    public @Nullable String getUpgradeData() { return this.upgradeData; }
    public void setUpgradeData(@Nullable String upgradeData) { this.upgradeData = upgradeData; }
    public @Nullable String getTrustedUuids() { return this.trustedUuids; }
    public void setTrustedUuids(@Nullable String trustedUuids) { this.trustedUuids = trustedUuids; }
    public @Nullable LocalDateTime getLastActiveAt() { return this.lastActiveAt; }
    public void setLastActiveAt(@Nullable LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    public boolean isDismantled() { return this.dismantled; }
    public void setDismantled(boolean dismantled) { this.dismantled = dismantled; }

    @Override
    public String toString() {
        return "Machine[" + this.machineType + " @ " + this.world + "(" + this.x + "," + this.y + "," + this.z + ") owner=" + this.ownerUuid + "]";
    }
}
