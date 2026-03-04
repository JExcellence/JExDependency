/*
 * ShopStorePricingSupport.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.view.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.devnatan.inventoryframework.context.Context;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.configs.StoreRequirementSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
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

/**
 * Provides support utilities for shop store pricing.
 */
final class ShopStorePricingSupport {

    private static final double EPSILON = 1.0E-6D;

    private ShopStorePricingSupport() {
    }

    static @NotNull List<ResolvedStoreRequirement> getConfiguredStoreRequirements(
        final @NotNull RDS plugin,
        final @NotNull ConfigSection config,
        final @NotNull Player player,
        final int purchaseNumber
    ) {
        final RequirementFactory factory = RequirementFactory.getInstance();
        final List<ResolvedStoreRequirement> requirements = new ArrayList<>();
        final Map<String, StoreRequirementSection> purchaseRequirements = config.getRequirementsForPurchase(purchaseNumber);
        for (final Map.Entry<String, StoreRequirementSection> entry : purchaseRequirements.entrySet()) {
            final String key = entry.getKey();
            final StoreRequirementSection section = entry.getValue();
            try {
                final AbstractRequirement requirement = factory.fromMap(section.toRequirementMap());
                requirements.add(new ResolvedStoreRequirement(
                    purchaseNumber,
                    key,
                    section,
                    requirement,
                    describeRequirement(plugin, requirement, section),
                    isRequirementOperational(requirement)
                ));
            } catch (Exception exception) {
                requirements.add(new ResolvedStoreRequirement(
                    purchaseNumber,
                    key,
                    section,
                    null,
                    describeMissingRequirement(section),
                    false
                ));
            }
        }

        return requirements;
    }

    static @NotNull PurchaseResult purchaseShop(
        final @NotNull Context context,
        final @NotNull RDS plugin,
        final @NotNull ConfigSection config,
        final int purchaseNumber,
        final @Nullable RDSPlayer progressPlayer
    ) {
        final Player player = context.getPlayer();
        final List<ResolvedStoreRequirement> requirements = getConfiguredStoreRequirements(
            plugin,
            config,
            player,
            purchaseNumber
        );
        if (requirements.isEmpty()) {
            return PurchaseResult.successful("");
        }

        final String summary = formatRequirementSummary(requirements);
        for (final ResolvedStoreRequirement requirement : requirements) {
            if (!requirement.operational() || requirement.requirement() == null) {
                return PurchaseResult.failure("feedback.requirement_unavailable", requirement.summary(), summary);
            }
        }

        for (final ResolvedStoreRequirement requirement : requirements) {
            if (!isRequirementMet(player, requirement, progressPlayer)) {
                return PurchaseResult.failure("feedback.requirement_unmet", requirement.summary(), summary);
            }
        }

        for (final ResolvedStoreRequirement requirement : requirements) {
            if (!consumeRequirement(plugin, player, requirement, progressPlayer)) {
                return PurchaseResult.failure("feedback.requirement_unmet", requirement.summary(), summary);
            }
        }

        return PurchaseResult.successful(summary);
    }

    static @NotNull String formatCurrency(
        final @NotNull RDS plugin,
        final @NotNull String currencyType,
        final double amount
    ) {
        if (usesVaultCurrency(currencyType)) {
            return plugin.formatVaultCurrency(amount);
        }

        return formatAmount(amount) + " " + getCurrencyDisplayName(currencyType);
    }

    static @NotNull String getCurrencyDisplayName(final @NotNull String currencyType) {
        if (usesVaultCurrency(currencyType)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge == null ? currencyType : bridge.getCurrencyDisplayName(currencyType);
    }

    static @NotNull String formatRequirementSummary(
        final @NotNull List<ResolvedStoreRequirement> requirements
    ) {
        final List<String> parts = new ArrayList<>();
        for (final ResolvedStoreRequirement requirement : requirements) {
            parts.add(requirement.summary());
        }
        return String.join(", ", parts);
    }

    static @NotNull String formatAmount(final double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    static @NotNull RequirementAvailability resolveAvailability(
        final @NotNull Player player,
        final @NotNull List<ResolvedStoreRequirement> requirements
    ) {
        return resolveAvailability(player, requirements, null);
    }

    static @NotNull RequirementAvailability resolveAvailability(
        final @NotNull Player player,
        final @NotNull List<ResolvedStoreRequirement> requirements,
        final @Nullable RDSPlayer progressPlayer
    ) {
        for (final ResolvedStoreRequirement requirement : requirements) {
            if (!requirement.operational() || requirement.requirement() == null) {
                return RequirementAvailability.UNAVAILABLE;
            }
        }

        for (final ResolvedStoreRequirement requirement : requirements) {
            if (!isRequirementMet(player, requirement, progressPlayer)) {
                return RequirementAvailability.PENDING;
            }
        }

        return RequirementAvailability.READY;
    }

    static boolean isRequirementMet(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ResolvedStoreRequirement requirement
    ) {
        return isRequirementMet(
            player,
            requirement,
            findProgressPlayer(plugin, player)
        );
    }

    static int getProgressPercentage(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ResolvedStoreRequirement requirement
    ) {
        return getProgressPercentage(player, requirement, findProgressPlayer(plugin, player));
    }

    static double calculateProgress(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ResolvedStoreRequirement requirement
    ) {
        return calculateProgress(player, requirement, findProgressPlayer(plugin, player));
    }

    static @NotNull String buildProgressBar(final int percentage) {
        final int normalizedPercentage = Math.max(0, Math.min(100, percentage));
        final int totalSegments = 10;
        final int filledSegments = Math.min(totalSegments, (int) Math.round(normalizedPercentage / 10.0D));
        final StringBuilder builder = new StringBuilder(totalSegments + 2);
        builder.append('[');
        for (int index = 0; index < totalSegments; index++) {
            builder.append(index < filledSegments ? '#' : '-');
        }
        builder.append(']');
        return builder.toString();
    }

    static double getStoredCurrencyProgress(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ResolvedStoreRequirement requirement
    ) {
        return getStoredCurrencyProgress(findProgressPlayer(plugin, player), requirement);
    }

    static int getStoredItemAmount(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ResolvedStoreRequirement requirement,
        final int itemIndex
    ) {
        return getStoredItemAmount(findProgressPlayer(plugin, player), requirement, itemIndex);
    }

    static @NotNull ProgressUpdateResult bankRequirementProgress(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @Nullable RDSPlayer progressPlayer,
        final @NotNull ResolvedStoreRequirement requirement
    ) {
        if (progressPlayer == null || plugin.getPlayerRepository() == null) {
            return new ProgressUpdateResult(ProgressUpdateStatus.PROFILE_MISSING, getProgressPercentage(plugin, player, requirement));
        }
        if (!requirement.operational() || requirement.requirement() == null) {
            return new ProgressUpdateResult(ProgressUpdateStatus.UNAVAILABLE, 0);
        }

        if (requirement.requirement() instanceof CurrencyRequirement currencyRequirement
            && !currencyRequirement.isConsumable()) {
            return new ProgressUpdateResult(ProgressUpdateStatus.UNSUPPORTED, getProgressPercentage(plugin, player, requirement));
        }
        if (requirement.requirement() instanceof ItemRequirement itemRequirement
            && !itemRequirement.isConsumeOnComplete()) {
            return new ProgressUpdateResult(ProgressUpdateStatus.UNSUPPORTED, getProgressPercentage(plugin, player, requirement));
        }

        final boolean changed = switch (requirement.requirement()) {
            case CurrencyRequirement currencyRequirement -> bankCurrencyProgress(plugin, player, progressPlayer, requirement, currencyRequirement);
            case ItemRequirement itemRequirement -> bankItemProgress(player, progressPlayer, requirement, itemRequirement);
            default -> false;
        };

        if (!(requirement.requirement() instanceof CurrencyRequirement) && !(requirement.requirement() instanceof ItemRequirement)) {
            return new ProgressUpdateResult(ProgressUpdateStatus.UNSUPPORTED, getProgressPercentage(plugin, player, requirement));
        }

        final int progressPercentage = getProgressPercentage(player, requirement, progressPlayer);
        if (!changed) {
            return new ProgressUpdateResult(
                isRequirementMet(player, requirement, progressPlayer)
                    ? ProgressUpdateStatus.COMPLETE
                    : ProgressUpdateStatus.NO_PROGRESS,
                progressPercentage
            );
        }

        plugin.getPlayerRepository().update(progressPlayer);
        return new ProgressUpdateResult(
            isRequirementMet(player, requirement, progressPlayer)
                ? ProgressUpdateStatus.COMPLETE
                : ProgressUpdateStatus.PROGRESSED,
            progressPercentage
        );
    }

    private static boolean usesVaultCurrency(final @NotNull String currencyType) {
        return "vault".equalsIgnoreCase(currencyType);
    }

    private static boolean isRequirementMet(
        final @NotNull Player player,
        final @NotNull ResolvedStoreRequirement requirement,
        final @Nullable RDSPlayer progressPlayer
    ) {
        if (!requirement.operational() || requirement.requirement() == null) {
            return false;
        }

        if (requirement.requirement() instanceof CurrencyRequirement currencyRequirement) {
            final double storedCurrencyProgress = currencyRequirement.isConsumable()
                ? getStoredCurrencyProgress(progressPlayer, requirement)
                : 0.0D;
            return thisOrGreater(
                currencyRequirement.getCurrentBalance(player) + storedCurrencyProgress,
                currencyRequirement.getAmount()
            );
        }

        if (requirement.requirement() instanceof ItemRequirement itemRequirement) {
            final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
            for (int index = 0; index < requiredItems.size(); index++) {
                final ItemStack requiredItem = requiredItems.get(index);
                final int storedAmount = itemRequirement.isConsumeOnComplete()
                    ? getStoredItemAmount(progressPlayer, requirement, index)
                    : 0;
                final int liveAmount = countMatchingItems(player, requiredItem, itemRequirement.isExactMatch());
                if (storedAmount + liveAmount < requiredItem.getAmount()) {
                    return false;
                }
            }
            return true;
        }

        return requirement.requirement().isMet(player);
    }

    private static double calculateProgress(
        final @NotNull Player player,
        final @NotNull ResolvedStoreRequirement requirement,
        final @Nullable RDSPlayer progressPlayer
    ) {
        if (!requirement.operational() || requirement.requirement() == null) {
            return 0.0D;
        }

        if (requirement.requirement() instanceof CurrencyRequirement currencyRequirement) {
            if (currencyRequirement.getAmount() <= 0.0D) {
                return 1.0D;
            }
            final double storedCurrencyProgress = currencyRequirement.isConsumable()
                ? getStoredCurrencyProgress(progressPlayer, requirement)
                : 0.0D;
            final double progressAmount = currencyRequirement.getCurrentBalance(player) + storedCurrencyProgress;
            return Math.max(0.0D, Math.min(1.0D, progressAmount / currencyRequirement.getAmount()));
        }

        if (requirement.requirement() instanceof ItemRequirement itemRequirement) {
            final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
            if (requiredItems.isEmpty()) {
                return 1.0D;
            }

            double collected = 0.0D;
            double requiredTotal = 0.0D;
            for (int index = 0; index < requiredItems.size(); index++) {
                final ItemStack requiredItem = requiredItems.get(index);
                final int requiredAmount = requiredItem.getAmount();
                final int storedAmount = itemRequirement.isConsumeOnComplete()
                    ? getStoredItemAmount(progressPlayer, requirement, index)
                    : 0;
                final int liveAmount = countMatchingItems(player, requiredItem, itemRequirement.isExactMatch());
                collected += Math.min(requiredAmount, storedAmount + liveAmount);
                requiredTotal += requiredAmount;
            }
            return requiredTotal <= 0.0D ? 1.0D : Math.max(0.0D, Math.min(1.0D, collected / requiredTotal));
        }

        return Math.max(0.0D, Math.min(1.0D, requirement.requirement().calculateProgress(player)));
    }

    private static boolean isRequirementOperational(final @NotNull AbstractRequirement requirement) {
        if (requirement instanceof CurrencyRequirement currencyRequirement) {
            try {
                currencyRequirement.validate();
                return true;
            } catch (IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof LocationRequirement locationRequirement) {
            try {
                locationRequirement.validate();
                return true;
            } catch (IllegalStateException exception) {
                return false;
            }
        }
        if (requirement instanceof PluginRequirement pluginRequirement) {
            try {
                pluginRequirement.validate();
                return true;
            } catch (IllegalStateException exception) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeRequirement(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ResolvedStoreRequirement requirement,
        final @Nullable RDSPlayer progressPlayer
    ) {
        if (!requirement.operational() || requirement.requirement() == null) {
            return false;
        }

        if (requirement.requirement() instanceof CurrencyRequirement currencyRequirement) {
            if (!currencyRequirement.isConsumable()) {
                clearStoredProgress(progressPlayer, requirement);
                return true;
            }
            final double storedAmount = getStoredCurrencyProgress(progressPlayer, requirement);
            final double remainingAmount = Math.max(0.0D, currencyRequirement.getAmount() - storedAmount);
            if (remainingAmount > EPSILON && !withdrawCurrency(plugin, player, currencyRequirement.getCurrencyId(), remainingAmount)) {
                return false;
            }
            clearStoredProgress(progressPlayer, requirement);
            return true;
        }

        if (requirement.requirement() instanceof ItemRequirement itemRequirement) {
            if (!itemRequirement.isConsumeOnComplete()) {
                clearStoredProgress(progressPlayer, requirement);
                return true;
            }
            final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
            for (int index = 0; index < requiredItems.size(); index++) {
                final ItemStack requiredItem = requiredItems.get(index);
                final int storedAmount = getStoredItemAmount(progressPlayer, requirement, index);
                final int remainingAmount = Math.max(0, requiredItem.getAmount() - storedAmount);
                if (remainingAmount <= 0) {
                    continue;
                }
                if (countMatchingItems(player, requiredItem, itemRequirement.isExactMatch()) < remainingAmount) {
                    return false;
                }
            }

            for (int index = 0; index < requiredItems.size(); index++) {
                final ItemStack requiredItem = requiredItems.get(index);
                final int storedAmount = getStoredItemAmount(progressPlayer, requirement, index);
                final int remainingAmount = Math.max(0, requiredItem.getAmount() - storedAmount);
                if (remainingAmount <= 0) {
                    continue;
                }
                removeMatchingItems(player, requiredItem, itemRequirement.isExactMatch(), remainingAmount);
            }

            clearStoredProgress(progressPlayer, requirement);
            return true;
        }

        RequirementService.getInstance().consume(player, requirement.requirement());
        clearStoredProgress(progressPlayer, requirement);
        return true;
    }

    static @NotNull String describeRequirement(
        final @NotNull RDS plugin,
        final @NotNull AbstractRequirement requirement
    ) {
        return describeRequirement(plugin, requirement, null);
    }

    private static @NotNull String describeRequirement(
        final @NotNull RDS plugin,
        final @NotNull AbstractRequirement requirement,
        final @Nullable StoreRequirementSection section
    ) {
        if (requirement instanceof CurrencyRequirement currencyRequirement) {
            return currencyRequirement.getCurrencyDisplayName() + ": "
                + formatCurrency(
                    plugin,
                    currencyRequirement.getCurrencyId(),
                    currencyRequirement.getAmount()
                );
        }
        if (requirement instanceof ItemRequirement itemRequirement) {
            return "Items: " + formatItems(itemRequirement.getRequiredItems());
        }
        if (requirement instanceof ExperienceLevelRequirement experienceRequirement) {
            return experienceRequirement.isLevelBased()
                ? "Levels: " + experienceRequirement.getRequiredLevel()
                : "Experience: " + experienceRequirement.getRequiredLevel();
        }
        if (requirement instanceof PermissionRequirement permissionRequirement) {
            return "Permission: " + String.join(" | ", permissionRequirement.getRequiredPermissions());
        }
        if (requirement instanceof PlaytimeRequirement playtimeRequirement) {
            return "Playtime: " + playtimeRequirement.getFormattedRequiredPlaytime();
        }
        if (requirement instanceof LocationRequirement locationRequirement) {
            return "Location: " + formatLocation(locationRequirement);
        }
        if (requirement instanceof PluginRequirement pluginRequirement) {
            return "Plugin: " + pluginRequirement.getPluginIntegrationId() + " " + pluginRequirement.getRequiredValues();
        }
        if (requirement instanceof CompositeRequirement compositeRequirement) {
            return "Composite(" + compositeRequirement.getOperator().name() + "): "
                + compositeRequirement.getRequirements().size() + " checks";
        }
        if (requirement instanceof ChoiceRequirement choiceRequirement) {
            return "Choice: " + choiceRequirement.getMinimumChoicesRequired() + " of "
                + choiceRequirement.getChoices().size();
        }
        if (requirement instanceof TimedRequirement timedRequirement) {
            return "Timed: "
                + describeRequirement(plugin, timedRequirement.getDelegate())
                + " within "
                + PlaytimeRequirement.formatDuration(timedRequirement.getTimeLimitSeconds());
        }

        final String description = section == null ? null : section.getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }
        return requirement.getTypeId().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static @NotNull String describeMissingRequirement(final @NotNull StoreRequirementSection section) {
        final String description = section.getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }
        return section.getType().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static @NotNull String formatItems(final @NotNull List<ItemStack> requiredItems) {
        final List<String> parts = new ArrayList<>();
        for (final ItemStack itemStack : requiredItems) {
            parts.add(itemStack.getAmount() + "x " + itemStack.getType().name().toLowerCase(Locale.ROOT));
        }
        return String.join(", ", parts);
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

    private static boolean bankCurrencyProgress(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull RDSPlayer progressPlayer,
        final @NotNull ResolvedStoreRequirement requirement,
        final @NotNull CurrencyRequirement currencyRequirement
    ) {
        final double storedAmount = getStoredCurrencyProgress(progressPlayer, requirement);
        final double remainingAmount = Math.max(0.0D, currencyRequirement.getAmount() - storedAmount);
        if (remainingAmount <= EPSILON) {
            return false;
        }

        final double availableBalance = Math.max(0.0D, currencyRequirement.getCurrentBalance(player));
        final double amountToStore = Math.min(remainingAmount, availableBalance);
        if (amountToStore <= EPSILON) {
            return false;
        }

        if (!withdrawCurrency(plugin, player, currencyRequirement.getCurrencyId(), amountToStore)) {
            return false;
        }

        progressPlayer.setStoreCurrencyProgress(
            getRequirementProgressKey(requirement),
            storedAmount + amountToStore
        );
        return true;
    }

    private static int getProgressPercentage(
        final @NotNull Player player,
        final @NotNull ResolvedStoreRequirement requirement,
        final @Nullable RDSPlayer progressPlayer
    ) {
        return (int) Math.round(calculateProgress(player, requirement, progressPlayer) * 100.0D);
    }

    private static boolean bankItemProgress(
        final @NotNull Player player,
        final @NotNull RDSPlayer progressPlayer,
        final @NotNull ResolvedStoreRequirement requirement,
        final @NotNull ItemRequirement itemRequirement
    ) {
        boolean changed = false;
        final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
        for (int index = 0; index < requiredItems.size(); index++) {
            final ItemStack requiredItem = requiredItems.get(index);
            final int storedAmount = getStoredItemAmount(progressPlayer, requirement, index);
            final int remainingAmount = Math.max(0, requiredItem.getAmount() - storedAmount);
            if (remainingAmount <= 0) {
                continue;
            }

            final int removedAmount = removeMatchingItems(player, requiredItem, itemRequirement.isExactMatch(), remainingAmount);
            if (removedAmount <= 0) {
                continue;
            }

            final ItemStack storedItem = requiredItem.clone();
            storedItem.setAmount(storedAmount + removedAmount);
            progressPlayer.setStoreItemProgress(getItemProgressKey(requirement, index), storedItem);
            changed = true;
        }
        return changed;
    }

    private static int getStoredItemAmount(
        final @Nullable RDSPlayer progressPlayer,
        final @NotNull ResolvedStoreRequirement requirement,
        final int itemIndex
    ) {
        if (progressPlayer == null) {
            return 0;
        }

        final ItemStack storedItem = progressPlayer.getStoreItemProgress(getItemProgressKey(requirement, itemIndex));
        return storedItem == null || storedItem.isEmpty() ? 0 : storedItem.getAmount();
    }

    private static double getStoredCurrencyProgress(
        final @Nullable RDSPlayer progressPlayer,
        final @NotNull ResolvedStoreRequirement requirement
    ) {
        return progressPlayer == null
            ? 0.0D
            : progressPlayer.getStoreCurrencyProgress(getRequirementProgressKey(requirement));
    }

    private static void clearStoredProgress(
        final @Nullable RDSPlayer progressPlayer,
        final @NotNull ResolvedStoreRequirement requirement
    ) {
        if (progressPlayer == null) {
            return;
        }
        progressPlayer.clearStoreRequirementProgress(getRequirementProgressKey(requirement));
    }

    private static @Nullable RDSPlayer findProgressPlayer(
        final @NotNull RDS plugin,
        final @NotNull Player player
    ) {
        return plugin.getPlayerRepository() == null
            ? null
            : plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
    }

    private static @NotNull String getRequirementProgressKey(final @NotNull ResolvedStoreRequirement requirement) {
        return requirement.purchaseNumber() + ":" + requirement.key();
    }

    private static @NotNull String getItemProgressKey(
        final @NotNull ResolvedStoreRequirement requirement,
        final int itemIndex
    ) {
        return getRequirementProgressKey(requirement) + ":item:" + itemIndex;
    }

    private static boolean thisOrGreater(final double actual, final double required) {
        return actual + EPSILON >= required;
    }

    private static int countMatchingItems(
        final @NotNull Player player,
        final @NotNull ItemStack requiredItem,
        final boolean exactMatch
    ) {
        int matchingAmount = 0;
        for (final ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) {
                continue;
            }

            final boolean matches = exactMatch
                ? stack.isSimilar(requiredItem)
                : stack.getType() == requiredItem.getType();
            if (!matches) {
                continue;
            }

            matchingAmount += stack.getAmount();
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
        for (int index = 0; index < contents.length && removedAmount < amountToRemove; index++) {
            final ItemStack stack = contents[index];
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
                contents[index] = null;
            }
        }

        if (removedAmount > 0) {
            player.getInventory().setContents(contents);
        }
        return removedAmount;
    }

    private static boolean withdrawCurrency(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull String currencyId,
        final double amount
    ) {
        if (amount <= EPSILON) {
            return true;
        }

        if (usesVaultCurrency(currencyId)) {
            return plugin.withdrawVault(player, amount);
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null && bridge.withdraw(player, currencyId, amount).join();
    }

    record ResolvedStoreRequirement(
        int purchaseNumber,
        @NotNull String key,
        @NotNull StoreRequirementSection section,
        @Nullable AbstractRequirement requirement,
        @NotNull String summary,
        boolean operational
    ) {
    }

    record PurchaseResult(
        boolean success,
        @NotNull String failureKey,
        @NotNull String failedRequirement,
        @NotNull String requirementSummary
    ) {
        static @NotNull PurchaseResult successful(final @NotNull String requirementSummary) {
            return new PurchaseResult(true, "", "", requirementSummary);
        }

        static @NotNull PurchaseResult failure(
            final @NotNull String failureKey,
            final @NotNull String failedRequirement,
            final @NotNull String requirementSummary
        ) {
            return new PurchaseResult(
                false,
                failureKey,
                failedRequirement,
                requirementSummary
            );
        }
    }

    enum RequirementAvailability {
        READY,
        PENDING,
        UNAVAILABLE
    }

    record ProgressUpdateResult(
        @NotNull ProgressUpdateStatus status,
        int progressPercentage
    ) {
    }

    enum ProgressUpdateStatus {
        PROFILE_MISSING,
        UNSUPPORTED,
        UNAVAILABLE,
        NO_PROGRESS,
        PROGRESSED,
        COMPLETE
    }
}