package com.raindropcentral.rdq.bounty.config;

import com.raindropcentral.rdq.bounty.DistributionMode;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * Configuration section for bounty system settings.
 * 
 * <p>This section is loaded from bounty/bounty.yml and provides
 * all configurable options for the bounty hunting system.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@CSAlways
public class BountySection extends AConfigSection {

    private Boolean enabled;
    private Double minAmount;
    private Double maxAmount;
    private Integer expirationHours;
    private Boolean selfTargetAllowed;
    private String defaultDistributionMode;
    private Boolean instantDistributionEnabled;
    private Boolean chestDistributionEnabled;
    private Boolean dropDistributionEnabled;
    private Boolean virtualDistributionEnabled;
    private Boolean announceOnCreate;
    private Boolean announceOnClaim;
    private String announcementScope;
    private String defaultCurrency;
    private Double taxRate;
    private Integer maxActiveBountiesPerPlayer;

    public BountySection(@NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    @NotNull
    public BigDecimal getMinAmount() {
        return BigDecimal.valueOf(minAmount != null ? minAmount : 100.0);
    }

    @NotNull
    public BigDecimal getMaxAmount() {
        return BigDecimal.valueOf(maxAmount != null ? maxAmount : 1000000.0);
    }

    public int getExpirationHours() {
        return expirationHours != null ? expirationHours : 168;
    }

    public boolean isSelfTargetAllowed() {
        return selfTargetAllowed != null && selfTargetAllowed;
    }

    @NotNull
    public DistributionMode getDefaultDistributionMode() {
        if (defaultDistributionMode == null) {
            return DistributionMode.INSTANT;
        }
        try {
            return DistributionMode.valueOf(defaultDistributionMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DistributionMode.INSTANT;
        }
    }

    public boolean isInstantDistributionEnabled() {
        return instantDistributionEnabled == null || instantDistributionEnabled;
    }

    public boolean isChestDistributionEnabled() {
        return chestDistributionEnabled != null && chestDistributionEnabled;
    }

    public boolean isDropDistributionEnabled() {
        return dropDistributionEnabled != null && dropDistributionEnabled;
    }

    public boolean isVirtualDistributionEnabled() {
        return virtualDistributionEnabled != null && virtualDistributionEnabled;
    }

    public boolean isAnnounceOnCreate() {
        return announceOnCreate == null || announceOnCreate;
    }

    public boolean isAnnounceOnClaim() {
        return announceOnClaim == null || announceOnClaim;
    }

    @NotNull
    public String getAnnouncementScope() {
        return announcementScope != null ? announcementScope : "server";
    }

    @NotNull
    public String getDefaultCurrency() {
        return defaultCurrency != null ? defaultCurrency : "coins";
    }

    public double getTaxRate() {
        return taxRate != null ? taxRate : 0.05;
    }

    public int getMaxActiveBountiesPerPlayer() {
        return maxActiveBountiesPerPlayer != null ? maxActiveBountiesPerPlayer : 5;
    }

    public boolean isAnnouncementScopeServer() {
        return "server".equalsIgnoreCase(getAnnouncementScope());
    }

    public boolean isAnnouncementScopeNearby() {
        return "nearby".equalsIgnoreCase(getAnnouncementScope());
    }

    public boolean isAnnouncementScopeTarget() {
        return "target".equalsIgnoreCase(getAnnouncementScope());
    }

    /**
     * Converts this section to a BountyConfig record for use in services.
     *
     * @return a BountyConfig with values from this section
     */
    @NotNull
    public BountyConfig toConfig() {
        return new BountyConfig(
            isEnabled(),
            getMinAmount(),
            getMaxAmount(),
            getExpirationHours(),
            isSelfTargetAllowed(),
            getDefaultDistributionMode(),
            isInstantDistributionEnabled(),
            isChestDistributionEnabled(),
            isDropDistributionEnabled(),
            isVirtualDistributionEnabled(),
            isAnnounceOnCreate(),
            isAnnounceOnClaim(),
            getAnnouncementScope(),
            getDefaultCurrency()
        );
    }
}
