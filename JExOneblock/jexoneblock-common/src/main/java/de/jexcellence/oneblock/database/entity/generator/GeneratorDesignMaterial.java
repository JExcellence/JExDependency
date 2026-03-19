package de.jexcellence.oneblock.database.entity.generator;

import jakarta.persistence.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Database entity tracking materials required for a generator design layer.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "oneblock_generator_design_materials")
public class GeneratorDesignMaterial {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layer_id", nullable = false)
    private GeneratorDesignLayer layer;
    
    @Column(name = "material", nullable = false, length = 64)
    private String material;
    
    @Column(name = "amount", nullable = false)
    private Integer amount;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    
    public GeneratorDesignMaterial() {}
    
    public GeneratorDesignMaterial(@NotNull Material material, int amount) {
        this.material = material.name();
        this.amount = amount;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public GeneratorDesignLayer getLayer() { return layer; }
    public void setLayer(GeneratorDesignLayer layer) { this.layer = layer; }
    
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    
    @NotNull
    public Material getMaterialType() {
        try {
            return Material.valueOf(material);
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
    
    public void setMaterialType(@NotNull Material material) {
        this.material = material.name();
    }
    
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    
    @Override
    public String toString() {
        return "GeneratorDesignMaterial{" +
                "material='" + material + '\'' +
                ", amount=" + amount +
                '}';
    }
}
