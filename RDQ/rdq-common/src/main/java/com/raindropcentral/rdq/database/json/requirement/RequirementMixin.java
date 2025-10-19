package com.raindropcentral.rdq.database.json.requirement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.raindropcentral.rdq.requirement.*;

/**
 * Jackson mixin for {@link AbstractRequirement} that enables polymorphic
 * deserialization of requirement definitions stored in JSON.
 *
 * <p>This mixin supplies Jackson with the discriminator property used by the
 * RaindropQuests data model. When the {@code type} attribute is encountered,
 * Jackson selects the matching concrete requirement listed in the
 * {@link JsonSubTypes} declaration and instantiates it with the remaining
 * document payload.</p>
 *
 * <h2>Supported requirement keys</h2>
 * <p>The mapping below mirrors the values accepted in quest configuration
 * files:</p>
 * <ul>
 *     <li><b>ITEM</b> – Item stack and quantity checks.</li>
 *     <li><b>CURRENCY</b> – Economy-based currency balance checks.</li>
 *     <li><b>EXPERIENCE_LEVEL</b> – Experience level or point validation.</li>
 *     <li><b>PERMISSION</b> – Permission node assertions.</li>
 *     <li><b>LOCATION</b> – World coordinate and region constraints.</li>
 *     <li><b>CUSTOM</b> – Script-driven requirements.</li>
 *     <li><b>COMPOSITE</b> – Logical composition of child requirements.</li>
 *     <li><b>CHOICE</b> – Alternative requirement paths.</li>
 *     <li><b>TIME_BASED</b> – Time limited objectives.</li>
 *     <li><b>PLAYTIME</b> – Aggregate playtime thresholds.</li>
 * </ul>
 *
 * <h2>Registration</h2>
 * <p>Register the mixin alongside the abstract base type to activate the
 * mapping:</p>
 * <pre>{@code
 * objectMapper.addMixIn(AbstractRequirement.class, RequirementMixin.class);
 * }</pre>
 *
 * <p>The mixin is stateless and may be reused across {@link com.fasterxml.jackson.databind.ObjectMapper}
 * instances. It imposes no threading constraints.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 * @see AbstractRequirement
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