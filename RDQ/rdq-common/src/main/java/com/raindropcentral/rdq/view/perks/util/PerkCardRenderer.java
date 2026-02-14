package com.raindropcentral.rdq.view.perks.util;

import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkCategory;
import com.raindropcentral.rdq.database.entity.perk.PerkType;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.perk.PerkRequirementService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders perk cards with icons, state indicators, progress, and cooldown information.
 * Similar to RequirementCardRenderer but for perks.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkCardRenderer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Mapping of perk categories to their display icons.
     */
    private static final Map<PerkCategory, Material> CATEGORY_ICONS = Map.of(
            PerkCategory.COMBAT, Material.DIAMOND_SWORD,
            PerkCategory.MOVEMENT, Material.FEATHER,
            PerkCategory.UTILITY, Material.COMPASS,
            PerkCategory.SURVIVAL, Material.SHIELD,
            PerkCategory.ECONOMY, Material.GOLD_INGOT,
            PerkCategory.SOCIAL, Material.PLAYER_HEAD,
            PerkCategory.COSMETIC, Material.GLOWSTONE_DUST,
            PerkCategory.SPECIAL, Material.NETHER_STAR
    );

    /**
     * Mapping of perk states to border materials.
     */
    private static final Map<PerkState, Material> STATE_BORDERS = Map.of(
            PerkState.ACTIVE, Material.LIME_STAINED_GLASS_PANE,
            PerkState.AVAILABLE, Material.YELLOW_STAINED_GLASS_PANE,
            PerkState.COOLDOWN, Material.ORANGE_STAINED_GLASS_PANE,
            PerkState.DISABLED, Material.GRAY_STAINED_GLASS_PANE,
            PerkState.LOCKED, Material.RED_STAINED_GLASS_PANE
    );

    private final PerkRequirementService requirementService;

    public PerkCardRenderer(@NotNull final PerkRequirementService requirementService) {
        this.requirementService = requirementService;
    }

    /**
     * Creates a perk card item with proper styling.
     *
     * @param player the player viewing the card
     * @param perk the perk to display
     * @param playerPerk the player's perk association (null if not unlocked)
     * @return the styled ItemStack
     */
    public @NotNull ItemStack renderPerkCard(
            @NotNull final Player player,
            @NotNull final Perk perk,
            @Nullable final PlayerPerk playerPerk
    ) {
        final Material icon = getIconForPerk(perk);
        final PerkState state = determinePerkState(playerPerk);
        final boolean shouldGlow = state == PerkState.ACTIVE || state == PerkState.AVAILABLE;

        final List<Component> lore = buildLore(player, perk, playerPerk, state);

        // Get perk name from i18n
        final Component displayName = new I18n.Builder(perk.getIcon().getDisplayNameKey(), player)
                .build()
                .component();

        return UnifiedBuilderFactory.item(icon)
                .setName(displayName)
                .setLore(lore)
                .setGlowing(shouldGlow)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build();
    }

    /**
     * Builds the lore for a perk card.
     */
    public @NotNull List<Component> buildLore(
            @NotNull final Player player,
            @NotNull final Perk perk,
            @Nullable final PlayerPerk playerPerk,
            @NotNull final PerkState state
    ) {
        final List<Component> lore = new ArrayList<>();

        lore.add(Component.empty());

        // Perk description
        final Component description = new I18n.Builder(perk.getIcon().getDescriptionKey(), player)
                .build()
                .component();
        lore.add(description);

        lore.add(Component.empty());

        // Perk type and category
        lore.add(MINI_MESSAGE.deserialize("<gray>Type: <white>" + formatPerkType(perk.getPerkType()) + "</white></gray>"));
        lore.add(MINI_MESSAGE.deserialize("<gray>Category: <white>" + formatPerkCategory(perk.getCategory()) + "</white></gray>"));

        lore.add(Component.empty());

        // State line
        lore.add(buildStateLine(player, state));

        // Progress line (if locked)
        if (state == PerkState.LOCKED) {
            final Component progressLine = buildProgressLine(player, perk);
            if (progressLine != null) {
                lore.add(progressLine);
            }
        }

        // Cooldown line (if on cooldown)
        if (state == PerkState.COOLDOWN && playerPerk != null) {
            lore.add(buildCooldownLine(player, playerPerk));
        }

        lore.add(Component.empty());

        // Action hint based on state
        lore.add(buildActionHint(player, state));

        return lore;
    }

    /**
     * Builds the state line showing the current perk state.
     */
    public @NotNull Component buildStateLine(
            @NotNull final Player player,
            @NotNull final PerkState state
    ) {
        return switch (state) {
            case ACTIVE -> MINI_MESSAGE.deserialize("<green>✓ Active</green>");
            case AVAILABLE -> MINI_MESSAGE.deserialize("<yellow>○ Available</yellow>");
            case COOLDOWN -> MINI_MESSAGE.deserialize("<gold>⏱ On Cooldown</gold>");
            case DISABLED -> MINI_MESSAGE.deserialize("<gray>○ Disabled</gray>");
            case LOCKED -> MINI_MESSAGE.deserialize("<red>✖ Locked</red>");
        };
    }

    /**
     * Builds the progress line for requirement progress.
     */
    public @Nullable Component buildProgressLine(
            @NotNull final Player player,
            @NotNull final Perk perk
    ) {
        try {
            final double progress = requirementService.getOverallProgress(player, perk);
            final int percentage = (int) (progress * 100);
            
            if (percentage == 0) {
                return MINI_MESSAGE.deserialize("<gray>Progress: <red>0%</red></gray>");
            } else if (percentage == 100) {
                return MINI_MESSAGE.deserialize("<gray>Progress: <green>100%</green></gray>");
            } else {
                return MINI_MESSAGE.deserialize("<gray>Progress: <yellow>" + percentage + "%</yellow></gray>");
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Builds the cooldown line showing remaining cooldown time.
     */
    public @NotNull Component buildCooldownLine(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk
    ) {
        final long remainingMillis = playerPerk.getRemainingCooldownMillis();
        final String timeString = formatDuration(remainingMillis);
        
        return MINI_MESSAGE.deserialize("<gray>Cooldown: <gold>" + timeString + "</gold></gray>");
    }

    /**
     * Builds an action hint based on the perk state.
     */
    private @NotNull Component buildActionHint(
            @NotNull final Player player,
            @NotNull final PerkState state
    ) {
        return switch (state) {
            case ACTIVE -> MINI_MESSAGE.deserialize("<gray>Click to disable</gray>");
            case AVAILABLE -> MINI_MESSAGE.deserialize("<green>Click to enable!</green>");
            case COOLDOWN -> MINI_MESSAGE.deserialize("<gray>Wait for cooldown...</gray>");
            case DISABLED -> MINI_MESSAGE.deserialize("<gray>Click to enable</gray>");
            case LOCKED -> MINI_MESSAGE.deserialize("<gray>Right-click for details</gray>");
        };
    }

    /**
     * Gets the icon material for a perk.
     */
    private @NotNull Material getIconForPerk(@NotNull final Perk perk) {
        try {
            final String materialName = perk.getIcon().getMaterial();
            return Material.valueOf(materialName.toUpperCase());
        } catch (Exception e) {
            // Fallback to category icon
            return CATEGORY_ICONS.getOrDefault(perk.getCategory(), Material.BOOK);
        }
    }

    /**
     * Determines the current state of a perk for a player.
     */
    private @NotNull PerkState determinePerkState(@Nullable final PlayerPerk playerPerk) {
        if (playerPerk == null || !playerPerk.isUnlocked()) {
            return PerkState.LOCKED;
        }

        if (playerPerk.isOnCooldown()) {
            return PerkState.COOLDOWN;
        }

        if (playerPerk.isActive()) {
            return PerkState.ACTIVE;
        }

        if (playerPerk.isEnabled()) {
            return PerkState.AVAILABLE;
        }

        return PerkState.DISABLED;
    }

    /**
     * Formats a perk type for display.
     */
    private @NotNull String formatPerkType(@NotNull final PerkType perkType) {
        return switch (perkType) {
            case PASSIVE -> "⚡ Passive";
            case EVENT_TRIGGERED -> "🎯 Event Triggered";
            case COOLDOWN_BASED -> "⏱ Cooldown Based";
            case PERCENTAGE_BASED -> "🎲 Percentage Based";
        };
    }

    /**
     * Formats a perk category for display.
     */
    private @NotNull String formatPerkCategory(@NotNull final PerkCategory category) {
        return switch (category) {
            case COMBAT -> "⚔️ Combat";
            case MOVEMENT -> "🏃 Movement";
            case UTILITY -> "🔧 Utility";
            case SURVIVAL -> "🛡️ Survival";
            case ECONOMY -> "💰 Economy";
            case SOCIAL -> "👥 Social";
            case COSMETIC -> "✨ Cosmetic";
            case SPECIAL -> "⭐ Special";
        };
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     */
    private @NotNull String formatDuration(final long millis) {
        final Duration duration = Duration.ofMillis(millis);
        final long hours = duration.toHours();
        final long minutes = duration.toMinutesPart();
        final long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Gets the border material for a perk state.
     */
    public static @NotNull Material getBorderForState(@NotNull final PerkState state) {
        return STATE_BORDERS.getOrDefault(state, Material.GRAY_STAINED_GLASS_PANE);
    }

    /**
     * Enumeration of perk states for UI display.
     */
    public enum PerkState {
        LOCKED,      // Not unlocked yet
        AVAILABLE,   // Unlocked and enabled but not active
        ACTIVE,      // Currently active and applying effects
        COOLDOWN,    // On cooldown
        DISABLED     // Unlocked but disabled
    }
}
