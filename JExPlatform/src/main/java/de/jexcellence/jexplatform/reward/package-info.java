/**
 * Reward system for granting benefits to players.
 *
 * <p>Provides a sealed {@link de.jexcellence.jexplatform.reward.Reward} interface
 * with polymorphic JSON serialization via Jackson. Rewards are registered in
 * {@link de.jexcellence.jexplatform.reward.RewardRegistry} and granted through
 * {@link de.jexcellence.jexplatform.reward.RewardService} with event, lifecycle,
 * and metrics support.
 *
 * @author JExcellence
 * @since 1.0.0
 */
package de.jexcellence.jexplatform.reward;
