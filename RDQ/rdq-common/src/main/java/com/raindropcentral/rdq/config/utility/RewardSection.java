package com.raindropcentral.rdq.config.utility;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.List;
import java.util.Map;

public class RewardSection extends AConfigSection {

    private String type;
    private IconSection icon;
    private Integer displayOrder;
    private Map<String, Object> item;
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
    
    // Perk reward fields
    private String perkIdentifier;
    private Boolean autoEnable;

    public RewardSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    public String getType() {
        return type;
    }

    public IconSection getIcon() {
        return icon == null ? new IconSection(new EvaluationEnvironmentBuilder()) : icon;
    }

    public Integer getDisplayOrder() {
        return displayOrder == null ? 0 : displayOrder;
    }

    public Map<String, Object> getItem() {
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
    
    public String getPerkIdentifier() {
        return perkIdentifier;
    }
    
    public Boolean getAutoEnable() {
        return autoEnable;
    }
}
