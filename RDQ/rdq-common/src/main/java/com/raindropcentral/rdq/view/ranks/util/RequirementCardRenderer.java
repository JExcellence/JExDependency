package com.raindropcentral.rdq.view.ranks.util;

import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager.RequirementProgressData;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager.RequirementStatus;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.requirement.ItemRequirement;
import com.raindropcentral.rdq.requirement.Requirement;
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
 * Renders requirement cards with type-specific icons, task previews, and status styling.
 *
 * @author ItsRainingHP
 * @version 1.0.0
 */
public class RequirementCardRenderer {

    /**
     * Mapping of requirement types to their display icons.
     */
    private static final Map<Requirement.Type, Material> TYPE_ICONS = Map.of(
            Requirement.Type.ITEM, Material.CHEST,
            Requirement.Type.CURRENCY, Material.EMERALD,
            Requirement.Type.EXPERIENCE_LEVEL, Material.EXPERIENCE_BOTTLE,
            Requirement.Type.PLAYTIME, Material.CLOCK,
            Requirement.Type.PERMISSION, Material.PAPER,
            Requirement.Type.LOCATION, Material.COMPASS
    );

    /**
     * Mapping of requirement status to border materials.
     */
    private static final Map<RequirementStatus, Material> STATUS_BORDERS = Map.of(
            RequirementStatus.COMPLETED, Material.LIME_STAINED_GLASS_PANE,
            RequirementStatus.READY_TO_COMPLETE, Material.YELLOW_STAINED_GLASS_PANE,
            RequirementStatus.IN_PROGRESS, Material.ORANGE_STAINED_GLASS_PANE,
            RequirementStatus.NOT_STARTED, Material.RED_STAINED_GLASS_PANE,
            RequirementStatus.ERROR, Material.GRAY_STAINED_GLASS_PANE
    );

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public RequirementCardRenderer() {
    }

    /**
     * Creates a requirement card item with proper styling.
     *
     * @param player      The player viewing the card
     * @param requirement The requirement to display
     * @param progress    The progress data for this requirement
     * @return The styled ItemStack
     */
    public @NotNull ItemStack createRequirementCard(
            final @NotNull Player player,
            final @NotNull RRankUpgradeRequirement requirement,
            final @NotNull RequirementProgressData progress
    ) {
        final AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();
        final Requirement.Type type = abstractReq.getType();
        final Material icon = getIconForType(type);
        final boolean shouldGlow = progress.getStatus() == RequirementStatus.READY_TO_COMPLETE 
                || progress.getStatus() == RequirementStatus.COMPLETED;

        final List<Component> lore = buildCardLore(player, requirement, progress);

        // Get type name
        final String typeName = formatTypeName(type);

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
            final @NotNull Player player,
            final @NotNull RRankUpgradeRequirement requirement,
            final @NotNull RequirementProgressData progress
    ) {
        final List<Component> lore = new ArrayList<>();
        final AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();

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
                
                lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize(prefixColor + prefix + closingTag + " <gray>" + task.name() + "</gray>"));
            }

            if (tasks.size() > 3) {
                lore.add(MINI_MESSAGE.deserialize("<gray>...and " + (tasks.size() - 3) + " more</gray>"));
            }

            lore.add(Component.empty());
        }

        // Progress bar (single line, not redundant)
        // getProgressPercentage() returns 0.0-1.0, createMiniProgressBar expects 0.0-1.0
        final double progressValue = progress.getProgressPercentage();
        lore.add(RequirementProgressRenderer.createMiniProgressBar(progressValue, 10));

        lore.add(Component.empty());

        // Status-specific action hint
        switch (progress.getStatus()) {
            case READY_TO_COMPLETE -> {
                lore.add(MINI_MESSAGE.deserialize("<green>Click to complete!</green>"));
            }
            case COMPLETED -> {
                lore.add(MINI_MESSAGE.deserialize("<green>✓ Completed</green>"));
            }
            case IN_PROGRESS, NOT_STARTED -> {
                lore.add(MINI_MESSAGE.deserialize("<gray>Right-click for details</gray>"));
            }
            default -> {}
        }

        return lore;
    }

    /**
     * Gets the icon material for a requirement type.
     */
    public static @NotNull Material getIconForType(final @NotNull Requirement.Type type) {
        return TYPE_ICONS.getOrDefault(type, Material.BOOK);
    }

    /**
     * Formats a requirement type name for display.
     */
    private static @NotNull String formatTypeName(final @NotNull Requirement.Type type) {
        return switch (type) {
            case ITEM -> "📦 Item Collection";
            case CURRENCY -> "💰 Currency";
            case EXPERIENCE_LEVEL -> "⭐ Experience Level";
            case PLAYTIME -> "🕐 Playtime";
            case PERMISSION -> "📄 Permission";
            case LOCATION -> "🧭 Location";
            default -> type.name().replace("_", " ");
        };
    }

    /**
     * Gets the border material for a requirement status.
     */
    public static @NotNull Material getBorderForStatus(final @NotNull RequirementStatus status) {
        return STATUS_BORDERS.getOrDefault(status, Material.GRAY_STAINED_GLASS_PANE);
    }

    /**
     * Generates task previews for a requirement.
     */
    public @NotNull List<TaskPreview> generateTaskPreviews(
            final @NotNull RRankUpgradeRequirement requirement,
            final @NotNull Player player
    ) {
        final List<TaskPreview> previews = new ArrayList<>();
        final AbstractRequirement abstractReq = requirement.getRequirement().getRequirement();

        if (abstractReq instanceof ItemRequirement itemReq) {
            final List<ItemRequirement.ItemProgress> itemProgressList = itemReq.getDetailedProgress(player);
            
            for (int i = 0; i < itemProgressList.size(); i++) {
                final ItemRequirement.ItemProgress itemProgress = itemProgressList.get(i);
                final String itemName = formatItemName(itemProgress.requiredItem());
                
                previews.add(new TaskPreview(
                        itemName + " x" + itemProgress.requiredAmount(),
                        itemProgress.completed(),
                        itemProgress.currentAmount(),
                        itemProgress.requiredAmount()
                ));
            }
        } else {
            // For non-item requirements, create a single task preview
            final String typeName = abstractReq.getType().name().toLowerCase().replace("_", " ");
            final String capitalizedName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
            
            previews.add(new TaskPreview(
                    capitalizedName,
                    false, // Will be determined by actual progress
                    0,
                    1
            ));
        }

        return previews;
    }

    /**
     * Formats an item name for display.
     */
    private @NotNull String formatItemName(final @NotNull ItemStack item) {
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
     * Record representing a task preview.
     */
    public record TaskPreview(
            String name,
            boolean completed,
            int current,
            int required
    ) {}
}
