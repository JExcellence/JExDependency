package de.jexcellence.oneblock.database.entity.oneblock;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "oneblock_island_ban")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneblockIslandBan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "island_id", referencedColumnName = "id", nullable = false)
    private OneblockIsland island;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_player_id", referencedColumnName = "id", nullable = false)
    private OneblockPlayer bannedPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_id", referencedColumnName = "id", nullable = false)
    private OneblockPlayer bannedBy;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "banned_at", nullable = false)
    private LocalDateTime bannedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "unbanned_at")
    private LocalDateTime unbannedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unbanned_by_id", referencedColumnName = "id")
    private OneblockPlayer unbannedBy;

    public OneblockIslandBan(@NotNull OneblockIsland island, @NotNull OneblockPlayer bannedPlayer,
                            @NotNull OneblockPlayer bannedBy, @NotNull String reason) {
        this.island = island;
        this.bannedPlayer = bannedPlayer;
        this.bannedBy = bannedBy;
        this.reason = reason;
        this.bannedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public OneblockIslandBan(@NotNull OneblockIsland island, @NotNull OneblockPlayer bannedPlayer,
                            @NotNull OneblockPlayer bannedBy, @NotNull String reason, 
                            @NotNull LocalDateTime expiresAt) {
        this(island, bannedPlayer, bannedBy, reason);
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isPermanent() {
        return this.expiresAt == null;
    }

    public void unban(@NotNull OneblockPlayer unbannedBy) {
        this.isActive = false;
        this.unbannedAt = LocalDateTime.now();
        this.unbannedBy = unbannedBy;
    }

    public boolean isCurrentlyBanned() {
        return this.isActive && !isExpired();
    }

    /**
     * Convenience method to get the banned player's UUID.
     * @return the UUID of the banned player
     */
    @NotNull
    public java.util.UUID getBannedPlayerUuid() {
        return this.bannedPlayer.getUuid();
    }
}