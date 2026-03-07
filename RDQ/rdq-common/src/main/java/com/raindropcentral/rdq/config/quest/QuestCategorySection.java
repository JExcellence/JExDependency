package com.raindropcentral.rdq.config.quest;

import com.raindropcentral.rdq.config.utility.IconSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * Represents a configuration section for a quest category.
 * Contains all properties and display information for a quest category.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@CSAlways
@Getter
public class QuestCategorySection extends AConfigSection {
    
    /** The unique identifier for this category. */
    private String identifier;
    
    /** The key for the display name of the category (for localization). */
    private String nameKey;
    
    /** The key for the description of the category (for localization). */
    private String descriptionKey;
    
    /** The icon representing this category. */
    private IconSection icon;
    
    /** The display order of this category. */
    private Integer displayOrder;
    
    /** Whether this category is enabled. */
    private Boolean enabled;
    
    /** The permission required to access this category. */
    private String requiredPermission;
    
    /** The category ID (set during parsing). */
    @CSIgnore
    private String categoryId;
    
    /**
     * Constructs a new QuestCategorySection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public QuestCategorySection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Sets the category ID.
     *
     * @param categoryId the category ID
     */
    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
        
        // Update icon keys if icon exists
        if (this.icon != null && categoryId != null) {
            // Always update to ensure consistency
            this.icon.setDisplayNameKey("quest.category." + categoryId + ".name");
            this.icon.setDescriptionKey("quest.category." + categoryId + ".icon.lore");
        }
    }
    
    /**
     * Called after parsing the configuration fields. Sets default localization keys if not provided.
     *
     * @param fields the list of fields parsed
     * @throws Exception if an error occurs during post-processing
     */
    @Override
    public void afterParsing(final List<Field> fields) throws Exception {
        super.afterParsing(fields);
        
        if (this.categoryId != null) {
            if (this.nameKey == null) {
                this.nameKey = "quest.category." + this.categoryId + ".name";
            }
            if (this.descriptionKey == null) {
                this.descriptionKey = "quest.category." + this.categoryId + ".description";
            }
        }
    }
    
    /**
     * Gets the unique identifier for this category.
     *
     * @return the category identifier, or a generated one if not set
     */
    public String getIdentifier() {
        return this.identifier == null ? "not_defined_" + UUID.randomUUID() : this.identifier;
    }
    
    /**
     * Gets the name key for this category.
     *
     * @return the name key, or "not_defined" if not set
     */
    public String getNameKey() {
        return this.nameKey == null ? "not_defined" : this.nameKey;
    }
    
    /**
     * Gets the description key for this category.
     *
     * @return the description key, or "not_defined" if not set
     */
    public String getDescriptionKey() {
        return this.descriptionKey == null ? "not_defined" : this.descriptionKey;
    }
    
    /**
     * Gets the icon section for this category.
     *
     * @return the icon section, or a new default IconSection if not set
     */
    public IconSection getIcon() {
        return this.icon == null ? new IconSection(new EvaluationEnvironmentBuilder()) : this.icon;
    }
    
    /**
     * Gets the display order of this category.
     *
     * @return the display order, or 0 if not set
     */
    public Integer getDisplayOrder() {
        return this.displayOrder == null ? 0 : this.displayOrder;
    }
    
    /**
     * Checks if this category is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnabled() {
        return this.enabled != null && this.enabled;
    }
}
