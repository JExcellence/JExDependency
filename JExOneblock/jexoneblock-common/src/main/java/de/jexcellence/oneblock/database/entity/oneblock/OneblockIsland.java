package de.jexcellence.oneblock.database.entity.oneblock;

import com.raindropcentral.rplatform.database.converter.LocationConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Modern Island entity with embedded components and clean relationships
 * Replaces JEIsland with optimized structure and evolution-based naming
 */
@Entity
@Table(name = "oneblock_islands",
    indexes = {
        @Index(name = "idx_island_owner", columnList = "owner_id"),
        @Index(name = "idx_island_identifier", columnList = "identifier"),
        @Index(name = "idx_island_level", columnList = "level"),
        @Index(name = "idx_island_privacy", columnList = "privacy"),
        @Index(name = "idx_island_created", columnList = "created_at"),
        @Index(name = "idx_island_visited", columnList = "last_visited")
    }
)
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "Island.withOwner",
        attributeNodes = {
            @NamedAttributeNode("owner")
        }
    ),
    @NamedEntityGraph(
        name = "Island.withMembers",
        attributeNodes = {
            @NamedAttributeNode("owner"),
            @NamedAttributeNode("members"),
            @NamedAttributeNode("bannedPlayers")
        }
    ),
    @NamedEntityGraph(
        name = "Island.full",
        attributeNodes = {
            @NamedAttributeNode("owner"),
            @NamedAttributeNode("members"),
            @NamedAttributeNode("bannedPlayers"),
            @NamedAttributeNode("islandMembers"),
            @NamedAttributeNode("islandBans"),
            @NamedAttributeNode("visitorSettings")
        }
    )
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "identifier", callSuper = false)
public class OneblockIsland extends BaseEntity {
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private OneblockPlayer owner;
    
    @Column(name = "identifier", nullable = false, unique = true, length = 64)
    private String identifier;
    
    @Column(name = "island_name", nullable = false, length = 100)
    private String islandName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "current_size", nullable = false)
    @Builder.Default
    private int currentSize = 50;
    
    @Column(name = "maximum_size", nullable = false)
    @Builder.Default
    private int maximumSize = 200;
    
    @Column(name = "level", nullable = false)
    @Builder.Default
    private int level = 1;
    
    @Column(name = "experience", nullable = false)
    @Builder.Default
    private double experience = 0.0;
    
    @Column(name = "privacy", nullable = false)
    @Builder.Default
    private boolean privacy = false;
    
    @Column(name = "island_coins", nullable = false)
    @Builder.Default
    private long islandCoins = 0L;
    
    @Column(name = "center_location", nullable = false)
    @Convert(converter = LocationConverter.class)
    private Location centerLocation;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "last_visited", nullable = false)
    @Builder.Default
    private LocalDateTime lastVisited = LocalDateTime.now();
    
    @Embedded
    private IslandRegion region;
    
    @OneToOne(mappedBy = "island", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OneblockRegion legacyRegion;
    
    @Embedded
    private OneblockCore oneblock;
    
    // Modern relationship mappings with proper entity management
    @OneToMany(mappedBy = "island", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<OneblockIslandMember> islandMembers = new HashSet<>();
    
    @OneToMany(mappedBy = "island", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<OneblockIslandBan> islandBans = new HashSet<>();
    
    @OneToOne(mappedBy = "island", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OneblockVisitorSettings visitorSettings;
    
    // Legacy many-to-many relationships for backward compatibility
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "island_members_legacy",
        joinColumns = @JoinColumn(name = "island_id"),
        inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    @Builder.Default
    private Set<OneblockPlayer> members = new HashSet<>();
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "island_banned_legacy",
        joinColumns = @JoinColumn(name = "island_id"),
        inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    @Builder.Default
    private Set<OneblockPlayer> bannedPlayers = new HashSet<>();
    
    /**
     * Constructor for creating a new island
     * @param owner the island owner
     * @param identifier unique island identifier
     * @param centerLocation center location of the island
     */
    public OneblockIsland(@NotNull OneblockPlayer owner, @NotNull String identifier, @NotNull Location centerLocation) {
        this.owner = owner;
        this.identifier = identifier;
        this.islandName = owner.getPlayerName() + "'s Island";
        this.description = "Welcome to " + owner.getPlayerName() + "'s island!";
        this.centerLocation = centerLocation.clone();
        this.currentSize = 50;
        this.maximumSize = 200;
        this.level = 1;
        this.experience = 0.0;
        this.privacy = false;
        this.islandCoins = 0L;
        this.createdAt = LocalDateTime.now();
        this.lastVisited = LocalDateTime.now();
        this.islandMembers = new HashSet<>();
        this.islandBans = new HashSet<>();
        this.members = new HashSet<>();
        this.bannedPlayers = new HashSet<>();
        
        // Initialize embedded components
        initializeEmbeddedComponents();
    }
    
    /**
     * Initializes embedded components with proper default values
     */
    public void initializeEmbeddedComponents() {
        if (centerLocation != null) {
            // Initialize region
            this.region = IslandRegion.builder()
                .minX(centerLocation.getBlockX() - currentSize / 2)
                .minY(centerLocation.getBlockY() - 64)
                .minZ(centerLocation.getBlockZ() - currentSize / 2)
                .maxX(centerLocation.getBlockX() + currentSize / 2)
                .maxY(centerLocation.getBlockY() + 320)
                .maxZ(centerLocation.getBlockZ() + currentSize / 2)
                .worldName(centerLocation.getWorld().getName())
                .spawnLocation(centerLocation.clone())
                .build();
            
            // Initialize oneblock core
            this.oneblock = OneblockCore.builder()
                .oneblockLocation(centerLocation.clone())
                .currentEvolution("Genesis") // Default evolution
                .evolutionLevel(1)
                .evolutionExperience(0.0)
                .totalBlocksBroken(0L)
                .prestigeLevel(0)
                .prestigePoints(0L)
                .build();
        }
    }
    
    // =====================================================
    // MEMBER MANAGEMENT METHODS (Modern Entity-based)
    // =====================================================
    
    /**
     * Adds a member using modern entity management
     * @param player the player to add
     * @return true if added successfully
     */
    public boolean addMemberEntity(@NotNull OneblockPlayer player) {
        if (isOwner(player) || isBannedEntity(player)) {
            return false;
        }
        
        OneblockIslandMember member = OneblockIslandMember.builder()
            .island(this)
            .player(player)
            .joinedAt(LocalDateTime.now())
            .isActive(true)
            .build();
        
        return islandMembers.add(member);
    }
    
    /**
     * Removes a member using modern entity management
     * @param player the player to remove
     * @return true if removed successfully
     */
    public boolean removeMemberEntity(@NotNull OneblockPlayer player) {
        return islandMembers.removeIf(member -> 
            member.getPlayer().equals(player));
    }
    
    /**
     * Checks if a player is a member using modern entity management
     * @param player the player to check
     * @return true if player is an active member
     */
    public boolean isMemberEntity(@NotNull OneblockPlayer player) {
        return islandMembers.stream()
            .anyMatch(member -> member.getPlayer().equals(player) && member.isActive());
    }
    
    /**
     * Bans a player using modern entity management
     * @param player the player to ban
     * @param reason the ban reason
     * @return true if banned successfully
     */
    public boolean banPlayerEntity(@NotNull OneblockPlayer player, String reason) {
        if (isOwner(player)) {
            return false; // Cannot ban owner
        }
        
        // Remove from members if present
        removeMemberEntity(player);
        
        OneblockIslandBan ban = OneblockIslandBan.builder()
            .island(this)
            .bannedPlayer(player)
            .bannedAt(LocalDateTime.now())
            .reason(reason)
            .isActive(true)
            .build();
        
        return islandBans.add(ban);
    }
    
    /**
     * Unbans a player using modern entity management
     * @param player the player to unban
     * @return true if unbanned successfully
     */
    public boolean unbanPlayerEntity(@NotNull OneblockPlayer player) {
        return islandBans.removeIf(ban -> 
            ban.getBannedPlayer().equals(player) && ban.isActive());
    }
    
    /**
     * Checks if a player is banned using modern entity management
     * @param player the player to check
     * @return true if player is actively banned
     */
    public boolean isBannedEntity(@NotNull OneblockPlayer player) {
        return islandBans.stream()
            .anyMatch(ban -> ban.getBannedPlayer().equals(player) && ban.isActive());
    }
    
    // =====================================================
    // LEGACY MEMBER MANAGEMENT METHODS (Backward Compatibility)
    // =====================================================
    
    /**
     * Checks if a player is a member (legacy method)
     * @param player the player to check
     * @return true if player is a member
     */
    public boolean isMember(@NotNull OneblockPlayer player) {
        // Check both modern and legacy systems
        return isMemberEntity(player) || members.contains(player);
    }
    
    /**
     * Checks if a player is banned (legacy method)
     * @param player the player to check
     * @return true if player is banned
     */
    public boolean isBanned(@NotNull OneblockPlayer player) {
        // Check both modern and legacy systems
        return isBannedEntity(player) || bannedPlayers.contains(player);
    }
    
    /**
     * Checks if a player is the owner
     * @param player the player to check
     * @return true if player is the owner
     */
    public boolean isOwner(@NotNull OneblockPlayer player) {
        return owner.equals(player);
    }
    
    /**
     * Checks if a player can access this island
     * @param player the player to check
     * @return true if player can access (owner, member, and not banned)
     */
    public boolean canAccess(@NotNull OneblockPlayer player) {
        return isOwner(player) || (isMember(player) && !isBanned(player));
    }
    
    /**
     * Adds a member (legacy method)
     * @param player the player to add
     * @return true if player was added successfully
     */
    public boolean addMember(@NotNull OneblockPlayer player) {
        if (isOwner(player) || isBanned(player)) {
            return false;
        }
        
        // Add to both systems for compatibility
        addMemberEntity(player);
        return members.add(player);
    }
    
    /**
     * Removes a member (legacy method)
     * @param player the player to remove
     * @return true if player was removed successfully
     */
    public boolean removeMember(@NotNull OneblockPlayer player) {
        // Remove from both systems
        removeMemberEntity(player);
        return members.remove(player);
    }
    
    /**
     * Bans a player (legacy method)
     * @param player the player to ban
     * @return true if player was banned successfully
     */
    public boolean banPlayer(@NotNull OneblockPlayer player) {
        if (isOwner(player)) {
            return false; // Cannot ban owner
        }
        
        // Remove from members and add to banned in both systems
        removeMember(player);
        banPlayerEntity(player, "Banned via legacy method");
        return bannedPlayers.add(player);
    }
    
    /**
     * Unbans a player (legacy method)
     * @param player the player to unban
     * @return true if player was unbanned successfully
     */
    public boolean unbanPlayer(@NotNull OneblockPlayer player) {
        // Remove from both systems
        unbanPlayerEntity(player);
        return bannedPlayers.remove(player);
    }
    
    // =====================================================
    // ISLAND MANAGEMENT METHODS
    // =====================================================
    
    /**
     * Updates the last visited timestamp
     */
    public void updateLastVisited() {
        this.lastVisited = LocalDateTime.now();
    }
    
    /**
     * Adds experience to the island
     * @param exp the experience to add
     */
    public void addExperience(double exp) {
        this.experience += exp;
        if (oneblock != null) {
            oneblock.addEvolutionExperience(exp);
        }
    }
    
    /**
     * Adds coins to the island
     * @param coins the coins to add
     */
    public void addCoins(long coins) {
        this.islandCoins += coins;
    }
    
    /**
     * Removes coins from the island
     * @param coins the coins to remove
     * @return true if successful, false if insufficient funds
     */
    public boolean removeCoins(long coins) {
        if (this.islandCoins >= coins) {
            this.islandCoins -= coins;
            return true;
        }
        return false;
    }
    
    /**
     * Checks if the island has enough coins
     * @param coins the amount to check
     * @return true if island has enough coins
     */
    public boolean hasCoins(long coins) {
        return this.islandCoins >= coins;
    }
    
    /**
     * Gets the total member count (excluding owner)
     * @return number of members
     */
    public int getMemberCount() {
        return Math.max(members.size(), (int) islandMembers.stream()
            .filter(OneblockIslandMember::isActive)
            .count());
    }
    
    /**
     * Gets the total banned player count
     * @return number of banned players
     */
    public int getBannedCount() {
        return Math.max(bannedPlayers.size(), (int) islandBans.stream()
            .filter(OneblockIslandBan::isActive)
            .count());
    }
    
    /**
     * Checks if the island is at maximum size
     * @return true if current size equals maximal size
     */
    public boolean isMaxSize() {
        return currentSize >= maximumSize;
    }
    
    /**
     * Expands the island size if possible
     * @param amount the amount to expand by
     * @return true if expansion was successful
     */
    public boolean expandSize(int amount) {
        if (currentSize + amount <= maximumSize) {
            currentSize += amount;
            if (region != null) {
                region.expand(amount / 2); // Expand region accordingly
            }
            return true;
        }
        return false;
    }
    
    /**
     * Checks if the island has an owner
     * @return true if island has an owner
     */
    public boolean hasOwner() {
        return owner != null;
    }
    
    /**
     * Gets the island's current evolution name
     * @return current evolution name or "Unknown" if not set
     */
    public String getCurrentEvolution() {
        return oneblock != null ? oneblock.getCurrentEvolution() : "Unknown";
    }
    
    /**
     * Gets the total blocks broken on this island
     * @return total blocks broken
     */
    public long getTotalBlocksBroken() {
        return oneblock != null ? oneblock.getTotalBlocksBroken() : 0L;
    }
    
    /**
     * Gets the current experience for the island
     * @return current experience
     */
    public double getCurrentExperience() {
        return this.experience;
    }
    
    /**
     * Gets the current level for the island
     * @return current level
     */
    public int getCurrentLevel() {
        return this.level;
    }
    
    /**
     * Gets the owner ID (UUID as string)
     * @return owner ID
     */
    public String getOwnerId() {
        return owner != null ? owner.getUniqueId().toString() : null;
    }
    
    /**
     * Gets the owner name
     * @return owner name
     */
    public String getOwnerName() {
        return owner != null ? owner.getPlayerName() : "Unknown";
    }
    
    /**
     * Gets the last activity timestamp (using lastVisited)
     * @return last activity timestamp
     */
    public long getLastActivity() {
        return lastVisited != null ? lastVisited.toEpochSecond(java.time.ZoneOffset.UTC) : 0L;
    }
    
    /**
     * Sets the current level for the island
     * @param level the level to set
     */
    public void setCurrentLevel(int level) {
        this.level = level;
        if (oneblock != null) {
            oneblock.setEvolutionLevel(level);
        }
    }
    
    /**
     * Gets the island identifier (required for repository registration)
     * @return the island identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * Gets the owner (required for repository queries)
     * @return the island owner
     */
    public OneblockPlayer getOwner() {
        return owner;
    }
    
    /**
     * Gets the center location (required for location-based queries)
     * @return the center location
     */
    public Location getCenterLocation() {
        return centerLocation;
    }
    
    /**
     * Gets the island region (required for region-based queries)
     * @return the island region
     */
    public IslandRegion getRegion() {
        return region;
    }
    
    /**
     * Gets the members set (required for member queries)
     * @return the members set
     */
    public Set<OneblockPlayer> getMembers() {
        return members;
    }
    
    /**
     * Checks if the island is private (required for privacy queries)
     * @return true if island is private
     */
    public boolean isPrivacy() {
        return privacy;
    }
    
    /**
     * Gets the owner UUID
     * @return owner UUID
     */
    @NotNull
    public java.util.UUID getOwnerUuid() {
        return owner != null ? owner.getUuid() : null;
    }

    /**
     * Gets the world name from center location
     * @return world name
     */
    @NotNull
    public String getWorldName() {
        return centerLocation != null ? centerLocation.getWorld().getName() : "world";
    }

    /**
     * Gets the center X coordinate
     * @return center X coordinate
     */
    public double getCenterX() {
        return centerLocation != null ? centerLocation.getX() : 0.0;
    }

    /**
     * Gets the center Z coordinate
     * @return center Z coordinate
     */
    public double getCenterZ() {
        return centerLocation != null ? centerLocation.getZ() : 0.0;
    }

    /**
     * Gets the spawn X coordinate
     * @return spawn X coordinate
     */
    public int getSpawnX() {
        return region != null && region.getSpawnLocation() != null ? 
            region.getSpawnLocation().getBlockX() : 
            (centerLocation != null ? centerLocation.getBlockX() : 0);
    }

    /**
     * Gets the spawn Y coordinate
     * @return spawn Y coordinate
     */
    public int getSpawnY() {
        return region != null && region.getSpawnLocation() != null ? 
            region.getSpawnLocation().getBlockY() : 
            (centerLocation != null ? centerLocation.getBlockY() : 64);
    }

    /**
     * Gets the spawn Z coordinate
     * @return spawn Z coordinate
     */
    public int getSpawnZ() {
        return region != null && region.getSpawnLocation() != null ? 
            region.getSpawnLocation().getBlockZ() : 
            (centerLocation != null ? centerLocation.getBlockZ() : 0);
    }

    /**
     * Sets the spawn X coordinate
     * @param x the X coordinate
     */
    public void setSpawnX(int x) {
        if (region != null && region.getSpawnLocation() != null) {
            region.getSpawnLocation().setX(x);
        }
    }

    /**
     * Sets the spawn Y coordinate
     * @param y the Y coordinate
     */
    public void setSpawnY(int y) {
        if (region != null && region.getSpawnLocation() != null) {
            region.getSpawnLocation().setY(y);
        }
    }

    /**
     * Sets the spawn Z coordinate
     * @param z the Z coordinate
     */
    public void setSpawnZ(int z) {
        if (region != null && region.getSpawnLocation() != null) {
            region.getSpawnLocation().setZ(z);
        }
    }

    /**
     * Gets the island size
     * @return current size
     */
    public int getSize() {
        return currentSize;
    }

    /**
     * Checks if the island is public
     * @return true if island is public (not private)
     */
    public boolean isPublic() {
        return !privacy;
    }

    /**
     * Sets the island's public status
     * @param isPublic true to make public, false to make private
     */
    public void setPublic(boolean isPublic) {
        this.privacy = !isPublic;
    }

    @Override
    public String toString() {
        return "OneblockIsland{" +
                "identifier='" + identifier + '\'' +
                ", islandName='" + islandName + '\'' +
                ", owner=" + (owner != null ? owner.getPlayerName() : "null") +
                ", level=" + level +
                ", experience=" + experience +
                ", currentSize=" + currentSize +
                ", memberCount=" + getMemberCount() +
                ", bannedCount=" + getBannedCount() +
                ", privacy=" + privacy +
                ", islandCoins=" + islandCoins +
                '}';
    }
}