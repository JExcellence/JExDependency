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
import java.util.stream.IntStream;

/**
 * Represents a requirement that combines multiple sub-requirements using logical operators.
 * <p>
 * The {@code CompositeRequirement} acts as a logical container for multiple {@link AbstractRequirement}
 * instances, supporting both AND and OR operations. This enables the creation of complex, multi-faceted
 * conditions for quests, upgrades, or features with flexible completion criteria.
 * </p>
 * <p>
 * <b>Logical Operators:</b>
 * <ul>
 *   <li><b>AND:</b> All constituent requirements must be met (default behavior).</li>
 *   <li><b>OR:</b> At least one constituent requirement must be met.</li>
 *   <li><b>MINIMUM:</b> At least N constituent requirements must be met (where N is specified by minimumRequired).</li>
 * </ul>
 * </p>
 * <p>
 * <b>Progress Calculation:</b> The progress calculation varies based on the operator:
 * <ul>
 *   <li><b>AND:</b> Average progress of all requirements.</li>
 *   <li><b>OR:</b> Maximum progress among all requirements.</li>
 *   <li><b>MINIMUM:</b> Average progress of the top N requirements with highest progress.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Resource Consumption:</b> Consumption strategy depends on the operator:
 * <ul>
 *   <li><b>AND:</b> Consumes from all requirements.</li>
 *   <li><b>OR:</b> Consumes from the requirement with highest progress.</li>
 *   <li><b>MINIMUM:</b> Consumes from the top N requirements with highest progress.</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public class CompositeRequirement extends AbstractRequirement {
	
	/**
	 * Enumeration of logical operators supported by CompositeRequirement.
	 */
	public enum Operator {
		/**
		 * All requirements must be met.
		 */
		AND,
		
		/**
		 * At least one requirement must be met.
		 */
		OR,
		
		/**
		 * At least N requirements must be met (where N is specified by minimumRequired).
		 */
		MINIMUM
	}
	
	/**
	 * The list of individual requirements that make up this composite requirement.
	 */
	@JsonProperty("requirements")
	private final List<AbstractRequirement> requirements = new ArrayList<>();
	
	/**
	 * The logical operator used to combine the requirements.
	 */
	@JsonProperty("operator")
	private final Operator operator;
	
	/**
	 * The minimum number of requirements that must be met when using MINIMUM operator.
	 * Ignored for AND and OR operators.
	 */
	@JsonProperty("minimumRequired")
	private final int minimumRequired;
	
	/**
	 * Optional description for this composite requirement.
	 */
	@JsonProperty("description")
	private final String description;
	
	/**
	 * Whether to allow partial progress calculation for MINIMUM operator.
	 */
	@JsonProperty("allowPartialProgress")
	private final boolean allowPartialProgress;
	
	/**
	 * Constructs a {@code CompositeRequirement} with AND operator (backward compatibility).
	 *
	 * @param requirements A non-empty list of requirements; all must be met for the composite to be satisfied.
	 *
	 * @throws IllegalArgumentException If the requirements list is empty.
	 */
	public CompositeRequirement(
		@NotNull final List<AbstractRequirement> requirements
	) {
		
		this(
			requirements,
			Operator.AND,
			requirements.size(),
			null,
			true
		);
	}
	
	/**
	 * Constructs a {@code CompositeRequirement} with the specified operator.
	 *
	 * @param requirements A non-empty list of requirements.
	 * @param operator     The logical operator to use.
	 *
	 * @throws IllegalArgumentException If the requirements list is empty.
	 */
	public CompositeRequirement(
		@NotNull final List<AbstractRequirement> requirements,
		@NotNull final Operator operator
	) {
		
		this(
			requirements,
			operator,
			operator == Operator.OR ?
			1 :
			requirements.size(),
			null,
			true
		);
	}
	
	/**
	 * Constructs a {@code CompositeRequirement} with MINIMUM operator and specified minimum count.
	 *
	 * @param requirements    A non-empty list of requirements.
	 * @param minimumRequired The minimum number of requirements that must be met.
	 *
	 * @throws IllegalArgumentException If the requirements list is empty or minimumRequired is invalid.
	 */
	public CompositeRequirement(
		@NotNull final List<AbstractRequirement> requirements,
		final int minimumRequired
	) {
		
		this(
			requirements,
			Operator.MINIMUM,
			minimumRequired,
			null,
			true
		);
	}
	
	/**
	 * Constructs a {@code CompositeRequirement} with full configuration options.
	 *
	 * @param requirements         A non-empty list of requirements.
	 * @param operator             The logical operator to use.
	 * @param minimumRequired      The minimum number of requirements that must be met (for MINIMUM operator).
	 * @param description          Optional description for this composite requirement.
	 * @param allowPartialProgress Whether to allow partial progress calculation.
	 *
	 * @throws IllegalArgumentException If the configuration is invalid.
	 */
	@JsonCreator
	public CompositeRequirement(
		@JsonProperty("requirements") @NotNull final List<AbstractRequirement> requirements,
		@JsonProperty("operator") @NotNull final Operator operator,
		@JsonProperty("minimumRequired") final int minimumRequired,
		@JsonProperty("description") @Nullable final String description,
		@JsonProperty("allowPartialProgress") final boolean allowPartialProgress
	) {
		
		super(Type.COMPOSITE);
		
		if (requirements.isEmpty()) {
			throw new IllegalArgumentException("CompositeRequirement must contain at least one requirement.");
		}
		
		if (minimumRequired < 1) {
			throw new IllegalArgumentException("Minimum required must be at least 1.");
		}
		
		if (minimumRequired > requirements.size()) {
			throw new IllegalArgumentException(
				"Minimum required (" + minimumRequired + ") cannot exceed total requirements (" + requirements.size() + ")."
			);
		}
		
		this.requirements.addAll(requirements);
		this.operator = operator;
		this.minimumRequired = minimumRequired;
		this.description = description;
		this.allowPartialProgress = allowPartialProgress;
	}
	
	/**
	 * Checks if the composite requirement is met based on the configured operator.
	 *
	 * @param player The player to check against for each requirement.
	 *
	 * @return {@code true} if the requirement is met according to the operator logic, {@code false} otherwise.
	 */
	@Override
	public boolean isMet(
		@NotNull final Player player
	) {
		
		return switch (this.operator) {
			case AND -> this.requirements.stream().allMatch(requirement -> requirement.isMet(player));
			case OR -> this.requirements.stream().anyMatch(requirement -> requirement.isMet(player));
			case MINIMUM -> {
				final long completedCount = this.requirements.stream()
				                                             .mapToLong(requirement -> requirement.isMet(player) ?
				                                                                       1 :
				                                                                       0)
				                                             .sum();
				yield completedCount >= this.minimumRequired;
			}
		};
	}
	
	/**
	 * Calculates the overall progress towards fulfilling the composite requirement.
	 * <p>
	 * The calculation method depends on the operator:
	 * <ul>
	 *   <li><b>AND:</b> Average progress of all requirements.</li>
	 *   <li><b>OR:</b> Maximum progress among all requirements.</li>
	 *   <li><b>MINIMUM:</b> Average progress of the top N requirements (or completed count if partial progress disabled).</li>
	 * </ul>
	 * </p>
	 *
	 * @param player The player whose progress is calculated.
	 *
	 * @return A double between 0.0 and 1.0 representing the composite progress.
	 */
	@Override
	public double calculateProgress(
		@NotNull final Player player
	) {
		
		if (this.requirements.isEmpty()) {
			return 0.0;
		}
		
		return switch (this.operator) {
			case AND -> {
				final double totalProgress = this.requirements.stream()
				                                              .mapToDouble(requirement -> requirement.calculateProgress(player))
				                                              .sum();
				yield Math.min(
					1.0,
					totalProgress / this.requirements.size()
				);
			}
			case OR -> this.requirements.stream()
			                            .mapToDouble(requirement -> requirement.calculateProgress(player))
			                            .max()
			                            .orElse(0.0);
			case MINIMUM -> {
				final List<Double> progressValues = this.requirements.stream()
				                                                     .mapToDouble(requirement -> requirement.calculateProgress(player))
				                                                     .boxed()
				                                                     .sorted(Comparator.reverseOrder())
				                                                     .toList();
				
				if (! this.allowPartialProgress) {
					// Only count completed requirements
					final long completedCount = progressValues.stream()
					                                          .mapToLong(progress -> progress >= 1.0 ?
					                                                                 1 :
					                                                                 0)
					                                          .sum();
					yield Math.min(
						1.0,
						(double) completedCount / this.minimumRequired
					);
				}
				
				// Calculate average progress of top N requirements
				final double totalProgress = progressValues.stream()
				                                           .limit(this.minimumRequired)
				                                           .mapToDouble(Double::doubleValue)
				                                           .sum();
				yield Math.min(
					1.0,
					totalProgress / this.minimumRequired
				);
			}
		};
	}
	
	/**
	 * Consumes resources based on the operator strategy.
	 *
	 * @param player The player from whose resources the requirements are consumed.
	 */
	@Override
	public void consume(
		@NotNull final Player player
	) {
		
		switch (this.operator) {
			case AND -> {
				// Consume from all requirements
				this.requirements.forEach(requirement -> requirement.consume(player));
			}
			case OR -> {
				// Consume from the requirement with the highest progress
				this.requirements.stream()
				                 .max(Comparator.comparingDouble(requirement -> requirement.calculateProgress(player)))
				                 .ifPresent(requirement -> requirement.consume(player));
			}
			case MINIMUM -> {
				// Consume from the top N requirements with the highest progress
				this.requirements.stream()
				                 .sorted(Comparator.comparingDouble((AbstractRequirement requirement) -> requirement.calculateProgress(player)).reversed())
				                 .limit(this.minimumRequired)
				                 .forEach(requirement -> requirement.consume(player));
			}
		}
	}
	
	/**
	 * Returns the translation key for this requirement's description.
	 *
	 * @return The language key for this requirement's description.
	 */
	@Override
	@NotNull
	public String getDescriptionKey() {
		
		return "requirement.composite." + this.operator.name().toLowerCase();
	}
	
	/**
	 * Returns a defensive copy of the list of constituent requirements.
	 *
	 * @return A new {@link List} containing the individual requirements.
	 */
	@NotNull
	public List<AbstractRequirement> getRequirements() {
		
		return new ArrayList<>(this.requirements);
	}
	
	/**
	 * Gets the logical operator used by this composite requirement.
	 *
	 * @return The operator.
	 */
	@NotNull
	public Operator getOperator() {
		
		return this.operator;
	}
	
	/**
	 * Gets the minimum number of requirements that must be met.
	 *
	 * @return The minimum required count.
	 */
	public int getMinimumRequired() {
		
		return this.minimumRequired;
	}
	
	/**
	 * Gets the optional description for this composite requirement.
	 *
	 * @return The description, or null if not provided.
	 */
	@Nullable
	public String getDescription() {
		
		return this.description;
	}
	
	/**
	 * Gets whether partial progress is allowed for MINIMUM operator.
	 *
	 * @return True if partial progress is allowed, false otherwise.
	 */
	public boolean isAllowPartialProgress() {
		
		return this.allowPartialProgress;
	}
	
	/**
	 * Gets detailed progress information for each sub-requirement.
	 *
	 * @param player The player whose progress will be calculated.
	 *
	 * @return A list of {@link RequirementProgress} objects containing detailed progress information.
	 */
	@JsonIgnore
	@NotNull
	public List<RequirementProgress> getDetailedProgress(
		@NotNull final Player player
	) {
		
		return IntStream.range(
			                0,
			                this.requirements.size()
		                )
		                .mapToObj(index -> {
			                final AbstractRequirement requirement = this.requirements.get(index);
			                final double              progress    = requirement.calculateProgress(player);
			                final boolean             completed   = requirement.isMet(player);
			                return new RequirementProgress(
				                index,
				                requirement,
				                progress,
				                completed
			                );
		                })
		                .toList();
	}
	
	/**
	 * Gets the requirements that are currently completed for the specified player.
	 *
	 * @param player The player whose completed requirements will be retrieved.
	 *
	 * @return A list of completed requirements.
	 */
	@JsonIgnore
	@NotNull
	public List<AbstractRequirement> getCompletedRequirements(
		@NotNull final Player player
	) {
		
		return this.requirements.stream()
		                        .filter(requirement -> requirement.isMet(player))
		                        .toList();
	}
	
	/**
	 * Gets the requirements sorted by progress (highest first) for the specified player.
	 *
	 * @param player The player whose requirements will be sorted.
	 *
	 * @return A list of requirements sorted by progress.
	 */
	@JsonIgnore
	@NotNull
	public List<AbstractRequirement> getRequirementsByProgress(
		@NotNull final Player player
	) {
		
		return this.requirements.stream()
		                        .sorted(Comparator.comparingDouble((AbstractRequirement requirement) -> requirement.calculateProgress(player)).reversed())
		                        .toList();
	}
	
	/**
	 * Checks if this composite requirement uses AND logic (all requirements must be met).
	 *
	 * @return True if using AND logic, false otherwise.
	 */
	@JsonIgnore
	public boolean isAndLogic() {
		
		return this.operator == Operator.AND;
	}
	
	/**
	 * Checks if this composite requirement uses OR logic (at least one requirement must be met).
	 *
	 * @return True if using OR logic, false otherwise.
	 */
	@JsonIgnore
	public boolean isOrLogic() {
		
		return this.operator == Operator.OR;
	}
	
	/**
	 * Checks if this composite requirement uses MINIMUM logic (at least N requirements must be met).
	 *
	 * @return True if using MINIMUM logic, false otherwise.
	 */
	@JsonIgnore
	public boolean isMinimumLogic() {
		
		return this.operator == Operator.MINIMUM;
	}
	
	/**
	 * Validates the internal state of this composite requirement.
	 *
	 * @throws IllegalStateException If the requirement is in an invalid state.
	 */
	@JsonIgnore
	public void validate() {
		
		if (this.requirements.isEmpty()) {
			throw new IllegalStateException("CompositeRequirement must have at least one requirement.");
		}
		
		if (this.minimumRequired < 1 || this.minimumRequired > this.requirements.size()) {
			throw new IllegalStateException(
				"Invalid minimumRequired: " + this.minimumRequired +
				" (must be between 1 and " + this.requirements.size() + ")."
			);
		}
		
		// Validate each requirement
		for (int i = 0; i < this.requirements.size(); i++) {
			final AbstractRequirement requirement = this.requirements.get(i);
			if (requirement == null) {
				throw new IllegalStateException("Requirement at index " + i + " is null.");
			}
		}
	}
	
	/**
	 * Creates a CompositeRequirement from a string operator.
	 * Useful for configuration parsing.
	 *
	 * @param requirements    The list of requirements.
	 * @param operatorString  The operator as a string ("AND", "OR", "MINIMUM").
	 * @param minimumRequired The minimum required count (for MINIMUM operator).
	 *
	 * @return A new CompositeRequirement instance.
	 *
	 * @throws IllegalArgumentException If the operator string is invalid.
	 */
	@JsonIgnore
	@NotNull
	public static CompositeRequirement fromString(
		@NotNull final List<AbstractRequirement> requirements,
		@NotNull final String operatorString,
		final int minimumRequired
	) {
		
		final Operator operator;
		try {
			operator = Operator.valueOf(operatorString.toUpperCase());
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid operator: " + operatorString + ". Valid operators are: AND, OR, MINIMUM.");
		}
		
		return new CompositeRequirement(
			requirements,
			operator,
			minimumRequired,
			null,
			true
		);
	}
	
	/**
	 * Represents detailed progress information for a single requirement within a CompositeRequirement.
	 */
	public record RequirementProgress(
		int index,
		AbstractRequirement requirement,
		double progress,
		boolean completed
	) {
		
		/**
		 * Constructs a new RequirementProgress instance.
		 *
		 * @param index       The index of the requirement in the requirements list.
		 * @param requirement The requirement.
		 * @param progress    The progress value (0.0 to 1.0).
		 * @param completed   Whether the requirement is completed.
		 */
		public RequirementProgress(
			final int index,
			@NotNull final AbstractRequirement requirement,
			final double progress,
			final boolean completed
		) {
			
			this.index = index;
			this.requirement = requirement;
			this.progress = progress;
			this.completed = completed;
		}
		
		/**
		 * Gets the index of this requirement in the requirements list.
		 *
		 * @return The requirement index.
		 */
		@Override
		public int index() {
			
			return this.index;
		}
		
		/**
		 * Gets the requirement.
		 *
		 * @return The requirement.
		 */
		@Override
		@NotNull
		public AbstractRequirement requirement() {
			
			return this.requirement;
		}
		
		/**
		 * Gets the progress value for this requirement.
		 *
		 * @return The progress value (0.0 to 1.0).
		 */
		@Override
		public double progress() {
			
			return this.progress;
		}
		
		/**
		 * Gets whether this requirement is completed.
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