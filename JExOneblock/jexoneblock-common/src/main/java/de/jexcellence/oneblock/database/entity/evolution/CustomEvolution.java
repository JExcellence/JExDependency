package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom evolution created by users or administrators
 * Extends OneblockEvolution for user-created evolution types with enhanced validation and factory support
 */
@Entity
@DiscriminatorValue("CUSTOM")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class CustomEvolution extends OneblockEvolution {
    
    @Column(name = "creator_name", length = 100)
    private String creatorName;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "is_template", nullable = false)
    @Builder.Default
    private boolean isTemplate = false;
    
    /**
     * Constructor for creating custom evolution with basic parameters
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     */
    public CustomEvolution(@NotNull String evolutionName, int level, int experienceToPass) {
        super(evolutionName, level, experienceToPass);
        this.createdAt = LocalDateTime.now();
        this.isTemplate = false;
    }
    
    /**
     * Constructor for creating custom evolution with creator information
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @param creatorName the name of the creator
     */
    public CustomEvolution(@NotNull String evolutionName, int level, int experienceToPass, @NotNull String creatorName) {
        super(evolutionName, level, experienceToPass);
        this.creatorName = creatorName;
        this.createdAt = LocalDateTime.now();
        this.isTemplate = false;
    }
    
    /**
     * Constructor for creating custom evolution with description and creator
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @param description the evolution description
     * @param creatorName the name of the creator
     */
    public CustomEvolution(@NotNull String evolutionName, int level, int experienceToPass, 
                          String description, @NotNull String creatorName) {
        super(evolutionName, level, experienceToPass);
        setDescription(description);
        this.creatorName = creatorName;
        this.createdAt = LocalDateTime.now();
        this.isTemplate = false;
    }
    
    /**
     * Validates the custom evolution configuration with enhanced error reporting
     * @return list of validation errors, empty if valid
     */
    public @NotNull List<String> validateConfiguration() {
        List<String> errors = new ArrayList<>();
        
        if (getEvolutionName() == null || getEvolutionName().trim().isEmpty()) {
            errors.add("Evolution name cannot be null or empty");
        }
        
        if (getLevel() < 0) {
            errors.add("Evolution level cannot be negative");
        }
        
        if (getExperienceToPass() <= 0) {
            errors.add("Experience to pass must be greater than 0");
        }
        
        if (creatorName != null && creatorName.trim().isEmpty()) {
            errors.add("Creator name cannot be empty if provided");
        }
        
        if (!isReady()) {
            errors.add("Evolution must have at least one valid content type (blocks, entities, or items)");
        }
        
        return errors;
    }
    
    /**
     * Checks if this custom evolution is valid for use
     * @return true if evolution passes all validation checks
     */
    public boolean isValidForUse() {
        return validateConfiguration().isEmpty();
    }
    
    /**
     * Marks this evolution as a template for reuse
     */
    public void markAsTemplate() {
        this.isTemplate = true;
    }
    
    /**
     * Removes template status from this evolution
     */
    public void removeTemplateStatus() {
        this.isTemplate = false;
    }
    
    /**
     * Checks if this evolution can be used as a template
     * @return true if evolution is marked as template and is valid
     */
    public boolean canBeUsedAsTemplate() {
        return isTemplate && isValidForUse();
    }
    
    // ==================== Additional Getter/Setter Methods ====================
    
    /**
     * Gets the creator name
     * @return the creator name
     */
    public String getCreatorName() {
        return creatorName;
    }
    
    /**
     * Sets the creator name
     * @param creatorName the creator name
     */
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    
    /**
     * Gets the created at timestamp
     * @return the created at timestamp
     */
    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the created at timestamp
     * @param createdAt the created at timestamp
     */
    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Checks if this is a template
     * @return true if template
     */
    public boolean isTemplate() {
        return isTemplate;
    }
    
    /**
     * Sets the template status
     * @param template the template status
     */
    public void setTemplate(boolean template) {
        this.isTemplate = template;
    }

    @Override
    public String toString() {
        return "CustomEvolution{" +
                "evolutionName='" + getEvolutionName() + '\'' +
                ", level=" + getLevel() +
                ", experienceToPass=" + getExperienceToPass() +
                ", creatorName='" + creatorName + '\'' +
                ", createdAt=" + createdAt +
                ", isTemplate=" + isTemplate +
                ", isDisabled=" + isDisabled() +
                ", isReady=" + isReady() +
                ", isValid=" + isValidForUse() +
                '}';
    }
}


