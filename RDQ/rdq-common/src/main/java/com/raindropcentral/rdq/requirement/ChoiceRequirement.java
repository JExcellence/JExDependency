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

public final class ChoiceRequirement extends AbstractRequirement {

    @JsonProperty("choices")
    private final List<AbstractRequirement> choices;

    @JsonProperty("minimumChoicesRequired")
    private final int minimumChoicesRequired;

    @JsonProperty("maximumRequired")
    private final Integer maximumRequired;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("allowPartialProgress")
    private final boolean allowPartialProgress;

    @JsonProperty("mutuallyExclusive")
    private final boolean mutuallyExclusive;

    @JsonProperty("allowChoiceChange")
    private final boolean allowChoiceChange;

    public ChoiceRequirement(@NotNull List<AbstractRequirement> choices) {
        this(choices, 1, null, null, null, true, false, true);
    }

    public ChoiceRequirement(
            @NotNull List<AbstractRequirement> choices,
            int minimumChoicesRequired
    ) {
        this(choices, minimumChoicesRequired, null, null, null, true, false, true);
    }

    @JsonCreator
    public ChoiceRequirement(
            @JsonProperty("choices") @NotNull List<AbstractRequirement> choices,
            @JsonProperty("minimumChoicesRequired") int minimumChoicesRequired,
            @JsonProperty("minimumRequired") @Nullable Integer minimumRequired,
            @JsonProperty("maximumRequired") @Nullable Integer maximumRequired,
            @JsonProperty("description") @Nullable String description,
            @JsonProperty("allowPartialProgress") @Nullable Boolean allowPartialProgress,
            @JsonProperty("mutuallyExclusive") @Nullable Boolean mutuallyExclusive,
            @JsonProperty("allowChoiceChange") @Nullable Boolean allowChoiceChange
    ) {
        super(Type.CHOICE);

        if (choices.isEmpty()) {
            throw new IllegalArgumentException("At least one alternative requirement is needed.");
        }
        
        // Use minimumRequired as fallback if minimumChoicesRequired is 0
        int effectiveMinimum = minimumChoicesRequired;
        if (effectiveMinimum == 0 && minimumRequired != null) {
            effectiveMinimum = minimumRequired;
        }
        
        // Auto-adjust minimumChoicesRequired if invalid
        int adjustedMinimum = effectiveMinimum;
        if (effectiveMinimum < 1) {
            adjustedMinimum = 1;
        } else if (effectiveMinimum > choices.size()) {
            adjustedMinimum = choices.size();
        }

        this.choices = new ArrayList<>(choices);
        this.minimumChoicesRequired = adjustedMinimum;
        this.maximumRequired = maximumRequired;
        this.description = description;
        this.allowPartialProgress = allowPartialProgress != null ? allowPartialProgress : true;
        this.mutuallyExclusive = mutuallyExclusive != null ? mutuallyExclusive : false;
        this.allowChoiceChange = allowChoiceChange != null ? allowChoiceChange : true;
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        var completedChoices = this.choices.stream()
                .filter(requirement -> requirement.isMet(player))
                .count();

        return completedChoices >= this.minimumChoicesRequired;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (this.choices.isEmpty()) {
            return 0.0;
        }

        if (this.minimumChoicesRequired == 1) {
            return this.choices.stream()
                    .mapToDouble(requirement -> requirement.calculateProgress(player))
                    .max()
                    .orElse(0.0);
        }

        var progressValues = this.choices.stream()
                .mapToDouble(requirement -> requirement.calculateProgress(player))
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (!this.allowPartialProgress) {
            var completedChoices = progressValues.stream()
                    .filter(progress -> progress >= 1.0)
                    .count();

            return Math.min(1.0, (double) completedChoices / this.minimumChoicesRequired);
        }

        var totalProgress = progressValues.stream()
                .limit(this.minimumChoicesRequired)
                .mapToDouble(Double::doubleValue)
                .sum();

        return Math.min(1.0, totalProgress / this.minimumChoicesRequired);
    }

    @Override
    public void consume(@NotNull Player player) {
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
    public Integer getMaximumRequired() {
        return this.maximumRequired;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public boolean isAllowPartialProgress() {
        return this.allowPartialProgress;
    }

    public boolean isMutuallyExclusive() {
        return this.mutuallyExclusive;
    }

    public boolean isAllowChoiceChange() {
        return this.allowChoiceChange;
    }

    @JsonIgnore
    @NotNull
    public List<ChoiceProgress> getDetailedProgress(@NotNull Player player) {
        return IntStream.range(0, this.choices.size())
                .mapToObj(index -> {
                    var choice = this.choices.get(index);
                    var progress = choice.calculateProgress(player);
                    var completed = choice.isMet(player);
                    return new ChoiceProgress(index, choice, progress, completed);
                })
                .toList();
    }

    @JsonIgnore
    @NotNull
    public Optional<AbstractRequirement> getBestChoice(@NotNull Player player) {
        return this.choices.stream()
                .max(Comparator.comparingDouble(requirement -> requirement.calculateProgress(player)));
    }

    @JsonIgnore
    @NotNull
    public List<AbstractRequirement> getCompletedChoices(@NotNull Player player) {
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

        for (var i = 0; i < this.choices.size(); i++) {
            var choice = this.choices.get(i);
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