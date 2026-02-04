package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ChoiceReward extends AbstractReward {

    private final List<AbstractReward> choices;
    private final int minimumRequired;
    private final Integer maximumRequired;
    private final boolean allowMultipleSelections;

    @JsonCreator
    public ChoiceReward(
        @JsonProperty("choices") @NotNull List<AbstractReward> choices,
        @JsonProperty("minimumRequired") int minimumRequired,
        @JsonProperty("maximumRequired") Integer maximumRequired,
        @JsonProperty("allowMultipleSelections") boolean allowMultipleSelections
    ) {
        this.choices = new ArrayList<>(choices);
        this.minimumRequired = Math.max(1, minimumRequired);
        this.maximumRequired = maximumRequired;
        this.allowMultipleSelections = allowMultipleSelections;
    }

    @Override
    public @NotNull String getTypeId() {
        return "CHOICE";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.completedFuture(false);
    }

    public @NotNull CompletableFuture<Boolean> grantChoices(
        @NotNull Player player,
        @NotNull List<Integer> selectedIndices
    ) {
        if (selectedIndices.size() < minimumRequired) {
            return CompletableFuture.completedFuture(false);
        }
        
        if (maximumRequired != null && selectedIndices.size() > maximumRequired) {
            return CompletableFuture.completedFuture(false);
        }
        
        List<AbstractReward> selectedRewards = new ArrayList<>();
        for (int index : selectedIndices) {
            if (index >= 0 && index < choices.size()) {
                selectedRewards.add(choices.get(index));
            }
        }
        
        if (selectedRewards.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        List<CompletableFuture<Boolean>> futures = selectedRewards.stream()
            .map(reward -> reward.grant(player))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream().allMatch(CompletableFuture::join));
    }

    @Override
    public double getEstimatedValue() {
        if (choices.isEmpty()) return 0.0;
        
        double totalValue = choices.stream()
            .mapToDouble(AbstractReward::getEstimatedValue)
            .sum();
        
        return totalValue / choices.size() * minimumRequired;
    }

    public List<AbstractReward> getChoices() {
        return List.copyOf(choices);
    }

    public int getMinimumRequired() {
        return minimumRequired;
    }

    public Integer getMaximumRequired() {
        return maximumRequired;
    }

    public boolean isAllowMultipleSelections() {
        return allowMultipleSelections;
    }

    public boolean isSingleChoice() {
        return minimumRequired == 1 && (maximumRequired == null || maximumRequired == 1);
    }

    @Override
    public void validate() {
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("Choice reward must have at least one choice");
        }
        if (minimumRequired < 1) {
            throw new IllegalArgumentException("Minimum required must be at least 1");
        }
        if (minimumRequired > choices.size()) {
            throw new IllegalArgumentException("Minimum required cannot exceed number of choices");
        }
        if (maximumRequired != null && maximumRequired < minimumRequired) {
            throw new IllegalArgumentException("Maximum required cannot be less than minimum required");
        }
        for (AbstractReward choice : choices) {
            choice.validate();
        }
    }
}
