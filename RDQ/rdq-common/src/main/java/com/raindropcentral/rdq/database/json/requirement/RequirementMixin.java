package com.raindropcentral.rdq.database.json.requirement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.raindropcentral.rdq.requirement.*;

/**
 * Jackson mixin for AbstractRequirement to handle polymorphic deserialization.
 * <p>
 * This class provides type information to Jackson's object mapper, allowing it to
 * correctly instantiate the appropriate requirement subclass based on the "type" property
 * in JSON configuration files. The mixin enables seamless serialization and deserialization
 * of complex requirement hierarchies used throughout the RaindropQuests system.
 * </p>
 *
 * <p><b>Supported Vanilla Requirement Types:</b></p>
 * <ul>
 *   <li><b>ITEM</b> - Item-based requirements (specific items and quantities)</li>
 *   <li><b>CURRENCY</b> - Currency-based requirements (economy integration)</li>
 *   <li><b>EXPERIENCE_LEVEL</b> - Experience level or points requirements</li>
 *   <li><b>PERMISSION</b> - Permission-based requirements (rank integration)</li>
 *   <li><b>LOCATION</b> - Location-based requirements (world, coordinates, regions)</li>
 *   <li><b>CUSTOM</b> - Custom scripted requirements (JavaScript-based logic)</li>
 *   <li><b>COMPOSITE</b> - Composite requirements (AND/OR/MINIMUM logic)</li>
 *   <li><b>CHOICE</b> - Choice requirements (multiple alternative paths)</li>
 *   <li><b>TIME_BASED</b> - Time-limited requirements (timed challenges)</li>
 * </ul>
 *
 * <p><b>Supported Plugin Integration Types:</b></p>
 * <ul>
 *   <li><b>JOBS</b> - Jobs plugin integration requirements</li>
 *   <li><b>SKILLS</b> - Skills plugin integration requirements</li>
 * </ul>
 *
 * <p><b>Future Requirement Types (Planned):</b></p>
 * <ul>
 *   <li><b>PLAYTIME</b> - Playtime-based requirements</li>
 *   <li><b>ACHIEVEMENT</b> - Achievement-based requirements</li>
 *   <li><b>PREVIOUS_LEVEL</b> - Previous rank/level requirements</li>
 *   <li><b>AURA_SKILLS</b> - AuraSkills plugin integration</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // JSON Configuration
 * {
 *   "type": "ITEM",
 *   "requiredItems": [
 *     {
 *       "type": "DIAMOND",
 *       "amount": 10
 *     }
 *   ]
 * }
 *
 * // Will be deserialized as ItemRequirement instance
 * }</pre>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.EXISTING_PROPERTY,
	property = "type",
	visible = true
)
@JsonSubTypes({
	// ~~~ VANILLA REQUIREMENT TYPES ~~~
	
	/**
	 * Item-based requirements for specific items and quantities.
	 * Supports custom metadata, enchantments, and consumption control.
	 */
	@JsonSubTypes.Type(
		value = ItemRequirement.class,
		name = "ITEM"
	),
	
	/**
	 * Currency-based requirements for economy integration.
	 * Supports multiple currencies and asynchronous operations.
	 */
	@JsonSubTypes.Type(
		value = CurrencyRequirement.class,
		name = "CURRENCY"
	),
	
	/**
	 * Experience level or points requirements.
	 * Supports both level-based and points-based checking with consumption.
	 */
	@JsonSubTypes.Type(
		value = ExperienceLevelRequirement.class,
		name = "EXPERIENCE_LEVEL"
	),
	
	/**
	 * Permission-based requirements for rank integration.
	 * Supports multiple permission modes (ALL/ANY/MINIMUM) and negation.
	 */
	@JsonSubTypes.Type(
		value = PermissionRequirement.class,
		name = "PERMISSION"
	),
	
	/**
	 * Location-based requirements for world, coordinates, and regions.
	 * Supports WorldGuard integration and distance-based checking.
	 */
	@JsonSubTypes.Type(
		value = LocationRequirement.class,
		name = "LOCATION"
	),
	
	/**
	 * Custom scripted requirements with JavaScript support.
	 * Allows server owners to create custom logic without code modification.
	 */
	@JsonSubTypes.Type(
		value = CustomRequirement.class,
		name = "CUSTOM"
	),
	
	// ~~~ COMPOSITE REQUIREMENT TYPES ~~~
	
	/**
	 * Composite requirements combining multiple sub-requirements.
	 * Supports AND, OR, and MINIMUM logical operators.
	 */
	@JsonSubTypes.Type(
		value = CompositeRequirement.class,
		name = "COMPOSITE"
	),
	
	/**
	 * Choice requirements offering multiple alternative paths.
	 * Players can choose between different requirement options.
	 */
	@JsonSubTypes.Type(
		value = ChoiceRequirement.class,
		name = "CHOICE"
	),
	
	/**
	 * Time-limited requirements with configurable time windows.
	 * Supports auto-start and flexible time configuration.
	 */
	@JsonSubTypes.Type(
		value = TimedRequirement.class,
		name = "TIME_BASED"
	),
	
	@JsonSubTypes.Type(
		value = PlaytimeRequirement.class,
		name = "PLAYTIME"
	),
	
	// ~~~ PLUGIN INTEGRATION TYPES ~~~
	
/*	*//**
	 * Jobs plugin integration for job levels and experience.
	 * Requires Jobs plugin to be installed and enabled.
	 *//*
	@JsonSubTypes.Type(
		value = JobsRequirement.class,
		name = "JOBS"
	),
	
	*//**
	 * Skills plugin integration for skill levels and experience.
	 * Requires Skills plugin to be installed and enabled.
	 *//*
	@JsonSubTypes.Type(
		value = SkillsRequirement.class,
		name = "SKILLS"
	),
	*/
	// ~~~ FUTURE REQUIREMENT TYPES (COMMENTED OUT) ~~~
	
	// Achievement-based requirements
	// @JsonSubTypes.Type(value = AchievementRequirement.class, name = "ACHIEVEMENT"),
	
	// Previous rank/level requirements
	// @JsonSubTypes.Type(value = PreviousLevelRequirement.class, name = "PREVIOUS_LEVEL"),
	
	// AuraSkills plugin integration
	// @JsonSubTypes.Type(value = AuraSkillsRequirement.class, name = "AURA_SKILLS"),
})
public abstract class RequirementMixin {
	
	/**
	 * This is a mixin class for Jackson polymorphic deserialization.
	 * <p>
	 * Mixin classes in Jackson are used to add annotations to existing classes
	 * without modifying their source code. This class provides the type information
	 * needed for Jackson to correctly deserialize AbstractRequirement subclasses
	 * from JSON configuration files.
	 * </p>
	 *
	 * <p><b>How it works:</b></p>
	 * <ol>
	 *   <li>Jackson reads the "type" property from JSON</li>
	 *   <li>Matches it against the @JsonSubTypes.Type annotations</li>
	 *   <li>Instantiates the corresponding requirement class</li>
	 *   <li>Deserializes the remaining properties into that instance</li>
	 * </ol>
	 *
	 * <p><b>Registration:</b></p>
	 * <p>This mixin must be registered with Jackson's ObjectMapper:</p>
	 * <pre>{@code
	 * objectMapper.addMixIn(AbstractRequirement.class, RequirementMixin.class);
	 * }</pre>
	 */
	
	// No implementation needed - this is purely for Jackson annotations
}