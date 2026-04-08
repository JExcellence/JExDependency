package de.jexcellence.oneblock.database.entity.oneblock;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "oneblock_island_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneblockIslandMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "island_id", referencedColumnName = "id", nullable = false)
    private OneblockIsland island;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", referencedColumnName = "id", nullable = false)
    private OneblockPlayer player;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MemberRole role;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id", referencedColumnName = "id", nullable = false)
    private OneblockPlayer invitedBy;

    public OneblockIslandMember(@NotNull OneblockIsland island, @NotNull OneblockPlayer player, 
                               @NotNull MemberRole role, @NotNull OneblockPlayer invitedBy) {
        this.island = island;
        this.player = player;
        this.role = role;
        this.invitedBy = invitedBy;
        this.invitedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public void acceptInvitation() {
        this.joinedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public void leave() {
        this.isActive = false;
    }

    public boolean hasJoined() {
        return this.joinedAt != null;
    }

    public boolean canManageMembers() {
        return this.role == MemberRole.MODERATOR || this.role == MemberRole.CO_OWNER;
    }

    public boolean canBuildAndDestroy() {
        return this.role != MemberRole.VISITOR;
    }

    public boolean canManageSettings() {
        return this.role == MemberRole.CO_OWNER;
    }

    /**
     * Convenience method to get the player's UUID.
     * @return the UUID of the player
     */
    @NotNull
    public java.util.UUID getPlayerUuid() {
        return this.player.getUuid();
    }

    /**
     * Gets the last activity timestamp (using joinedAt as proxy)
     * @return the last activity timestamp
     */
    @NotNull
    public LocalDateTime getLastActivity() {
        return this.joinedAt != null ? this.joinedAt : this.invitedAt;
    }

    @Getter
    public enum MemberRole {
        VISITOR("Visitor"),
        MEMBER("Member"),
        TRUSTED("Trusted"),
        MODERATOR("Moderator"),
        CO_OWNER("Co-Owner");

        private final String displayName;

        MemberRole(String displayName) {
            this.displayName = displayName;
        }

        public boolean hasManagementPermissions() {
            return this == MODERATOR || this == CO_OWNER;
        }
    }
}