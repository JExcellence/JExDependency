package de.jexcellence.jexplatform.config;

import de.jexcellence.configmapper.sections.ConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for GUI icon display properties.
 *
 * <p>Handles material type, localization keys for display name and description,
 * stack size, custom model data, enchanted appearance, and hidden item flags.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@CSAlways
public class IconSection extends ConfigSection {

    private String type;
    private String displayNameKey;
    private String descriptionKey;
    private Integer amount;
    private Integer customModelData;
    private Boolean enchanted;
    private List<String> hideFlags;

    /**
     * Creates an icon section with the given evaluation environment.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public IconSection(@NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Returns the material type name for this icon.
     *
     * @return the material type, or {@code "PAPER"} if not set
     */
    public @NotNull String getMaterial() {
        return type == null ? "PAPER" : type;
    }

    /**
     * Sets the material type for this icon.
     *
     * @param type the material type name
     */
    public void setMaterial(String type) {
        this.type = type;
    }

    /**
     * Returns the localization key for the display name.
     *
     * @return the display name key, or {@code "not_defined"} if not set
     */
    public @NotNull String getDisplayNameKey() {
        return displayNameKey == null ? "not_defined" : displayNameKey;
    }

    /**
     * Sets the localization key for the display name.
     *
     * @param displayNameKey the display name key
     */
    public void setDisplayNameKey(String displayNameKey) {
        this.displayNameKey = displayNameKey;
    }

    /**
     * Returns the localization key for the description/lore.
     *
     * @return the description key, or {@code "not_defined"} if not set
     */
    public @NotNull String getDescriptionKey() {
        return descriptionKey == null ? "not_defined" : descriptionKey;
    }

    /**
     * Sets the localization key for the description/lore.
     *
     * @param descriptionKey the description key
     */
    public void setDescriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    /**
     * Returns the stack size for this icon.
     *
     * @return the amount, or {@code 1} if not set
     */
    public int getAmount() {
        return amount == null ? 1 : amount;
    }

    /**
     * Sets the stack size for this icon.
     *
     * @param amount the stack size
     */
    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    /**
     * Returns the custom model data value.
     *
     * @return the custom model data, or {@code 0} if not set
     */
    public int getCustomModelData() {
        return customModelData == null ? 0 : customModelData;
    }

    /**
     * Sets the custom model data value.
     *
     * @param customModelData the custom model data
     */
    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    /**
     * Returns whether the icon should have an enchanted glow.
     *
     * @return {@code true} if enchanted
     */
    public boolean getEnchanted() {
        return enchanted != null && enchanted;
    }

    /**
     * Sets whether the icon should have an enchanted glow.
     *
     * @param enchanted {@code true} for enchanted appearance
     */
    public void setEnchanted(Boolean enchanted) {
        this.enchanted = enchanted;
    }

    /**
     * Returns the list of item flags to hide from tooltips.
     *
     * @return the hide flags, or an empty list if not set
     */
    public @NotNull List<String> getHideFlags() {
        return hideFlags == null ? new ArrayList<>() : hideFlags;
    }

    /**
     * Sets the list of item flags to hide from tooltips.
     *
     * @param hideFlags the hide flags
     */
    public void setHideFlags(List<String> hideFlags) {
        this.hideFlags = hideFlags;
    }
}
