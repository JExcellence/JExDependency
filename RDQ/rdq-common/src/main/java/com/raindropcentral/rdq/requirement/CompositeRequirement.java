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

public final class CompositeRequirement extends AbstractRequirement {

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

    @JsonProperty("maximumRequired")
    private final Integer maximumRequired;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("allowPartialProgress")
    private final boolean allowPartialProgress;

    public CompositeRequirement(@NotNull List<AbstractRequirement> requirements) {
        this(requirements, Operator.AND, requirements.size(), null, null, true);
    }

    public CompositeRequirement(@NotNull List<AbstractRequirement> requirements, @NotNull Operator operator) {
        this(requirements, operator, operator == Operator.OR ? 1 : requirements.size(), null, null, true);
    }

    public CompositeRequirement(@NotNull List<AbstractRequirement> requirements, int minimumRequired) {
        this(requirements, Operator.MINIMUM, minimumRequired, null, null, true);
    }

    @JsonCreator
    public CompositeRequirement(@JsonProperty("requirements") @NotNull List<AbstractRequirement> requirements,
                               @JsonProperty("operator") @Nullable Operator operator,
                               @JsonProperty("minimumRequired") int minimumRequired,
                               @JsonProperty("maximumRequired") @Nullable Integer maximumRequired,
                               @JsonProperty("description") @Nullable String description,
                               @JsonProperty("allowPartialProgress") @Nullable Boolean allowPartialProgress) {
        super(Type.COMPOSITE);

        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("CompositeRequirement must contain at least one requirement.");
        }
        
        // Auto-adjust minimumRequired if it exceeds requirements size
        int adjustedMinimum = minimumRequired;
        if (minimumRequired < 1) {
            adjustedMinimum = 1;
        } else if (minimumRequired > requirements.size()) {
            adjustedMinimum = requirements.size();
        }

        this.requirements = new ArrayList<>(requirements);
        this.operator = operator != null ? operator : Operator.AND;
        this.minimumRequired = adjustedMinimum;
        this.maximumRequired = maximumRequired;
        this.description = description;
        this.allowPartialProgress = allowPartialProgress != null ? allowPartialProgress : true;
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return switch (operator) {
            case AND -> requirements.stream().allMatch(req -> req.isMet(player));
            case OR -> requirements.stream().anyMatch(req -> req.isMet(player));
            case MINIMUM -> {
                var completedCount = requirements.stream()
                        .filter(req -> req.isMet(player))
                        .count();
                yield completedCount >= minimumRequired;
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
     * Returns the maximum number of requirements that can be satisfied.
     *
     * @return the maximum number of completed sub-requirements, or {@code null} if unlimited
     */
    @Nullable
    public Integer getMaximumRequired() {
        return this.maximumRequired;
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
        return new CompositeRequirement(requirements, operator, minimumRequired, null, null, true);
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