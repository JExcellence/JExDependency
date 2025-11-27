package com.raindropcentral.rdq.database.json.reward;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.raindropcentral.rdq.reward.*;

/**
 * Jackson mixin for {@link com.raindropcentral.rdq.reward.AbstractReward} to handle polymorphic deserialization.
 * This class provides type metadata to Jackson's object mapper so that it can resolve concrete
 * {@link com.raindropcentral.rdq.reward.AbstractReward} implementations from the {@code type} discriminator.
 *
 * <p>The mixin exposes the {@code type} property as an existing field, ensuring the mapper can look up
 * the correct reward subtype without requiring explicit annotations on the model classes.</p>
 *
 * <p>Currently supported reward types:</p>
 * <ul>
 *   <li>{@code ITEM} – Item rewards</li>
 *   <li>{@code CURRENCY} – Currency payouts</li>
 *   <li>{@code COMPOSITE} – Composite rewards that group other rewards</li>
 *   <li>{@code EXPERIENCE_LEVEL} – Experience level grants</li>
 *   <li>{@code COMMAND} – Server command executions</li>
 * </ul>
 *
 * <p>Additional reward types can be registered by introducing further {@link JsonSubTypes.Type} entries when the
 * corresponding reward implementations become available.</p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
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