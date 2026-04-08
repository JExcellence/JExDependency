package de.jexcellence.oneblock.database.entity.generator;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Database entity representing a generator design template.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "oneblock_generator_designs")
public class GeneratorDesign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "design_key", unique = true, nullable = false, length = 64)
    private String designKey;
    
    @Column(name = "name_key", nullable = false, length = 128)
    private String nameKey;
    
    @Column(name = "description_key", nullable = false, length = 128)
    private String descriptionKey;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "design_type", nullable = false, length = 32)
    private EGeneratorDesignType designType;
    
    @Column(name = "tier", nullable = false)
    private Integer tier;
    
    @Column(name = "difficulty", nullable = false)
    private Integer difficulty = 1;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "icon_material", nullable = false, length = 64)
    private String iconMaterial;
    
    @Column(name = "icon_custom_model_data")
    private Integer iconCustomModelData;
    
    @Column(name = "particle_effect", length = 64)
    private String particleEffect;
    
    @Column(name = "speed_multiplier", nullable = false)
    private Double speedMultiplier = 1.0;
    
    @Column(name = "xp_multiplier", nullable = false)
    private Double xpMultiplier = 1.0;
    
    @Column(name = "fortune_bonus", nullable = false)
    private Double fortuneBonus = 0.0;
    
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("layerIndex ASC")
    private List<GeneratorDesignLayer> layers = new ArrayList<>();
    
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<GeneratorDesignRequirement> requirements = new ArrayList<>();
    
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GeneratorDesignReward> rewards = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;
    
    public GeneratorDesign() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public GeneratorDesign(@NotNull String designKey, @NotNull EGeneratorDesignType designType) {
        this();
        this.designKey = designKey;
        this.designType = designType;
        this.tier = designType.getTier();
        this.nameKey = designType.getNameKey();
        this.descriptionKey = designType.getDescriptionKey();
        this.iconMaterial = designType.getIcon().name();
        this.speedMultiplier = designType.getSpeedMultiplier();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getDesignKey() { return designKey; }
    public void setDesignKey(String designKey) { 
        this.designKey = designKey;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getNameKey() { return nameKey; }
    public void setNameKey(String nameKey) { 
        this.nameKey = nameKey;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getDescriptionKey() { return descriptionKey; }
    public void setDescriptionKey(String descriptionKey) { 
        this.descriptionKey = descriptionKey;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public EGeneratorDesignType getDesignType() { return designType; }
    public void setDesignType(EGeneratorDesignType designType) { 
        this.designType = designType;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Integer getTier() { return tier; }
    public void setTier(Integer tier) { 
        this.tier = tier;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { 
        this.difficulty = difficulty;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { 
        this.enabled = enabled;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getIconMaterial() { return iconMaterial; }
    public void setIconMaterial(String iconMaterial) { 
        this.iconMaterial = iconMaterial;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Integer getIconCustomModelData() { return iconCustomModelData; }
    public void setIconCustomModelData(Integer iconCustomModelData) { 
        this.iconCustomModelData = iconCustomModelData;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getParticleEffect() { return particleEffect; }
    public void setParticleEffect(String particleEffect) { 
        this.particleEffect = particleEffect;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(Double speedMultiplier) { 
        this.speedMultiplier = speedMultiplier;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Double getXpMultiplier() { return xpMultiplier; }
    public void setXpMultiplier(Double xpMultiplier) { 
        this.xpMultiplier = xpMultiplier;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Double getFortuneBonus() { return fortuneBonus; }
    public void setFortuneBonus(Double fortuneBonus) { 
        this.fortuneBonus = fortuneBonus;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public List<GeneratorDesignLayer> getLayers() { return layers; }
    public void setLayers(List<GeneratorDesignLayer> layers) { this.layers = layers; }
    
    public List<GeneratorDesignRequirement> getRequirements() { return requirements; }
    public void setRequirements(List<GeneratorDesignRequirement> requirements) { this.requirements = requirements; }
    
    public List<GeneratorDesignReward> getRewards() { return rewards; }
    public void setRewards(List<GeneratorDesignReward> rewards) { this.rewards = rewards; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    
    // Utility methods
    public void addLayer(@NotNull GeneratorDesignLayer layer) {
        layer.setDesign(this);
        this.layers.add(layer);
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void addRequirement(@NotNull GeneratorDesignRequirement requirement) {
        requirement.setDesign(this);
        this.requirements.add(requirement);
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void addReward(@NotNull GeneratorDesignReward reward) {
        reward.setDesign(this);
        this.rewards.add(reward);
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getTotalLayers() {
        return layers.size();
    }
    
    public int getWidth() {
        return designType != null ? designType.getWidth() : 5;
    }
    
    public int getDepth() {
        return designType != null ? designType.getDepth() : 5;
    }
    
    public int getHeight() {
        return designType != null ? designType.getHeight() : 3;
    }
    
    @Override
    public String toString() {
        return "GeneratorDesign{" +
                "id=" + id +
                ", designKey='" + designKey + '\'' +
                ", designType=" + designType +
                ", tier=" + tier +
                ", enabled=" + enabled +
                '}';
    }
}
