package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Represents the CompositeRequirement API type.
 */
public final class CompositeRequirement extends AbstractRequirement {

    /**
     * Represents the Operator API type.
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

    @JsonProperty("maximumRequired")
    private final Integer maximumRequired;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("allowPartialProgress")
    private final boolean allowPartialProgress;

    /**
     * Executes CompositeRequirement.
     */
    public CompositeRequirement(@NotNull List<AbstractRequirement> requirements) {
        this(requirements, Operator.AND, requirements.size(), null, null, true);
    }

    /**
     * Executes CompositeRequirement.
     */
    public CompositeRequirement(@NotNull List<AbstractRequirement> requirements, @NotNull Operator operator) {
        this(requirements, operator, operator == Operator.OR ? 1 : requirements.size(), null, null, true);
    }

    /**
     * Executes CompositeRequirement.
     */
    public CompositeRequirement(@NotNull List<AbstractRequirement> requirements, int minimumRequired) {
        this(requirements, Operator.MINIMUM, minimumRequired, null, null, true);
    }

    /**
     * Executes CompositeRequirement.
     */
    @JsonCreator
    public CompositeRequirement(@JsonProperty("requirements") @NotNull List<AbstractRequirement> requirements,
                               @JsonProperty("operator") @Nullable Operator operator,
                               @JsonProperty("minimumRequired") int minimumRequired,
                               @JsonProperty("maximumRequired") @Nullable Integer maximumRequired,
                               @JsonProperty("description") @Nullable String description,
                               @JsonProperty("allowPartialProgress") @Nullable Boolean allowPartialProgress) {
        super("COMPOSITE");

        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("CompositeRequirement must contain at least one requirement.");
        }
        
        int adjustedMinimum = minimumRequired;
        if (minimumRequired < 1) adjustedMinimum = 1;
        else if (minimumRequired > requirements.size()) adjustedMinimum = requirements.size();

        this.requirements = new ArrayList<>(requirements);
        this.operator = operator != null ? operator : Operator.AND;
        this.minimumRequired = adjustedMinimum;
        this.maximumRequired = maximumRequired;
        this.description = description;
        this.allowPartialProgress = allowPartialProgress != null ? allowPartialProgress : true;
    }

    /**
     * Returns whether met.
     */
    @Override
    public boolean isMet(@NotNull Player player) {
        return switch (operator) {
            case AND -> requirements.stream().allMatch(req -> req.isMet(player));
            case OR -> requirements.stream().anyMatch(req -> req.isMet(player));
            case MINIMUM -> {
                var completedCount = requirements.stream().filter(req -> req.isMet(player)).count();
                yield completedCount >= minimumRequired;
            }
        };
    }

    /**
     * Executes calculateProgress.
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.requirements.isEmpty()) return 0.0;

        return switch (this.operator) {
            case AND -> {
                final double totalProgress = this.requirements.stream()
                        .mapToDouble(req -> req.calculateProgress(player)).sum();
                yield Math.min(1.0, totalProgress / this.requirements.size());
            }
            case OR -> this.requirements.stream()
                    .mapToDouble(req -> req.calculateProgress(player)).max().orElse(0.0);
            case MINIMUM -> {
                final List<Double> progressValues = this.requirements.stream()
                        .mapToDouble(req -> req.calculateProgress(player)).boxed()
                        .sorted(Comparator.reverseOrder()).toList();

                if (!this.allowPartialProgress) {
                    final long completedCount = progressValues.stream().filter(p -> p >= 1.0).count();
                    yield Math.min(1.0, (double) completedCount / this.minimumRequired);
                }

                final double totalProgress = progressValues.stream().limit(this.minimumRequired)
                        .mapToDouble(Double::doubleValue).sum();
                yield Math.min(1.0, totalProgress / this.minimumRequired);
            }
        };
    }

    /**
     * Executes consume.
     */
    @Override
    public void consume(final @NotNull Player player) {
        switch (this.operator) {
            case AND -> this.requirements.forEach(req -> req.consume(player));
            case OR -> this.requirements.stream()
                    .max(Comparator.comparingDouble(req -> req.calculateProgress(player)))
                    .ifPresent(req -> req.consume(player));
            case MINIMUM -> this.requirements.stream()
                    .sorted(Comparator.comparingDouble((AbstractRequirement req) -> req.calculateProgress(player)).reversed())
                    .limit(this.minimumRequired).forEach(req -> req.consume(player));
        }
    }

    /**
     * Gets descriptionKey.
     */
    @Override
    @NotNull
    public String getDescriptionKey() { return "requirement.composite." + this.operator.name().toLowerCase(); }

    /**
     * Gets requirements.
     */
    @NotNull
    public List<AbstractRequirement> getRequirements() { return new ArrayList<>(this.requirements); }

    /**
     * Gets operator.
     */
    @NotNull
    public Operator getOperator() { return this.operator; }

    /**
     * Gets minimumRequired.
     */
    public int getMinimumRequired() { return this.minimumRequired; }

    /**
     * Gets maximumRequired.
     */
    @Nullable
    public Integer getMaximumRequired() { return this.maximumRequired; }

    /**
     * Gets description.
     */
    @Nullable
    public String getDescription() { return this.description; }

    /**
     * Returns whether allowPartialProgress.
     */
    public boolean isAllowPartialProgress() { return this.allowPartialProgress; }

    /**
     * Gets detailedProgress.
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
                }).toList();
    }

    /**
     * Gets completedRequirements.
     */
    @JsonIgnore
    @NotNull
    public List<AbstractRequirement> getCompletedRequirements(final @NotNull Player player) {
        return this.requirements.stream().filter(req -> req.isMet(player)).toList();
    }

    /**
     * Gets requirementsByProgress.
     */
    @JsonIgnore
    @NotNull
    public List<AbstractRequirement> getRequirementsByProgress(final @NotNull Player player) {
        return this.requirements.stream()
                .sorted(Comparator.comparingDouble((AbstractRequirement req) -> req.calculateProgress(player)).reversed())
                .toList();
    }

    /**
     * Returns whether andLogic.
     */
    @JsonIgnore
    public boolean isAndLogic() { return this.operator == Operator.AND; }

    /**
     * Returns whether orLogic.
     */
    @JsonIgnore
    public boolean isOrLogic() { return this.operator == Operator.OR; }

    /**
     * Returns whether minimumLogic.
     */
    @JsonIgnore
    public boolean isMinimumLogic() { return this.operator == Operator.MINIMUM; }

    /**
     * Executes validate.
     */
    @JsonIgnore
    public void validate() {
        if (this.requirements.isEmpty()) throw new IllegalStateException("CompositeRequirement must have at least one requirement.");
        if (this.minimumRequired < 1 || this.minimumRequired > this.requirements.size())
            throw new IllegalStateException("Invalid minimumRequired: " + this.minimumRequired);

        for (int i = 0; i < this.requirements.size(); i++) {
            if (this.requirements.get(i) == null) throw new IllegalStateException("Requirement at index " + i + " is null.");
        }
    }

    /**
     * Executes fromString.
     */
    @JsonIgnore
    @NotNull
    public static CompositeRequirement fromString(final @NotNull List<AbstractRequirement> requirements,
                                                   final @NotNull String operatorString, final int minimumRequired) {
        final Operator operator;
        try {
            operator = Operator.valueOf(operatorString.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operator: " + operatorString + ". Valid: AND, OR, MINIMUM.");
        }
        return new CompositeRequirement(requirements, operator, minimumRequired, null, null, true);
    }

    /**
     * Represents the RequirementProgress API type.
     */
    public record RequirementProgress(int index, @NotNull AbstractRequirement requirement, double progress, boolean completed) {
        /**
         * Gets progressPercentage.
         */
        public int getProgressPercentage() { return (int) (this.progress * 100); }
    }
}
