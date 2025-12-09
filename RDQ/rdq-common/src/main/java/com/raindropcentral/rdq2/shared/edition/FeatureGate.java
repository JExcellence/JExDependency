package com.raindropcentral.rdq2.shared.edition;

import com.raindropcentral.rdq2.shared.translation.RDQTranslationService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public final class FeatureGate {

    private final EditionFeatures features;
    private final RDQTranslationService translations;

    public FeatureGate(@NotNull EditionFeatures features, @Nullable RDQTranslationService translations) {
        this.features = features;
        this.translations = translations;
    }

    public boolean checkFeature(@NotNull String featureId) {
        return features.isFeatureAvailable(featureId);
    }

    public boolean checkFeature(@NotNull String featureId, @NotNull Player player) {
        if (features.isFeatureAvailable(featureId)) {
            return true;
        }
        sendPremiumRequiredMessage(player, featureId);
        return false;
    }

    public boolean checkFeature(@NotNull String featureId, @NotNull Player player, @NotNull Consumer<Player> onAllowed) {
        if (features.isFeatureAvailable(featureId)) {
            onAllowed.accept(player);
            return true;
        }
        sendPremiumRequiredMessage(player, featureId);
        return false;
    }

    public boolean checkMultipleRankTrees(@NotNull Player player) {
        return checkFeature("rank.multiple_trees", player);
    }

    public boolean checkCrossTreeSwitching(@NotNull Player player) {
        return checkFeature("rank.cross_tree_switching", player);
    }

    public boolean checkMultipleActivePerks(@NotNull Player player) {
        return checkFeature("perk.multiple_active", player);
    }

    public boolean checkPremiumPerks(@NotNull Player player) {
        return checkFeature("perk.premium_types", player);
    }

    public boolean checkAdvancedBountyDistribution(@NotNull Player player) {
        return checkFeature("bounty.advanced_distribution", player);
    }

    public boolean checkMultipleBountyTargets(@NotNull Player player) {
        return checkFeature("bounty.multiple_targets", player);
    }

    private void sendPremiumRequiredMessage(@NotNull Player player, @NotNull String featureId) {
        if (translations == null) {
            player.sendMessage("<red>This feature requires RDQ Premium.");
            return;
        }

        var featureName = getFeatureDisplayName(featureId);
        translations.sendMessage(player, "error.premium_required", Map.of(
            "feature", featureName
        ));
    }

    @NotNull
    private String getFeatureDisplayName(@NotNull String featureId) {
        return switch (featureId) {
            case "rank.multiple_trees" -> "Multiple Rank Trees";
            case "rank.cross_tree_switching" -> "Cross-Tree Switching";
            case "perk.multiple_active" -> "Multiple Active Perks";
            case "perk.premium_types" -> "Premium Perk Types";
            case "bounty.advanced_distribution" -> "Advanced Bounty Distribution";
            case "bounty.multiple_targets" -> "Multiple Bounty Targets";
            default -> featureId;
        };
    }

    @NotNull
    public EditionFeatures getFeatures() {
        return features;
    }

    public boolean isPremium() {
        return features.isPremium();
    }

    public int getMaxActiveRankTrees() {
        return features.getMaxActiveRankTrees();
    }

    public int getMaxActivePerks() {
        return features.getMaxActivePerks();
    }
}
