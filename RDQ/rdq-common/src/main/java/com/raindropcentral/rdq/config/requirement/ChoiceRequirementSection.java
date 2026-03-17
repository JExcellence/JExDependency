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

package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration section for choice requirements.
 *
 * <p>This section handles all configuration options specific to ChoiceRequirement,
 * including choice options, selection modes, and choice descriptions.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class ChoiceRequirementSection extends AConfigSection {
	
	// ~~~ CHOICE-SPECIFIC PROPERTIES ~~~
	
	/**
	 * List of choice options.
	 * YAML key: "choices"
	 */
	private List<BaseRequirementSection> choices;
	
	/**
	 * Alternative: Map-based choices for named options.
	 * YAML key: "choiceMap"
	 */
	private Map<String, BaseRequirementSection> choiceMap;
	
	/**
	 * Alternative choice list field name.
	 * YAML key: "choiceList"
	 */
	private List<BaseRequirementSection> choiceList;
	
	/**
	 * Number of choices that must be completed.
	 * YAML key: "minimumRequired"
	 */
	private Integer minimumRequired;
	
	/**
	 * Maximum number of choices that can be completed.
	 * YAML key: "maximumRequired"
	 */
	private Integer maximumRequired;
	
	/**
	 * Description for the choice requirement.
	 * YAML key: "description"
	 */
	private String description;
	
	/**
	 * Alternative description field name.
	 * YAML key: "choiceDescription"
	 */
	private String choiceDescription;
	
	/**
	 * Whether to allow partial progress on choices.
	 * YAML key: "allowPartialProgress"
	 */
	private Boolean allowPartialProgress;
	
	/**
	 * Whether choices are mutually exclusive (completing one prevents others).
	 * YAML key: "mutuallyExclusive"
	 */
	private Boolean mutuallyExclusive;
	
	/**
	 * Whether the player can change their choice after making one.
	 * YAML key: "allowChoiceChange"
	 */
	private Boolean allowChoiceChange;
	
	/**
	 * Constructs a new ChoiceRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public ChoiceRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	// ~~~ GETTERS ~~~
	
	
	/**
	 * Gets allowPartialProgress.
	 */
	public Boolean getAllowPartialProgress() {
		return this.allowPartialProgress != null ? this.allowPartialProgress : true;
	}
	
	/**
	 * Gets mutuallyExclusive.
	 */
	public Boolean getMutuallyExclusive() {
		return this.mutuallyExclusive != null ? this.mutuallyExclusive : false;
	}
	
	/**
	 * Gets allowChoiceChange.
	 */
	public Boolean getAllowChoiceChange() {
		return this.allowChoiceChange != null ? this.allowChoiceChange : true;
	}
	
	/**
	 * Gets the description, trying multiple field names.
	 *
	 * @return the description
	 */
	public String getDescription() {
		if (this.description != null) {
			return this.description;
		}
		if (this.choiceDescription != null) {
			return this.choiceDescription;
		}
		return "";
	}
	
	/**
	 * Gets the complete list of choices from all sources.
	 *
	 * @return combined list of all choices
	 */
	public List<BaseRequirementSection> getChoices() {
		List<BaseRequirementSection> choicesList = new ArrayList<>();
		
		// Add choices from choices list
		if (this.choices != null) {
			choicesList.addAll(this.choices);
		}
		
		// Add choices from choiceList
		if (this.choiceList != null) {
			choicesList.addAll(this.choiceList);
		}
		
		// Add choices from choiceMap
		if (this.choiceMap != null) {
			choicesList.addAll(this.choiceMap.values());
		}
		
		return choicesList;
	}
	
	/**
	 * Gets the choice map.
	 *
	 * @return the map of choices
	 */
	public Map<String, BaseRequirementSection> getChoiceMap() {
		return this.choiceMap != null ? this.choiceMap : new HashMap<>();
	}
	
	/**
	 * Gets the minimum number of choices that must be completed.
	 *
	 * @return the minimum required, defaulting to 1
	 */
	public Integer getMinimumRequired() {
		return this.minimumRequired != null ? this.minimumRequired : 1;
	}
	
	/**
	 * Gets the maximum number of choices that can be completed.
	 *
	 * @return the maximum required, defaulting to all choices
	 */
	public Integer getMaximumRequired() {
		if (this.maximumRequired != null) {
			return this.maximumRequired;
		}
		
		// If mutually exclusive, only one choice can be completed
		if (getMutuallyExclusive()) {
			return 1;
		}
		
		// Otherwise, all choices can be completed
		return getChoices().size();
	}
}
