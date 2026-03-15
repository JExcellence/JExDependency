package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.configs.ConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.RequirementService;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import com.raindropcentral.rplatform.requirement.impl.ChoiceRequirement;
import com.raindropcentral.rplatform.requirement.impl.CompositeRequirement;
import com.raindropcentral.rplatform.requirement.impl.CurrencyRequirement;
import com.raindropcentral.rplatform.requirement.impl.ExperienceLevelRequirement;
import com.raindropcentral.rplatform.requirement.impl.ItemRequirement;
import com.raindropcentral.rplatform.requirement.impl.LocationRequirement;
import com.raindropcentral.rplatform.requirement.impl.PermissionRequirement;
import com.raindropcentral.rplatform.requirement.impl.PlaytimeRequirement;
import com.raindropcentral.rplatform.requirement.impl.PluginRequirement;
import com.raindropcentral.rplatform.requirement.impl.TimedRequirement;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.RewardService;
import com.raindropcentral.rplatform.reward.config.RewardFactory;
import com.raindropcentral.rplatform.reward.impl.CurrencyReward;
import com.raindropcentral.rplatform.reward.impl.ItemReward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for town level-up requirements, progress banking, and rewards.
 *
 * <p>This support class intentionally mirrors the requirement/reward handling approach used
 * by other Raindrop modules while persisting partial requirement progress on {@link RTown}.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
final class TownLevelUpSupport {

    private static final double EPSILON = 1.0E-6D;
    private static final int MAX_REQUIREMENT_DETAIL_LINES = 4;
    private static final int MAX_PERMISSION_DETAIL_LINES = 4;
    private static final int MAX_CHILD_REQUIREMENT_LINES = 3;
    private static final int MAX_PLUGIN_VALUE_LINES = 4;
    private static final int MAX_WORLD_PLAYTIME_LINES = 3;
    private static final String VAULT_CURRENCY_ID = "vault";

    private TownLevelUpSupport() {
    }

    static @Nullable Integer resolveNextLevel(
            final @NotNull RDT plugin,
            final @NotNull RTown town
    ) {
        return plugin.getDefaultConfig().getNextTownLevel(town.getTownLevel());
    }

    static @NotNull List<ResolvedTownRequirement> getConfiguredRequirements(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final int targetLevel
    ) {
        final ConfigSection.TownLevelSection levelSection = plugin.getDefaultConfig().getTownLevelSection(targetLevel);
        if (levelSection == null) {
            return List.of();
        }

        final RequirementFactory factory = RequirementFactory.getInstance();
        final List<ResolvedTownRequirement> requirements = new ArrayList<>();
        for (final Map.Entry<String, Map<String, Object>> entry : levelSection.getRequirements().entrySet()) {
            final String key = normalizeDefinitionKey(entry.getKey());
            final Map<String, Object> definition = deepCopyMap(entry.getValue());
            try {
                final AbstractRequirement requirement = factory.fromMap(definition);
                requirements.add(new ResolvedTownRequirement(
                        targetLevel,
                        key,
                        definition,
                        requirement,
                        describeRequirement(plugin, requirement, definition),
                        isRequirementOperational(requirement)
                ));
            } catch (final Exception exception) {
                requirements.add(new ResolvedTownRequirement(
                        targetLevel,
                        key,
                        definition,
                        null,
                        describeMissingDefinition(key, definition),
                        false
                ));
            }
        }

        return requirements;
    }

    static @NotNull List<ResolvedTownReward> getConfiguredRewards(
            final @NotNull RDT plugin,
            final int targetLevel
    ) {
        final ConfigSection.TownLevelSection levelSection = plugin.getDefaultConfig().getTownLevelSection(targetLevel);
        if (levelSection == null) {
            return List.of();
        }

        final RewardFactory<Map<String, Object>> factory = RewardFactory.getInstance();
        final List<ResolvedTownReward> rewards = new ArrayList<>();
        for (final Map.Entry<String, Map<String, Object>> entry : levelSection.getRewards().entrySet()) {
            final String key = normalizeDefinitionKey(entry.getKey());
            final Map<String, Object> definition = deepCopyMap(entry.getValue());
            try {
                final AbstractReward reward = factory.fromMap(definition);
                rewards.add(new ResolvedTownReward(
                        targetLevel,
                        key,
                        definition,
                        reward,
                        describeReward(reward, definition, key),
                        isRewardOperational(reward)
                ));
            } catch (final Exception exception) {
                rewards.add(new ResolvedTownReward(
                        targetLevel,
                        key,
                        definition,
                        null,
                        describeMissingDefinition(key, definition),
                        false
                ));
            }
        }

        return rewards;
    }

    static @NotNull RequirementAvailability resolveRequirementAvailability(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull List<ResolvedTownRequirement> requirements
    ) {
        for (final ResolvedTownRequirement requirement : requirements) {
            if (!requirement.operational() || requirement.requirement() == null) {
                return RequirementAvailability.UNAVAILABLE;
            }
        }

        for (final ResolvedTownRequirement requirement : requirements) {
            if (!isRequirementMet(plugin, player, town, requirement)) {
                return RequirementAvailability.PENDING;
            }
        }

        return RequirementAvailability.READY;
    }

    static @NotNull ProgressUpdateResult attemptRequirementProgress(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement
    ) {
        if (!requirement.operational() || requirement.requirement() == null) {
            return new ProgressUpdateResult(ProgressUpdateStatus.UNAVAILABLE, 0, false);
        }

        final AbstractRequirement parsedRequirement = requirement.requirement();
        final boolean changed = switch (parsedRequirement) {
            case CurrencyRequirement currencyRequirement -> {
                if (!currencyRequirement.isConsumable()) {
                    yield false;
                }
                yield bankCurrencyProgress(plugin, player, town, requirement, currencyRequirement);
            }
            case ItemRequirement itemRequirement -> {
                if (!itemRequirement.isConsumeOnComplete()) {
                    yield false;
                }
                yield bankItemProgress(player, town, requirement, itemRequirement);
            }
            default -> false;
        };

        if (!(parsedRequirement instanceof CurrencyRequirement) && !(parsedRequirement instanceof ItemRequirement)) {
            final int progress = getProgressPercentage(plugin, player, town, requirement);
            return new ProgressUpdateResult(
                    isRequirementMet(plugin, player, town, requirement)
                            ? ProgressUpdateStatus.COMPLETE
                            : ProgressUpdateStatus.UNSUPPORTED,
                    progress,
                    false
            );
        }

        final int progress = getProgressPercentage(plugin, player, town, requirement);
        if (!changed) {
            return new ProgressUpdateResult(
                    isRequirementMet(plugin, player, town, requirement)
                            ? ProgressUpdateStatus.COMPLETE
                            : ProgressUpdateStatus.NO_PROGRESS,
                    progress,
                    false
            );
        }

        return new ProgressUpdateResult(
                isRequirementMet(plugin, player, town, requirement)
                        ? ProgressUpdateStatus.COMPLETE
                        : ProgressUpdateStatus.PROGRESSED,
                progress,
                true
        );
    }

    static @NotNull LevelUpResult attemptLevelUp(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town
    ) {
        if (plugin.getTownRepository() == null) {
            return new LevelUpResult(
                    LevelUpStatus.SYSTEM_UNAVAILABLE,
                    town.getTownLevel(),
                    town.getTownLevel(),
                    "",
                    "",
                    "",
                    0,
                    0
            );
        }

        final Integer targetLevel = resolveNextLevel(plugin, town);
        if (targetLevel == null) {
            return new LevelUpResult(
                    LevelUpStatus.MAX_LEVEL,
                    town.getTownLevel(),
                    town.getTownLevel(),
                    "",
                    "",
                    "",
                    0,
                    0
            );
        }

        final int previousLevel = town.getTownLevel();
        final List<ResolvedTownRequirement> requirements = getConfiguredRequirements(plugin, player, town, targetLevel);
        final String requirementSummary = formatRequirementSummary(requirements);
        final RequirementAvailability availability = resolveRequirementAvailability(plugin, player, town, requirements);

        if (availability == RequirementAvailability.UNAVAILABLE) {
            final String failedRequirement = requirements.stream()
                    .filter(requirement -> !requirement.operational() || requirement.requirement() == null)
                    .map(ResolvedTownRequirement::summary)
                    .findFirst()
                    .orElse("");
            return new LevelUpResult(
                    LevelUpStatus.REQUIREMENT_UNAVAILABLE,
                    previousLevel,
                    previousLevel,
                    failedRequirement,
                    requirementSummary,
                    "",
                    0,
                    0
            );
        }
        if (availability == RequirementAvailability.PENDING) {
            final String failedRequirement = requirements.stream()
                    .filter(requirement -> !isRequirementMet(plugin, player, town, requirement))
                    .map(ResolvedTownRequirement::summary)
                    .findFirst()
                    .orElse("");
            return new LevelUpResult(
                    LevelUpStatus.REQUIREMENT_UNMET,
                    previousLevel,
                    previousLevel,
                    failedRequirement,
                    requirementSummary,
                    "",
                    0,
                    0
            );
        }

        for (final ResolvedTownRequirement requirement : requirements) {
            if (!consumeRequirement(plugin, player, town, requirement)) {
                return new LevelUpResult(
                        LevelUpStatus.CONSUME_FAILED,
                        previousLevel,
                        previousLevel,
                        requirement.summary(),
                        requirementSummary,
                        "",
                        0,
                        0
                );
            }
        }

        final List<ResolvedTownReward> rewards = getConfiguredRewards(plugin, targetLevel);
        final String rewardSummary = formatRewardSummary(rewards);
        int rewardFailures = 0;
        int grantedRewards = 0;
        for (final ResolvedTownReward reward : rewards) {
            if (!grantReward(plugin, player, reward)) {
                rewardFailures++;
                continue;
            }
            grantedRewards++;
        }

        town.setTownLevel(targetLevel);
        plugin.getTownRepository().update(town);

        final LevelUpStatus status = rewardFailures > 0
                ? LevelUpStatus.SUCCESS_WITH_REWARD_ERRORS
                : LevelUpStatus.SUCCESS;
        return new LevelUpResult(
                status,
                previousLevel,
                targetLevel,
                "",
                requirementSummary,
                rewardSummary,
                grantedRewards,
                rewardFailures
        );
    }

    static boolean isRequirementMet(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement
    ) {
        if (!requirement.operational() || requirement.requirement() == null) {
            return false;
        }

        final AbstractRequirement parsedRequirement = requirement.requirement();
        if (parsedRequirement instanceof CurrencyRequirement currencyRequirement) {
            final double stored = currencyRequirement.isConsumable()
                    ? getStoredCurrencyProgress(town, requirement)
                    : 0.0D;
            return thisOrGreater(
                    currencyRequirement.getCurrentBalance(player) + stored,
                    currencyRequirement.getAmount()
            );
        }

        if (parsedRequirement instanceof ItemRequirement itemRequirement) {
            final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
            for (int itemIndex = 0; itemIndex < requiredItems.size(); itemIndex++) {
                final ItemStack requiredItem = requiredItems.get(itemIndex);
                final int stored = itemRequirement.isConsumeOnComplete()
                        ? getStoredItemAmount(town, requirement, itemIndex)
                        : 0;
                final int live = countMatchingItems(player, requiredItem, itemRequirement.isExactMatch());
                if (stored + live < requiredItem.getAmount()) {
                    return false;
                }
            }
            return true;
        }

        if (parsedRequirement instanceof PlaytimeRequirement playtimeRequirement) {
            return isTownPlaytimeRequirementMet(town, playtimeRequirement);
        }

        return parsedRequirement.isMet(player);
    }

    static int getProgressPercentage(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement
    ) {
        return (int) Math.round(calculateProgress(plugin, player, town, requirement) * 100.0D);
    }

    static double calculateProgress(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement
    ) {
        if (!requirement.operational() || requirement.requirement() == null) {
            return 0.0D;
        }

        final AbstractRequirement parsedRequirement = requirement.requirement();
        if (parsedRequirement instanceof CurrencyRequirement currencyRequirement) {
            if (currencyRequirement.getAmount() <= 0.0D) {
                return 1.0D;
            }
            final double stored = currencyRequirement.isConsumable()
                    ? getStoredCurrencyProgress(town, requirement)
                    : 0.0D;
            final double progressAmount = currencyRequirement.getCurrentBalance(player) + stored;
            return Math.max(0.0D, Math.min(1.0D, progressAmount / currencyRequirement.getAmount()));
        }

        if (parsedRequirement instanceof ItemRequirement itemRequirement) {
            final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
            if (requiredItems.isEmpty()) {
                return 1.0D;
            }

            double collected = 0.0D;
            double requiredTotal = 0.0D;
            for (int itemIndex = 0; itemIndex < requiredItems.size(); itemIndex++) {
                final ItemStack requiredItem = requiredItems.get(itemIndex);
                final int requiredAmount = requiredItem.getAmount();
                final int stored = itemRequirement.isConsumeOnComplete()
                        ? getStoredItemAmount(town, requirement, itemIndex)
                        : 0;
                final int live = countMatchingItems(player, requiredItem, itemRequirement.isExactMatch());
                collected += Math.min(requiredAmount, stored + live);
                requiredTotal += requiredAmount;
            }

            if (requiredTotal <= 0.0D) {
                return 1.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, collected / requiredTotal));
        }

        if (parsedRequirement instanceof PlaytimeRequirement playtimeRequirement) {
            return calculateTownPlaytimeProgress(town, playtimeRequirement);
        }

        return Math.max(0.0D, Math.min(1.0D, parsedRequirement.calculateProgress(player)));
    }

    static @NotNull String buildProgressBar(
            final int percentage,
            final @NotNull String emptySegment,
            final @NotNull String partialSegment,
            final @NotNull String filledSegment
    ) {
        final int normalizedPercentage = Math.max(0, Math.min(100, percentage));
        final int totalSegments = 10;
        final double progress = normalizedPercentage / 100.0D;
        final int filledSegments = Math.min(totalSegments, (int) Math.floor(progress * totalSegments));
        final double remainder = (progress * totalSegments) - filledSegments;
        final StringBuilder builder = new StringBuilder(totalSegments * filledSegment.length());
        for (int index = 0; index < totalSegments; index++) {
            if (index < filledSegments) {
                builder.append(filledSegment);
            } else if (index == filledSegments && remainder > 0.3D && filledSegments < totalSegments) {
                builder.append(partialSegment);
            } else {
                builder.append(emptySegment);
            }
        }
        return builder.toString();
    }

    static @NotNull Material resolveRequirementMaterial(
            final @NotNull ResolvedTownRequirement requirement
    ) {
        final Material configuredMaterial = parseConfiguredIconMaterial(requirement.definition());
        if (configuredMaterial != null) {
            return configuredMaterial;
        }

        final String typeId = requirement.requirement() == null
                ? String.valueOf(requirement.definition().getOrDefault("type", "UNKNOWN"))
                : requirement.requirement().getTypeId();
        return switch (typeId.toUpperCase(Locale.ROOT)) {
            case "CURRENCY" -> Material.GOLD_INGOT;
            case "ITEM" -> Material.CHEST;
            case "EXPERIENCE_LEVEL" -> Material.EXPERIENCE_BOTTLE;
            case "PLAYTIME", "TIME_BASED" -> Material.CLOCK;
            case "PERMISSION" -> Material.PAPER;
            case "LOCATION" -> Material.COMPASS;
            case "PLUGIN" -> Material.BEACON;
            case "COMPOSITE" -> Material.CRAFTING_TABLE;
            case "CHOICE" -> Material.HOPPER;
            default -> Material.BOOK;
        };
    }

    static @NotNull Material resolveRewardMaterial(
            final @NotNull ResolvedTownReward reward
    ) {
        final Material configuredMaterial = parseConfiguredIconMaterial(reward.definition());
        if (configuredMaterial != null) {
            return configuredMaterial;
        }

        if (reward.reward() instanceof ItemReward itemReward) {
            return itemReward.getItem().getType();
        }

        final String typeId = reward.reward() == null
                ? String.valueOf(reward.definition().getOrDefault("type", "UNKNOWN"))
                : reward.reward().getTypeId();
        return switch (typeId.toUpperCase(Locale.ROOT)) {
            case "CURRENCY" -> Material.GOLD_BLOCK;
            case "ITEM" -> Material.CHEST;
            case "EXPERIENCE" -> Material.EXPERIENCE_BOTTLE;
            case "PERMISSION" -> Material.PAPER;
            case "COMMAND" -> Material.COMMAND_BLOCK;
            case "COMPOSITE", "CHOICE" -> Material.ENCHANTED_BOOK;
            default -> Material.CHEST_MINECART;
        };
    }

    static @NotNull String resolveRequirementDisplayName(
            final @NotNull ResolvedTownRequirement requirement
    ) {
        final String description = String.valueOf(requirement.definition().getOrDefault("description", ""));
        if (!description.isBlank()) {
            return description;
        }
        return formatLabel(requirement.key());
    }

    static @NotNull String resolveRewardDisplayName(
            final @NotNull ResolvedTownReward reward
    ) {
        final String description = String.valueOf(reward.definition().getOrDefault("description", ""));
        if (!description.isBlank()) {
            return description;
        }
        return formatLabel(reward.key());
    }

    static @NotNull List<String> buildRequirementDetailLines(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement
    ) {
        if (!requirement.operational() || requirement.requirement() == null) {
            return List.of("Requirement data is unavailable on this server.");
        }

        final AbstractRequirement parsedRequirement = requirement.requirement();
        if (parsedRequirement instanceof CurrencyRequirement currencyRequirement) {
            final double banked = getStoredCurrencyProgress(town, requirement);
            final List<String> lines = new ArrayList<>();
            lines.add(
                    "Balance: "
                            + formatCurrency(plugin, currencyRequirement.getCurrencyId(), currencyRequirement.getCurrentBalance(player))
                            + " / "
                            + formatCurrency(plugin, currencyRequirement.getCurrencyId(), currencyRequirement.getAmount())
            );
            if (banked > 0.0D) {
                lines.add(
                        "Banked: "
                                + formatCurrency(plugin, currencyRequirement.getCurrencyId(), banked)
                );
                lines.add(
                        "Remaining: "
                                + formatCurrency(
                                plugin,
                                currencyRequirement.getCurrencyId(),
                                Math.max(0.0D, currencyRequirement.getAmount() - banked)
                        )
                );
            }
            if (currencyRequirement.isConsumable()) {
                lines.add("Click to bank available currency progress.");
            } else {
                lines.add("This requirement is checked but not consumed.");
            }
            return lines;
        }

        if (parsedRequirement instanceof ItemRequirement itemRequirement) {
            final List<String> lines = new ArrayList<>();
            final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
            for (int itemIndex = 0; itemIndex < Math.min(requiredItems.size(), MAX_REQUIREMENT_DETAIL_LINES); itemIndex++) {
                final ItemStack requiredItem = requiredItems.get(itemIndex);
                final int liveAmount = countMatchingItems(player, requiredItem, itemRequirement.isExactMatch());
                final int bankedAmount = getStoredItemAmount(town, requirement, itemIndex);
                final int effectiveAmount = Math.min(requiredItem.getAmount(), liveAmount + bankedAmount);
                lines.add(
                        effectiveAmount
                                + "/"
                                + requiredItem.getAmount()
                                + "x "
                                + formatLabel(requiredItem.getType().name())
                );
                if (bankedAmount > 0) {
                    lines.add("  banked " + bankedAmount + "x");
                }
            }
            if (requiredItems.size() > MAX_REQUIREMENT_DETAIL_LINES) {
                lines.add("+" + (requiredItems.size() - MAX_REQUIREMENT_DETAIL_LINES) + " more item checks");
            }
            if (itemRequirement.isConsumeOnComplete()) {
                lines.add("Click to bank matching items.");
            } else {
                lines.add("This requirement is checked but not consumed.");
            }
            return lines;
        }

        if (parsedRequirement instanceof PlaytimeRequirement playtimeRequirement) {
            return buildPlaytimeDetailLines(town, playtimeRequirement);
        }

        if (parsedRequirement instanceof ExperienceLevelRequirement experienceLevelRequirement) {
            final String unit = experienceLevelRequirement.isLevelBased() ? "levels" : "points";
            final List<String> lines = new ArrayList<>();
            lines.add(
                    "Current: "
                            + experienceLevelRequirement.getCurrentExperience(player)
                            + " "
                            + unit
            );
            lines.add(
                    "Required: "
                            + experienceLevelRequirement.getRequiredLevel()
                            + " "
                            + unit
            );
            if (experienceLevelRequirement.getShortage(player) > 0) {
                lines.add(
                        "Missing: "
                                + experienceLevelRequirement.getShortage(player)
                                + " "
                                + unit
                );
            }
            if (experienceLevelRequirement.isConsumeOnComplete()) {
                lines.add("Consumed on level up.");
            }
            return lines;
        }

        if (parsedRequirement instanceof PermissionRequirement permissionRequirement) {
            final List<String> lines = new ArrayList<>();
            lines.add("Mode: " + permissionRequirement.getPermissionMode().name());
            if (permissionRequirement.isMinimumMode()) {
                lines.add("Minimum matches: " + permissionRequirement.getMinimumRequired());
            }

            final List<PermissionRequirement.PermissionStatus> statuses =
                    permissionRequirement.getDetailedPermissionStatus(player);
            for (int index = 0; index < Math.min(statuses.size(), MAX_PERMISSION_DETAIL_LINES); index++) {
                final PermissionRequirement.PermissionStatus status = statuses.get(index);
                lines.add((status.hasPermission() ? "[OK] " : "[X] ") + status.getPermission());
            }
            if (statuses.size() > MAX_PERMISSION_DETAIL_LINES) {
                lines.add("+" + (statuses.size() - MAX_PERMISSION_DETAIL_LINES) + " more permission checks");
            }
            return lines;
        }

        if (parsedRequirement instanceof TimedRequirement timedRequirement) {
            final List<String> lines = new ArrayList<>();
            lines.add("Started: " + (timedRequirement.isStarted() ? "yes" : "no"));
            lines.add("Remaining: " + timedRequirement.getFormattedRemainingTime());
            lines.add(
                    "Time Limit: "
                            + PlaytimeRequirement.formatDuration(
                            TimeUnit.MILLISECONDS.toSeconds(timedRequirement.getTimeLimitMillis())
                    )
            );
            lines.add("Delegate: " + formatLabel(timedRequirement.getDelegate().getTypeId()));
            return lines;
        }

        if (parsedRequirement instanceof CompositeRequirement compositeRequirement) {
            final List<String> lines = new ArrayList<>();
            lines.add("Operator: " + compositeRequirement.getOperator().name());
            if (compositeRequirement.isMinimumLogic()) {
                lines.add("Minimum required: " + compositeRequirement.getMinimumRequired());
            }
            lines.add(
                    "Completed: "
                            + compositeRequirement.getCompletedRequirements(player).size()
                            + "/"
                            + compositeRequirement.getRequirements().size()
            );

            final List<CompositeRequirement.RequirementProgress> progressEntries =
                    compositeRequirement.getDetailedProgress(player);
            for (int index = 0; index < Math.min(progressEntries.size(), MAX_CHILD_REQUIREMENT_LINES); index++) {
                final CompositeRequirement.RequirementProgress progressEntry = progressEntries.get(index);
                lines.add(
                        (progressEntry.completed() ? "[OK] " : "[ ] ")
                                + formatLabel(progressEntry.requirement().getTypeId())
                                + " "
                                + progressEntry.getProgressPercentage()
                                + "%"
                );
            }
            if (progressEntries.size() > MAX_CHILD_REQUIREMENT_LINES) {
                lines.add("+" + (progressEntries.size() - MAX_CHILD_REQUIREMENT_LINES) + " more child checks");
            }
            return lines;
        }

        if (parsedRequirement instanceof ChoiceRequirement choiceRequirement) {
            final List<String> lines = new ArrayList<>();
            lines.add("Choices required: " + choiceRequirement.getMinimumChoicesRequired());
            lines.add(
                    "Completed choices: "
                            + choiceRequirement.getCompletedChoices(player).size()
                            + "/"
                            + choiceRequirement.getChoices().size()
            );

            final List<ChoiceRequirement.ChoiceProgress> progressEntries = choiceRequirement.getDetailedProgress(player);
            for (int index = 0; index < Math.min(progressEntries.size(), MAX_CHILD_REQUIREMENT_LINES); index++) {
                final ChoiceRequirement.ChoiceProgress progressEntry = progressEntries.get(index);
                lines.add(
                        (progressEntry.completed() ? "[OK] " : "[ ] ")
                                + formatLabel(progressEntry.choice().getTypeId())
                                + " "
                                + progressEntry.getProgressPercentage()
                                + "%"
                );
            }
            if (progressEntries.size() > MAX_CHILD_REQUIREMENT_LINES) {
                lines.add("+" + (progressEntries.size() - MAX_CHILD_REQUIREMENT_LINES) + " more choice checks");
            }
            return lines;
        }

        if (parsedRequirement instanceof LocationRequirement locationRequirement) {
            final List<String> lines = new ArrayList<>();
            if (locationRequirement.getRequiredWorld() != null) {
                lines.add(
                        "World: "
                                + player.getWorld().getName()
                                + " / "
                                + locationRequirement.getRequiredWorld()
                );
            }
            if (locationRequirement.getRequiredCoordinates() != null) {
                lines.add("Target: " + formatLocation(locationRequirement));
                lines.add(
                        "Distance: "
                                + formatAmount(locationRequirement.getCurrentDistance(player))
                                + " / "
                                + formatAmount(locationRequirement.getRequiredDistance())
                );
            }
            if (locationRequirement.getRequiredRegion() != null) {
                lines.add("Region: " + locationRequirement.getRequiredRegion());
            }
            return lines;
        }

        if (parsedRequirement instanceof PluginRequirement pluginRequirement) {
            final List<String> lines = new ArrayList<>();
            lines.add("Plugin: " + pluginRequirement.getPluginIntegrationId());
            if (pluginRequirement.getCategory() != null && !pluginRequirement.getCategory().isBlank()) {
                lines.add("Category: " + pluginRequirement.getCategory());
            }

            final Map<String, Double> currentValues = pluginRequirement.getCurrentValues(player);
            int index = 0;
            for (final Map.Entry<String, Double> entry : pluginRequirement.getRequiredValues().entrySet()) {
                if (index >= MAX_PLUGIN_VALUE_LINES) {
                    lines.add("+" + (pluginRequirement.getRequiredValues().size() - MAX_PLUGIN_VALUE_LINES) + " more plugin values");
                    break;
                }
                final double currentValue = currentValues.getOrDefault(entry.getKey(), 0.0D);
                lines.add(
                        entry.getKey()
                                + ": "
                                + formatAmount(currentValue)
                                + " / "
                                + formatAmount(entry.getValue())
                );
                index++;
            }
            return lines;
        }

        return List.of(describeRequirement(plugin, parsedRequirement, requirement.definition()));
    }

    static @NotNull List<String> buildRewardDetailLines(
            final @NotNull RDT plugin,
            final @NotNull ResolvedTownReward reward
    ) {
        if (!reward.operational() || reward.reward() == null) {
            return List.of("Reward data is unavailable on this server.");
        }

        final AbstractReward parsedReward = reward.reward();
        if (parsedReward instanceof CurrencyReward currencyReward) {
            return List.of(
                    "Currency: "
                            + formatCurrency(plugin, currencyReward.getCurrencyId(), currencyReward.getAmount())
            );
        }
        if (parsedReward instanceof ItemReward itemReward) {
            final ItemStack item = itemReward.getItem();
            return List.of(
                    "Item: " + formatLabel(item.getType().name()) + " x" + itemReward.getAmount()
            );
        }

        return List.of(reward.summary());
    }

    static @NotNull String formatRequirementSummary(
            final @NotNull List<ResolvedTownRequirement> requirements
    ) {
        final List<String> parts = new ArrayList<>();
        for (final ResolvedTownRequirement requirement : requirements) {
            parts.add(requirement.summary());
        }
        return String.join(", ", parts);
    }

    static @NotNull String formatRewardSummary(
            final @NotNull List<ResolvedTownReward> rewards
    ) {
        final List<String> parts = new ArrayList<>();
        for (final ResolvedTownReward reward : rewards) {
            parts.add(reward.summary());
        }
        return String.join(", ", parts);
    }

    static @NotNull String formatAmount(final double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 0.0001D) {
            return String.format(Locale.US, "%.0f", amount);
        }
        return String.format(Locale.US, "%.2f", amount);
    }

    static @NotNull String formatCurrency(
            final @NotNull RDT plugin,
            final @NotNull String currencyId,
            final double amount
    ) {
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (usesVaultCurrency(normalizedCurrencyId)) {
            if (plugin.getEco() != null) {
                return plugin.getEco().format(amount);
            }
            return formatAmount(amount) + " Vault";
        }
        return formatAmount(amount) + " " + resolveCurrencyDisplayName(normalizedCurrencyId);
    }

    private static @NotNull List<String> buildPlaytimeDetailLines(
            final @NotNull RTown town,
            final @NotNull PlaytimeRequirement playtimeRequirement
    ) {
        final List<String> lines = new ArrayList<>();
        if (playtimeRequirement.isUseTotalPlaytime()) {
            final long totalSeconds = getTownTotalPlaytimeSeconds(town);
            lines.add("Current: " + PlaytimeRequirement.formatDuration(totalSeconds));
            lines.add("Required: " + playtimeRequirement.getFormattedRequiredPlaytime());
            lines.add("Scope: Aggregate town playtime.");
            return lines;
        }

        final Map<String, Long> worldRequirements = playtimeRequirement.getWorldPlaytimeRequirements();
        int index = 0;
        for (final Map.Entry<String, Long> worldRequirement : worldRequirements.entrySet()) {
            if (index >= MAX_WORLD_PLAYTIME_LINES) {
                lines.add("+" + (worldRequirements.size() - MAX_WORLD_PLAYTIME_LINES) + " more world checks");
                break;
            }
            final long currentSeconds = getTownWorldPlaytimeSeconds(town, worldRequirement.getKey());
            lines.add(
                    worldRequirement.getKey()
                            + ": "
                            + PlaytimeRequirement.formatDuration(currentSeconds)
                            + " / "
                            + PlaytimeRequirement.formatDuration(worldRequirement.getValue())
            );
            index++;
        }
        lines.add("Scope: Aggregate town playtime.");
        return lines;
    }

    private static boolean isTownPlaytimeRequirementMet(
            final @NotNull RTown town,
            final @NotNull PlaytimeRequirement playtimeRequirement
    ) {
        if (playtimeRequirement.isUseTotalPlaytime()) {
            return getTownTotalPlaytimeSeconds(town) >= playtimeRequirement.getRequiredPlaytimeSeconds();
        }

        for (final Map.Entry<String, Long> worldRequirement : playtimeRequirement.getWorldPlaytimeRequirements().entrySet()) {
            if (getTownWorldPlaytimeSeconds(town, worldRequirement.getKey()) < worldRequirement.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static double calculateTownPlaytimeProgress(
            final @NotNull RTown town,
            final @NotNull PlaytimeRequirement playtimeRequirement
    ) {
        if (playtimeRequirement.isUseTotalPlaytime()) {
            final long requiredSeconds = playtimeRequirement.getRequiredPlaytimeSeconds();
            if (requiredSeconds <= 0L) {
                return 1.0D;
            }
            final long currentSeconds = getTownTotalPlaytimeSeconds(town);
            return Math.max(0.0D, Math.min(1.0D, (double) currentSeconds / requiredSeconds));
        }

        final Map<String, Long> worldRequirements = playtimeRequirement.getWorldPlaytimeRequirements();
        if (worldRequirements.isEmpty()) {
            return 1.0D;
        }

        double totalProgress = 0.0D;
        int requirementCount = 0;
        for (final Map.Entry<String, Long> worldRequirement : worldRequirements.entrySet()) {
            final long requiredSeconds = worldRequirement.getValue();
            if (requiredSeconds <= 0L) {
                totalProgress += 1.0D;
            } else {
                final long currentSeconds = getTownWorldPlaytimeSeconds(town, worldRequirement.getKey());
                totalProgress += Math.min(1.0D, (double) currentSeconds / requiredSeconds);
            }
            requirementCount++;
        }

        if (requirementCount == 0) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, totalProgress / requirementCount));
    }

    private static long getTownTotalPlaytimeSeconds(final @NotNull RTown town) {
        long totalSeconds = 0L;
        for (final UUID memberUuid : resolveTownMemberUuids(town)) {
            totalSeconds += getPlayerPlaytimeSeconds(memberUuid);
        }
        return Math.max(0L, totalSeconds);
    }

    private static long getTownWorldPlaytimeSeconds(
            final @NotNull RTown town,
            final @NotNull String worldName
    ) {
        long totalSeconds = 0L;
        for (final UUID memberUuid : resolveTownMemberUuids(town)) {
            totalSeconds += getPlayerWorldPlaytimeSeconds(memberUuid, worldName);
        }
        return Math.max(0L, totalSeconds);
    }

    private static long getPlayerPlaytimeSeconds(final @NotNull UUID playerUuid) {
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        try {
            return Math.max(0L, offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L);
        } catch (final Exception exception) {
            return 0L;
        }
    }

    private static long getPlayerWorldPlaytimeSeconds(
            final @NotNull UUID playerUuid,
            final @NotNull String worldName
    ) {
        if (worldName.isBlank()) {
            return getPlayerPlaytimeSeconds(playerUuid);
        }
        return getPlayerPlaytimeSeconds(playerUuid);
    }

    private static @NotNull List<UUID> resolveTownMemberUuids(final @NotNull RTown town) {
        final LinkedHashSet<UUID> memberUuids = new LinkedHashSet<>();
        memberUuids.add(town.getMayor());
        for (final RDTPlayer member : town.getMembers()) {
            final UUID memberUuid = member.getIdentifier();
            if (memberUuid == null) {
                continue;
            }
            memberUuids.add(memberUuid);
        }
        return new ArrayList<>(memberUuids);
    }

    private static boolean consumeRequirement(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement
    ) {
        if (!requirement.operational() || requirement.requirement() == null) {
            return false;
        }

        if (requirement.requirement() instanceof CurrencyRequirement currencyRequirement) {
            if (!currencyRequirement.isConsumable()) {
                town.clearLevelRequirementProgress(getRequirementProgressKey(requirement));
                return true;
            }

            final double stored = getStoredCurrencyProgress(town, requirement);
            final double remaining = Math.max(0.0D, currencyRequirement.getAmount() - stored);
            if (remaining > EPSILON && !withdrawCurrency(plugin, player, currencyRequirement.getCurrencyId(), remaining)) {
                return false;
            }
            town.clearLevelRequirementProgress(getRequirementProgressKey(requirement));
            return true;
        }

        if (requirement.requirement() instanceof ItemRequirement itemRequirement) {
            if (!itemRequirement.isConsumeOnComplete()) {
                town.clearLevelRequirementProgress(getRequirementProgressKey(requirement));
                return true;
            }

            final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
            for (int itemIndex = 0; itemIndex < requiredItems.size(); itemIndex++) {
                final ItemStack requiredItem = requiredItems.get(itemIndex);
                final int storedAmount = getStoredItemAmount(town, requirement, itemIndex);
                final int remainingAmount = Math.max(0, requiredItem.getAmount() - storedAmount);
                if (remainingAmount <= 0) {
                    continue;
                }
                if (countMatchingItems(player, requiredItem, itemRequirement.isExactMatch()) < remainingAmount) {
                    return false;
                }
            }

            for (int itemIndex = 0; itemIndex < requiredItems.size(); itemIndex++) {
                final ItemStack requiredItem = requiredItems.get(itemIndex);
                final int storedAmount = getStoredItemAmount(town, requirement, itemIndex);
                final int remainingAmount = Math.max(0, requiredItem.getAmount() - storedAmount);
                if (remainingAmount <= 0) {
                    continue;
                }
                removeMatchingItems(player, requiredItem, itemRequirement.isExactMatch(), remainingAmount);
            }

            town.clearLevelRequirementProgress(getRequirementProgressKey(requirement));
            return true;
        }

        RequirementService.getInstance().consume(player, requirement.requirement());
        town.clearLevelRequirementProgress(getRequirementProgressKey(requirement));
        return true;
    }

    private static boolean grantReward(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull ResolvedTownReward reward
    ) {
        if (!reward.operational() || reward.reward() == null) {
            return false;
        }

        if (reward.reward() instanceof CurrencyReward currencyReward) {
            return depositCurrency(plugin, player, currencyReward.getCurrencyId(), currencyReward.getAmount());
        }

        try {
            return Boolean.TRUE.equals(RewardService.getInstance().grant(player, reward.reward()).join());
        } catch (final Exception exception) {
            return false;
        }
    }

    private static boolean bankCurrencyProgress(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement,
            final @NotNull CurrencyRequirement currencyRequirement
    ) {
        final double stored = getStoredCurrencyProgress(town, requirement);
        final double remaining = Math.max(0.0D, currencyRequirement.getAmount() - stored);
        if (remaining <= EPSILON) {
            return false;
        }

        final double available = Math.max(0.0D, currencyRequirement.getCurrentBalance(player));
        final double toStore = Math.min(remaining, available);
        if (toStore <= EPSILON) {
            return false;
        }

        if (!withdrawCurrency(plugin, player, currencyRequirement.getCurrencyId(), toStore)) {
            return false;
        }

        town.setLevelCurrencyProgress(
                getRequirementProgressKey(requirement),
                stored + toStore
        );
        return true;
    }

    private static boolean bankItemProgress(
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement,
            final @NotNull ItemRequirement itemRequirement
    ) {
        boolean changed = false;
        final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
        for (int itemIndex = 0; itemIndex < requiredItems.size(); itemIndex++) {
            final ItemStack requiredItem = requiredItems.get(itemIndex);
            final int storedAmount = getStoredItemAmount(town, requirement, itemIndex);
            final int remainingAmount = Math.max(0, requiredItem.getAmount() - storedAmount);
            if (remainingAmount <= 0) {
                continue;
            }

            final int removedAmount = removeMatchingItems(
                    player,
                    requiredItem,
                    itemRequirement.isExactMatch(),
                    remainingAmount
            );
            if (removedAmount <= 0) {
                continue;
            }

            final ItemStack storedItem = requiredItem.clone();
            storedItem.setAmount(storedAmount + removedAmount);
            town.setLevelItemProgress(getItemProgressKey(requirement, itemIndex), storedItem);
            changed = true;
        }
        return changed;
    }

    private static double getStoredCurrencyProgress(
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement
    ) {
        return town.getLevelCurrencyProgress(getRequirementProgressKey(requirement));
    }

    private static int getStoredItemAmount(
            final @NotNull RTown town,
            final @NotNull ResolvedTownRequirement requirement,
            final int itemIndex
    ) {
        final ItemStack itemProgress = town.getLevelItemProgress(getItemProgressKey(requirement, itemIndex));
        if (itemProgress == null || itemProgress.isEmpty()) {
            return 0;
        }
        return itemProgress.getAmount();
    }

    private static @NotNull String getRequirementProgressKey(
            final @NotNull ResolvedTownRequirement requirement
    ) {
        return requirement.targetLevel() + ":" + requirement.key();
    }

    private static @NotNull String getItemProgressKey(
            final @NotNull ResolvedTownRequirement requirement,
            final int itemIndex
    ) {
        return getRequirementProgressKey(requirement) + ":item:" + itemIndex;
    }

    private static int countMatchingItems(
            final @NotNull Player player,
            final @NotNull ItemStack requiredItem,
            final boolean exactMatch
    ) {
        int matchingAmount = 0;
        for (final ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null) {
                continue;
            }

            final boolean matches = exactMatch
                    ? itemStack.isSimilar(requiredItem)
                    : itemStack.getType() == requiredItem.getType();
            if (!matches) {
                continue;
            }

            matchingAmount += itemStack.getAmount();
        }
        return matchingAmount;
    }

    private static int removeMatchingItems(
            final @NotNull Player player,
            final @NotNull ItemStack requiredItem,
            final boolean exactMatch,
            final int amountToRemove
    ) {
        if (amountToRemove <= 0) {
            return 0;
        }

        int removedAmount = 0;
        final ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && removedAmount < amountToRemove; slot++) {
            final ItemStack stack = contents[slot];
            if (stack == null) {
                continue;
            }

            final boolean matches = exactMatch
                    ? stack.isSimilar(requiredItem)
                    : stack.getType() == requiredItem.getType();
            if (!matches) {
                continue;
            }

            final int removable = Math.min(amountToRemove - removedAmount, stack.getAmount());
            stack.setAmount(stack.getAmount() - removable);
            removedAmount += removable;
            if (stack.getAmount() <= 0) {
                contents[slot] = null;
            }
        }

        if (removedAmount > 0) {
            player.getInventory().setContents(contents);
        }
        return removedAmount;
    }

    private static boolean withdrawCurrency(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull String currencyId,
            final double amount
    ) {
        if (amount <= EPSILON) {
            return true;
        }

        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (usesVaultCurrency(normalizedCurrencyId)) {
            return plugin.getEco() != null
                    && plugin.getEco().withdrawPlayer(player, amount).transactionSuccess();
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null
                && bridge.hasCurrency(normalizedCurrencyId)
                && Boolean.TRUE.equals(bridge.withdraw(player, normalizedCurrencyId, amount).join());
    }

    private static boolean depositCurrency(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull String currencyId,
            final double amount
    ) {
        if (amount <= EPSILON) {
            return true;
        }

        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (usesVaultCurrency(normalizedCurrencyId)) {
            return plugin.getEco() != null
                    && plugin.getEco().depositPlayer(player, amount).transactionSuccess();
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null
                && bridge.hasCurrency(normalizedCurrencyId)
                && Boolean.TRUE.equals(bridge.deposit(player, normalizedCurrencyId, amount).join());
    }

    private static @NotNull String resolveCurrencyDisplayName(final @NotNull String currencyId) {
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (usesVaultCurrency(normalizedCurrencyId)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null || !bridge.hasCurrency(normalizedCurrencyId)) {
            return currencyId;
        }
        return bridge.getCurrencyDisplayName(normalizedCurrencyId);
    }

    private static boolean usesVaultCurrency(final @NotNull String currencyId) {
        return VAULT_CURRENCY_ID.equals(normalizeCurrencyId(currencyId));
    }

    private static @NotNull String normalizeCurrencyId(final @NotNull String currencyId) {
        return currencyId.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isRequirementOperational(final @NotNull AbstractRequirement requirement) {
        if (requirement instanceof CurrencyRequirement currencyRequirement) {
            try {
                currencyRequirement.validate();
                return true;
            } catch (final IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof LocationRequirement locationRequirement) {
            try {
                locationRequirement.validate();
                return true;
            } catch (final IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof PlaytimeRequirement playtimeRequirement) {
            try {
                playtimeRequirement.validate();
                return true;
            } catch (final IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof ExperienceLevelRequirement experienceLevelRequirement) {
            try {
                experienceLevelRequirement.validate();
                return true;
            } catch (final IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof PermissionRequirement permissionRequirement) {
            try {
                permissionRequirement.validate();
                return true;
            } catch (final IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof TimedRequirement timedRequirement) {
            try {
                timedRequirement.validate();
                return true;
            } catch (final IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof CompositeRequirement compositeRequirement) {
            try {
                compositeRequirement.validate();
                return true;
            } catch (final IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof ChoiceRequirement choiceRequirement) {
            try {
                choiceRequirement.validate();
                return true;
            } catch (final IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof PluginRequirement pluginRequirement) {
            try {
                pluginRequirement.validate();
                return true;
            } catch (final IllegalStateException exception) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRewardOperational(final @NotNull AbstractReward reward) {
        try {
            reward.validate();
            return true;
        } catch (final Exception exception) {
            return false;
        }
    }

    private static @NotNull String describeRequirement(
            final @NotNull RDT plugin,
            final @NotNull AbstractRequirement requirement,
            final @NotNull Map<String, Object> definition
    ) {
        if (requirement instanceof CurrencyRequirement currencyRequirement) {
            return currencyRequirement.getCurrencyDisplayName()
                    + ": "
                    + formatCurrency(plugin, currencyRequirement.getCurrencyId(), currencyRequirement.getAmount());
        }
        if (requirement instanceof ItemRequirement itemRequirement) {
            final List<String> itemParts = new ArrayList<>();
            for (final ItemStack itemStack : itemRequirement.getRequiredItems()) {
                itemParts.add(itemStack.getAmount() + "x " + itemStack.getType().name().toLowerCase(Locale.ROOT));
            }
            return "Items: " + String.join(", ", itemParts);
        }
        if (requirement instanceof PlaytimeRequirement playtimeRequirement) {
            return "Playtime: " + playtimeRequirement.getFormattedRequiredPlaytime();
        }
        if (requirement instanceof ExperienceLevelRequirement experienceLevelRequirement) {
            final String unit = experienceLevelRequirement.isLevelBased() ? "levels" : "points";
            return "Experience: " + experienceLevelRequirement.getRequiredLevel() + " " + unit;
        }
        if (requirement instanceof PermissionRequirement permissionRequirement) {
            return "Permissions: "
                    + permissionRequirement.getPermissionMode().name().toLowerCase(Locale.ROOT)
                    + " "
                    + permissionRequirement.getRequiredPermissions().size();
        }
        if (requirement instanceof TimedRequirement timedRequirement) {
            return "Timed: "
                    + PlaytimeRequirement.formatDuration(
                    TimeUnit.MILLISECONDS.toSeconds(timedRequirement.getTimeLimitMillis())
            );
        }
        if (requirement instanceof CompositeRequirement compositeRequirement) {
            return "Composite: "
                    + compositeRequirement.getOperator().name().toLowerCase(Locale.ROOT)
                    + " ("
                    + compositeRequirement.getRequirements().size()
                    + ")";
        }
        if (requirement instanceof ChoiceRequirement choiceRequirement) {
            return "Choice: "
                    + choiceRequirement.getMinimumChoicesRequired()
                    + " of "
                    + choiceRequirement.getChoices().size();
        }
        if (requirement instanceof LocationRequirement locationRequirement) {
            return "Location: " + formatLocation(locationRequirement);
        }
        if (requirement instanceof PluginRequirement pluginRequirement) {
            return "Plugin: " + pluginRequirement.getPluginIntegrationId();
        }

        final String description = String.valueOf(definition.getOrDefault("description", ""));
        if (!description.isBlank()) {
            return description;
        }
        return requirement.getTypeId().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static @NotNull String describeReward(
            final @NotNull AbstractReward reward,
            final @NotNull Map<String, Object> definition,
            final @NotNull String key
    ) {
        if (reward instanceof CurrencyReward currencyReward) {
            return "Currency: " + formatAmount(currencyReward.getAmount()) + " " + currencyReward.getCurrencyId();
        }
        if (reward instanceof ItemReward itemReward) {
            return "Item: " + itemReward.getAmount() + "x " + itemReward.getItem().getType().name().toLowerCase(Locale.ROOT);
        }

        final String description = String.valueOf(definition.getOrDefault("description", ""));
        if (!description.isBlank()) {
            return description;
        }
        return formatLabel(key);
    }

    private static @NotNull String describeMissingDefinition(
            final @NotNull String key,
            final @NotNull Map<String, Object> definition
    ) {
        final String description = String.valueOf(definition.getOrDefault("description", ""));
        if (!description.isBlank()) {
            return description;
        }
        return formatLabel(key);
    }

    private static @NotNull String formatLocation(final @NotNull LocationRequirement requirement) {
        if (requirement.getRequiredCoordinates() != null) {
            return requirement.getRequiredCoordinates().toString();
        }
        if (requirement.getRequiredRegion() != null) {
            return requirement.getRequiredRegion();
        }
        if (requirement.getRequiredWorld() != null) {
            return requirement.getRequiredWorld();
        }
        return "configured";
    }

    private static @Nullable Material parseConfiguredIconMaterial(final @NotNull Map<String, Object> definition) {
        final Object iconObject = definition.get("icon");
        if (!(iconObject instanceof Map<?, ?> iconMap)) {
            return null;
        }
        final Object iconType = iconMap.get("type");
        if (iconType == null) {
            return null;
        }
        final String iconTypeString = iconType.toString().trim();
        if (iconTypeString.isBlank()) {
            return null;
        }

        return Material.matchMaterial(iconTypeString.toUpperCase(Locale.ROOT));
    }

    private static @NotNull String normalizeDefinitionKey(final @NotNull String key) {
        final String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "entry" : normalized;
    }

    private static @NotNull String formatLabel(final @NotNull String value) {
        final String normalized = value.replace('-', ' ').replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        final String[] words = normalized.split("\\s+");
        final StringBuilder builder = new StringBuilder();
        for (final String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.length() == 0 ? value : builder.toString();
    }

    private static boolean thisOrGreater(final double actual, final double required) {
        return actual + EPSILON >= required;
    }

    private static @NotNull Map<String, Object> deepCopyMap(final @NotNull Map<String, Object> source) {
        final Map<String, Object> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    private static @Nullable Object deepCopyValue(final @Nullable Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            final Map<String, Object> nested = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                nested.put(entry.getKey().toString(), deepCopyValue(entry.getValue()));
            }
            return nested;
        }
        if (value instanceof List<?> listValue) {
            final List<Object> nested = new ArrayList<>(listValue.size());
            for (final Object entry : listValue) {
                nested.add(deepCopyValue(entry));
            }
            return nested;
        }
        return value;
    }

    record ResolvedTownRequirement(
            int targetLevel,
            @NotNull String key,
            @NotNull Map<String, Object> definition,
            @Nullable AbstractRequirement requirement,
            @NotNull String summary,
            boolean operational
    ) {

        ResolvedTownRequirement {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(summary, "summary");
        }
    }

    record ResolvedTownReward(
            int targetLevel,
            @NotNull String key,
            @NotNull Map<String, Object> definition,
            @Nullable AbstractReward reward,
            @NotNull String summary,
            boolean operational
    ) {

        ResolvedTownReward {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(summary, "summary");
        }
    }

    enum RequirementAvailability {
        READY,
        PENDING,
        UNAVAILABLE
    }

    enum ProgressUpdateStatus {
        UNSUPPORTED,
        UNAVAILABLE,
        NO_PROGRESS,
        PROGRESSED,
        COMPLETE
    }

    record ProgressUpdateResult(
            @NotNull ProgressUpdateStatus status,
            int progressPercentage,
            boolean changed
    ) {

        ProgressUpdateResult {
            Objects.requireNonNull(status, "status");
        }
    }

    enum LevelUpStatus {
        SUCCESS,
        SUCCESS_WITH_REWARD_ERRORS,
        MAX_LEVEL,
        REQUIREMENT_UNAVAILABLE,
        REQUIREMENT_UNMET,
        CONSUME_FAILED,
        SYSTEM_UNAVAILABLE
    }

    record LevelUpResult(
            @NotNull LevelUpStatus status,
            int previousLevel,
            int newLevel,
            @NotNull String failedRequirement,
            @NotNull String requirementSummary,
            @NotNull String rewardSummary,
            int grantedRewardCount,
            int failedRewardCount
    ) {

        LevelUpResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(failedRequirement, "failedRequirement");
            Objects.requireNonNull(requirementSummary, "requirementSummary");
            Objects.requireNonNull(rewardSummary, "rewardSummary");
        }

        boolean success() {
            return this.status == LevelUpStatus.SUCCESS
                    || this.status == LevelUpStatus.SUCCESS_WITH_REWARD_ERRORS;
        }
    }
}
