package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.evaluable.section.ItemStackSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration section for item-based requirements.
 * <p>
 * This section handles all configuration options specific to ItemRequirement,
 * including required items, amounts, and consumption settings.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class ItemRequirementSection extends AConfigSection {
	
	@CSAlways
	private Boolean consumeOnComplete;
	
	@CSAlways
	private Map<String, ItemStackSection> requiredItems;

	@CSAlways
	private Boolean allowPartialProgress;
	
	/**
	 * Constructs a new ItemRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public ItemRequirementSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
	public Boolean getConsumeOnComplete() {
		return this.consumeOnComplete != null ? this.consumeOnComplete : true;
	}
	
	public Boolean getAllowPartialProgress() {
		return this.allowPartialProgress != null ? this.allowPartialProgress : false;
	}
	
	/**
	 * Gets the complete list of required items from all sources.
	 *
	 * @return combined list of all required items
	 */
	public List<ItemStack> getRequiredItemsList() {
		List<ItemStack> items = new ArrayList<>();
		
		if (
			this.requiredItems != null
		) {
			items.addAll(
				this.requiredItems.values().stream()
				                  .map(
									  itemSection -> itemSection.asItem().build()
				                  )
				                  .toList()
			);
		}
		
		return items;
	}
	
	public Map<String, ItemStackSection> getRequiredItems() {
		return this.requiredItems == null ? new HashMap<>() : this.requiredItems;
	}
}