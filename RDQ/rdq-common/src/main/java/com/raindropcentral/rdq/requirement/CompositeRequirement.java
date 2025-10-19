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
 * instances, supporting AND, OR, and MINIMUM operations. This enables the creation of complex, multi-faceted
 * conditions for quests, upgrades, or features with flexible completion criteria.
 * </p>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since 1.0.0
 */
public final class CompositeRequirement extends AbstractRequirement {

    /**
     * Supported logical operators that can be applied when evaluating the composite requirement.
     */
    public enum Operator {
        AND,
        OR,
        MINIMUM
    }

    @JsonProperty("requirements")
    private final List<AbstractRequirement> requirements;

    @JsonProperty("operator")
    private final Operator operator;

    @JsonProperty("minimumRequired")
    private final int minimumRequired;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("allowPartialProgress")
    private final boolean allowPartialProgress;

    /**
     * Creates a composite requirement that requires all supplied requirements to be satisfied.
     *
     * @param requirements the list of requirements that must all be met
     */
    public CompositeRequirement(final @NotNull List<AbstractRequirement> requirements) {
        this(requirements, Operator.AND, requirements.size(), null, true);
    }

    /**
     * Creates a composite requirement with the provided operator.
     *
     * @param requirements the list of requirements that will be evaluated
     * @param operator     the logical operator that determines how completion is calculated
     */
    public CompositeRequirement(
            final @NotNull List<AbstractRequirement> requirements,
            final @NotNull Operator operator
    ) {
        this(requirements, operator, operator == Operator.OR ? 1 : requirements.size(), null, true);
    }

    /**
     * Creates a composite requirement that requires a specific number of requirements to be completed.
     *
     * @param requirements    the list of requirements considered for completion
     * @param minimumRequired the minimum number of requirements that must be met
     */
    public CompositeRequirement(
            final @NotNull List<AbstractRequirement> requirements,
            final int minimumRequired
    ) {
        this(requirements, Operator.MINIMUM, minimumRequired, null, true);
    }

    /**
     * Creates a composite requirement from its serialized form.
     *
     * @param requirements         the requirements included in this composite structure
     * @param operator             the operator used to evaluate completion, defaults to {@link Operator#AND} when {@code null}
     * @param minimumRequired      the minimum requirements that must be completed when {@link Operator#MINIMUM} is used
     * @param description          an optional description for the composite requirement
     * @param allowPartialProgress whether partial progress counts towards the minimum requirement
     */
    @JsonCreator
    public CompositeRequirement(
            @JsonProperty("requirements") final @NotNull List<AbstractRequirement> requirements,
            @JsonProperty("operator") final @Nullable Operator operator,
            @JsonProperty("minimumRequired") final int minimumRequired,
            @JsonProperty("description") final @Nullable String description,
            @JsonProperty("allowPartialProgress") final @Nullable Boolean allowPartialProgress
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
                    "Minimum required (" + minimumRequired + ") cannot exceed total requirements (" +
                    requirements.size() + ")."
            );
        }

        this.requirements = new ArrayList<>(requirements);
        this.operator = operator != null ? operator : Operator.AND;
        this.minimumRequired = minimumRequired;
        this.description = description;
        this.allowPartialProgress = allowPartialProgress != null ? allowPartialProgress : true;
    }

    /**
     * Determines whether the player meets the composite requirement based on the configured operator.
     *
     * @param player the player whose progress is evaluated
     * @return {@code true} if the player satisfies the requirement, {@code false} otherwise
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        return switch (this.operator) {
            case AND -> this.requirements.stream().allMatch(req -> req.isMet(player));
            case OR -> this.requirements.stream().anyMatch(req -> req.isMet(player));
            case MINIMUM -> {
                final long completedCount = this.requirements.stream()
                        .filter(req -> req.isMet(player))
                        .count();
                yield completedCount >= this.minimumRequired;
            }
        };
    }

    /**
     * Calculates the player's progress against this composite requirement.
     *
     * @param player the player whose progress is being calculated
     * @return a value between {@code 0.0} and {@code 1.0} representing overall completion
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.requirements.isEmpty()) {
            return 0.0;
        }

        return switch (this.operator) {
            case AND -> {
                final double totalProgress = this.requirements.stream()
                        .mapToDouble(req -> req.calculateProgress(player))
                        .sum();
                yield Math.min(1.0, totalProgress / this.requirements.size());
            }
            case OR -> this.requirements.stream()
                    .mapToDouble(req -> req.calculateProgress(player))
                    .max()
                    .orElse(0.0);
            case MINIMUM -> {
                final List<Double> progressValues = this.requirements.stream()
                        .mapToDouble(req -> req.calculateProgress(player))
                        .boxed()
                        .sorted(Comparator.reverseOrder())
                        .toList();

                if (!this.allowPartialProgress) {
                    final long completedCount = progressValues.stream()
                            .filter(progress -> progress >= 1.0)
                            .count();
                    yield Math.min(1.0, (double) completedCount / this.minimumRequired);
                }

                final double totalProgress = progressValues.stream()
                        .limit(this.minimumRequired)
                        .mapToDouble(Double::doubleValue)
                        .sum();
                yield Math.min(1.0, totalProgress / this.minimumRequired);
            }
        };
    }

    /**
     * Consumes the progress recorded for the player based on the operator configuration.
     *
     * @param player the player whose progress should be consumed
     */
    @Override
    public void consume(final @NotNull Player player) {
        switch (this.operator) {
            case AND -> this.requirements.forEach(req -> req.consume(player));
            case OR -> this.requirements.stream()
                    .max(Comparator.comparingDouble(req -> req.calculateProgress(player)))
                    .ifPresent(req -> req.consume(player));
            case MINIMUM -> this.requirements.stream()
                    .sorted(Comparator.comparingDouble(
                            (AbstractRequirement req) -> req.calculateProgress(player)
                    ).reversed())
                    .limit(this.minimumRequired)
                    .forEach(req -> req.consume(player));
        }
    }

    /**
     * Provides the translation key representing this composite requirement.
     *
     * @return the translation key used for localized descriptions
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.composite." + this.operator.name().toLowerCase();
    }

    /**
     * Retrieves a copy of the child requirements contained within this composite requirement.
     *
     * @return an immutable copy of the requirement list
     */
    @NotNull
    public List<AbstractRequirement> getRequirements() {
        return new ArrayList<>(this.requirements);
    }

    /**
     * Gets the operator applied when evaluating the composite requirement.
     *
     * @return the operator for this composite
     */
    @NotNull
    public Operator getOperator() {
        return this.operator;
    }

    /**
     * Returns the minimum number of requirements that must be satisfied.
     *
     * @return the required number of completed sub-requirements
     */
    public int getMinimumRequired() {
        return this.minimumRequired;
    }

    /**
     * Retrieves the optional description associated with this composite requirement.
     *
     * @return the description or {@code null} if none is defined
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Indicates whether partial progress counts towards meeting the minimum requirement threshold.
     *
     * @return {@code true} if partial progress is allowed, {@code false} otherwise
     */
    public boolean isAllowPartialProgress() {
        return this.allowPartialProgress;
    }

    /**
     * Generates a detailed view of each requirement's progress for the specified player.
     *
     * @param player the player whose detailed progress should be captured
     * @return a list of progress snapshots ordered by the original requirement indices
     */
    @JsonIgnore
    @NotNull
    public List<RequirementProgress> getDetailedProgress(final @NotNull Player player) {
        return IntStream.range(0, this.requirements.size())
                .mapToObj(index -> {
                    final AbstractRequirement requirement = this.requirements.get(index);
                    final double progress = requirement.calculateProgress(player);
                    final boolean completed = requirement.isMet(player);
                    return new RequirementProgress(index, requirement, progress, completed);
                })
                .toList();
    }

    /**
     * Retrieves the requirements that the player has already satisfied.
     *
     * @param player the player whose completion state is evaluated
     * @return a list of completed requirements
     */
    @JsonIgnore
    @NotNull
    public List<AbstractRequirement> getCompletedRequirements(final @NotNull Player player) {
        return this.requirements.stream()
                .filter(req -> req.isMet(player))
                .toList();
    }

    /**
     * Returns the requirements sorted by their progress for the provided player in descending order.
     *
     * @param player the player whose progress ordering is evaluated
     * @return a list of requirements sorted by progress
     */
    @JsonIgnore
    @NotNull
    public List<AbstractRequirement> getRequirementsByProgress(final @NotNull Player player) {
        return this.requirements.stream()
                .sorted(Comparator.comparingDouble(
                        (AbstractRequirement req) -> req.calculateProgress(player)
                ).reversed())
                .toList();
    }

    /**
     * Determines whether this composite requirement evaluates using logical AND semantics.
     *
     * @return {@code true} when the operator is {@link Operator#AND}, otherwise {@code false}
     */
    @JsonIgnore
    public boolean isAndLogic() {
        return this.operator == Operator.AND;
    }

    /**
     * Determines whether this composite requirement evaluates using logical OR semantics.
     *
     * @return {@code true} when the operator is {@link Operator#OR}, otherwise {@code false}
     */
    @JsonIgnore
    public boolean isOrLogic() {
        return this.operator == Operator.OR;
    }

    /**
     * Determines whether this composite requirement evaluates using minimum completion semantics.
     *
     * @return {@code true} when the operator is {@link Operator#MINIMUM}, otherwise {@code false}
     */
    @JsonIgnore
    public boolean isMinimumLogic() {
        return this.operator == Operator.MINIMUM;
    }

    /**
     * Validates the internal configuration of this composite requirement.
     *
     * @throws IllegalStateException when the configuration violates logical constraints
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

        for (int i = 0; i < this.requirements.size(); i++) {
            final AbstractRequirement requirement = this.requirements.get(i);
            if (requirement == null) {
                throw new IllegalStateException("Requirement at index " + i + " is null.");
            }
        }
    }

    /**
     * Builds a composite requirement from textual operator input.
     *
     * @param requirements    the requirements to include in the composite
     * @param operatorString  the textual representation of the operator
     * @param minimumRequired the number of requirements that must be completed for {@code MINIMUM} logic
     * @return a newly constructed {@link CompositeRequirement}
     * @throws IllegalArgumentException when the operator string is invalid
     */
    @JsonIgnore
    @NotNull
    public static CompositeRequirement fromString(
            final @NotNull List<AbstractRequirement> requirements,
            final @NotNull String operatorString,
            final int minimumRequired
    ) {
        final Operator operator;
        try {
            operator = Operator.valueOf(operatorString.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid operator: " + operatorString + ". Valid operators are: AND, OR, MINIMUM."
            );
        }
        return new CompositeRequirement(requirements, operator, minimumRequired, null, true);
    }

    /**
     * Represents a snapshot of a player's progress for a specific requirement within the composite.
     *
     * @param index       the original index of the requirement inside the composite list
     * @param requirement the requirement for which progress is recorded
     * @param progress    the raw progress value between {@code 0.0} and {@code 1.0}
     * @param completed   whether the requirement is fully completed
     */
    public record RequirementProgress(
            int index,
            @NotNull AbstractRequirement requirement,
            double progress,
            boolean completed
    ) {
        /**
         * Returns the progress value as a whole-number percentage for display purposes.
         *
         * @return the progress percentage rounded down to an integer value
         */
        public int getProgressPercentage() {
            return (int) (this.progress * 100);
        }
    }
}