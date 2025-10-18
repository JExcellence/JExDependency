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
 * @since TBD
 */
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

    @JsonProperty("description")
    private final String description;

    @JsonProperty("allowPartialProgress")
    private final boolean allowPartialProgress;

    public CompositeRequirement(final @NotNull List<AbstractRequirement> requirements) {
        this(requirements, Operator.AND, requirements.size(), null, true);
    }

    public CompositeRequirement(
            final @NotNull List<AbstractRequirement> requirements,
            final @NotNull Operator operator
    ) {
        this(requirements, operator, operator == Operator.OR ? 1 : requirements.size(), null, true);
    }

    public CompositeRequirement(
            final @NotNull List<AbstractRequirement> requirements,
            final int minimumRequired
    ) {
        this(requirements, Operator.MINIMUM, minimumRequired, null, true);
    }

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

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.composite." + this.operator.name().toLowerCase();
    }

    @NotNull
    public List<AbstractRequirement> getRequirements() {
        return new ArrayList<>(this.requirements);
    }

    @NotNull
    public Operator getOperator() {
        return this.operator;
    }

    public int getMinimumRequired() {
        return this.minimumRequired;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public boolean isAllowPartialProgress() {
        return this.allowPartialProgress;
    }

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

    @JsonIgnore
    @NotNull
    public List<AbstractRequirement> getCompletedRequirements(final @NotNull Player player) {
        return this.requirements.stream()
                .filter(req -> req.isMet(player))
                .toList();
    }

    @JsonIgnore
    @NotNull
    public List<AbstractRequirement> getRequirementsByProgress(final @NotNull Player player) {
        return this.requirements.stream()
                .sorted(Comparator.comparingDouble(
                        (AbstractRequirement req) -> req.calculateProgress(player)
                ).reversed())
                .toList();
    }

    @JsonIgnore
    public boolean isAndLogic() {
        return this.operator == Operator.AND;
    }

    @JsonIgnore
    public boolean isOrLogic() {
        return this.operator == Operator.OR;
    }

    @JsonIgnore
    public boolean isMinimumLogic() {
        return this.operator == Operator.MINIMUM;
    }

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

    public record RequirementProgress(
            int index,
            @NotNull AbstractRequirement requirement,
            double progress,
            boolean completed
    ) {
        public int getProgressPercentage() {
            return (int) (this.progress * 100);
        }
    }
}