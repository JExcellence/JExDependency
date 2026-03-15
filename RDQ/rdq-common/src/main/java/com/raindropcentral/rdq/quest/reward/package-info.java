/**
 * Quest-specific reward implementations for RPlatform integration.
 *
 * <p>This package contains reward classes that integrate with the quest system:
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.quest.reward.QuestReward} - 
 *         Grants quest start or completion as a reward</li>
 * </ul>
 *
 * <p>These rewards extend {@link com.raindropcentral.rplatform.reward.AbstractReward}
 * and integrate with the quest service layer to grant quest-related rewards. They are
 * registered with RPlatform's RewardRegistry during quest system initialization.
 *
 * <p>Example usage in quest configuration:
 * <pre>
 * rewards:
 *   - type: QUEST
 *     questIdentifier: advanced_zombie_slayer
 *     action: START
 *   - type: CURRENCY
 *     currencyId: coins
 *     amount: 1000
 * </pre>
 *
 * <p>Quest rewards are particularly useful for creating quest chains where completing
 * one quest automatically starts the next quest in the sequence.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
package com.raindropcentral.rdq.quest.reward;
