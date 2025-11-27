package com.raindropcentral.rdq.config.bounty;

import com.raindropcentral.rdq.type.EBountyClaimMode;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
public class BountySection extends AConfigSection {

    private String claimMode;
    private Double creationCost;
    private Integer maxBountiesPerPlayer;
    private Boolean broadcastCreation;
    private Boolean broadcastCompletion;
    private Boolean enabled;
    private Double minimumValue;
    private Integer expiryDays;
    private String killAttributionMode;
    private String rewardDistributionMode;
    private Boolean announceCreation;
    private Boolean announceClaim;
    private String tabPrefix;
    private String chatPrefix;
    private String nameColor;

    public BountySection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    public EBountyClaimMode getClaimMode() {
        return claimMode == null ? EBountyClaimMode.LAST_HIT : EBountyClaimMode.valueOf(claimMode);
    }

    public double getCreationCost() { return creationCost != null ? creationCost : 0.0; }
    public int getMaxBountiesPerPlayer() { return maxBountiesPerPlayer != null ? maxBountiesPerPlayer : 1; }
    public boolean shouldBroadcastCreation() { return broadcastCreation != null && broadcastCreation; }
    public boolean shouldBroadcastCompletion() { return broadcastCompletion != null && broadcastCompletion; }
    public boolean isEnabled() { return enabled == null || enabled; }
    public double getMinimumValue() { return minimumValue != null ? minimumValue : 100.0; }
    public int getExpiryDays() { return expiryDays != null ? expiryDays : 7; }
    public String getKillAttributionMode() { return killAttributionMode != null ? killAttributionMode : "LAST_HIT"; }
    public String getRewardDistributionMode() { return rewardDistributionMode != null ? rewardDistributionMode : "INSTANT"; }
    public boolean isAnnounceCreation() { return announceCreation == null || announceCreation; }
    public boolean isAnnounceClaim() { return announceClaim == null || announceClaim; }
    public String getTabPrefix() { return tabPrefix != null ? tabPrefix : "bounty.config.tab_prefix"; }
    public String getChatPrefix() { return chatPrefix != null ? chatPrefix : "bounty.config.chat_prefix"; }
    public String getNameColor() { return nameColor != null ? nameColor : "bounty.config.name_color"; }
}
