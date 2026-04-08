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
 * Entity representing blocks that can appear in an evolution
 * Replaces StageBlock with evolution-based naming and Lombok optimization
 */
@Entity
@Table(name = "evolution_blocks", 
    indexes = {
        @Index(name = "idx_evolution_block_evolution_id", columnList = "evolution_id"),
        @Index(name = "idx_evolution_block_rarity", columnList = "rarity"),
        @Index(name = "idx_evolution_block_enabled", columnList = "is_enabled"),
        @Index(name = "idx_evolution_block_weight", columnList = "weight"),
        @Index(name = "idx_evolution_block_composite", columnList = "evolution_id, rarity, is_enabled")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_evolution_block_evolution_rarity", 
                         columnNames = {"evolution_id", "rarity"})
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"evolution", "rarity"}, callSuper = false)
public class EvolutionBlock extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evolution_id", nullable = false)
    private OneblockEvolution evolution;
    
    @Column(name = "rarity", nullable = false)
    @Enumerated(EnumType.STRING)
    private EEvolutionRarityType rarity;
    
    @Column(name = "materials", columnDefinition = "TEXT")
    @Convert(converter = MaterialListConverter.class)
    @Builder.Default
    private List<Material> materials = new ArrayList<>();
    
    @Column(name = "weight", nullable = false)
    @Builder.Default
    private double weight = 1.0;
    
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean isEnabled = true;
    
    /**
     * Constructor for creating evolution block with basic parameters
     * @param evolution the parent evolution
     * @param rarity the rarity level
     * @param materials the list of materials
     */
    public EvolutionBlock(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity, @NotNull List<Material> materials) {
        this.evolution = evolution;
        this.rarity = rarity;
        this.materials = new ArrayList<>(materials);
        this.weight = 1.0;
        this.isEnabled = true;
    }
    
    /**
     * Adds a material to this block configuration
     * @param material the material to add
     */
    public void addMaterial(@NotNull Material material) {
        if (this.materials == null) {
            this.materials = new ArrayList<>();
        }
        this.materials.add(material);
    }
    
    /**
     * Removes a material from this block configuration
     * @param material the material to remove
     * @return true if the material was removed
     */
    public boolean removeMaterial(@NotNull Material material) {
        return this.materials != null && this.materials.remove(material);
    }
    
    /**
     * Checks if this block configuration contains a specific material
     * @param material the material to check
     * @return true if the material is present
     */
    public boolean containsMaterial(@NotNull Material material) {
        return this.materials != null && this.materials.contains(material);
    }
    
    /**
     * Gets the number of materials in this configuration
     * @return the count of materials
     */
    public int getMaterialCount() {
        return this.materials != null ? this.materials.size() : 0;
    }
    
    /**
     * Checks if this block configuration is valid
     * @return true if enabled and has at least one material
     */
    public boolean isValid() {
        return isEnabled && materials != null && !materials.isEmpty();
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
        return "EvolutionBlock{" +
                "rarity=" + rarity +
                ", materialCount=" + getMaterialCount() +
                ", weight=" + weight +
                ", isEnabled=" + isEnabled +
                '}';
    }
}