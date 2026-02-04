package com.raindropcentral.rdq.config.utility;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RewardSection extends AConfigSection {

    private String type;
    private ItemStack item;
    private String currencyId;
    private Double amount;
    private Integer experienceAmount;
    private String experienceType;
    private String command;
    private Boolean executeAsPlayer;
    private Long delayTicks;
    private List<String> permissions;
    private Long durationSeconds;
    private Boolean temporary;
    private List<RewardSection> rewards;
    private List<RewardSection> choices;
    private Boolean continueOnError;
    private Integer minimumRequired;
    private Integer maximumRequired;
    private Boolean allowMultipleSelections;

    public RewardSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    public String getType() {
        return type;
    }

    public ItemStack getItem() {
        return item;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public Double getAmount() {
        return amount;
    }

    public Integer getExperienceAmount() {
        return experienceAmount;
    }

    public String getExperienceType() {
        return experienceType;
    }

    public String getCommand() {
        return command;
    }

    public Boolean getExecuteAsPlayer() {
        return executeAsPlayer;
    }

    public Long getDelayTicks() {
        return delayTicks;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public Boolean getTemporary() {
        return temporary;
    }

    public List<RewardSection> getRewards() {
        return rewards;
    }

    public List<RewardSection> getChoices() {
        return choices;
    }

    public Boolean getContinueOnError() {
        return continueOnError;
    }

    public Integer getMinimumRequired() {
        return minimumRequired;
    }

    public Integer getMaximumRequired() {
        return maximumRequired;
    }

    public Boolean getAllowMultipleSelections() {
        return allowMultipleSelections;
    }
}
