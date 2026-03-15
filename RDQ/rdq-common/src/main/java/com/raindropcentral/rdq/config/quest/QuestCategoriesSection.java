package com.raindropcentral.rdq.config.quest;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for quest categories.
 *
 * <p>This section contains a map of all quest categories loaded from the categories.yml file.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCategoriesSection extends AConfigSection {
	
	private Map<String, QuestCategorySection> categories;
	
	/**
	 * Constructs a new QuestCategoriesSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public QuestCategoriesSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Gets the categories map.
	 *
	 * @return the categories map, never null
	 */
	public Map<String, QuestCategorySection> getCategories() {
		return categories != null ? categories : new HashMap<>();
	}
	
	/**
	 * Sets the categories map.
	 *
	 * @param categories the categories map
	 */
	public void setCategories(final Map<String, QuestCategorySection> categories) {
		this.categories = categories;
	}
}
