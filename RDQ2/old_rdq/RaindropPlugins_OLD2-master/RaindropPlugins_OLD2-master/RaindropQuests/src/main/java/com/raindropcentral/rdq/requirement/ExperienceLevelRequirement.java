package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Requirement that checks if a player has a minimum experience level or experience points.
 * <p>
 * The {@code ExperienceLevelRequirement} is satisfied when the player has at least the specified
 * experience level or experience points. When consumed, the required amount is subtracted from
 * the player's experience. This requirement is useful for quests, upgrades, or features that
 * require players to reach or spend a certain amount of experience.
 * </p>
 *
 * <ul>
 *   <li>Supports both experience levels and experience points.</li>
 *   <li>Progress is calculated as the ratio of the player's current experience to the required amount.</li>
 *   <li>Consumption can subtract levels or experience points based on configuration.</li>
 *   <li>Supports optional consumption (checking without consuming).</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public class ExperienceLevelRequirement extends AbstractRequirement {
	
	/**
	 * Enumeration of experience types supported by this requirement.
	 */
	public enum ExperienceType {
		/**
		 * Experience levels (default).
		 */
		LEVEL,
		
		/**
		 * Total experience points.
		 */
		POINTS
	}
	
	/**
	 * The required experience level or points.
	 */
	@JsonProperty("requiredLevel")
	private final int requiredLevel;
	
	/**
	 * The type of experience requirement (LEVEL or POINTS).
	 */
	@JsonProperty("experienceType")
	private final ExperienceType experienceType;
	
	/**
	 * Whether this requirement should consume experience when completed.
	 */
	@JsonProperty("consumeOnComplete")
	private final boolean consumeOnComplete;
	
	/**
	 * Optional description for this experience requirement.
	 */
	@JsonProperty("description")
	private final String description;
	
	/**
	 * Constructs an {@code ExperienceLevelRequirement} with the specified required level.
	 * Uses default values: LEVEL type and consumption enabled.
	 *
	 * @param requiredLevel The minimum experience level required.
	 */
	public ExperienceLevelRequirement(
		final int requiredLevel
	) {
		this(requiredLevel, ExperienceType.LEVEL, true, null);
	}
	
	/**
	 * Constructs an {@code ExperienceLevelRequirement} with the specified level and type.
	 *
	 * @param requiredLevel The minimum experience level or points required.
	 * @param experienceType The type of experience requirement.
	 */
	public ExperienceLevelRequirement(
		final int requiredLevel,
		@NotNull final ExperienceType experienceType
	) {
		this(requiredLevel, experienceType, true, null);
	}
	
	/**
	 * Constructs an {@code ExperienceLevelRequirement} with full configuration options.
	 *
	 * @param requiredLevel The minimum experience level or points required.
	 * @param experienceType The type of experience requirement.
	 * @param consumeOnComplete Whether to consume experience when the requirement is met.
	 * @param description Optional description for this requirement.
	 */
	@JsonCreator
	public ExperienceLevelRequirement(
		@JsonProperty("requiredLevel") final int requiredLevel,
		@JsonProperty("experienceType") @Nullable final ExperienceType experienceType,
		@JsonProperty("consumeOnComplete") @Nullable final Boolean consumeOnComplete,
		@JsonProperty("description") @Nullable final String description
	) {
		super(Type.EXPERIENCE_LEVEL);
		
		if (requiredLevel < 0) {
			throw new IllegalArgumentException("Required level cannot be negative: " + requiredLevel);
		}
		
		this.requiredLevel = requiredLevel;
		this.experienceType = experienceType != null ? experienceType : ExperienceType.LEVEL;
		this.consumeOnComplete = consumeOnComplete != null ? consumeOnComplete : true;
		this.description = description;
	}
	
	/**
	 * Checks if the player has at least the required experience level or points.
	 *
	 * @param player The player whose experience will be checked.
	 * @return {@code true} if the player meets the experience requirement; {@code false} otherwise.
	 */
	@Override
	public boolean isMet(
		@NotNull final Player player
	) {
		return switch (this.experienceType) {
			case LEVEL -> player.getLevel() >= this.requiredLevel;
			case POINTS -> player.getTotalExperience() >= this.requiredLevel;
		};
	}
	
	/**
	 * Calculates the progress towards fulfilling the experience requirement.
	 * <p>
	 * Progress is determined as the ratio between the player's current experience and the required amount,
	 * clamped between 0.0 and 1.0. If no experience is required, progress is considered complete ({@code 1.0}).
	 * </p>
	 *
	 * @param player The player whose experience will be evaluated.
	 * @return A double between 0.0 and 1.0 representing progress.
	 */
	@Override
	public double calculateProgress(
		@NotNull final Player player
	) {
		if (this.requiredLevel <= 0) {
			return 1.0;
		}
		
		final double currentAmount = switch (this.experienceType) {
			case LEVEL -> player.getLevel();
			case POINTS -> player.getTotalExperience();
		};
		
		final double progress = currentAmount / this.requiredLevel;
		return Math.max(0.0, Math.min(1.0, progress));
	}
	
	/**
	 * Consumes the required experience from the player if consumption is enabled.
	 * <p>
	 * This method deducts the specified amount from the player's experience,
	 * but only if the player has enough and consumption is enabled.
	 * </p>
	 *
	 * @param player The player from whose experience the amount will be consumed.
	 */
	@Override
	public void consume(
		@NotNull final Player player
	) {
		if (!this.consumeOnComplete) {
			return; // Consumption disabled
		}
		
		switch (this.experienceType) {
			case LEVEL -> {
				if (player.getLevel() >= this.requiredLevel) {
					player.setLevel(player.getLevel() - this.requiredLevel);
				}
			}
			case POINTS -> {
				if (player.getTotalExperience() >= this.requiredLevel) {
					// Calculate new total experience
					final int newTotalExp = player.getTotalExperience() - this.requiredLevel;
					
					// Reset player experience and set new total
					player.setTotalExperience(0);
					player.setLevel(0);
					player.setExp(0);
					
					// Give back the remaining experience
					if (newTotalExp > 0) {
						player.giveExp(newTotalExp);
					}
				}
			}
		}
	}
	
	/**
	 * Returns the translation key for this requirement's description.
	 * <p>
	 * The key varies based on the experience type for more specific localization.
	 * </p>
	 *
	 * @return The language key for this requirement's description.
	 */
	@Override
	@NotNull
	public String getDescriptionKey() {
		return switch (this.experienceType) {
			case LEVEL -> "requirement.experience_level";
			case POINTS -> "requirement.experience_points";
		};
	}
	
	/**
	 * Returns the required experience level or points for this requirement.
	 *
	 * @return The required experience amount.
	 */
	public int getRequiredLevel() {
		return this.requiredLevel;
	}
	
	/**
	 * Gets the experience type for this requirement.
	 *
	 * @return The experience type (LEVEL or POINTS).
	 */
	@NotNull
	public ExperienceType getExperienceType() {
		return this.experienceType;
	}
	
	/**
	 * Gets whether this requirement consumes experience when completed.
	 *
	 * @return True if experience is consumed, false otherwise.
	 */
	public boolean isConsumeOnComplete() {
		return this.consumeOnComplete;
	}
	
	/**
	 * Gets the optional description for this experience requirement.
	 *
	 * @return The description, or null if not provided.
	 */
	@Nullable
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * Gets the current experience amount for the player based on the requirement type.
	 *
	 * @param player The player whose experience will be retrieved.
	 * @return The current experience level or points.
	 */
	@JsonIgnore
	public int getCurrentExperience(
		@NotNull final Player player
	) {
		return switch (this.experienceType) {
			case LEVEL -> player.getLevel();
			case POINTS -> player.getTotalExperience();
		};
	}
	
	/**
	 * Gets the shortage amount (how much more experience is needed).
	 *
	 * @param player The player whose shortage will be calculated.
	 * @return The shortage amount, or 0 if requirement is met.
	 */
	@JsonIgnore
	public int getShortage(
		@NotNull final Player player
	) {
		final int current = this.getCurrentExperience(player);
		return Math.max(0, this.requiredLevel - current);
	}
	
	/**
	 * Checks if this requirement uses experience levels.
	 *
	 * @return True if using levels, false if using points.
	 */
	@JsonIgnore
	public boolean isLevelBased() {
		return this.experienceType == ExperienceType.LEVEL;
	}
	
	/**
	 * Checks if this requirement uses experience points.
	 *
	 * @return True if using points, false if using levels.
	 */
	@JsonIgnore
	public boolean isPointsBased() {
		return this.experienceType == ExperienceType.POINTS;
	}
	
	/**
	 * Validates the internal state of this experience requirement.
	 *
	 * @throws IllegalStateException If the requirement is in an invalid state.
	 */
	@JsonIgnore
	public void validate() {
		if (this.requiredLevel < 0) {
			throw new IllegalStateException("Required level cannot be negative: " + this.requiredLevel);
		}
		
		if (this.experienceType == null) {
			throw new IllegalStateException("Experience type cannot be null");
		}
	}
	
	/**
	 * Creates an ExperienceLevelRequirement from a string experience type.
	 * Useful for configuration parsing.
	 *
	 * @param requiredLevel The required experience amount.
	 * @param experienceTypeString The experience type as a string ("LEVEL" or "POINTS").
	 * @param consumeOnComplete Whether to consume experience.
	 * @return A new ExperienceLevelRequirement instance.
	 * @throws IllegalArgumentException If the experience type string is invalid.
	 */
	@JsonIgnore
	@NotNull
	public static ExperienceLevelRequirement fromString(
		final int requiredLevel,
		@NotNull final String experienceTypeString,
		final boolean consumeOnComplete
	) {
		final ExperienceType experienceType;
		try {
			experienceType = ExperienceType.valueOf(experienceTypeString.toUpperCase());
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid experience type: " + experienceTypeString + ". Valid types are: LEVEL, POINTS.");
		}
		
		return new ExperienceLevelRequirement(requiredLevel, experienceType, consumeOnComplete, null);
	}
}