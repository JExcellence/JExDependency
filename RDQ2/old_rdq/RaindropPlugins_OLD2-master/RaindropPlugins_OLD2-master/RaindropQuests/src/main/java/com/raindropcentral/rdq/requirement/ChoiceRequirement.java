package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Represents a requirement satisfied if at least one of its alternative sub-requirements is met.
 * <p>
 * The {@code ChoiceRequirement} allows for flexible quest or upgrade conditions by holding a list of
 * alternative requirements. A player only needs to fulfill one of these alternatives to satisfy the
 * entire requirement. This enables multiple paths to progression, supporting diverse playstyles and
 * player choice.
 * </p>
 * <p>
 * Progress is calculated based on the alternative with the highest completion percentage, and resource
 * consumption is performed on the alternative with the most progress. This design ensures that the
 * player's most advanced path is always prioritized for both progress tracking and resource deduction.
 * </p>
 *
 * <ul>
 *   <li>Use this requirement to offer players a choice between different objectives.</li>
 *   <li>Each alternative is itself an {@link AbstractRequirement} and can be of any supported type.</li>
 *   <li>Consumption and progress logic always operate on the most advanced alternative.</li>
 *   <li>Supports minimum choice requirements (e.g., complete at least 2 out of 5 choices).</li>
 *   <li>Provides detailed choice tracking and progress information.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public class ChoiceRequirement extends AbstractRequirement {
	
	/**
	 * List of alternative requirements that can satisfy this choice requirement.
	 * <p>
	 * The player only needs to fulfill one of these alternatives for the requirement to be considered met,
	 * unless {@link #minimumChoicesRequired} specifies otherwise.
	 * </p>
	 */
	@JsonProperty("choices")
	private final List<AbstractRequirement> choices = new ArrayList<>();
	
	/**
	 * Minimum number of choices that must be completed to satisfy this requirement.
	 * <p>
	 * Defaults to 1, meaning only one choice needs to be completed. Setting this to a higher value
	 * allows for requirements like "complete at least 2 out of 5 choices".
	 * </p>
	 */
	@JsonProperty("minimumChoicesRequired")
	private final int minimumChoicesRequired;
	
	/**
	 * Optional description for this choice requirement.
	 * <p>
	 * This can be used to provide context about the choices available to the player.
	 * </p>
	 */
	@JsonProperty("description")
	private final String description;
	
	/**
	 * Whether to allow partial progress when multiple choices are required.
	 * <p>
	 * If true, progress is calculated as the average of the top N choices (where N is minimumChoicesRequired).
	 * If false, progress is only counted when choices are fully completed.
	 * </p>
	 */
	@JsonProperty("allowPartialProgress")
	private final boolean allowPartialProgress;
	
	/**
	 * Constructs a new {@code ChoiceRequirement} with the given list of alternative requirements.
	 * Uses default values for minimum choices (1) and allows partial progress.
	 *
	 * @param choices A non-empty list of requirements, any one of which fulfilling will satisfy this ChoiceRequirement.
	 *
	 * @throws IllegalArgumentException If the choices list is empty.
	 */
	public ChoiceRequirement(
		@NotNull final List<AbstractRequirement> choices
	) {
		
		this(
			choices,
			1,
			null,
			true
		);
	}
	
	/**
	 * Constructs a new {@code ChoiceRequirement} with the given list of alternative requirements and minimum choices.
	 *
	 * @param choices                A non-empty list of requirements.
	 * @param minimumChoicesRequired The minimum number of choices that must be completed.
	 *
	 * @throws IllegalArgumentException If the choices list is empty or minimumChoicesRequired is invalid.
	 */
	public ChoiceRequirement(
		@NotNull final List<AbstractRequirement> choices,
		final int minimumChoicesRequired
	) {
		
		this(
			choices,
			minimumChoicesRequired,
			null,
			true
		);
	}
	
	/**
	 * Constructs a new {@code ChoiceRequirement} with full configuration options.
	 *
	 * @param choices                A non-empty list of requirements.
	 * @param minimumChoicesRequired The minimum number of choices that must be completed.
	 * @param description            Optional description for this choice requirement.
	 * @param allowPartialProgress   Whether to allow partial progress calculation.
	 *
	 * @throws IllegalArgumentException If the choices list is empty or minimumChoicesRequired is invalid.
	 */
	@JsonCreator
	public ChoiceRequirement(
		@JsonProperty("choices") @NotNull final List<AbstractRequirement> choices,
		@JsonProperty("minimumChoicesRequired") final int minimumChoicesRequired,
		@JsonProperty("description") @Nullable final String description,
		@JsonProperty("allowPartialProgress") final boolean allowPartialProgress
	) {
		
		super(Type.CHOICE);
		
		if (choices.isEmpty()) {
			throw new IllegalArgumentException("At least one alternative requirement is needed.");
		}
		
		if (minimumChoicesRequired < 1) {
			throw new IllegalArgumentException("Minimum choices required must be at least 1.");
		}
		
		if (minimumChoicesRequired > choices.size()) {
			throw new IllegalArgumentException(
				"Minimum choices required (" + minimumChoicesRequired + ") cannot exceed total choices (" + choices.size() + ")."
			);
		}
		
		this.choices.addAll(choices);
		this.minimumChoicesRequired = minimumChoicesRequired;
		this.description = description;
		this.allowPartialProgress = allowPartialProgress;
	}
	
	/**
	 * Checks if the required number of alternative requirements are met for the specified player.
	 *
	 * @param player The player whose state will be checked.
	 *
	 * @return {@code true} if at least {@link #minimumChoicesRequired} alternatives are met, {@code false} otherwise.
	 */
	@Override
	public boolean isMet(
		@NotNull final Player player
	) {
		
		final long completedChoices = this.choices.stream()
		                                          .mapToLong(requirement -> requirement.isMet(player) ?
		                                                                    1 :
		                                                                    0)
		                                          .sum();
		
		return completedChoices >= this.minimumChoicesRequired;
	}
	
	/**
	 * Calculates the progress towards meeting this ChoiceRequirement for the specified player.
	 * <p>
	 * The calculation method depends on {@link #minimumChoicesRequired} and {@link #allowPartialProgress}:
	 * <ul>
	 *   <li>If only 1 choice is required: Returns the maximum progress among all alternatives.</li>
	 *   <li>If multiple choices are required and partial progress is allowed: Returns the average progress of the top N choices.</li>
	 *   <li>If multiple choices are required and partial progress is disabled: Returns progress only based on completed choices.</li>
	 * </ul>
	 * </p>
	 *
	 * @param player The player whose progress will be calculated.
	 *
	 * @return A double value between 0.0 and 1.0 representing the progress towards completion.
	 */
	@Override
	public double calculateProgress(
		@NotNull final Player player
	) {
		
		if (this.choices.isEmpty()) {
			return 0.0;
		}
		
		// For single choice requirements, return maximum progress
		if (this.minimumChoicesRequired == 1) {
			return this.choices.stream()
			                   .mapToDouble(requirement -> requirement.calculateProgress(player))
			                   .max()
			                   .orElse(0.0);
		}
		
		// For multiple choice requirements
		final List<Double> progressValues = this.choices.stream()
		                                                .mapToDouble(requirement -> requirement.calculateProgress(player))
		                                                .boxed()
		                                                .sorted(Comparator.reverseOrder()) // Sort in descending order
		                                                .toList();
		
		if (! this.allowPartialProgress) {
			// Only count completed choices
			final long completedChoices = progressValues.stream()
			                                            .mapToLong(progress -> progress >= 1.0 ?
			                                                                   1 :
			                                                                   0)
			                                            .sum();
			return Math.min(
				1.0,
				(double) completedChoices / this.minimumChoicesRequired
			);
		}
		
		// Calculate average progress of top N choices
		final double totalProgress = progressValues.stream()
		                                           .limit(this.minimumChoicesRequired)
		                                           .mapToDouble(Double::doubleValue)
		                                           .sum();
		
		return Math.min(
			1.0,
			totalProgress / this.minimumChoicesRequired
		);
	}
	
	/**
	 * Consumes the resources from the alternative requirements based on the consumption strategy.
	 * <p>
	 * The consumption strategy depends on {@link #minimumChoicesRequired}:
	 * <ul>
	 *   <li>If only 1 choice is required: Consumes from the choice with the highest progress.</li>
	 *   <li>If multiple choices are required: Consumes from the top N choices with the highest progress.</li>
	 * </ul>
	 * </p>
	 *
	 * @param player The player from whom resources will be consumed.
	 */
	@Override
	public void consume(
		@NotNull final Player player
	) {
		
		if (this.choices.isEmpty()) {
			return;
		}
		
		// Get choices sorted by progress (highest first)
		final List<AbstractRequirement> sortedChoices = this.choices.stream().sorted(Comparator.comparingDouble((AbstractRequirement requirement) -> requirement.calculateProgress(player)).reversed()).toList();
		
		// Consume from the top N choices
		sortedChoices.stream().limit(this.minimumChoicesRequired).forEach(requirement -> requirement.consume(player));
	}
	
	/**
	 * Returns the translation key for this requirement's description.
	 * <p>
	 * This key can be used for localization and user-facing descriptions.
	 * </p>
	 *
	 * @return The language key for this requirement's description, typically {@code "requirement.choice"}.
	 */
	@Override
	@NotNull
	public String getDescriptionKey() {
		
		return "requirement.choice";
	}
	
	/**
	 * Returns a defensive copy of the list of alternative requirements.
	 * <p>
	 * This prevents external modification of the internal choices list.
	 * </p>
	 *
	 * @return A new {@link List} containing the alternative requirements.
	 */
	@NotNull
	public List<AbstractRequirement> getChoices() {
		
		return new ArrayList<>(this.choices);
	}
	
	/**
	 * Gets the minimum number of choices that must be completed to satisfy this requirement.
	 *
	 * @return The minimum number of choices required.
	 */
	public int getMinimumChoicesRequired() {
		
		return this.minimumChoicesRequired;
	}
	
	/**
	 * Gets the optional description for this choice requirement.
	 *
	 * @return The description, or null if not provided.
	 */
	@Nullable
	public String getDescription() {
		
		return this.description;
	}
	
	/**
	 * Gets whether partial progress is allowed for multiple choice requirements.
	 *
	 * @return True if partial progress is allowed, false otherwise.
	 */
	public boolean isAllowPartialProgress() {
		
		return this.allowPartialProgress;
	}
	
	/**
	 * Gets detailed progress information for each choice for the specified player.
	 * <p>
	 * This method is useful for displaying detailed progress information in GUIs or debug output.
	 * </p>
	 *
	 * @param player The player whose progress will be calculated.
	 *
	 * @return A list of {@link ChoiceProgress} objects containing detailed progress information.
	 */
	@JsonIgnore
	@NotNull
	public List<ChoiceProgress> getDetailedProgress(
		@NotNull final Player player
	) {
		
		return IntStream.range(
			                0,
			                this.choices.size()
		                )
		                .mapToObj(index -> {
			                final AbstractRequirement choice    = this.choices.get(index);
			                final double              progress  = choice.calculateProgress(player);
			                final boolean             completed = choice.isMet(player);
			                return new ChoiceProgress(
				                index,
				                choice,
				                progress,
				                completed
			                );
		                })
		                .toList();
	}
	
	/**
	 * Gets the choice with the highest progress for the specified player.
	 * <p>
	 * This method is useful for determining which choice the player is closest to completing.
	 * </p>
	 *
	 * @param player The player whose progress will be evaluated.
	 *
	 * @return An {@link Optional} containing the choice with the highest progress, or empty if no choices exist.
	 */
	@JsonIgnore
	@NotNull
	public Optional<AbstractRequirement> getBestChoice(
		@NotNull final Player player
	) {
		
		return this.choices.stream()
		                   .max(Comparator.comparingDouble(requirement -> requirement.calculateProgress(player)));
	}
	
	/**
	 * Gets the choices that are currently completed for the specified player.
	 *
	 * @param player The player whose completed choices will be retrieved.
	 *
	 * @return A list of completed choices.
	 */
	@JsonIgnore
	@NotNull
	public List<AbstractRequirement> getCompletedChoices(
		@NotNull final Player player
	) {
		
		return this.choices.stream()
		                   .filter(requirement -> requirement.isMet(player))
		                   .toList();
	}
	
	/**
	 * Checks if this choice requirement is satisfied by a single choice or requires multiple choices.
	 *
	 * @return True if only one choice is required, false if multiple choices are required.
	 */
	@JsonIgnore
	public boolean isSingleChoice() {
		
		return this.minimumChoicesRequired == 1;
	}
	
	/**
	 * Validates the internal state of this choice requirement.
	 * <p>
	 * This method can be used to ensure the requirement is properly configured.
	 * </p>
	 *
	 * @throws IllegalStateException If the requirement is in an invalid state.
	 */
	@JsonIgnore
	public void validate() {
		
		if (this.choices.isEmpty()) {
			throw new IllegalStateException("ChoiceRequirement must have at least one choice.");
		}
		
		if (this.minimumChoicesRequired < 1 || this.minimumChoicesRequired > this.choices.size()) {
			throw new IllegalStateException(
				"Invalid minimumChoicesRequired: " + this.minimumChoicesRequired + " (must be between 1 and " + this.choices.size() + ")."
			);
		}
		
		// Validate each choice
		for (int i = 0; i < this.choices.size(); i++) {
			final AbstractRequirement choice = this.choices.get(i);
			if (choice == null) {
				throw new IllegalStateException("Choice at index " + i + " is null.");
			}
		}
	}
	
	/**
	 * Represents detailed progress information for a single choice within a ChoiceRequirement.
	 */
	public record ChoiceProgress(
		int index,
		AbstractRequirement choice,
		double progress,
		boolean completed
	) {
		
		/**
		 * Constructs a new ChoiceProgress instance.
		 *
		 * @param index     The index of the choice in the choices list.
		 * @param choice    The choice requirement.
		 * @param progress  The progress value (0.0 to 1.0).
		 * @param completed Whether the choice is completed.
		 */
		public ChoiceProgress(
			final int index,
			@NotNull final AbstractRequirement choice,
			final double progress,
			final boolean completed
		) {
			
			this.index = index;
			this.choice = choice;
			this.progress = progress;
			this.completed = completed;
		}
		
		/**
		 * Gets the index of this choice in the choices list.
		 *
		 * @return The choice index.
		 */
		@Override
		public int index() {
			
			return this.index;
		}
		
		/**
		 * Gets the choice requirement.
		 *
		 * @return The choice requirement.
		 */
		@Override
		@NotNull
		public AbstractRequirement choice() {
			
			return this.choice;
		}
		
		/**
		 * Gets the progress value for this choice.
		 *
		 * @return The progress value (0.0 to 1.0).
		 */
		@Override
		public double progress() {
			
			return this.progress;
		}
		
		/**
		 * Gets whether this choice is completed.
		 *
		 * @return True if completed, false otherwise.
		 */
		@Override
		public boolean completed() {
			
			return this.completed;
		}
		
		/**
		 * Gets the progress as a percentage.
		 *
		 * @return The progress percentage (0 to 100).
		 */
		public int getProgressPercentage() {
			
			return (int) (this.progress * 100);
		}
		
	}
	
}