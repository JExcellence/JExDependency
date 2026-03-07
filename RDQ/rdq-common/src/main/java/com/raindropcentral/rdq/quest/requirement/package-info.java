/**
 * Quest-specific requirement implementations for RPlatform integration.
 * <p>
 * This package contains requirement classes that integrate with the quest system:
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.quest.requirement.QuestCompletionRequirement} - 
 *         Checks if a player has completed a specific quest</li>
 *     <li>{@link com.raindropcentral.rdq.quest.requirement.QuestTaskCompletionRequirement} - 
 *         Checks if a player has completed a specific quest task</li>
 * </ul>
 * </p>
 * <p>
 * These requirements extend {@link com.raindropcentral.rplatform.requirement.AbstractRequirement}
 * and integrate with the quest repository layer to check player progress. They are registered
 * with RPlatform's RequirementRegistry during quest system initialization.
 * </p>
 * <p>
 * Example usage in quest configuration:
 * <pre>
 * requirements:
 *   - type: QUEST_COMPLETION
 *     questIdentifier: zombie_slayer
 *     minCompletions: 1
 *   - type: QUEST_TASK_COMPLETION
 *     questIdentifier: advanced_quest
 *     taskIdentifier: collect_items
 * </pre>
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
package com.raindropcentral.rdq.quest.requirement;
