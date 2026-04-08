package de.jexcellence.oneblock.database.entity.generator;

import jakarta.persistence.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Database entity representing a single layer of a generator design.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "oneblock_generator_design_layers")
public class GeneratorDesignLayer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private GeneratorDesign design;
    
    @Column(name = "layer_index", nullable = false)
    private Integer layerIndex;
    
    @Column(name = "name_key", nullable = false, length = 128)
    private String nameKey;
    
    @Column(name = "width", nullable = false)
    private Integer width;
    
    @Column(name = "depth", nullable = false)
    private Integer depth;
    
    @Column(name = "pattern", columnDefinition = "LONGTEXT", nullable = false)
    @Convert(converter = MaterialPatternConverter.class)
    private Material[][] pattern;
    
    @Column(name = "core_offset_x")
    private Integer coreOffsetX;
    
    @Column(name = "core_offset_z")
    private Integer coreOffsetZ;
    
    @Column(name = "is_foundation", nullable = false)
    private Boolean isFoundation = false;
    
    @Column(name = "is_core_layer", nullable = false)
    private Boolean isCoreLayer = false;
    
    @OneToMany(mappedBy = "layer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GeneratorDesignMaterial> materials = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;
    
    public GeneratorDesignLayer() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public GeneratorDesignLayer(int layerIndex, int width, int depth) {
        this();
        this.layerIndex = layerIndex;
        this.width = width;
        this.depth = depth;
        this.nameKey = "generator.layer." + layerIndex;
        this.pattern = new Material[width][depth];
        initializeEmptyPattern();
    }
    
    private void initializeEmptyPattern() {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                pattern[x][z] = Material.AIR;
            }
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public GeneratorDesign getDesign() { return design; }
    public void setDesign(GeneratorDesign design) { 
        this.design = design;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Integer getLayerIndex() { return layerIndex; }
    public void setLayerIndex(Integer layerIndex) { 
        this.layerIndex = layerIndex;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getNameKey() { return nameKey; }
    public void setNameKey(String nameKey) { 
        this.nameKey = nameKey;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { 
        this.width = width;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Integer getDepth() { return depth; }
    public void setDepth(Integer depth) { 
        this.depth = depth;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Material[][] getPattern() { return pattern; }
    public void setPattern(Material[][] pattern) { 
        this.pattern = pattern;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Integer getCoreOffsetX() { return coreOffsetX; }
    public void setCoreOffsetX(Integer coreOffsetX) { 
        this.coreOffsetX = coreOffsetX;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Integer getCoreOffsetZ() { return coreOffsetZ; }
    public void setCoreOffsetZ(Integer coreOffsetZ) { 
        this.coreOffsetZ = coreOffsetZ;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Boolean getIsFoundation() { return isFoundation; }
    public void setIsFoundation(Boolean isFoundation) { 
        this.isFoundation = isFoundation;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Boolean getIsCoreLayer() { return isCoreLayer; }
    public void setIsCoreLayer(Boolean isCoreLayer) { 
        this.isCoreLayer = isCoreLayer;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public List<GeneratorDesignMaterial> getMaterials() { return materials; }
    public void setMaterials(List<GeneratorDesignMaterial> materials) { this.materials = materials; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    
    // Utility methods
    @Nullable
    public Material getMaterialAt(int x, int z) {
        if (x < 0 || x >= width || z < 0 || z >= depth) {
            return null;
        }
        return pattern[x][z];
    }
    
    public void setMaterialAt(int x, int z, @NotNull Material material) {
        if (x >= 0 && x < width && z >= 0 && z < depth) {
            pattern[x][z] = material;
            this.updatedAt = System.currentTimeMillis();
        }
    }
    
    public int getTotalBlocks() {
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                if (pattern[x][z] != null && pattern[x][z] != Material.AIR) {
                    count++;
                }
            }
        }
        return count;
    }
    
    public void addMaterial(@NotNull GeneratorDesignMaterial material) {
        material.setLayer(this);
        this.materials.add(material);
        this.updatedAt = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "GeneratorDesignLayer{" +
                "id=" + id +
                ", layerIndex=" + layerIndex +
                ", width=" + width +
                ", depth=" + depth +
                ", isCoreLayer=" + isCoreLayer +
                '}';
    }
}
