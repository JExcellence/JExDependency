/**
 * Comprehensive reward system for granting rewards to players.
 * <p>
 * The reward system provides a flexible, event-driven framework for managing and granting
 * various types of rewards including items, currency, experience, commands, and more.
 * </p>
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.rplatform.reward.RewardService} - Main service for granting rewards</li>
 *   <li>{@link com.raindropcentral.rplatform.reward.RewardRegistry} - Central registry for reward types</li>
 *   <li>{@link com.raindropcentral.rplatform.reward.AbstractReward} - Base class for all rewards</li>
 *   <li>{@link com.raindropcentral.rplatform.reward.config.RewardBuilder} - Fluent API for creating rewards</li>
 * </ul>
 *
 * <h2>Reward Types</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.rplatform.reward.impl.ItemReward} - Give items to players</li>
 *   <li>{@link com.raindropcentral.rplatform.reward.impl.CurrencyReward} - Grant currency via Vault</li>
 *   <li>{@link com.raindropcentral.rplatform.reward.impl.ExperienceReward} - Grant XP points or levels</li>
 *   <li>{@link com.raindropcentral.rplatform.reward.impl.CommandReward} - Execute commands</li>
 *   <li>{@link com.raindropcentral.rplatform.reward.impl.CompositeReward} - Grant multiple rewards</li>
 *   <li>{@link com.raindropcentral.rplatform.reward.impl.ChoiceReward} - Player chooses rewards</li>
 *   <li>{@link com.raindropcentral.rplatform.reward.impl.PermissionReward} - Grant permissions</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a reward
 * ItemReward reward = RewardBuilder.item()
 *     .item(new ItemStack(Material.DIAMOND, 10))
 *     .build();
 *
 * // Grant the reward
 * RewardService.getInstance().grant(player, reward)
 *     .thenAccept(success -> {
 *         if (success) {
 *             player.sendMessage("Reward granted!");
 *         }
 *     });
 * }</pre>
 *
 * @see com.raindropcentral.rplatform.reward.RewardService
 * @see com.raindropcentral.rplatform.reward.config.RewardBuilder
 * @since 1.0.0
 */
package com.raindropcentral.rplatform.reward;
