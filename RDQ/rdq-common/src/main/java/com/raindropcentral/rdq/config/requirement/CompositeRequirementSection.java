package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration section for composite requirements.
 *
 * <p>This section handles all configuration options specific to CompositeRequirement,
 * including sub-requirements, logical operators, and minimum required counts.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class CompositeRequirementSection extends AConfigSection {
	
	
	/**
	 * Logical operator for combining sub-requirements (AND, OR, XOR).
	 * YAML key: "operator"
	 */
	private String operator;
	
	/**
	 * Alternative operator field name.
	 * YAML key: "compositeOperator"
	 */
	private String compositeOperator;
	
	/**
	 * List of sub-requirements.
	 * YAML key: "requirements"
	 */
	private List<BaseRequirementSection> requirements;
	
	/**
	 * Alternative: Map-based sub-requirements for named requirements.
	 * YAML key: "subRequirements"
	 */
	private Map<String, BaseRequirementSection> subRequirements;
	
	/**
	 * Minimum number of sub-requirements that must be met.
	 * YAML key: "minimumRequired"
	 */
	private Integer minimumRequired;
	
	/**
	 * Maximum number of sub-requirements that can be met (for XOR logic).
	 * YAML key: "maximumRequired"
	 */
	private Integer maximumRequired;
	
	/**
	 * Whether to allow partial progress on sub-requirements.
	 * YAML key: "allowPartialProgress"
	 */
	private Boolean allowPartialProgress;
	
	/**
	 * Description for this composite requirement.
	 * YAML key: "description"
	 */
	private String description;
	
	/**
	 * Constructs a new CompositeRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public CompositeRequirementSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
	
	/**
	 * Gets description.
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * Gets allowPartialProgress.
	 */
	public Boolean getAllowPartialProgress() {
		return this.allowPartialProgress != null ? this.allowPartialProgress : true;
	}
	
	/**
	 * Gets the logical operator, trying multiple field names.
	 *
	 * @return the operator, defaulting to "AND"
	 */
	public String getOperator() {
		if (
			this.operator != null
		) {
			return this.operator.toUpperCase();
		}
		if (
			this.compositeOperator != null
		) {
			return this.compositeOperator.toUpperCase();
		}
		return "AND";
	}
	
	/**
	 * Gets the list of sub-requirements from all sources.
	 *
	 * @return combined list of all sub-requirements
	 */
	public List<BaseRequirementSection> getCompositeRequirements() {
		List<BaseRequirementSection> requirementList = new ArrayList<>();
		
		if (
			this.requirements != null
		) {
			requirementList.addAll(this.requirements);
		}
		
		if (
			this.subRequirements != null
		) {
			requirementList.addAll(this.subRequirements.values());
		}
		
		return requirementList;
	}
	
	/**
	 * Gets the sub-requirements map.
	 *
	 * @return the map of sub-requirements
	 */
	public Map<String, BaseRequirementSection> getSubRequirements() {
		return this.subRequirements != null ? this.subRequirements : new HashMap<>();
	}
	
	/**
	 * Gets the minimum number of requirements that must be met.
	 *
	 * @return the minimum required, defaulting based on operator
	 */
	public Integer getMinimumRequired() {
		if (
			this.minimumRequired != null
		) {
			return this.minimumRequired;
		}
		
		String op = this.getOperator();
		return switch (op) {
			case "OR", "XOR" -> 1;
			case "AND" -> this.getCompositeRequirements().size();
			default -> 1;
		};
	}
	
	/**
	 * Gets the maximum number of requirements that can be met.
	 *
	 * @return the maximum required
	 */
	public Integer getMaximumRequired() {
		if (
			this.maximumRequired != null
		) {
			return this.maximumRequired;
		}
		
		String op = this.getOperator();
		return switch (op) {
			case "XOR" -> 1;
			default -> this.getCompositeRequirements().size();
		};
	}
}
