/**
 * Configuration and factory utilities for the RPlatform requirement system.
 * <p>
 * This package provides tools for creating requirements from configuration data:
 * </p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.rplatform.requirement.config.RequirementFactory} - Creates requirements from config maps</li>
 *   <li>{@link com.raindropcentral.rplatform.requirement.config.RequirementBuilder} - Fluent builder API for requirements</li>
 *   <li>{@link com.raindropcentral.rplatform.requirement.config.RequirementSectionAdapter} - Interface for custom config adapters</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <p><b>Using RequirementBuilder:</b></p>
 * <pre>{@code
 * // Create an item requirement
 * AbstractRequirement itemReq = RequirementBuilder.item()
 *     .addItem(new ItemStack(Material.DIAMOND, 10))
 *     .consumeOnComplete(true)
 *     .build();
 *
 * // Create a composite requirement
 * AbstractRequirement composite = RequirementBuilder.composite()
 *     .add(RequirementBuilder.experience().level(30).build())
 *     .add(RequirementBuilder.permission().permission("rank.vip").build())
 *     .and()
 *     .build();
 * }</pre>
 *
 * <p><b>Using RequirementFactory:</b></p>
 * <pre>{@code
 * // Create from a config map
 * Map<String, Object> config = Map.of(
 *     "type", "EXPERIENCE_LEVEL",
 *     "requiredLevel", 30,
 *     "consumeOnComplete", true
 * );
 * AbstractRequirement req = RequirementFactory.getInstance().fromMap(config);
 *
 * // Register a custom converter for plugin-specific types
 * RequirementFactory.getInstance().registerConverter("EVOLUTION_LEVEL", config -> {
 *     int level = ((Number) config.get("level")).intValue();
 *     return new EvolutionLevelRequirement(level);
 * });
 * }</pre>
 *
 * @see com.raindropcentral.rplatform.requirement.RequirementRegistry
 */
package com.raindropcentral.rplatform.requirement.config;
