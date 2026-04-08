package de.jexcellence.oneblock.database.entity.region;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a permission granted to a player within an island region.
 * Permissions control what actions players can perform in specific regions.
 */
@Entity
@Table(name = "oneblock_region_permissions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"region_id", "player_id", "permission_type"}))
public class RegionPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private IslandRegion region;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "permission_type", nullable = false, length = 100)
    private String permissionType;

    @Column(name = "granted", nullable = false)
    private boolean granted = true;

    @Column(name = "granted_by", nullable = true)
    private UUID grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expires_at", nullable = true)
    private LocalDateTime expiresAt;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "is_temporary", nullable = false)
    private boolean temporary = false;

    // Constructors
    public RegionPermission() {
        this.grantedAt = LocalDateTime.now();
    }

    public RegionPermission(@NotNull IslandRegion region, @NotNull UUID playerId, 
                           @NotNull String permissionType, boolean granted) {
        this();
        this.region = region;
        this.playerId = playerId;
        this.permissionType = permissionType;
        this.granted = granted;
    }

    public RegionPermission(@NotNull IslandRegion region, @NotNull UUID playerId, 
                           @NotNull String permissionType, boolean granted, @Nullable UUID grantedBy) {
        this(region, playerId, permissionType, granted);
        this.grantedBy = grantedBy;
    }

    // Utility methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return granted && !isExpired();
    }

    public void setTemporary(@NotNull LocalDateTime expiresAt) {
        this.temporary = true;
        this.expiresAt = expiresAt;
    }

    public void makePermanent() {
        this.temporary = false;
        this.expiresAt = null;
    }

    public void revoke(@Nullable UUID revokedBy, @Nullable String reason) {
        this.granted = false;
        this.grantedBy = revokedBy;
        this.reason = reason;
        this.grantedAt = LocalDateTime.now();
    }

    public void grant(@Nullable UUID grantedBy, @Nullable String reason) {
        this.granted = true;
        this.grantedBy = grantedBy;
        this.reason = reason;
        this.grantedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IslandRegion getRegion() {
        return region;
    }

    public void setRegion(IslandRegion region) {
        this.region = region;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionPermission that = (RegionPermission) o;
        return Objects.equals(region, that.region) &&
               Objects.equals(playerId, that.playerId) &&
               Objects.equals(permissionType, that.permissionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, playerId, permissionType);
    }

    @Override
    public String toString() {
        return "RegionPermission{" +
                "id=" + id +
                ", playerId=" + playerId +
                ", permissionType='" + permissionType + '\'' +
                ", granted=" + granted +
                ", grantedBy=" + grantedBy +
                ", grantedAt=" + grantedAt +
                ", expiresAt=" + expiresAt +
                ", temporary=" + temporary +
                '}';
    }

    /**
     * Common permission types for island regions.
     */
    public static final class PermissionTypes {
        public static final String BUILD = "build";
        public static final String BREAK = "break";
        public static final String INTERACT = "interact";
        public static final String CHEST_ACCESS = "chest_access";
        public static final String DOOR_ACCESS = "door_access";
        public static final String BUTTON_ACCESS = "button_access";
        public static final String LEVER_ACCESS = "lever_access";
        public static final String REDSTONE_ACCESS = "redstone_access";
        public static final String ANIMAL_INTERACT = "animal_interact";
        public static final String VILLAGER_TRADE = "villager_trade";
        public static final String ITEM_PICKUP = "item_pickup";
        public static final String ITEM_DROP = "item_drop";
        public static final String PVP = "pvp";
        public static final String TELEPORT = "teleport";
        public static final String INVITE_OTHERS = "invite_others";
        public static final String MANAGE_PERMISSIONS = "manage_permissions";
        public static final String ADMIN_ACCESS = "admin_access";

        private PermissionTypes() {
            // Utility class
        }
    }
}