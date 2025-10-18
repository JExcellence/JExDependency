package com.raindropcentral.rdq.database.json.reward;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.raindropcentral.rdq.reward.*;

/**
 * Jackson mixin for AbstractRequirement to handle polymorphic deserialization.
 * This class provides type information to Jackson's object mapper, allowing it to
 * correctly instantiate the appropriate requirement subclass based on the "type" property.
 *
 * <p>The mixin uses the "type" property to determine which concrete implementation
 * of AbstractRequirement to instantiate during deserialization.</p>
 *
 * <p>Currently supported requirement types:</p>
 * <ul>
 *   <li>ITEM - For item-based requirements</li>
 *   <li>CURRENCY - For currency-based requirements</li>
 *   <li>COMPOSITE - For requirements composed of multiple sub-requirements</li>
 *   <li>CHOICE - For requirements offering multiple options</li>
 * </ul>
 *
 * <p>Additional requirement types are commented out but may be implemented in the future.</p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.EXISTING_PROPERTY,
	property = "type",
	visible = true
)
@JsonSubTypes(
	{
		@JsonSubTypes.Type(
			value = ItemReward.class,
			name = "ITEM"
		),
		@JsonSubTypes.Type(
			value = CurrencyReward.class,
			name = "CURRENCY"
		),
		//@JsonSubTypes.Type(value = AchievementReward.class, name = "ACHIEVEMENT"),
		@JsonSubTypes.Type(
			value = CompositeReward.class,
			name = "COMPOSITE"
		),
		//@JsonSubTypes.Type(value = ChoiceReward.class, name = "CHOICE"),
		@JsonSubTypes.Type(
			value = ExperienceReward.class,
			name = "EXPERIENCE_LEVEL"
		),
		@JsonSubTypes.Type(
			value = CommandReward.class,
			name = "COMMAND"
		)
	}
)
public abstract class RewardMixin {
	// This is a mixin class for Jackson and doesn't contain implementation
}