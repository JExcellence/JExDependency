/*
 * ShopStoreRequirementBrowserSupport.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.view.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
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

final class ShopStoreRequirementBrowserSupport {

    private static final int MAX_ITEM_LINES = 4;
    private static final int MAX_CHILD_LINES = 3;
    private static final int MAX_PERMISSION_LINES = 4;
    private static final int MAX_PLUGIN_LINES = 4;
    private static final int MAX_WORLD_LINES = 3;

    private ShopStoreRequirementBrowserSupport() {
    }

    static @NotNull Material resolveMaterial(
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement requirement
    ) {
        final Material configuredMaterial = parseMaterial(requirement.section().getIcon().getType());
        if (configuredMaterial != null) {
            return configuredMaterial;
        }

        final String typeId = requirement.requirement() == null
            ? requirement.section().getType()
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

    static @NotNull String resolveDisplayName(
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement requirement
    ) {
        final String description = requirement.section().getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }

        return formatLabel(requirement.key());
    }

    static boolean isRequirementMet(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement requirement
    ) {
        return ShopStorePricingSupport.isRequirementMet(plugin, player, requirement);
    }

    static int getProgressPercentage(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement requirement
    ) {
        return ShopStorePricingSupport.getProgressPercentage(plugin, player, requirement);
    }

    static @NotNull List<String> buildDetailLines(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement requirement
    ) {
        if (!requirement.operational() || requirement.requirement() == null) {
            return List.of("Requirement data is unavailable on this server.");
        }

        return buildDetailLines(plugin, player, requirement, requirement.requirement());
    }

    private static @NotNull List<String> buildDetailLines(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement resolvedRequirement,
        final @NotNull AbstractRequirement requirement
    ) {
        final List<String> lines = new ArrayList<>();

        if (requirement instanceof CurrencyRequirement currencyRequirement) {
            final double bankedAmount = ShopStorePricingSupport.getStoredCurrencyProgress(
                plugin,
                player,
                resolvedRequirement
            );
            lines.add(
                "Balance: "
                    + ShopStorePricingSupport.formatCurrency(
                    plugin,
                    currencyRequirement.getCurrencyId(),
                    currencyRequirement.getCurrentBalance(player)
                )
                    + " / "
                    + ShopStorePricingSupport.formatCurrency(
                    plugin,
                    currencyRequirement.getCurrencyId(),
                    currencyRequirement.getAmount()
                )
            );
            if (bankedAmount > 0.0D) {
                lines.add(
                    "Banked: "
                        + ShopStorePricingSupport.formatCurrency(
                        plugin,
                        currencyRequirement.getCurrencyId(),
                        bankedAmount
                    )
                );
                lines.add(
                    "Remaining: "
                        + ShopStorePricingSupport.formatCurrency(
                        plugin,
                        currencyRequirement.getCurrencyId(),
                        Math.max(0.0D, currencyRequirement.getAmount() - bankedAmount)
                    )
                );
            }
            if (currencyRequirement.isConsumable()) {
                lines.add("Right-click to bank available currency progress.");
            }
            if (!currencyRequirement.isConsumable()) {
                lines.add("This currency is checked but not consumed.");
            }
            return lines;
        }

        if (requirement instanceof ItemRequirement itemRequirement) {
            final List<ItemRequirement.ItemProgress> itemProgress = itemRequirement.getDetailedProgress(player);
            for (int index = 0; index < Math.min(itemProgress.size(), MAX_ITEM_LINES); index++) {
                final ItemRequirement.ItemProgress progress = itemProgress.get(index);
                final int bankedAmount = ShopStorePricingSupport.getStoredItemAmount(
                    plugin,
                    player,
                    resolvedRequirement,
                    index
                );
                final int effectiveAmount = Math.min(
                    progress.requiredAmount(),
                    progress.currentAmount() + bankedAmount
                );
                lines.add(
                    effectiveAmount
                        + "/"
                        + progress.requiredAmount()
                        + "x "
                        + formatLabel(progress.requiredItem().getType().name())
                );
                if (bankedAmount > 0) {
                    lines.add("  banked " + bankedAmount + "x");
                }
            }
            if (itemProgress.size() > MAX_ITEM_LINES) {
                lines.add("+" + (itemProgress.size() - MAX_ITEM_LINES) + " more item checks");
            }
            if (itemRequirement.isConsumeOnComplete()) {
                lines.add("Right-click to bank matching items.");
            } else {
                lines.add("These items are checked but not consumed.");
            }
            lines.add(itemRequirement.isExactMatch() ? "Exact item match required." : "Material-only match allowed.");
            return lines;
        }

        if (requirement instanceof ExperienceLevelRequirement experienceRequirement) {
            final String unit = experienceRequirement.isLevelBased() ? "levels" : "points";
            lines.add("Current: " + experienceRequirement.getCurrentExperience(player) + " " + unit);
            lines.add("Required: " + experienceRequirement.getRequiredLevel() + " " + unit);
            if (experienceRequirement.getShortage(player) > 0) {
                lines.add("Missing: " + experienceRequirement.getShortage(player) + " " + unit);
            }
            return lines;
        }

        if (requirement instanceof PlaytimeRequirement playtimeRequirement) {
            lines.add("Current: " + playtimeRequirement.getFormattedCurrentPlaytime(player));
            lines.add("Required: " + playtimeRequirement.getFormattedRequiredPlaytime());
            if (!playtimeRequirement.isUseTotalPlaytime()) {
                int index = 0;
                for (final Map.Entry<String, Long> entry : playtimeRequirement.getWorldPlaytimeRequirements().entrySet()) {
                    if (index >= MAX_WORLD_LINES) {
                        lines.add("+" + (playtimeRequirement.getWorldPlaytimeRequirements().size() - MAX_WORLD_LINES) + " more world checks");
                        break;
                    }
                    lines.add(
                        entry.getKey()
                            + ": "
                            + PlaytimeRequirement.formatDuration(
                            playtimeRequirement.getWorldPlaytimeSeconds(player, entry.getKey())
                        )
                            + " / "
                            + PlaytimeRequirement.formatDuration(entry.getValue())
                    );
                    index++;
                }
            }
            return lines;
        }

        if (requirement instanceof PermissionRequirement permissionRequirement) {
            lines.add("Mode: " + permissionRequirement.getPermissionMode().name());
            if (permissionRequirement.isMinimumMode()) {
                lines.add("Minimum matches: " + permissionRequirement.getMinimumRequired());
            }

            final List<PermissionRequirement.PermissionStatus> permissionStatuses =
                permissionRequirement.getDetailedPermissionStatus(player);
            for (int index = 0; index < Math.min(permissionStatuses.size(), MAX_PERMISSION_LINES); index++) {
                final PermissionRequirement.PermissionStatus status = permissionStatuses.get(index);
                lines.add((status.hasPermission() ? "[OK] " : "[X] ") + status.getPermission());
            }
            if (permissionStatuses.size() > MAX_PERMISSION_LINES) {
                lines.add("+" + (permissionStatuses.size() - MAX_PERMISSION_LINES) + " more permissions");
            }
            return lines;
        }

        if (requirement instanceof LocationRequirement locationRequirement) {
            if (locationRequirement.getRequiredWorld() != null) {
                lines.add(
                    "World: "
                        + player.getWorld().getName()
                        + " / "
                        + locationRequirement.getRequiredWorld()
                );
            }
            if (locationRequirement.getRequiredCoordinates() != null) {
                lines.add("Target: " + locationRequirement.getRequiredCoordinates());
                lines.add(
                    "Distance: "
                        + ShopStorePricingSupport.formatAmount(locationRequirement.getCurrentDistance(player))
                        + " / "
                        + ShopStorePricingSupport.formatAmount(locationRequirement.getRequiredDistance())
                );
            }
            if (locationRequirement.getRequiredRegion() != null) {
                lines.add("Region: " + locationRequirement.getRequiredRegion());
            }
            return lines;
        }

        if (requirement instanceof PluginRequirement pluginRequirement) {
            lines.add("Plugin: " + pluginRequirement.getPluginIntegrationId());
            if (pluginRequirement.getCategory() != null && !pluginRequirement.getCategory().isBlank()) {
                lines.add("Category: " + pluginRequirement.getCategory());
            }

            final Map<String, Double> currentValues = pluginRequirement.getCurrentValues(player);
            int index = 0;
            for (final Map.Entry<String, Double> entry : pluginRequirement.getRequiredValues().entrySet()) {
                if (index >= MAX_PLUGIN_LINES) {
                    lines.add("+" + (pluginRequirement.getRequiredValues().size() - MAX_PLUGIN_LINES) + " more plugin values");
                    break;
                }
                final double currentValue = currentValues.getOrDefault(entry.getKey(), 0.0D);
                lines.add(
                    entry.getKey()
                        + ": "
                        + ShopStorePricingSupport.formatAmount(currentValue)
                        + " / "
                        + ShopStorePricingSupport.formatAmount(entry.getValue())
                );
                index++;
            }
            return lines;
        }

        if (requirement instanceof CompositeRequirement compositeRequirement) {
            lines.add("Operator: " + compositeRequirement.getOperator().name());
            if (compositeRequirement.isMinimumLogic()) {
                lines.add("Minimum required: " + compositeRequirement.getMinimumRequired());
            }

            final List<AbstractRequirement> completedRequirements = compositeRequirement.getCompletedRequirements(player);
            lines.add(
                "Completed: "
                    + completedRequirements.size()
                    + "/"
                    + compositeRequirement.getRequirements().size()
            );
            appendChildRequirementLines(
                plugin,
                player,
                lines,
                compositeRequirement.getDetailedProgress(player).stream()
                    .map(CompositeRequirement.RequirementProgress::requirement)
                    .toList(),
                compositeRequirement.getCompletedRequirements(player)
            );
            return lines;
        }

        if (requirement instanceof ChoiceRequirement choiceRequirement) {
            lines.add("Choices required: " + choiceRequirement.getMinimumChoicesRequired());
            lines.add(
                "Completed: "
                    + choiceRequirement.getCompletedChoices(player).size()
                    + "/"
                    + choiceRequirement.getChoices().size()
            );
            appendChildRequirementLines(
                plugin,
                player,
                lines,
                choiceRequirement.getChoices(),
                choiceRequirement.getCompletedChoices(player)
            );
            return lines;
        }

        if (requirement instanceof TimedRequirement timedRequirement) {
            lines.add("Remaining: " + timedRequirement.getFormattedRemainingTime());
            lines.add("Started: " + (timedRequirement.isStarted() ? timedRequirement.getFormattedStartTime() : "Not started"));
            lines.add("Delegate: " + ShopStorePricingSupport.describeRequirement(plugin, timedRequirement.getDelegate()));
            return lines;
        }

        lines.add(ShopStorePricingSupport.describeRequirement(plugin, requirement));
        return lines;
    }

    private static void appendChildRequirementLines(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull List<String> lines,
        final @NotNull List<AbstractRequirement> children,
        final @NotNull List<AbstractRequirement> completedChildren
    ) {
        for (int index = 0; index < Math.min(children.size(), MAX_CHILD_LINES); index++) {
            final AbstractRequirement childRequirement = children.get(index);
            final boolean completed = completedChildren.contains(childRequirement) || childRequirement.isMet(player);
            lines.add(
                (completed ? "[OK] " : "[X] ")
                    + ShopStorePricingSupport.describeRequirement(plugin, childRequirement)
            );
        }

        if (children.size() > MAX_CHILD_LINES) {
            lines.add("+" + (children.size() - MAX_CHILD_LINES) + " more sub-requirements");
        }
    }

    private static @Nullable Material parseMaterial(final @Nullable String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return null;
        }

        try {
            return Material.valueOf(materialName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
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
}
