package com.raindropcentral.rdq.bounty.config;

import com.raindropcentral.rdq.bounty.DistributionMode;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

public record BountyConfig(
    boolean enabled,
    @NotNull BigDecimal minAmount,
    @NotNull BigDecimal maxAmount,
    int expirationHours,
    boolean selfTargetAllowed,
    @NotNull DistributionMode defaultDistributionMode,
    boolean instantDistributionEnabled,
    boolean chestDistributionEnabled,
    boolean dropDistributionEnabled,
    boolean virtualDistributionEnabled,
    boolean announceOnCreate,
    boolean announceOnClaim,
    @NotNull String announcementScope,
    @NotNull String defaultCurrency
) {
    public BountyConfig {
        Objects.requireNonNull(minAmount, "minAmount");
        Objects.requireNonNull(maxAmount, "maxAmount");
        Objects.requireNonNull(defaultDistributionMode, "defaultDistributionMode");
        Objects.requireNonNull(announcementScope, "announcementScope");
        Objects.requireNonNull(defaultCurrency, "defaultCurrency");

        if (minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("minAmount cannot be negative");
        }
        if (maxAmount.compareTo(minAmount) < 0) {
            throw new IllegalArgumentException("maxAmount cannot be less than minAmount");
        }
        if (expirationHours < 0) {
            throw new IllegalArgumentException("expirationHours cannot be negative");
        }
    }


    public static BountyConfig defaults() {
        return new BountyConfig(
            true,
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(1000000),
            168,
            false,
            DistributionMode.INSTANT,
            true,
            false,
            false,
            false,
            true,
            true,
            "server",
            "coins"
        );
    }

    /**
     * Loads BountyConfig from a YAML configuration section.
     * 
     * <p>Expected YAML structure:
     * <pre>
     * enabled: true
     * amounts:
     *   minimum: 100
     *   maximum: 1000000
     * expiration:
     *   hours: 168
     * selfTargetAllowed: false
     * distribution: INSTANT
     * announcements:
     *   onCreate:
     *     enabled: true
     *     scope: SERVER
     *   onClaim:
     *     enabled: true
     * economy:
     *   currency: "coins"
     * </pre>
     *
     * @param section the configuration section to load from
     * @return the loaded BountyConfig
     */
    public static BountyConfig fromConfig(@NotNull ConfigurationSection section) {
        // Load amounts section
        var amountsSection = section.getConfigurationSection("amounts");
        var minAmount = amountsSection != null 
            ? BigDecimal.valueOf(amountsSection.getDouble("minimum", 100))
            : BigDecimal.valueOf(100);
        var maxAmount = amountsSection != null 
            ? BigDecimal.valueOf(amountsSection.getDouble("maximum", 1000000))
            : BigDecimal.valueOf(1000000);

        // Load expiration section
        var expirationSection = section.getConfigurationSection("expiration");
        var expirationHours = expirationSection != null 
            ? expirationSection.getInt("hours", 168)
            : 168;

        // Load distribution mode
        var distributionMode = parseDistributionMode(section.getString("distribution", "INSTANT"));

        // Check which distribution modes are enabled based on the main distribution setting
        var instantEnabled = distributionMode == DistributionMode.INSTANT;
        var chestEnabled = distributionMode == DistributionMode.CHEST;
        var dropEnabled = distributionMode == DistributionMode.DROP;
        var virtualEnabled = distributionMode == DistributionMode.VIRTUAL;

        // Load announcements section
        var announcementsSection = section.getConfigurationSection("announcements");
        var onCreateSection = announcementsSection != null 
            ? announcementsSection.getConfigurationSection("onCreate") 
            : null;
        var onClaimSection = announcementsSection != null 
            ? announcementsSection.getConfigurationSection("onClaim") 
            : null;

        var announceOnCreate = onCreateSection != null 
            ? onCreateSection.getBoolean("enabled", true) 
            : true;
        var announceOnClaim = onClaimSection != null 
            ? onClaimSection.getBoolean("enabled", true) 
            : true;
        var announcementScope = onCreateSection != null 
            ? onCreateSection.getString("scope", "SERVER") 
            : "SERVER";

        // Load economy section
        var economySection = section.getConfigurationSection("economy");
        var defaultCurrency = economySection != null 
            ? economySection.getString("currency", "coins") 
            : "coins";

        return new BountyConfig(
            section.getBoolean("enabled", true),
            minAmount,
            maxAmount,
            expirationHours,
            section.getBoolean("selfTargetAllowed", false),
            distributionMode,
            instantEnabled,
            chestEnabled,
            dropEnabled,
            virtualEnabled,
            announceOnCreate,
            announceOnClaim,
            announcementScope.toLowerCase(),
            defaultCurrency
        );
    }

    private static DistributionMode parseDistributionMode(String value) {
        try {
            return DistributionMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DistributionMode.INSTANT;
        }
    }

    public boolean isAnnouncementScopeServer() {
        return "server".equalsIgnoreCase(announcementScope);
    }

    public boolean isAnnouncementScopeNearby() {
        return "nearby".equalsIgnoreCase(announcementScope);
    }

    public boolean isAnnouncementScopeTarget() {
        return "target".equalsIgnoreCase(announcementScope);
    }
}
