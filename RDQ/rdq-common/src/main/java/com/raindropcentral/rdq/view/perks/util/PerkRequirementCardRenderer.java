package com.raindropcentral.rdq.view.perks.util;

import com.raindropcentral.rdq.database.entity.perk.PerkRequirement;
import com.raindropcentral.rdq.perk.PerkRequirementService;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.impl.*;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders perk requirement cards with type-specific icons, task previews, and progress bars.
 * Similar to RequirementCardRenderer but specifically for perk requirements.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkRequirementCardRenderer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Mapping of requirement types to their display icons.
     */
    private static final Map<String, Material> TYPE_ICONS = Map.of(
            "ITEM", Material.CHEST,
            "CURRENCY", Material.EMERALD,
            "EXPERIENCE_LEVEL", Material.EXPERIENCE_BOTTLE,
            "PLAYTIME", Material.CLOCK,
            "PERMISSION", Material.PAPER,
            "LOCATION", Material.COMPASS
    );

    private final PerkRequirementService requirementService;

    /**
     * Executes PerkRequirementCardRenderer.
     */
    public PerkRequirementCardRenderer(@NotNull final PerkRequirementService requirementService) {
        this.requirementService = requirementService;
    }

    /**
     * Record representing a task preview.
     */
    public record TaskPreview(String name, boolean completed) {}

    /**
     * Generates task previews for a requirement.
     *
     * @param requirement the perk requirement
     * @param player the player viewing the requirement
     * @return list of task previews
     */
    public @NotNull List<TaskPreview> generateTaskPreviews(
            @NotNull final PerkRequirement requirement,
            @NotNull final Player player
    ) {
        final AbstractRequirement abstractReq = requirement.getRequirement();
        final String typeId = abstractReq.getTypeId();

        return switch (typeId) {
            case "CURRENCY" -> generateCurrencyTaskPreviews((CurrencyRequirement) abstractReq, player);
            case "ITEM" -> generateItemTaskPreviews((ItemRequirement) abstractReq, player);
            case "EXPERIENCE_LEVEL" -> generateExperienceLevelTaskPreviews((ExperienceLevelRequirement) abstractReq, player);
            case "PLAYTIME" -> generatePlaytimeTaskPreviews((PlaytimeRequirement) abstractReq, player);
            case "PERMISSION" -> generatePermissionTaskPreviews((PermissionRequirement) abstractReq, player);
            default -> generateGenericTaskPreview(abstractReq, player);
        };
    }

    /**
     * Generates task previews for currency requirements.
     * Shows "Need X coins" when not met or "Have X/Y coins" when partially or fully met.
     */
    private @NotNull List<TaskPreview> generateCurrencyTaskPreviews(
            @NotNull final CurrencyRequirement requirement,
            @NotNull final Player player
    ) {
        final double current = requirement.getCurrentBalance(player);
        final double required = requirement.getAmount();
        final String currencyId = requirement.getCurrencyId();
        
        // Get currency display name - use the requirement's display name directly
        final String currencyName = requirement.getCurrencyDisplayName();

        final boolean completed = current >= required;
        final String taskName;

        if (completed) {
            // Format: "Have X/Y Currency"
            taskName = String.format("Have %.0f/%.0f %s", current, required, currencyName);
        } else {
            // Format: "Need X Currency"
            taskName = String.format("Need %.0f %s", required, currencyName);
        }

        return List.of(new TaskPreview(taskName, completed));
    }


    /**
     * Generates task previews for item requirements.
     * Shows "Need Xx item_name" for each required item.
     */
    private @NotNull List<TaskPreview> generateItemTaskPreviews(
            @NotNull final ItemRequirement requirement,
            @NotNull final Player player
    ) {
        final List<TaskPreview> previews = new ArrayList<>();
        final List<ItemRequirement.ItemProgress> itemProgressList = requirement.getDetailedProgress(player);

        for (ItemRequirement.ItemProgress itemProgress : itemProgressList) {
            final String itemName = formatItemName(itemProgress.requiredItem());
            final String taskName = String.format("Need %dx %s", itemProgress.requiredAmount(), itemName);
            previews.add(new TaskPreview(taskName, itemProgress.completed()));
        }

        return previews;
    }

    /**
     * Generates task previews for experience level requirements.
     * Shows "Reach level X".
     */
    private @NotNull List<TaskPreview> generateExperienceLevelTaskPreviews(
            @NotNull final ExperienceLevelRequirement requirement,
            @NotNull final Player player
    ) {
        final int requiredLevel = requirement.getRequiredLevel();
        final int currentLevel = requirement.getCurrentExperience(player);
        final boolean completed = currentLevel >= requiredLevel;

        final String taskName;
        if (requirement.isLevelBased()) {
            taskName = String.format("Reach level %d", requiredLevel);
        } else {
            taskName = String.format("Reach %d experience points", requiredLevel);
        }

        return List.of(new TaskPreview(taskName, completed));
    }

    /**
     * Generates task previews for playtime requirements.
     * Shows "Play for X hours".
     */
    private @NotNull List<TaskPreview> generatePlaytimeTaskPreviews(
            @NotNull final PlaytimeRequirement requirement,
            @NotNull final Player player
    ) {
        final long requiredSeconds = requirement.getRequiredPlaytimeSeconds();
        final long currentSeconds = requirement.getTotalPlaytimeSeconds(player);
        final boolean completed = currentSeconds >= requiredSeconds;

        final String formattedTime = PlaytimeRequirement.formatDuration(requiredSeconds);
        final String taskName = String.format("Play for %s", formattedTime);

        return List.of(new TaskPreview(taskName, completed));
    }

    /**
     * Generates task previews for permission requirements.
     * Shows "Requires permission: X" for each permission.
     */
    private @NotNull List<TaskPreview> generatePermissionTaskPreviews(
            @NotNull final PermissionRequirement requirement,
            @NotNull final Player player
    ) {
        final List<TaskPreview> previews = new ArrayList<>();
        final List<String> requiredPermissions = requirement.getRequiredPermissions();

        for (String permission : requiredPermissions) {
            final boolean hasPermission = player.hasPermission(permission);
            final String taskName = String.format("Requires permission: %s", permission);
            previews.add(new TaskPreview(taskName, hasPermission));
        }

        return previews;
    }

    /**
     * Generates a generic task preview for unknown requirement types.
     */
    private @NotNull List<TaskPreview> generateGenericTaskPreview(
            @NotNull final AbstractRequirement requirement,
            @NotNull final Player player
    ) {
        final String typeName = requirement.getTypeId().toLowerCase().replace("_", " ");
        final String capitalizedName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
        final boolean completed = requirement.isMet(player);

        return List.of(new TaskPreview(capitalizedName, completed));
    }

    /**
     * Formats an item name for display.
     */
    private @NotNull String formatItemName(@NotNull final ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName());
        }

        // Format material name: DIAMOND_SWORD -> Diamond Sword
        final String materialName = item.getType().name().toLowerCase().replace("_", " ");
        final String[] words = materialName.split(" ");
        final StringBuilder formatted = new StringBuilder();

        for (final String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }


    /**
     * Creates an enhanced requirement card with task previews and progress bar.
     *
     * @param player the player viewing the card
     * @param requirement the perk requirement to display
     * @return the styled ItemStack
     */
    public @NotNull ItemStack createEnhancedRequirementCard(
            @NotNull final Player player,
            @NotNull final PerkRequirement requirement
    ) {
        final AbstractRequirement abstractReq = requirement.getRequirement();
        final String typeId = abstractReq.getTypeId();
        final Material icon = getIconForType(typeId);
        
        // Calculate progress
        final double progress = abstractReq.calculateProgress(player);
        final boolean shouldGlow = progress >= 1.0;

        final List<Component> lore = buildCardLore(player, requirement, progress);

        // Get type name
        final String typeName = formatTypeName(typeId);

        return UnifiedBuilderFactory.item(icon)
                .setName(Component.text(typeName))
                .setLore(lore)
                .setGlowing(shouldGlow)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build();
    }

    /**
     * Builds the lore for a requirement card.
     */
    private @NotNull List<Component> buildCardLore(
            @NotNull final Player player,
            @NotNull final PerkRequirement requirement,
            final double progress
    ) {
        final List<Component> lore = new ArrayList<>();

        // Empty line for spacing
        lore.add(Component.empty());

        // Task previews (max 3)
        final List<TaskPreview> tasks = generateTaskPreviews(requirement, player);
        if (!tasks.isEmpty()) {
            for (int i = 0; i < Math.min(3, tasks.size()); i++) {
                final TaskPreview task = tasks.get(i);
                final String prefix = task.completed() ? "✓" : "○";
                final String prefixColor = task.completed() ? "<green>" : "<gray>";
                final String closingTag = task.completed() ? "</green>" : "</gray>";

                lore.add(MINI_MESSAGE.deserialize(prefixColor + prefix + closingTag + " <gray>" + task.name() + "</gray>"));
            }

            if (tasks.size() > 3) {
                lore.add(MINI_MESSAGE.deserialize("<gray>...and " + (tasks.size() - 3) + " more</gray>"));
            }

            lore.add(Component.empty());
        }

        // Progress bar
        lore.add(com.raindropcentral.rdq.view.ranks.util.RequirementProgressRenderer.createMiniProgressBar(progress, 10));

        lore.add(Component.empty());

        // Status-specific action hint
        if (progress >= 1.0) {
            lore.add(MINI_MESSAGE.deserialize("<green>✓ Completed</green>"));
        } else {
            lore.add(MINI_MESSAGE.deserialize("<gray>Right-click for details</gray>"));
        }

        return lore;
    }

    /**
     * Gets the icon material for a requirement type.
     */
    public static @NotNull Material getIconForType(@NotNull final String typeId) {
        return TYPE_ICONS.getOrDefault(typeId, Material.BOOK);
    }

    /**
     * Formats a requirement type name for display.
     */
    private static @NotNull String formatTypeName(@NotNull final String typeId) {
        return switch (typeId) {
            case "ITEM" -> "📦 Item Collection";
            case "CURRENCY" -> "💰 Currency";
            case "EXPERIENCE_LEVEL" -> "⭐ Experience Level";
            case "PLAYTIME" -> "🕐 Playtime";
            case "PERMISSION" -> "📄 Permission";
            case "LOCATION" -> "🧭 Location";
            default -> typeId.replace("_", " ");
        };
    }
}
