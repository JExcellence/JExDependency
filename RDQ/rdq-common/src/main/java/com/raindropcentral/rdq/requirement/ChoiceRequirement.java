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
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public final class ChoiceRequirement extends AbstractRequirement {

    @JsonProperty("choices")
    private final List<AbstractRequirement> choices;

    @JsonProperty("minimumChoicesRequired")
    private final int minimumChoicesRequired;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("allowPartialProgress")
    private final boolean allowPartialProgress;

    /**
     * Constructs a new {@code ChoiceRequirement} with the given list of alternative requirements.
     * Uses default values for minimum choices (1) and allows partial progress.
     *
     * @param choices A non-empty list of requirements, any one of which fulfilling will satisfy this ChoiceRequirement.
     * @throws IllegalArgumentException If the choices list is empty.
     */
    public ChoiceRequirement(final @NotNull List<AbstractRequirement> choices) {
        this(choices, 1, null, true);
    }

    /**
     * Constructs a new {@code ChoiceRequirement} with the given list of alternative requirements and minimum choices.
     *
     * @param choices                 A non-empty list of requirements.
     * @param minimumChoicesRequired The minimum number of choices that must be completed.
     * @throws IllegalArgumentException If the choices list is empty or minimumChoicesRequired is invalid.
     */
    public ChoiceRequirement(
            final @NotNull List<AbstractRequirement> choices,
            final int minimumChoicesRequired
    ) {
        this(choices, minimumChoicesRequired, null, true);
    }

    /**
     * Constructs a new {@code ChoiceRequirement} with full configuration options.
     *
     * @param choices                 A non-empty list of requirements.
     * @param minimumChoicesRequired The minimum number of choices that must be completed.
     * @param description            Optional description for this choice requirement.
     * @param allowPartialProgress   Whether to allow partial progress calculation.
     * @throws IllegalArgumentException If the choices list is empty or minimumChoicesRequired is invalid.
     */
    @JsonCreator
    public ChoiceRequirement(
            @JsonProperty("choices") final @NotNull List<AbstractRequirement> choices,
            @JsonProperty("minimumChoicesRequired") final int minimumChoicesRequired,
            @JsonProperty("description") final @Nullable String description,
            @JsonProperty("allowPartialProgress") final @Nullable Boolean allowPartialProgress
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
                    "Minimum choices required (" + minimumChoicesRequired +
                    ") cannot exceed total choices (" + choices.size() + ")."
            );
        }

        this.choices = new ArrayList<>(choices);
        this.minimumChoicesRequired = minimumChoicesRequired;
        this.description = description;
        this.allowPartialProgress = allowPartialProgress != null ? allowPartialProgress : true;
    }

    @Override
    public boolean isMet(final @NotNull Player player) {
        final long completedChoices = this.choices.stream()
                .filter(requirement -> requirement.isMet(player))
                .count();

        return completedChoices >= this.minimumChoicesRequired;
    }

    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.choices.isEmpty()) {
            return 0.0;
        }

        if (this.minimumChoicesRequired == 1) {
            return this.choices.stream()
                    .mapToDouble(requirement -> requirement.calculateProgress(player))
                    .max()
                    .orElse(0.0);
        }

        final List<Double> progressValues = this.choices.stream()
                .mapToDouble(requirement -> requirement.calculateProgress(player))
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (!this.allowPartialProgress) {
            final long completedChoices = progressValues.stream()
                    .filter(progress -> progress >= 1.0)
                    .count();

            return Math.min(1.0, (double) completedChoices / this.minimumChoicesRequired);
        }

        final double totalProgress = progressValues.stream()
                .limit(this.minimumChoicesRequired)
                .mapToDouble(Double::doubleValue)
                .sum();

        return Math.min(1.0, totalProgress / this.minimumChoicesRequired);
    }

    @Override
    public void consume(final @NotNull Player player) {
        if (this.choices.isEmpty()) {
            return;
        }

        this.choices.stream()
                .sorted(Comparator.comparingDouble(
                        (AbstractRequirement requirement) -> requirement.calculateProgress(player)
                ).reversed())
                .limit(this.minimumChoicesRequired)
                .forEach(requirement -> requirement.consume(player));
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.choice";
    }

    @NotNull
    public List<AbstractRequirement> getChoices() {
        return new ArrayList<>(this.choices);
    }

    public int getMinimumChoicesRequired() {
        return this.minimumChoicesRequired;
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
    public List<ChoiceProgress> getDetailedProgress(final @NotNull Player player) {
        return IntStream.range(0, this.choices.size())
                .mapToObj(index -> {
                    final AbstractRequirement choice = this.choices.get(index);
                    final double progress = choice.calculateProgress(player);
                    final boolean completed = choice.isMet(player);
                    return new ChoiceProgress(index, choice, progress, completed);
                })
                .toList();
    }

    @JsonIgnore
    @NotNull
    public Optional<AbstractRequirement> getBestChoice(final @NotNull Player player) {
        return this.choices.stream()
                .max(Comparator.comparingDouble(requirement -> requirement.calculateProgress(player)));
    }

    @JsonIgnore
    @NotNull
    public List<AbstractRequirement> getCompletedChoices(final @NotNull Player player) {
        return this.choices.stream()
                .filter(requirement -> requirement.isMet(player))
                .toList();
    }

    @JsonIgnore
    public boolean isSingleChoice() {
        return this.minimumChoicesRequired == 1;
    }

    @JsonIgnore
    public void validate() {
        if (this.choices.isEmpty()) {
            throw new IllegalStateException("ChoiceRequirement must have at least one choice.");
        }
        if (this.minimumChoicesRequired < 1 || this.minimumChoicesRequired > this.choices.size()) {
            throw new IllegalStateException(
                    "Invalid minimumChoicesRequired: " + this.minimumChoicesRequired +
                    " (must be between 1 and " + this.choices.size() + ")."
            );
        }

        for (int i = 0; i < this.choices.size(); i++) {
            final AbstractRequirement choice = this.choices.get(i);
            if (choice == null) {
                throw new IllegalStateException("Choice at index " + i + " is null.");
            }
        }
    }

    public record ChoiceProgress(
            int index,
            @NotNull AbstractRequirement choice,
            double progress,
            boolean completed
    ) {
        public int getProgressPercentage() {
            return (int) (this.progress * 100);
        }
    }
}