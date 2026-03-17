/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.config.utility;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.List;
import java.util.Map;

/**
 * Represents the RewardSection API type.
 */
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

    /**
     * Executes RewardSection.
     */
    public RewardSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Gets type.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets icon.
     */
    public IconSection getIcon() {
        return icon == null ? new IconSection(new EvaluationEnvironmentBuilder()) : icon;
    }

    /**
     * Gets displayOrder.
     */
    public Integer getDisplayOrder() {
        return displayOrder == null ? 0 : displayOrder;
    }

    /**
     * Gets item.
     */
    public Map<String, Object> getItem() {
        return item;
    }

    /**
     * Gets currencyId.
     */
    public String getCurrencyId() {
        return currencyId;
    }

    /**
     * Gets amount.
     */
    public Double getAmount() {
        return amount;
    }

    /**
     * Gets experienceAmount.
     */
    public Integer getExperienceAmount() {
        return experienceAmount;
    }

    /**
     * Gets experienceType.
     */
    public String getExperienceType() {
        return experienceType;
    }

    /**
     * Gets command.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Gets executeAsPlayer.
     */
    public Boolean getExecuteAsPlayer() {
        return executeAsPlayer;
    }

    /**
     * Gets delayTicks.
     */
    public Long getDelayTicks() {
        return delayTicks;
    }

    /**
     * Gets permissions.
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * Gets durationSeconds.
     */
    public Long getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Gets temporary.
     */
    public Boolean getTemporary() {
        return temporary;
    }

    /**
     * Gets rewards.
     */
    public List<RewardSection> getRewards() {
        return rewards;
    }

    /**
     * Gets choices.
     */
    public List<RewardSection> getChoices() {
        return choices;
    }

    /**
     * Gets continueOnError.
     */
    public Boolean getContinueOnError() {
        return continueOnError;
    }

    /**
     * Gets minimumRequired.
     */
    public Integer getMinimumRequired() {
        return minimumRequired;
    }

    /**
     * Gets maximumRequired.
     */
    public Integer getMaximumRequired() {
        return maximumRequired;
    }

    /**
     * Gets allowMultipleSelections.
     */
    public Boolean getAllowMultipleSelections() {
        return allowMultipleSelections;
    }
    
    /**
     * Gets perkIdentifier.
     */
    public String getPerkIdentifier() {
        return perkIdentifier;
    }
    
    /**
     * Gets autoEnable.
     */
    public Boolean getAutoEnable() {
        return autoEnable;
    }
}
