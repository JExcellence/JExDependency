/*
package com.raindropcentral.rdq.utility;

import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkState;
import com.raindropcentral.rdq.type.EPerkType;
import com.raindropcentral.rdq.view.perks.PerkDisplayData;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

*/
/**
 * Utility class providing helper methods for perk-related operations.
 * <p>
 * This class contains static utility methods for common perk operations such as:
 * <ul>
 *   <li>Time formatting and duration calculations</li>
 *   <li>Perk filtering and grouping operations</li>
 *   <li>Material and icon determination</li>
 *   <li>Validation and compatibility checks</li>
 *   <li>String formatting and display helpers</li>
 * </ul>
 * </p>
 *
 * <p>
 * All methods in this class are static and thread-safe. The class cannot be instantiated
 * and serves purely as a collection of utility functions.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 *//*

public final class PerkUtils {
    
    */
/**
     * Date/time formatter for displaying cooldown expiry times.
     *//*

    private static final DateTimeFormatter COOLDOWN_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    */
/**
     * Date/time formatter for displaying full timestamps.
     *//*

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    */
/**
     * Private constructor to prevent instantiation.
     *//*

    private PerkUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ========== Time and Duration Utilities ==========
    
    */
/**
     * Formats a duration in seconds to a human-readable string.
     * <p>
     * Examples:
     * <ul>
     *   <li>30 seconds → "30s"</li>
     *   <li>90 seconds → "1m 30s"</li>
     *   <li>3661 seconds → "1h 1m 1s"</li>
     *   <li>86461 seconds → "1d 1m 1s"</li>
     * </ul>
     * </p>
     *
     * @param seconds the duration in seconds
     * @return formatted duration string
     *//*

    public static @NotNull String formatDuration(final long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        
        final long days = seconds / 86400;
        final long hours = (seconds % 86400) / 3600;
        final long minutes = (seconds % 3600) / 60;
        final long remainingSeconds = seconds % 60;
        
        final StringBuilder builder = new StringBuilder();
        
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m ");
        }
        if (remainingSeconds > 0 || builder.isEmpty()) {
            builder.append(remainingSeconds).append("s");
        }
        
        return builder.toString().trim();
    }
    
    */
/**
     * Formats a LocalDateTime to a cooldown display string.
     *
     * @param dateTime the date/time to format (null returns empty string)
     * @return formatted time string (HH:mm:ss format)
     *//*

    public static @NotNull String formatCooldownTime(final @Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(COOLDOWN_FORMATTER);
    }
    
    */
/**
     * Formats a LocalDateTime to a full timestamp string.
     *
     * @param dateTime the date/time to format (null returns empty string)
     * @return formatted timestamp string (yyyy-MM-dd HH:mm:ss format)
     *//*

    public static @NotNull String formatTimestamp(final @Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIMESTAMP_FORMATTER);
    }
    
    */
/**
     * Calculates the remaining duration until a specific time.
     *
     * @param targetTime the target time (null returns Duration.ZERO)
     * @return duration until target time, or Duration.ZERO if target is in the past or null
     *//*

    public static @NotNull Duration calculateRemainingDuration(final @Nullable LocalDateTime targetTime) {
        if (targetTime == null) {
            return Duration.ZERO;
        }
        
        final LocalDateTime now = LocalDateTime.now();
        if (targetTime.isBefore(now) || targetTime.isEqual(now)) {
            return Duration.ZERO;
        }
        
        return Duration.between(now, targetTime);
    }
    
    // ========== Perk Filtering and Grouping Utilities ==========
    
    */
/**
     * Filters a list of perks by category.
     *
     * @param perks    the list of perks to filter
     * @param category the category to filter by (null for no filtering)
     * @return filtered list of perks
     *//*

    public static @NotNull List<PerkDisplayData> filterByCategory(
        final @NotNull List<PerkDisplayData> perks,
        final @Nullable EPerkCategory category
    ) {
        if (category == null) {
            return perks;
        }
        
        return perks.stream()
            .filter(perk -> perk.getCategory() == category)
            .collect(Collectors.toList());
    }
    
    */
/**
     * Filters a list of perks by state.
     *
     * @param perks the list of perks to filter
     * @param state the state to filter by (null for no filtering)
     * @return filtered list of perks
     *//*

    public static @NotNull List<PerkDisplayData> filterByState(
        final @NotNull List<PerkDisplayData> perks,
        final @Nullable EPerkState state
    ) {
        if (state == null) {
            return perks;
        }
        
        return perks.stream()
            .filter(perk -> perk.getState() == state)
            .collect(Collectors.toList());
    }
    
    */
/**
     * Groups perks by their category.
     *
     * @param perks the list of perks to group
     * @return map of category to list of perks
     *//*

    public static @NotNull Map<EPerkCategory, List<PerkDisplayData>> groupByCategory(
        final @NotNull List<PerkDisplayData> perks
    ) {
        return perks.stream()
            .collect(Collectors.groupingBy(PerkDisplayData::getCategory));
    }
    
    */
/**
     * Groups perks by their state.
     *
     * @param perks the list of perks to group
     * @return map of state to list of perks
     *//*

    public static @NotNull Map<EPerkState, List<PerkDisplayData>> groupByState(
        final @NotNull List<PerkDisplayData> perks
    ) {
        return perks.stream()
            .collect(Collectors.groupingBy(PerkDisplayData::getState));
    }
    
    */
/**
     * Counts perks by category.
     *
     * @param perks the list of perks to count
     * @return map of category to count
     *//*

    public static @NotNull Map<EPerkCategory, Long> countByCategory(
        final @NotNull List<PerkDisplayData> perks
    ) {
        return perks.stream()
            .collect(Collectors.groupingBy(PerkDisplayData::getCategory, Collectors.counting()));
    }
    
    */
/**
     * Counts perks by state.
     *
     * @param perks the list of perks to count
     * @return map of state to count
     *//*

    public static @NotNull Map<EPerkState, Long> countByState(
        final @NotNull List<PerkDisplayData> perks
    ) {
        return perks.stream()
            .collect(Collectors.groupingBy(PerkDisplayData::getState, Collectors.counting()));
    }
    
    // ========== Material and Icon Utilities ==========
    
    */
/**
     * Gets the appropriate material for a perk state indicator.
     *
     * @param state the perk state
     * @return material representing the state
     *//*

    public static @NotNull Material getStateIndicatorMaterial(final @NotNull EPerkState state) {
        return state.getIconMaterial();
    }
    
    */
/**
     * Gets the appropriate material for a perk category icon.
     *
     * @param category the perk category
     * @return material representing the category
     *//*

    public static @NotNull Material getCategoryIconMaterial(final @NotNull EPerkCategory category) {
        return category.getIconMaterial();
    }
    
    */
/**
     * Determines the best material to represent a perk based on its properties.
     *
     * @param perkData the perk display data
     * @return material best representing the perk
     *//*

    public static @NotNull Material determinePerkMaterial(final @NotNull PerkDisplayData perkData) {
        // Priority: State-specific material > Category material > Default
        final EPerkState state = perkData.getState();
        final EPerkCategory category = perkData.getCategory();
        
        // Use state-specific materials for certain states
        switch (state) {
            case LOCKED -> {
                return Material.BARRIER;
            }
            case DISABLED -> {
                return Material.REDSTONE;
            }
            case COOLDOWN -> {
                return Material.CLOCK;
            }
            default -> {
                // Use category material for other states
                return category.getIconMaterial();
            }
        }
    }
    
    // ========== Validation and Compatibility Utilities ==========
    
    */
/**
     * Checks if a perk type is compatible with a specific operation.
     *
     * @param perkType  the perk type to check
     * @param operation the operation to check compatibility for
     * @return true if compatible, false otherwise
     *//*

    public static boolean isCompatibleWithOperation(
        final @NotNull EPerkType perkType,
        final @NotNull PerkOperation operation
    ) {
        return switch (operation) {
            case TOGGLE -> perkType.isToggleable();
            case ACTIVATE -> !perkType.isToggleable() || perkType == EPerkType.INSTANT_USE;
            case TRIGGER -> perkType.isEventBased();
            case COOLDOWN_CHECK -> perkType.hasCooldown();
        };
    }
    
    */
/**
     * Validates if a perk identifier follows the expected format.
     *
     * @param identifier the identifier to validate
     * @return true if valid, false otherwise
     *//*

    public static boolean isValidPerkIdentifier(final @Nullable String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        
        // Check format: lowercase letters, numbers, underscores, dots
        return identifier.matches("^[a-z0-9_.]+$") && identifier.length() <= 64;
    }
    
    */
/**
     * Checks if two perks are conflicting (cannot be active simultaneously).
     * TODO: Implement actual conflict resolution logic
     *
     * @param perk1 first perk
     * @param perk2 second perk
     * @return true if perks conflict, false otherwise
     *//*

    public static boolean arePerksConflicting(
        final @NotNull PerkDisplayData perk1,
        final @NotNull PerkDisplayData perk2
    ) {
        // TODO: Implement conflict detection based on:
        // - Perk types and categories
        // - Custom conflict rules
        // - Resource usage
        // - Mutual exclusivity settings
        
        // Placeholder: no conflicts for now
        return false;
    }
    
    // ========== String Formatting Utilities ==========
    
    */
/**
     * Creates a formatted display name for a perk with color coding.
     *
     * @param perkData the perk display data
     * @return formatted display name with appropriate colors
     *//*

    public static @NotNull String formatPerkDisplayName(final @NotNull PerkDisplayData perkData) {
        final EPerkState state = perkData.getState();
        final String colorCode = state.getColorCode();
        final String identifier = perkData.getIdentifier();
        
        // TODO: Use i18n for actual display names
        return colorCode + "§l" + identifier;
    }
    
    */
/**
     * Creates a formatted status line for a perk.
     *
     * @param perkData the perk display data
     * @return formatted status string
     *//*

    public static @NotNull String formatPerkStatus(final @NotNull PerkDisplayData perkData) {
        final EPerkState state = perkData.getState();
        final StringBuilder status = new StringBuilder();
        
        status.append("§7State: ").append(state.getColoredDisplayName());
        
        if (perkData.isOnCooldown()) {
            status.append(" §7(").append(perkData.getFormattedCooldown()).append(")");
        }
        
        return status.toString();
    }
    
    */
/**
     * Creates action hint text for a perk based on its current state.
     *
     * @param perkData the perk display data
     * @return action hint string
     *//*

    public static @NotNull String formatActionHint(final @NotNull PerkDisplayData perkData) {
        final EPerkState state = perkData.getState();
        
        return switch (state) {
            case AVAILABLE -> {
                if (perkData.canActivate()) {
                    yield "§a§l▶ Click to activate";
                } else {
                    yield "§7§l- Not available";
                }
            }
            case ACTIVE -> {
                if (perkData.canToggle()) {
                    yield "§c§l⏸ Click to deactivate";
                } else {
                    yield "§a§l✓ Active";
                }
            }
            case LOCKED -> "§c§l✗ Locked";
            case COOLDOWN -> "§6§l⏳ On cooldown";
            case DISABLED -> "§8§l✗ Disabled";
        };
    }
    
    */
/**
     * Enumeration of perk operations for compatibility checking.
     *//*

    public enum PerkOperation {
        TOGGLE,
        ACTIVATE,
        TRIGGER,
        COOLDOWN_CHECK
    }
}*/
