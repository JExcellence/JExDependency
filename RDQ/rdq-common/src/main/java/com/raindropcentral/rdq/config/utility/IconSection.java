package com.raindropcentral.rdq.config.utility;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for icon display properties.
 *
 * <p>This section handles the visual representation of items in GUIs, including type,
 * display name, description/lore, and various visual effects like enchantments and flags.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
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
	 */
	private List<String> hideFlags;
	
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
	 * Gets the type type for this icon.
	 *
	 * @return the type type, or "PAPER" if not set
	 */
	public String getMaterial() {
		return this.type == null ? "PAPER" : this.type;
	}
	
	/**
	 * Sets the type type for this icon.
	 *
	 * @param type the type type
	 */
	public void setMaterial(final String type) {
		this.type = type;
	}
	
	/**
	 * Gets the localization key for the display name of this icon.
	 *
	 * @return the display name key, or "not_defined" if not set
	 */
	public String getDisplayNameKey() {
		return this.displayNameKey == null ? "not_defined" : this.displayNameKey;
	}
	
	/**
	 * Sets the localization key for the display name of this icon.
	 *
	 * @param displayNameKey the display name key
	 */
	public void setDisplayNameKey(final String displayNameKey) {
		this.displayNameKey = displayNameKey;
	}
	
	/**
	 * Gets the localization key for the description/lore of this icon.
	 *
	 * @return the description key, or "not_defined" if not set
	 */
	public String getDescriptionKey() {
		return this.descriptionKey == null ? "not_defined" : this.descriptionKey;
	}
	
	/**
	 * Sets the localization key for the description/lore of this icon.
	 *
	 * @param descriptionKey the description key
	 */
	public void setDescriptionKey(final String descriptionKey) {
		this.descriptionKey = descriptionKey;
	}
	
	/**
	 * Gets the amount/stack size for this icon.
	 *
	 * @return the amount, or 1 if not set
	 */
	public Integer getAmount() {
		return this.amount == null ? 1 : this.amount;
	}
	
	/**
	 * Sets the amount/stack size for this icon.
	 *
	 * @param amount the amount
	 */
	public void setAmount(final Integer amount) {
		this.amount = amount;
	}
	
	/**
	 * Gets the custom model data for this icon.
	 *
	 * @return the custom model data, or 0 if not set
	 */
	public Integer getCustomModelData() {
		return this.customModelData == null ? 0 : this.customModelData;
	}
	
	/**
	 * Sets the custom model data for this icon.
	 *
	 * @param customModelData the custom model data
	 */
	public void setCustomModelData(final Integer customModelData) {
		this.customModelData = customModelData;
	}
	
	/**
	 * Checks if this icon should have an enchanted appearance.
	 *
	 * @return true if enchanted, false otherwise
	 */
	public Boolean getEnchanted() {
		return this.enchanted != null && this.enchanted;
	}
	
	/**
	 * Sets whether this icon should have an enchanted appearance.
	 *
	 * @param enchanted true for enchanted appearance
	 */
	public void setEnchanted(final Boolean enchanted) {
		this.enchanted = enchanted;
	}
	
	/**
	 * Gets the list of item flags to hide certain properties.
	 *
	 * @return the list of hide flags, or an empty list if not set
	 */
	public List<String> getHideFlags() {
		return this.hideFlags == null ? new ArrayList<>() : this.hideFlags;
	}
	
	/**
	 * Sets the list of item flags to hide certain properties.
	 *
	 * @param hideFlags the list of hide flags
	 */
	public void setHideFlags(final List<String> hideFlags) {
		this.hideFlags = hideFlags;
	}
}
