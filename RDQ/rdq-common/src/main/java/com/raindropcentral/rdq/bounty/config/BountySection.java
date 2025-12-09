package com.raindropcentral.rdq.bounty.config;

import com.raindropcentral.rdq.bounty.type.EAnnouncementScope;
import com.raindropcentral.rdq.bounty.type.EClaimMode;
import com.raindropcentral.rdq.bounty.type.EDistributionMode;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

@CSAlways
public class BountySection extends AConfigSection {

    private Boolean enabled;
    private Boolean selfTargetAllowed;
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

    public BountySection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    public Boolean getEnabled() {
        return enabled != null && enabled;
    }

    public Boolean getSelfTargetAllowed() {
        return selfTargetAllowed == null || selfTargetAllowed;
    }

    public Boolean getInstantDistributionEnabled() {
        return instantDistributionEnabled == null || instantDistributionEnabled;
    }

    public Boolean getChestDistributionEnabled() {
        return chestDistributionEnabled == null || chestDistributionEnabled;
    }

    public Boolean getDropDistributionEnabled() {
        return dropDistributionEnabled == null || dropDistributionEnabled;
    }

    public Boolean getVirtualDistributionEnabled() {
        return virtualDistributionEnabled == null || virtualDistributionEnabled;
    }

    public Boolean getAnnounceOnCreate() {
        return announceOnCreate == null || announceOnCreate;
    }

    public Boolean getAnnounceOnClaim() {
        return announceOnClaim == null || announceOnClaim;
    }

    public Boolean isVisualIndicatorsEnabled() {
        return visualIndicatorsEnabled != null && visualIndicatorsEnabled;
    }

    public EDistributionMode getDefaultDistributionMode() {
        return defaultDistributionMode == null ? EDistributionMode.DROP : EDistributionMode.of(defaultDistributionMode);
    }

    public EAnnouncementScope getAnnouncementScope() {
        return announcementScope == null ? EAnnouncementScope.SERVER : EAnnouncementScope.of(announcementScope);
    }

    public EClaimMode getClaimMode() {
        return claimMode == null ? EClaimMode.LAST_HIT : EClaimMode.of(claimMode);
    }

    public List<String> getSupportedCurrencies() {
        return supportedCurrencies == null ?  new ArrayList<>() : supportedCurrencies;
    }

    public Double getTaxRate() {
        return taxRate == null ? 0.00 : taxRate;
    }

    public Integer getMaxActiveBountiesPerPlayer() {
        return maxActiveBountiesPerPlayer == null ? 1 : maxActiveBountiesPerPlayer;
    }

    public Integer getExpirationHours() {
        return expirationHours == null ? -1 : expirationHours;
    }

    public Long getTrackingWindowInMs() {
        return trackingWindowInMs == null ? 0L : trackingWindowInMs;
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
