package de.jexcellence.oneblock.database.entity.generator;

import com.raindropcentral.rplatform.database.converter.RequirementConverter;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Database entity for generator design requirements using RPlatform requirement system.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "oneblock_generator_design_requirements")
public class GeneratorDesignRequirement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private GeneratorDesign design;
    
    @Column(name = "requirement_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RequirementConverter.class)
    private AbstractRequirement requirement;
    
    @Column(name = "icon_material", length = 64)
    private String iconMaterial;
    
    @Column(name = "icon_custom_model_data")
    private Integer iconCustomModelData;
    
    @Column(name = "icon_display_name", length = 256)
    private String iconDisplayName;
    
    @Column(name = "icon_lore", columnDefinition = "TEXT")
    private String iconLore;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "description_override", columnDefinition = "TEXT")
    private String descriptionOverride;
    
    public GeneratorDesignRequirement() {}
    
    public GeneratorDesignRequirement(@NotNull AbstractRequirement requirement) {
        this.requirement = requirement;
    }
    
    public GeneratorDesignRequirement(@NotNull AbstractRequirement requirement, int displayOrder) {
        this.requirement = requirement;
        this.displayOrder = displayOrder;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public GeneratorDesign getDesign() { return design; }
    public void setDesign(GeneratorDesign design) { this.design = design; }
    
    public AbstractRequirement getRequirement() { return requirement; }
    public void setRequirement(AbstractRequirement requirement) { this.requirement = requirement; }
    
    public String getIconMaterial() { return iconMaterial; }
    public void setIconMaterial(String iconMaterial) { this.iconMaterial = iconMaterial; }
    
    public Integer getIconCustomModelData() { return iconCustomModelData; }
    public void setIconCustomModelData(Integer iconCustomModelData) { this.iconCustomModelData = iconCustomModelData; }
    
    public String getIconDisplayName() { return iconDisplayName; }
    public void setIconDisplayName(String iconDisplayName) { this.iconDisplayName = iconDisplayName; }
    
    public String getIconLore() { return iconLore; }
    public void setIconLore(String iconLore) { this.iconLore = iconLore; }
    
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public String getDescriptionOverride() { return descriptionOverride; }
    public void setDescriptionOverride(String descriptionOverride) { this.descriptionOverride = descriptionOverride; }
    
    // Convenience methods delegating to requirement
    public boolean isMet(@NotNull Player player) {
        return enabled && requirement != null && requirement.isMet(player);
    }
    
    public double calculateProgress(@NotNull Player player) {
        if (!enabled || requirement == null) {
            return 0.0;
        }
        return requirement.calculateProgress(player);
    }
    
    public void consume(@NotNull Player player) {
        if (enabled && requirement != null) {
            requirement.consume(player);
        }
    }
    
    @NotNull
    public String getDescriptionKey() {
        if (descriptionOverride != null && !descriptionOverride.isEmpty()) {
            return descriptionOverride;
        }
        return requirement != null ? requirement.getDescriptionKey() : "requirement.unknown";
    }
    
    public boolean shouldConsume() {
        return requirement != null && requirement.shouldConsume();
    }

    @Override
    public String toString() {
        return "GeneratorDesignRequirement{" +
                "id=" + id +
                ", requirement=" + (requirement != null ? requirement.getClass().getSimpleName() : "null") +
                ", displayOrder=" + displayOrder +
                ", enabled=" + enabled +
                '}';
    }
}
