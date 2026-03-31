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

package com.raindropcentral.rdq.bounty.config;

import com.raindropcentral.rdq.bounty.type.EAnnouncementScope;
import com.raindropcentral.rdq.bounty.type.EClaimMode;
import com.raindropcentral.rdq.bounty.type.EDistributionMode;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the BountySection API type.
 */
@CSAlways
public class BountySection extends AConfigSection {

    private Boolean enabled;
    private Boolean selfTargetAllowed;
    private Boolean selfClaimAllowed;
    private Boolean instantDistributionEnabled;
    private Boolean chestDistributionEnabled;
    private Boolean dropDistributionEnabled;
    private Boolean virtualDistributionEnabled;
    private Boolean announceOnCreate;
    private Boolean announceOnClaim;
    private Boolean visualIndicatorsEnabled;

    private String defaultDistributionMode;
    private String announcementScope;
    private String claimMode;
    
    private List<String> supportedCurrencies;

    private Double taxRate;

    private Long trackingWindowInMs;

    private Integer maxActiveBountiesPerPlayer;
    private Integer expirationHours;

    /**
     * Executes BountySection.
     */
    public BountySection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Gets enabled.
     */
    public Boolean getEnabled() {
        return enabled != null && enabled;
    }

    /**
     * Gets selfTargetAllowed.
     */
    public Boolean getSelfTargetAllowed() {
        return selfTargetAllowed == null || selfTargetAllowed;
    }

    /**
     * Gets selfClaimAllowed.
     */
    public Boolean getSelfClaimAllowed() {
        return selfClaimAllowed != null && selfClaimAllowed;
    }

    /**
     * Gets instantDistributionEnabled.
     */
    public Boolean getInstantDistributionEnabled() {
        return instantDistributionEnabled == null || instantDistributionEnabled;
    }

    /**
     * Gets chestDistributionEnabled.
     */
    public Boolean getChestDistributionEnabled() {
        return chestDistributionEnabled == null || chestDistributionEnabled;
    }

    /**
     * Gets dropDistributionEnabled.
     */
    public Boolean getDropDistributionEnabled() {
        return dropDistributionEnabled == null || dropDistributionEnabled;
    }

    /**
     * Gets virtualDistributionEnabled.
     */
    public Boolean getVirtualDistributionEnabled() {
        return virtualDistributionEnabled == null || virtualDistributionEnabled;
    }

    /**
     * Gets announceOnCreate.
     */
    public Boolean getAnnounceOnCreate() {
        return announceOnCreate == null || announceOnCreate;
    }

    /**
     * Gets announceOnClaim.
     */
    public Boolean getAnnounceOnClaim() {
        return announceOnClaim == null || announceOnClaim;
    }

    /**
     * Returns whether visualIndicatorsEnabled.
     */
    public Boolean isVisualIndicatorsEnabled() {
        return visualIndicatorsEnabled != null && visualIndicatorsEnabled;
    }

    /**
     * Gets defaultDistributionMode.
     */
    public EDistributionMode getDefaultDistributionMode() {
        return defaultDistributionMode == null ? EDistributionMode.DROP : EDistributionMode.of(defaultDistributionMode);
    }

    /**
     * Gets announcementScope.
     */
    public EAnnouncementScope getAnnouncementScope() {
        return announcementScope == null ? EAnnouncementScope.SERVER : EAnnouncementScope.of(announcementScope);
    }

    /**
     * Gets claimMode.
     */
    public EClaimMode getClaimMode() {
        return claimMode == null ? EClaimMode.LAST_HIT : EClaimMode.of(claimMode);
    }

    /**
     * Gets supportedCurrencies.
     */
    public List<String> getSupportedCurrencies() {
        return supportedCurrencies == null ?  new ArrayList<>() : supportedCurrencies;
    }

    /**
     * Gets taxRate.
     */
    public Double getTaxRate() {
        return taxRate == null ? 0.00 : taxRate;
    }

    /**
     * Gets maxActiveBountiesPerPlayer.
     */
    public Integer getMaxActiveBountiesPerPlayer() {
        return maxActiveBountiesPerPlayer == null ? 1 : maxActiveBountiesPerPlayer;
    }

    /**
     * Gets expirationHours.
     */
    public Integer getExpirationHours() {
        return expirationHours == null ? -1 : expirationHours;
    }

    /**
     * Gets trackingWindowInMs.
     */
    public Long getTrackingWindowInMs() {
        return trackingWindowInMs == null ? 30000L : trackingWindowInMs; // Default 30 seconds
    }

    /**
     * Gets the maximum number of bounties a commissioner can have active.
     * Alias for getMaxActiveBountiesPerPlayer().
     *
     * @return the maximum bounties per commissioner
     */
    public Integer getMaxBountiesPerCommissioner() {
        return getMaxActiveBountiesPerPlayer();
    }

    /**
     * Gets the maximum number of rewards per bounty.
     * Default is 10 if not configured.
     *
     * @return the maximum rewards per bounty
     */
    public Integer getMaxRewardsPerBounty() {
        return 10; // Default value, can be made configurable later
    }
}
