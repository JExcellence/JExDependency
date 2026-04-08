package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.hibernate.entity.BaseEntity;
import de.jexcellence.oneblock.database.converter.MaterialListConverter;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import jakarta.persistence.*;
import lombok.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing entities (mobs) that can spawn in an evolution
 * Replaces StageEntity with evolution-based naming and Lombok optimization
 */
@Entity
@Table(name = "evolution_entities",
    indexes = {
        @Index(name = "idx_evolution_entity_evolution_id", columnList = "evolution_id"),
        @Index(name = "idx_evolution_entity_rarity", columnList = "rarity"),
        @Index(name = "idx_evolution_entity_enabled", columnList = "is_enabled"),
        @Index(name = "idx_evolution_entity_spawn_chance", columnList = "spawn_chance"),
        @Index(name = "idx_evolution_entity_composite", columnList = "evolution_id, rarity, is_enabled")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_evolution_entity_evolution_rarity", 
                         columnNames = {"evolution_id", "rarity"})
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"evolution", "rarity"}, callSuper = false)
public class EvolutionEntity extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evolution_id", nullable = false)
    private OneblockEvolution evolution;
    
    @Column(name = "rarity", nullable = false)
    @Enumerated(EnumType.STRING)
    private EEvolutionRarityType rarity;
    
    @Column(name = "spawn_eggs", columnDefinition = "TEXT")
    @Convert(converter = MaterialListConverter.class)
    @Builder.Default
    private List<Material> spawnEggs = new ArrayList<>();
    
    @Column(name = "weight", nullable = false)
    @Builder.Default
    private double weight = 1.0;
    
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean isEnabled = true;
    
    @Column(name = "spawn_chance", nullable = false)
    @Builder.Default
    private double spawnChance = 0.1; // 10% default spawn chance
    
    @Column(name = "max_spawns_per_break", nullable = false)
    @Builder.Default
    private int maxSpawnsPerBreak = 1;
    
    /**
     * Constructor for creating evolution entity with basic parameters
     * @param evolution the parent evolution
     * @param rarity the rarity level
     * @param spawnEggs the list of spawn egg materials
     */
    public EvolutionEntity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity, @NotNull List<Material> spawnEggs) {
        this.evolution = evolution;
        this.rarity = rarity;
        this.spawnEggs = new ArrayList<>(spawnEggs);
        this.weight = 1.0;
        this.isEnabled = true;
        this.spawnChance = 0.1;
        this.maxSpawnsPerBreak = 1;
    }
    
    /**
     * Adds a spawn egg to this entity configuration
     * @param spawnEgg the spawn egg material to add
     */
    public void addSpawnEgg(@NotNull Material spawnEgg) {
        if (this.spawnEggs == null) {
            this.spawnEggs = new ArrayList<>();
        }
        this.spawnEggs.add(spawnEgg);
    }
    
    /**
     * Removes a spawn egg from this entity configuration
     * @param spawnEgg the spawn egg material to remove
     * @return true if the spawn egg was removed
     */
    public boolean removeSpawnEgg(@NotNull Material spawnEgg) {
        return this.spawnEggs != null && this.spawnEggs.remove(spawnEgg);
    }
    
    /**
     * Checks if this entity configuration contains a specific spawn egg
     * @param spawnEgg the spawn egg material to check
     * @return true if the spawn egg is present
     */
    public boolean containsSpawnEgg(@NotNull Material spawnEgg) {
        return this.spawnEggs != null && this.spawnEggs.contains(spawnEgg);
    }
    
    /**
     * Gets the number of spawn eggs in this configuration
     * @return the count of spawn eggs
     */
    public int getSpawnEggCount() {
        return this.spawnEggs != null ? this.spawnEggs.size() : 0;
    }
    
    /**
     * Checks if this entity configuration is valid
     * @return true if enabled and has at least one spawn egg
     */
    public boolean isValid() {
        return isEnabled && spawnEggs != null && !spawnEggs.isEmpty() && spawnChance > 0;
    }
    
    /**
     * Calculates if an entity should spawn based on spawn chance
     * @return true if entity should spawn
     */
    public boolean shouldSpawn() {
        return isValid() && Math.random() < spawnChance;
    }
    
    /**
     * Gets a random spawn egg from this configuration
     * @return a random spawn egg material, or null if none available
     */
    public Material getRandomSpawnEgg() {
        if (spawnEggs == null || spawnEggs.isEmpty()) {
            return null;
        }
        return spawnEggs.get((int) (Math.random() * spawnEggs.size()));
    }
    
    /**
     * Sets the parent evolution
     * @param evolution the parent evolution
     */
    public void setEvolution(@NotNull OneblockEvolution evolution) {
        this.evolution = evolution;
    }
    
    @Override
    public String toString() {
        return "EvolutionEntity{" +
                "rarity=" + rarity +
                ", spawnEggCount=" + getSpawnEggCount() +
                ", weight=" + weight +
                ", spawnChance=" + spawnChance +
                ", isEnabled=" + isEnabled +
                '}';
    }
}