package com.raindropcentral.rdq.view.ranks.util;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import me.devnatan.inventoryframework.context.RenderContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Renders a visual slot-based progress bar for rank requirements.
 * Uses colored glass panes to show completion progress across 7 inventory slots.
 *
 * @author ItsRainingHP
 * @version 1.0.0
 */
public class RequirementProgressRenderer {

    /**
     * Slots used for the progress bar (row 1, slots 10-16).
     */
    public static final int[] PROGRESS_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    /**
     * Material for empty/incomplete progress slots.
     */
    private static final Material EMPTY_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;

    /**
     * Material for partially filled progress slots.
     */
    private static final Material PARTIAL_MATERIAL = Material.YELLOW_STAINED_GLASS_PANE;

    /**
     * Material for fully filled progress slots.
     */
    private static final Material FILLED_MATERIAL = Material.LIME_STAINED_GLASS_PANE;

    /**
     * Renders the progress bar across the designated slots.
     *
     * @param context           The render context
     * @param player            The player viewing the UI
     * @param progress          Progress as a decimal (0.0 to 1.0)
     * @param completedCount    Number of completed requirements
     * @param totalCount        Total number of requirements
     */
    public void renderProgressBar(
            final @NotNull RenderContext context,
            final @NotNull Player player,
            final double progress,
            final int completedCount,
            final int totalCount
    ) {
        final int totalSlots = PROGRESS_SLOTS.length;
        final int filledSlots = (int) Math.floor(progress * totalSlots);
        final double remainder = (progress * totalSlots) - filledSlots;

        for (int i = 0; i < totalSlots; i++) {
            final int slotIndex = i;
            final Material material;
            final String stateKey;

            if (i < filledSlots) {
                material = FILLED_MATERIAL;
                stateKey = "filled";
            } else if (i == filledSlots && remainder > 0.3) {
                material = PARTIAL_MATERIAL;
                stateKey = "partial";
            } else {
                material = EMPTY_MATERIAL;
                stateKey = "empty";
            }

            final int percentage = (int) Math.round(progress * 100);
            
            context.slot(PROGRESS_SLOTS[i])
                    .renderWith(() -> createProgressSlotItem(
                            material,
                            slotIndex + 1,
                            totalSlots,
                            percentage,
                            completedCount,
                            totalCount,
                            stateKey
                    ));
        }
    }

    /**
     * Creates a progress slot item with appropriate styling.
     */
    private @NotNull ItemStack createProgressSlotItem(
            final @NotNull Material material,
            final int slotNumber,
            final int totalSlots,
            final int percentage,
            final int completedCount,
            final int totalCount,
            final String stateKey
    ) {
        final Component name = Component.text(" ");
        
        return UnifiedBuilderFactory.item(material)
                .setName(name)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    /**
     * Creates a mini progress bar component for use in item lore.
     * Uses block characters to create a visual bar.
     * Uses floor rounding to prevent showing progress that hasn't been earned.
     *
     * @param progress   Progress as a decimal (0.0 to 1.0)
     * @param barLength  Number of characters in the bar
     * @return Component representing the progress bar
     */
    public static @NotNull Component createMiniProgressBar(final double progress, final int barLength) {
        // Use floor to prevent showing unearned progress (e.g., 0.5% shouldn't show as 1%)
        final int filled = (int) Math.floor(progress * barLength);
        final int empty = barLength - filled;
        
        final StringBuilder bar = new StringBuilder();
        bar.append("<green>");
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }
        bar.append("</green><gray>");
        for (int i = 0; i < empty; i++) {
            bar.append("░");
        }
        bar.append("</gray>");
        
        // Use floor to prevent showing unearned progress percentage
        final int percentage = (int) Math.floor(progress * 100);
        bar.append(" <white>").append(percentage).append("%</white>");
        
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(bar.toString());
    }

    /**
     * Creates a mini progress bar with default length of 10.
     */
    public static @NotNull Component createMiniProgressBar(final double progress) {
        return createMiniProgressBar(progress, 10);
    }

    /**
     * Animates progress bar change by updating slots sequentially.
     * This creates a visual "filling" effect when progress increases.
     *
     * @param context     The render context
     * @param plugin      The plugin instance for scheduling
     * @param oldProgress Previous progress value (0.0 to 1.0)
     * @param newProgress New progress value (0.0 to 1.0)
     */
    public void animateProgressChange(
            final @NotNull RenderContext context,
            final @NotNull org.bukkit.plugin.java.JavaPlugin plugin,
            final double oldProgress,
            final double newProgress
    ) {
        if (newProgress <= oldProgress) {
            return; // No animation needed for decrease
        }

        final int totalSlots = PROGRESS_SLOTS.length;
        final int oldFilledSlots = (int) Math.floor(oldProgress * totalSlots);
        final int newFilledSlots = (int) Math.floor(newProgress * totalSlots);

        if (newFilledSlots <= oldFilledSlots) {
            return;
        }

        // Calculate delay between each slot update (500ms total / number of slots to fill)
        final int slotsToFill = newFilledSlots - oldFilledSlots;
        final long delayPerSlot = Math.max(1L, 10L / slotsToFill); // ~500ms total, in ticks

        for (int i = 0; i < slotsToFill; i++) {
            final int slotToFill = oldFilledSlots + i;
            final long delay = delayPerSlot * (i + 1);

            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (slotToFill < PROGRESS_SLOTS.length) {
                    context.slot(PROGRESS_SLOTS[slotToFill])
                            .renderWith(() -> UnifiedBuilderFactory.item(FILLED_MATERIAL)
                                    .setName(Component.text(" "))
                                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                    .build());
                }
            }, delay);
        }
    }
}
