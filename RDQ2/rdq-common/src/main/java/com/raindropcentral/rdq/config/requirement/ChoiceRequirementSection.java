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
 * <p>
 * This section handles configuration options specific to {@code ChoiceRequirement},
 * including the available choice options, selection boundaries, and human readable
 * descriptions that can be supplied in multiple field aliases for backwards
 * compatibility.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
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
         * Constructs a new ChoiceRequirementSection bound to the provided evaluation
         * environment builder so expressions inside choice requirements can be
         * evaluated consistently.
         *
         * @param evaluationEnvironmentBuilder the evaluation environment builder used
         *                                     for requirement evaluation
         */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected ChoiceRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

        public ChoiceRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
                super(evaluationEnvironmentBuilder);
        }

        // ~~~ GETTERS ~~~

        /**
         * Determines whether partial progress across multiple choice options is
         * permitted.
         *
         * @return {@code true} when unspecified or explicitly enabled, otherwise
         * {@code false}
         */
        public Boolean getAllowPartialProgress() {
                return this.allowPartialProgress != null ? this.allowPartialProgress : true;
        }

        /**
         * Determines if completing one choice prevents the remaining choices from being
         * completed.
         *
         * @return {@code true} when mutually exclusive choices are configured,
         * {@code false} otherwise
         */
        public Boolean getMutuallyExclusive() {
                return this.mutuallyExclusive != null ? this.mutuallyExclusive : false;
        }

        /**
         * Determines if the player can change their selection after making an initial
         * choice.
         *
         * @return {@code true} when unspecified or explicitly enabled, otherwise
         * {@code false}
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
         * Aggregates the complete list of configured choices, merging list-based,
         * alternate list, and map based representations into a single collection.
         *
         * @return combined list of all available choices
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
         * Obtains the map of named choices when provided in configuration.
         *
         * @return a mutable map containing named choices, or an empty map when not
         * configured
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
