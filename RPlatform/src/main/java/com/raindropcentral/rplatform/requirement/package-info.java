/**
 * RPlatform Requirement System - Shared foundation for all requirement types.
 * <p>
 * This package provides the core requirement infrastructure that can be used by any plugin
 * in the RaindropCentral ecosystem.
 * </p>
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.rplatform.requirement.Requirement} - Sealed interface for all requirements</li>
 *   <li>{@link com.raindropcentral.rplatform.requirement.AbstractRequirement} - Base class for implementations</li>
 *   <li>{@link com.raindropcentral.rplatform.requirement.RequirementRegistry} - Dynamic type registration</li>
 *   <li>{@link com.raindropcentral.rplatform.requirement.PluginRequirementProvider} - Plugin integration interface</li>
 *   <li>{@link com.raindropcentral.rplatform.requirement.RequirementService} - Caching service for requirement checks</li>
 * </ul>
 *
 * <h2>Database Persistence</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.rplatform.database.converter.RequirementConverter} - JPA converter for single requirements</li>
 *   <li>{@link com.raindropcentral.rplatform.database.converter.RequirementListConverter} - JPA converter for requirement lists</li>
 * </ul>
 *
 * <h2>JSON Serialization</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.rplatform.requirement.json.RequirementParser} - JSON serialization/deserialization</li>
 *   <li>{@link com.raindropcentral.rplatform.requirement.json.RequirementMixin} - Jackson polymorphic type handling</li>
 * </ul>
 *
 * <h2>Built-in Requirement Types</h2>
 * <ul>
 *   <li>ITEM - Item-based requirements</li>
 *   <li>CURRENCY - Economy integration</li>
 *   <li>EXPERIENCE_LEVEL - XP level requirements</li>
 *   <li>PERMISSION - Permission-based requirements</li>
 *   <li>LOCATION - World/region/coordinate requirements</li>
 *   <li>COMPOSITE - AND/OR/MINIMUM combinations</li>
 *   <li>CHOICE - Alternative requirement paths</li>
 *   <li>TIME_BASED - Time-limited requirements</li>
 *   <li>PLAYTIME - Player playtime requirements</li>
 *   <li>PLUGIN - External plugin integration (skills/jobs and other value providers)</li>
 * </ul>
 *
 * <h2>Plugin Extension</h2>
 * <p>
 * Plugins can register their own requirement types by implementing
 * {@link com.raindropcentral.rplatform.requirement.PluginRequirementProvider}
 * and registering with {@link com.raindropcentral.rplatform.requirement.RequirementRegistry}.
 * </p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * public class OneBlockRequirementProvider implements PluginRequirementProvider {
 *     @Override
 *     public String getPluginId() { return "jexoneblock"; }
 *
 *     @Override
 *     public Map<String, Class<? extends AbstractRequirement>> getRequirementTypes() {
 *         return Map.of(
 *             "EVOLUTION_LEVEL", EvolutionLevelRequirement.class,
 *             "BLOCKS_BROKEN", BlocksBrokenRequirement.class
 *         );
 *     }
 * }
 *
 * // Register on plugin enable
 * RequirementRegistry.getInstance().registerProvider(new OneBlockRequirementProvider());
 * }</pre>
 *
 * @see com.raindropcentral.rplatform.requirement.impl
 * @see com.raindropcentral.rplatform.requirement.json
 */
package com.raindropcentral.rplatform.requirement;
