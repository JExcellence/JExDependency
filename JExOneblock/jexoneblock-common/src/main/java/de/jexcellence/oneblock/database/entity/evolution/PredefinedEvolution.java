package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Predefined evolution provided by the system
 * Extends OneblockEvolution for system-defined evolution types with enhanced factory support
 */
@Entity
@DiscriminatorValue("PREDEFINED")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class PredefinedEvolution extends OneblockEvolution {
    
    @Column(name = "system_version", length = 20)
    private String systemVersion;
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private int priority = 0;
    
    @Column(name = "is_core_evolution", nullable = false)
    @Builder.Default
    private boolean isCoreEvolution = false;
    
    @Column(name = "is_template", nullable = false)
    @Builder.Default
    private boolean isTemplate = false;
    
    /**
     * Constructor for creating predefined evolution with basic parameters
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     */
    public PredefinedEvolution(@NotNull String evolutionName, int level, int experienceToPass) {
        super(evolutionName, level, experienceToPass);
        this.isDefault = false;
        this.priority = 0;
        this.isCoreEvolution = false;
        this.isTemplate = false;
    }
    
    /**
     * Constructor for creating predefined evolution with system version and category
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @param systemVersion the system version this evolution was introduced in
     * @param category the evolution category
     */
    public PredefinedEvolution(@NotNull String evolutionName, int level, int experienceToPass, 
                              String systemVersion, @NotNull String category) {
        super(evolutionName, level, experienceToPass);
        this.systemVersion = systemVersion;
        this.category = category;
        this.isDefault = false;
        this.priority = 0;
        this.isCoreEvolution = false;
        this.isTemplate = false;
    }
    
    /**
     * Constructor for creating predefined evolution with full parameters
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @param systemVersion the system version this evolution was introduced in
     * @param category the evolution category
     * @param isDefault whether this is a default evolution
     * @param priority the priority for loading order
     */
    public PredefinedEvolution(@NotNull String evolutionName, int level, int experienceToPass, 
                              String systemVersion, @NotNull String category, boolean isDefault, int priority) {
        super(evolutionName, level, experienceToPass);
        this.systemVersion = systemVersion;
        this.category = category;
        this.isDefault = isDefault;
        this.priority = priority;
        this.isCoreEvolution = false;
        this.isTemplate = false;
    }
    
    /**
     * Validates the predefined evolution configuration with enhanced error reporting
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
        
        if (category == null || category.trim().isEmpty()) {
            errors.add("Category is required for predefined evolutions");
        }
        
        if (systemVersion != null && systemVersion.trim().isEmpty()) {
            errors.add("System version cannot be empty if provided");
        }
        
        if (priority < 0) {
            errors.add("Priority cannot be negative");
        }
        
        if (!isReady()) {
            errors.add("Evolution must have at least one valid content type (blocks, entities, or items)");
        }
        
        return errors;
    }
    
    /**
     * Checks if this predefined evolution is valid for use
     * @return true if evolution passes all validation checks
     */
    public boolean isValidForUse() {
        return validateConfiguration().isEmpty();
    }
    
    /**
     * Checks if this evolution is a system default
     * @return true if this is a default evolution
     */
    public boolean isSystemDefault() {
        return isDefault;
    }
    
    /**
     * Marks this evolution as a system default
     */
    public void markAsDefault() {
        this.isDefault = true;
    }
    
    /**
     * Removes the default status from this evolution
     */
    public void removeDefaultStatus() {
        this.isDefault = false;
    }
    
    /**
     * Marks this evolution as a core system evolution
     */
    public void markAsCoreEvolution() {
        this.isCoreEvolution = true;
    }
    
    /**
     * Removes core evolution status
     */
    public void removeCoreEvolutionStatus() {
        this.isCoreEvolution = false;
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
    
    /**
     * Checks if this evolution belongs to a specific category
     * @param categoryName the category to check
     * @return true if evolution belongs to the specified category
     */
    public boolean belongsToCategory(String categoryName) {
        return category != null && category.equalsIgnoreCase(categoryName);
    }
    
    /**
     * Checks if this evolution was introduced in a specific system version
     * @param version the version to check
     * @return true if evolution was introduced in the specified version
     */
    public boolean isFromVersion(String version) {
        return systemVersion != null && systemVersion.equals(version);
    }
    
    /**
     * Checks if this evolution has higher priority than another
     * @param other the other evolution to compare with
     * @return true if this evolution has higher priority
     */
    public boolean hasHigherPriorityThan(@NotNull PredefinedEvolution other) {
        return this.priority > other.priority;
    }
    
    /**
     * Checks if this evolution can be safely disabled
     * @return true if evolution can be disabled (not core and not default)
     */
    public boolean canBeDisabled() {
        return !isCoreEvolution && !isDefault;
    }
    
    // ==================== Additional Getter/Setter Methods ====================
    
    /**
     * Gets the system version
     * @return the system version
     */
    public String getSystemVersion() {
        return systemVersion;
    }
    
    /**
     * Sets the system version
     * @param systemVersion the system version
     */
    public void setSystemVersion(String systemVersion) {
        this.systemVersion = systemVersion;
    }
    
    /**
     * Gets the category
     * @return the category
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Sets the category
     * @param category the category
     */
    public void setCategory(String category) {
        this.category = category;
    }
    
    /**
     * Gets the priority
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Sets the priority
     * @param priority the priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    /**
     * Checks if this is a default evolution
     * @return true if default
     */
    public boolean isDefault() {
        return isDefault;
    }
    
    /**
     * Sets the default status
     * @param isDefault the default status
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    /**
     * Checks if this is a core evolution
     * @return true if core evolution
     */
    public boolean isCoreEvolution() {
        return isCoreEvolution;
    }
    
    /**
     * Sets the core evolution status
     * @param coreEvolution the core evolution status
     */
    public void setCoreEvolution(boolean coreEvolution) {
        this.isCoreEvolution = coreEvolution;
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
    
    /**
     * Creates a factory supplier for this evolution type
     * @return a supplier that can create instances of this evolution
     */
    public @NotNull java.util.function.Supplier<PredefinedEvolution> createFactorySupplier() {
        return () -> {
            PredefinedEvolution evolution = new PredefinedEvolution();
            evolution.setEvolutionName(getEvolutionName());
            evolution.setLevel(getLevel());
            evolution.setExperienceToPass(getExperienceToPass());
            evolution.setSystemVersion(systemVersion);
            evolution.setCategory(category);
            evolution.setDefault(isDefault);
            evolution.setPriority(priority);
            evolution.setCoreEvolution(isCoreEvolution);
            evolution.setTemplate(isTemplate);
            evolution.setShowcase(getShowcase());
            evolution.setDescription(getDescription());
            evolution.setDisabled(isDisabled());
            return evolution;
        };
    }
    
    @Override
    public String toString() {
        return "PredefinedEvolution{" +
                "evolutionName='" + getEvolutionName() + '\'' +
                ", level=" + getLevel() +
                ", experienceToPass=" + getExperienceToPass() +
                ", systemVersion='" + systemVersion + '\'' +
                ", category='" + category + '\'' +
                ", priority=" + priority +
                ", isDefault=" + isDefault +
                ", isCoreEvolution=" + isCoreEvolution +
                ", isTemplate=" + isTemplate +
                ", isDisabled=" + isDisabled() +
                ", isReady=" + isReady() +
                ", isValid=" + isValidForUse() +
                '}';
    }
}


