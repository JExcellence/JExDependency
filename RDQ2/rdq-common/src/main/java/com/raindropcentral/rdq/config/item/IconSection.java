package com.raindropcentral.rdq.config.item;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for icon display properties.
 * <p>
 * This section handles the visual representation of items in GUIs, including type,
 * display name, description/lore, and various visual effects like enchantments and flags.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class IconSection extends AConfigSection {

    /**
     * The type for this icon.
     * If null, defaults to "PAPER".
     */
    private String type;

    /**
     * The localization key for the display name of this icon.
     * If null, defaults to "not_defined".
     */
    private String displayNameKey;

    /**
     * The localization key for the description/lore of this icon.
     * If null, defaults to "not_defined".
     */
    private String descriptionKey;

    /**
     * The amount/stack size for this icon.
     * If null, defaults to 1.
     */
    private Integer amount;

    /**
     * The custom model data for this icon.
     * If null, defaults to 0.
     */
    private Integer customModelData;

    /**
     * Whether this icon should have an enchanted appearance.
     * If null, defaults to false.
     */
    private Boolean enchanted;

    /**
     * List of item flags to hide certain properties.
     * If null, defaults to an empty list.
     */    private List<String> hideFlags;

    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected IconSection() {
        super(new EvaluationEnvironmentBuilder());
    }

    /**
     * Constructs a new IconSection with the given evaluation environment.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public IconSection(
            final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Gets the material type for this icon.
     *
     * @return the configured material type, or {@code "PAPER"} if no type is defined
     */
    public String getMaterial() {
        return this.type == null ? "PAPER" : this.type;
    }

    /**
     * Sets the material type for this icon.
     *
     * @param type the material type to display, or {@code null} to revert to the default
     */
    public void setMaterial(final String type) {
        this.type = type;
    }

    /**
     * Gets the localization key for the display name of this icon.
     *
     * @return the display name localization key, or {@code "not_defined"} if not set
     */
    public String getDisplayNameKey() {
        return this.displayNameKey == null ? "not_defined" : this.displayNameKey;
    }

    /**
     * Sets the localization key for the display name of this icon.
     *
     * @param displayNameKey the display name localization key, or {@code null} to use the default
     */
    public void setDisplayNameKey(final String displayNameKey) {
        this.displayNameKey = displayNameKey;
    }

    /**
     * Gets the localization key for the description/lore of this icon.
     *
     * @return the description localization key, or {@code "not_defined"} if not set
     */
    public String getDescriptionKey() {
        return this.descriptionKey == null ? "not_defined" : this.descriptionKey;
    }

    /**
     * Sets the localization key for the description/lore of this icon.
     *
     * @param descriptionKey the description localization key, or {@code null} to use the default
     */
    public void setDescriptionKey(final String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    /**
     * Gets the amount/stack size for this icon.
     *
     * @return the configured amount, or {@code 1} if not set
     */
    public Integer getAmount() {
        return this.amount == null ? 1 : this.amount;
    }

    /**
     * Sets the amount/stack size for this icon.
     *
     * @param amount the amount to display, or {@code null} to use the default
     */
    public void setAmount(final Integer amount) {
        this.amount = amount;
    }

    /**
     * Gets the custom model data for this icon.
     *
     * @return the configured custom model data, or {@code 0} if not set
     */
    public Integer getCustomModelData() {
        return this.customModelData == null ? 0 : this.customModelData;
    }

    /**
     * Sets the custom model data for this icon.
     *
     * @param customModelData the custom model data, or {@code null} to use the default
     */
    public void setCustomModelData(final Integer customModelData) {
        this.customModelData = customModelData;
    }

    /**
     * Checks if this icon should have an enchanted appearance.
     *
     * @return {@code true} if enchanted, {@code false} otherwise
     */
    public Boolean getEnchanted() {
        return this.enchanted != null && this.enchanted;
    }

    /**
     * Sets whether this icon should have an enchanted appearance.
     *
     * @param enchanted {@code true} to enable the enchanted effect, or {@code null} to use the default
     */
    public void setEnchanted(final Boolean enchanted) {
        this.enchanted = enchanted;
    }

    /**
     * Gets the list of item flags to hide certain properties.
     *
     * @return a mutable list of hide flags, or an empty list if not set
     */
    public List<String> getHideFlags() {
        return this.hideFlags == null ? new ArrayList<>() : this.hideFlags;
    }

    /**
     * Sets the list of item flags to hide certain properties.
     *
     * @param hideFlags the list of hide flags, or {@code null} to use an empty list
     */
    public void setHideFlags(final List<String> hideFlags) {
        this.hideFlags = hideFlags;
    }
}
