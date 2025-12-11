package com.raindropcentral.rdq2.config.requirement;

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
 * This section handles all configuration options specific to {@link com.raindropcentral.rdq.requirement.ItemRequirement},
 * including whether items should be consumed on completion, if partial progress is allowed, and the full set of required
 * {@link ItemStack item stacks} to validate.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
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
     * @param evaluationEnvironmentBuilder the evaluation environment builder that resolves dynamic values
     */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected ItemRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

    public ItemRequirementSection(
            final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Indicates if required items should be consumed when the associated requirement is completed.
     *
     * @return {@code true} when the items should be consumed; {@code false} otherwise
     */
    public Boolean getConsumeOnComplete() {
        return this.consumeOnComplete != null ? this.consumeOnComplete : true;
    }

    /**
     * Determines whether partial progress can be tracked when not all required items are delivered.
     *
     * @return {@code true} if partial progress is permitted; {@code false} otherwise
     */
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

    /**
     * Provides the configured map of required item sections.
     *
     * @return mutable map of required item identifiers to their {@link ItemStackSection} definitions
     */
    public Map<String, ItemStackSection> getRequiredItems() {
        return this.requiredItems == null ? new HashMap<>() : this.requiredItems;
    }
}