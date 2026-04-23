/**
 * Plugin-agnostic reward primitives. {@link de.jexcellence.core.api.reward.Reward}
 * is a sealed hierarchy (xp, currency, item, command, composite, custom);
 * {@link de.jexcellence.core.api.reward.RewardExecutor} is the Bukkit
 * service any plugin can use to grant them.
 */
package de.jexcellence.core.api.reward;
