package de.jexcellence.oneblock.database.entity.oneblock;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modern OneblockPlayer entity with full Lombok optimization and enhanced functionality
 * Replaces JEPlayer with cleaner structure and evolution-based naming
 */
@Entity
@Table(name = "oneblock_players", 
    indexes = {
        @Index(name = "idx_oneblock_player_uuid", columnList = "unique_id"),
        @Index(name = "idx_oneblock_player_name", columnList = "player_name"),
        @Index(name = "idx_oneblock_player_last_seen", columnList = "last_seen"),
        @Index(name = "idx_oneblock_player_active", columnList = "is_active")
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "uniqueId", callSuper = false)
public class OneblockPlayer extends BaseEntity {
    
    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;
    
    @Column(name = "player_name", nullable = false, length = 16)
    private String playerName;
    
    @Column(name = "first_joined")
    @Builder.Default
    private LocalDateTime firstJoined = LocalDateTime.now();
    
    @Column(name = "last_seen")
    @Builder.Default
    private LocalDateTime lastSeen = LocalDateTime.now();
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
    
    @Column(name = "total_playtime_minutes", nullable = false)
    @Builder.Default
    private long totalPlaytimeMinutes = 0L;
    
    @OneToOne(mappedBy = "owner", cascade = CascadeType.ALL)
    private OneblockIsland ownedIsland;
    
    /**
     * Constructor for creating from Bukkit Player
     * @param player the Bukkit player instance
     */
    public OneblockPlayer(@NotNull Player player) {
        this.uniqueId = player.getUniqueId();
        this.playerName = player.getName();
        this.firstJoined = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.isActive = true;
        this.totalPlaytimeMinutes = 0L;
    }
    
    /**
     * Constructor for creating with UUID and name
     * @param uniqueId the player UUID
     * @param playerName the player name
     */
    public OneblockPlayer(@NotNull UUID uniqueId, @NotNull String playerName) {
        this.uniqueId = uniqueId;
        this.playerName = playerName;
        this.firstJoined = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.isActive = true;
        this.totalPlaytimeMinutes = 0L;
    }
    
    /**
     * Updates the player name (useful for name changes)
     * @param newName the new player name
     */
    public void updatePlayerName(@NotNull String newName) {
        this.playerName = newName;
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Updates the last seen timestamp
     */
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Adds playtime to the total
     * @param minutes the minutes to add
     */
    public void addPlaytime(long minutes) {
        this.totalPlaytimeMinutes += minutes;
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Checks if this player matches a Bukkit player
     * @param player the Bukkit player to check
     * @return true if UUIDs match
     */
    public boolean matches(@NotNull Player player) {
        return this.uniqueId.equals(player.getUniqueId());
    }
    
    /**
     * Checks if the player has an owned island
     * @return true if player owns an island
     */
    public boolean hasIsland() {
        return this.ownedIsland != null;
    }
    
    /**
     * Marks the player as inactive
     */
    public void markInactive() {
        this.isActive = false;
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Marks the player as active
     */
    public void markActive() {
        this.isActive = true;
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Gets the total playtime in hours
     * @return playtime in hours
     */
    public double getPlaytimeHours() {
        return totalPlaytimeMinutes / 60.0;
    }
    
    /**
     * Gets the total playtime in days
     * @return playtime in days
     */
    public double getPlaytimeDays() {
        return totalPlaytimeMinutes / (60.0 * 24.0);
    }
    
    /**
     * Checks if the player is considered new (less than 1 hour playtime)
     * @return true if player is new
     */
    public boolean isNewPlayer() {
        return totalPlaytimeMinutes < 60;
    }
    
    /**
     * Checks if the player has been seen recently (within last 7 days)
     * @return true if player was seen recently
     */
    public boolean isRecentlyActive() {
        return lastSeen != null && lastSeen.isAfter(LocalDateTime.now().minusDays(7));
    }

    /**
     * Convenience method to get UUID (alias for getUniqueId)
     * @return the player's UUID
     */
    @NotNull
    public UUID getUuid() {
        return this.uniqueId;
    }
    
    @Override
    public String toString() {
        return "OneblockPlayer{" +
                "uniqueId=" + uniqueId +
                ", playerName='" + playerName + '\'' +
                ", isActive=" + isActive +
                ", hasIsland=" + hasIsland() +
                ", playtimeHours=" + String.format("%.1f", getPlaytimeHours()) +
                '}';
    }
}