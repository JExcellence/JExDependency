package de.jexcellence.oneblock.database.entity.generator;

import de.jexcellence.hibernate.entity.BaseEntity;
import de.jexcellence.oneblock.database.converter.LocationConverter;
import jakarta.persistence.*;
import lombok.*;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Database entity representing a player-built generator structure.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "oneblock_player_generator_structures",
    indexes = {
        @Index(name = "idx_player_generator_island", columnList = "island_id"),
        @Index(name = "idx_player_generator_owner", columnList = "owner_id"),
        @Index(name = "idx_player_generator_design", columnList = "design_id"),
        @Index(name = "idx_player_generator_active", columnList = "is_active"),
        @Index(name = "idx_player_generator_valid", columnList = "is_valid")
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class PlayerGeneratorStructure extends BaseEntity {

    @Column(name = "island_id", nullable = false)
    private Long islandId;
    
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private GeneratorDesign design;

    @Column(name = "core_location")
    @Convert(converter = LocationConverter.class)
    private Location coreLocation;

    @Column(name = "is_valid", nullable = false)
    @Builder.Default
    private Boolean isValid = false;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;
    
    @Column(name = "build_progress", nullable = false)
    @Builder.Default
    private Double buildProgress = 0.0;
    
    @Column(name = "current_layer", nullable = false)
    @Builder.Default
    private Integer currentLayer = 0;

    @Column(name = "blocks_generated", nullable = false)
    @Builder.Default
    private Long blocksGenerated = 0L;
    
    @Column(name = "total_experience", nullable = false)
    @Builder.Default
    private Long totalExperience = 0L;

    @Column(name = "speed_level", nullable = false)
    @Builder.Default
    private Integer speedLevel = 0;
    
    @Column(name = "efficiency_level", nullable = false)
    @Builder.Default
    private Integer efficiencyLevel = 0;
    
    @Column(name = "fortune_level", nullable = false)
    @Builder.Default
    private Integer fortuneLevel = 0;

    @Column(name = "built_at")
    private Long builtAt;
    
    @Column(name = "last_used")
    private Long lastUsed;
    
    /**
     * Constructor for creating a new structure
     */
    public PlayerGeneratorStructure(@NotNull Long islandId, @NotNull UUID ownerId, @NotNull GeneratorDesign design) {
        this.islandId = islandId;
        this.ownerId = ownerId;
        this.design = design;
        this.isValid = false;
        this.isActive = false;
        this.buildProgress = 0.0;
        this.currentLayer = 0;
        this.blocksGenerated = 0L;
        this.totalExperience = 0L;
        this.speedLevel = 0;
        this.efficiencyLevel = 0;
        this.fortuneLevel = 0;
    }
    
    // Statistics methods
    public void incrementBlocksGenerated(long count) {
        this.blocksGenerated += count;
        this.lastUsed = System.currentTimeMillis();
    }
    
    public void addExperience(long experience) {
        this.totalExperience += experience;
    }
    
    public void markAsBuilt() {
        this.builtAt = System.currentTimeMillis();
        this.buildProgress = 1.0;
        this.isValid = true;
    }
    
    public boolean isComplete() {
        return buildProgress >= 1.0;
    }
    
    public int getBuildProgressPercentage() {
        return (int) (buildProgress * 100);
    }
    
    @Override
    public String toString() {
        return "PlayerGeneratorStructure{" +
                "id=" + getId() +
                ", islandId=" + islandId +
                ", design=" + (design != null ? design.getDesignKey() : "null") +
                ", isValid=" + isValid +
                ", isActive=" + isActive +
                '}';
    }
}
