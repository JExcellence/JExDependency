/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.config.perk;

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rdq.config.utility.RewardSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration section for perk definitions.
 *
 * <p>This section handles the complete perk configuration including identifier, type, category,
 * requirements, unlock rewards, and effect configuration. Display name and description keys
 * are automatically generated if not provided.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class PerkSection extends AConfigSection {
	
	@CSIgnore
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	/**
	 * The unique identifier for this perk (e.g., "speed_boost", "flight").
	 * This is used to reference the perk in the database and configuration.
	 */
	private String identifier;
	
	/**
	 * The perk type defining activation behavior.
	 * Valid values: PASSIVE, EVENT_TRIGGERED, COOLDOWN_BASED, PERCENTAGE_BASED
	 * If null, defaults to "PASSIVE".
	 */
	private String perkType;
	
	/**
	 * The thematic category of this perk.
	 * Valid values: COMBAT, MOVEMENT, UTILITY, SURVIVAL, ECONOMY, SOCIAL, COSMETIC, SPECIAL
	 * If null, defaults to "UTILITY".
	 */
	private String category;
	
	/**
	 * Whether this perk is enabled and can be unlocked by players.
	 * If null, defaults to true.
	 */
	private Boolean enabled;
	
	/**
	 * The display order of this perk in UI lists (lower numbers appear first).
	 * If null, defaults to 0.
	 */
	private Integer displayOrder;
	
	/**
	 * The icon configuration for this perk in GUIs.
	 * If null, a default IconSection is provided.
	 */
	private IconSection icon;
	
	/**
	 * Map of requirements that must be met to unlock this perk.
	 * Key is the requirement identifier, value is the requirement configuration.
	 */
	private Map<String, BaseRequirementSection> requirements;
	
	/**
	 * Map of rewards granted when this perk is unlocked.
	 * Key is the reward identifier, value is the reward configuration.
	 */
	private Map<String, RewardSection> unlockRewards;
	
	/**
	 * The effect configuration for this perk.
	 * Defines what happens when the perk is active.
	 */
	private PerkEffectSection effect;
	
	/**
	 * Constructs a new PerkSection with the given evaluation environment.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public PerkSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Called after parsing the configuration fields. Auto-generates i18n keys and validates configuration.
	 *
	 * @param fields the list of fields parsed
	 * @throws Exception if an error occurs during post-processing
	 */
	@Override
	public void afterParsing(final List<Field> fields) throws Exception {
		super.afterParsing(fields);
		
		// Validate identifier
		if (identifier == null || identifier.isEmpty()) {
			throw new IllegalArgumentException("Perk identifier cannot be null or empty");
		}
		
		// Set defaults
		if (perkType == null || perkType.isEmpty()) {
			perkType = "PASSIVE";
			LOGGER.fine("Perk " + identifier + " has no perkType specified, defaulting to PASSIVE");
		}
		
		if (category == null || category.isEmpty()) {
			category = "UTILITY";
			LOGGER.fine("Perk " + identifier + " has no category specified, defaulting to UTILITY");
		}
		
		// Validate perk type
		validatePerkType();
		
		// Validate category
		validateCategory();
		
		// Auto-generate i18n keys for icon if not provided
		if (icon != null) {
			String baseKey = "perk." + identifier;
			
			if (icon.getDisplayNameKey() == null || icon.getDisplayNameKey().equals("not_defined")) {
				icon.setDisplayNameKey(baseKey + ".name");
			}
			
			if (icon.getDescriptionKey() == null || icon.getDescriptionKey().equals("not_defined")) {
				icon.setDescriptionKey(baseKey + ".description");
			}
		}
		
		// Set context for requirements
		if (requirements != null) {
			for (Map.Entry<String, BaseRequirementSection> entry : requirements.entrySet()) {
				BaseRequirementSection requirement = entry.getValue();
				requirement.setContext("perk", identifier, entry.getKey());
			}
		}
		
		// Validate that effect is configured
		if (effect == null) {
			LOGGER.warning("Perk " + identifier + " has no effect configured");
		}
	}
	
	/**
	 * Validates the perk type.
	 */
	private void validatePerkType() {
		String upperType = perkType.toUpperCase();
		switch (upperType) {
			case "PASSIVE", "EVENT_TRIGGERED", "COOLDOWN_BASED", "PERCENTAGE_BASED" -> {
				// Valid perk types
				perkType = upperType;
			}
			default -> {
				LOGGER.warning("Unknown perk type: " + perkType + 
					". Valid types: PASSIVE, EVENT_TRIGGERED, COOLDOWN_BASED, PERCENTAGE_BASED. Defaulting to PASSIVE.");
				perkType = "PASSIVE";
			}
		}
	}
	
	/**
	 * Validates the category.
	 */
	private void validateCategory() {
		String upperCategory = category.toUpperCase();
		switch (upperCategory) {
			case "COMBAT", "MOVEMENT", "UTILITY", "SURVIVAL", "ECONOMY", "SOCIAL", "COSMETIC", "SPECIAL" -> {
				// Valid categories
				category = upperCategory;
			}
			default -> {
				LOGGER.warning("Unknown category: " + category + 
					". Valid categories: COMBAT, MOVEMENT, UTILITY, SURVIVAL, ECONOMY, SOCIAL, COSMETIC, SPECIAL. Defaulting to UTILITY.");
				category = "UTILITY";
			}
		}
	}
	
	// ==================== Getters ====================
	
	/**
	 * Gets identifier.
	 */
	public String getIdentifier() {
		return identifier;
	}
	
	/**
	 * Gets perkType.
	 */
	public String getPerkType() {
		return perkType == null ? "PASSIVE" : perkType;
	}
	
	/**
	 * Gets category.
	 */
	public String getCategory() {
		return category == null ? "UTILITY" : category;
	}
	
	/**
	 * Gets enabled.
	 */
	public Boolean getEnabled() {
		return enabled == null || enabled;
	}
	
	/**
	 * Gets displayOrder.
	 */
	public Integer getDisplayOrder() {
		return displayOrder == null ? 0 : displayOrder;
	}
	
	/**
	 * Gets icon.
	 */
	public IconSection getIcon() {
		return icon == null ? new IconSection(new EvaluationEnvironmentBuilder()) : icon;
	}
	
	/**
	 * Gets requirements.
	 */
	public Map<String, BaseRequirementSection> getRequirements() {
		return requirements == null ? new HashMap<>() : requirements;
	}
	
	/**
	 * Gets unlockRewards.
	 */
	public Map<String, RewardSection> getUnlockRewards() {
		return unlockRewards == null ? new HashMap<>() : unlockRewards;
	}
	
	/**
	 * Gets effect.
	 */
	public PerkEffectSection getEffect() {
		return effect == null ? new PerkEffectSection(new EvaluationEnvironmentBuilder()) : effect;
	}
}
