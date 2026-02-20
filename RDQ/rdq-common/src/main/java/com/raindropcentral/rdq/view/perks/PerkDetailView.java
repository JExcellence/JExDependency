package com.raindropcentral.rdq.view.perks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkRequirement;
import com.raindropcentral.rdq.database.entity.perk.PerkUnlockReward;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.perk.PerkActivationService;
import com.raindropcentral.rdq.perk.PerkManagementService;
import com.raindropcentral.rdq.perk.PerkRequirementService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Detailed view showing perk information, requirements, rewards, and actions.
 * Similar to RankRewardsDetailView but for perks.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkDetailView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // State
    private final State<RDQ> rdq = initialState("plugin");
    private final State<RDQPlayer> currentPlayer = initialState("player");
    private final State<Perk> targetPerk = initialState("perk");

    // Layout constants
    private static final int PERK_INFO_SLOT = 4;
    private static final int[] REQUIREMENT_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] REWARD_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    private static final int STATE_INFO_SLOT = 31;
    private static final int UNLOCK_BUTTON_SLOT = 38;
    private static final int TOGGLE_BUTTON_SLOT = 40;
    private static final int DISABLE_BUTTON_SLOT = 42;

    public PerkDetailView() {
        super(PerkOverviewView.class);
    }

    @Override
    protected String getKey() {
        return "perk_detail_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final Perk perk = targetPerk.get(openContext);
        return Map.of("perk_name", perk != null ? perk.getIdentifier() : "Unknown");
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            final Perk perk = targetPerk.get(render);
            final RDQPlayer rdqPlayer = currentPlayer.get(render);
            final RDQ plugin = rdq.get(render);

            if (perk == null) {
                renderErrorState(render, player);
                return;
            }

            // Render perk header
            renderPerkHeader(render, player, perk);

            // Render requirements
            renderRequirements(render, player, perk, rdqPlayer, plugin);

            // Render unlock rewards
            renderUnlockRewards(render, player, perk);

            // Render perk state
            renderPerkState(render, player, perk, rdqPlayer, plugin);

            // Render action buttons
            renderActions(render, player, perk, rdqPlayer, plugin);

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to render perk detail view", e);
            renderErrorState(render, player);
        }
    }

    /**
     * Renders the perk header with icon and description.
     */
    private void renderPerkHeader(
            @NotNull final RenderContext render,
            @NotNull final Player player,
            @NotNull final Perk perk
    ) {
        render.slot(PERK_INFO_SLOT)
                .renderWith(() -> {
                    try {
                        final Material icon = Material.valueOf(perk.getIcon().getMaterial().toUpperCase());
                        final Component name = new I18n.Builder(perk.getIcon().getDisplayNameKey(), player)
                                .build()
                                .component();
                        final Component description = new I18n.Builder(perk.getIcon().getDescriptionKey(), player)
                                .build()
                                .component();

                        final List<Component> lore = new ArrayList<>();
                        lore.add(Component.empty());
                        lore.add(description);
                        lore.add(Component.empty());
                        lore.add(MINI_MESSAGE.deserialize("<gray>Type: <white>" + formatPerkType(perk.getPerkType()) + "</white></gray>"));
                        lore.add(MINI_MESSAGE.deserialize("<gray>Category: <white>" + formatPerkCategory(perk.getCategory()) + "</white></gray>"));

                        return UnifiedBuilderFactory.item(icon)
                                .setName(name)
                                .setLore(lore)
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                                .build();
                    } catch (final Exception e) {
                        return UnifiedBuilderFactory.item(Material.BOOK)
                                .setName(Component.text(perk.getIdentifier()))
                                .build();
                    }
                });
    }

    /**
     * Renders the requirements section.
     */
    private void renderRequirements(
            @NotNull final RenderContext render,
            @NotNull final Player player,
            @NotNull final Perk perk,
            @NotNull final RDQPlayer rdqPlayer,
            @NotNull final RDQ plugin
    ) {
        final List<PerkRequirement> requirements = perk.getRequirements().stream()
                .sorted(Comparator.comparingInt(PerkRequirement::getDisplayOrder))
                .collect(Collectors.toList());

        if (requirements.isEmpty()) {
            // Show "no requirements" message
            render.slot(REQUIREMENT_SLOTS[3])
                    .renderWith(() -> createNoRequirementsCard(player));
            return;
        }

        final PerkRequirementService requirementService = plugin.getPerkRequirementService();
        final com.raindropcentral.rdq.view.perks.util.PerkRequirementCardRenderer cardRenderer = 
                new com.raindropcentral.rdq.view.perks.util.PerkRequirementCardRenderer(requirementService);

        for (int i = 0; i < Math.min(REQUIREMENT_SLOTS.length, requirements.size()); i++) {
            final PerkRequirement requirement = requirements.get(i);
            final int slot = REQUIREMENT_SLOTS[i];

            render.slot(slot)
                    .renderWith(() -> cardRenderer.createEnhancedRequirementCard(player, requirement));
        }
    }

    /**
     * Creates a "no requirements" card.
     */
    private @NotNull ItemStack createNoRequirementsCard(@NotNull final Player player) {
        return UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)
                .setName(MINI_MESSAGE.deserialize("<green>No Requirements</green>"))
                .setLore(List.of(MINI_MESSAGE.deserialize("<gray>This perk has no unlock requirements</gray>")))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    /**
     * Renders the unlock rewards section.
     */
    private void renderUnlockRewards(
            @NotNull final RenderContext render,
            @NotNull final Player player,
            @NotNull final Perk perk
    ) {
        final List<PerkUnlockReward> rewards = perk.getUnlockRewards().stream()
                .sorted(Comparator.comparingInt(PerkUnlockReward::getDisplayOrder))
                .collect(Collectors.toList());

        if (rewards.isEmpty()) {
            // Show "no rewards" message
            render.slot(REWARD_SLOTS[3])
                    .renderWith(() -> createNoRewardsCard(player));
            return;
        }

        for (int i = 0; i < Math.min(REWARD_SLOTS.length, rewards.size()); i++) {
            final PerkUnlockReward reward = rewards.get(i);
            final int slot = REWARD_SLOTS[i];

            render.slot(slot)
                    .renderWith(() -> createRewardCard(player, reward));
        }
    }

    /**
     * Creates a reward card.
     */
    private @NotNull ItemStack createRewardCard(
            @NotNull final Player player,
            @NotNull final PerkUnlockReward reward
    ) {
        try {
            final AbstractReward abstractReward = reward.getReward();
            final Material icon = reward.getIcon() != null ?
                    Material.valueOf(reward.getIcon().getMaterial().toUpperCase()) :
                    Material.DIAMOND;

            final List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MINI_MESSAGE.deserialize("<gray>Type: <white>" + abstractReward.getTypeId() + "</white></gray>"));

            final double value = abstractReward.getEstimatedValue();
            if (value > 0) {
                lore.add(MINI_MESSAGE.deserialize("<gray>Value: <gold>" + String.format("%.2f", value) + "</gold></gray>"));
            }

            return UnifiedBuilderFactory.item(icon)
                    .setName(MINI_MESSAGE.deserialize("<gold>Unlock Reward</gold>"))
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                    .build();
        } catch (final Exception e) {
            return UnifiedBuilderFactory.item(Material.DIAMOND)
                    .setName(Component.text("Reward"))
                    .build();
        }
    }

    /**
     * Creates a "no rewards" card.
     */
    private @NotNull ItemStack createNoRewardsCard(@NotNull final Player player) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(MINI_MESSAGE.deserialize("<gray>No Unlock Rewards</gray>"))
                .setLore(List.of(MINI_MESSAGE.deserialize("<gray>This perk has no unlock rewards</gray>")))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    /**
     * Renders the perk state information.
     */
    private void renderPerkState(
            @NotNull final RenderContext render,
            @NotNull final Player player,
            @NotNull final Perk perk,
            @NotNull final RDQPlayer rdqPlayer,
            @NotNull final RDQ plugin
    ) {
        render.slot(STATE_INFO_SLOT)
                .renderWith(() -> {
                    final PerkManagementService managementService = plugin.getPerkManagementService();
                    final Optional<PlayerPerk> playerPerkOpt = managementService.getPlayerPerk(rdqPlayer, perk);

                    return createStateInfoCard(player, playerPerkOpt.orElse(null), managementService, rdqPlayer);
                });
    }

    /**
     * Creates the state info card.
     */
    private @NotNull ItemStack createStateInfoCard(
            @NotNull final Player player,
            @org.jetbrains.annotations.Nullable final PlayerPerk playerPerk,
            @NotNull final PerkManagementService managementService,
            @NotNull final RDQPlayer rdqPlayer
    ) {
        final List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (playerPerk == null || !playerPerk.isUnlocked()) {
            lore.add(MINI_MESSAGE.deserialize("<red>✖ Locked</red>"));
            lore.add(Component.empty());
            lore.add(MINI_MESSAGE.deserialize("<gray>Complete requirements to unlock</gray>"));

            return UnifiedBuilderFactory.item(Material.RED_STAINED_GLASS_PANE)
                    .setName(MINI_MESSAGE.deserialize("<white>Perk Status</white>"))
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }

        // Unlocked perk
        if (playerPerk.isActive()) {
            lore.add(MINI_MESSAGE.deserialize("<green>✓ Active</green>"));
        } else if (playerPerk.isEnabled()) {
            lore.add(MINI_MESSAGE.deserialize("<yellow>○ Enabled</yellow>"));
        } else {
            lore.add(MINI_MESSAGE.deserialize("<gray>○ Disabled</gray>"));
        }

        lore.add(Component.empty());

        // Show enabled perk count
        final int enabledCount = managementService.getEnabledPerkCount(rdqPlayer);
        final int maxEnabled = managementService.getMaxEnabledPerks();
        lore.add(MINI_MESSAGE.deserialize("<gray>Enabled Perks: <white>" + enabledCount + "/" + maxEnabled + "</white></gray>"));

        // Show cooldown if applicable
        if (playerPerk.isOnCooldown()) {
            final long remainingMillis = playerPerk.getRemainingCooldownMillis();
            final String timeString = formatDuration(remainingMillis);
            lore.add(MINI_MESSAGE.deserialize("<gray>Cooldown: <gold>" + timeString + "</gold></gray>"));
        }

        // Show statistics
        lore.add(Component.empty());
        lore.add(MINI_MESSAGE.deserialize("<gray>Activations: <white>" + playerPerk.getActivationCount() + "</white></gray>"));

        final Material material = playerPerk.isActive() ? Material.LIME_STAINED_GLASS_PANE :
                playerPerk.isEnabled() ? Material.YELLOW_STAINED_GLASS_PANE :
                        Material.GRAY_STAINED_GLASS_PANE;

        return UnifiedBuilderFactory.item(material)
                .setName(MINI_MESSAGE.deserialize("<white>Perk Status</white>"))
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    /**
     * Renders action buttons.
     */
    private void renderActions(
            @NotNull final RenderContext render,
            @NotNull final Player player,
            @NotNull final Perk perk,
            @NotNull final RDQPlayer rdqPlayer,
            @NotNull final RDQ plugin
    ) {
        final PerkManagementService managementService = plugin.getPerkManagementService();
        final Optional<PlayerPerk> playerPerkOpt = managementService.getPlayerPerk(rdqPlayer, perk);

        if (playerPerkOpt.isEmpty() || !playerPerkOpt.get().isUnlocked()) {
            // Show unlock button
            renderUnlockButton(render, player, perk, rdqPlayer, plugin);
        } else {
            // Show enable/disable buttons
            final PlayerPerk playerPerk = playerPerkOpt.get();
            if (playerPerk.isEnabled()) {
                renderDisableButton(render, player, perk, rdqPlayer, plugin);
            } else {
                renderEnableButton(render, player, perk, rdqPlayer, plugin);
            }
        }
    }

    /**
     * Renders the unlock button.
     */
    private void renderUnlockButton(
            @NotNull final RenderContext render,
            @NotNull final Player player,
            @NotNull final Perk perk,
            @NotNull final RDQPlayer rdqPlayer,
            @NotNull final RDQ plugin
    ) {
        render.slot(UNLOCK_BUTTON_SLOT)
                .renderWith(() -> {
                    final PerkRequirementService requirementService = plugin.getPerkRequirementService();
                    
                    // Run requirement check on main thread to avoid async event errors
                    final java.util.concurrent.atomic.AtomicBoolean canUnlock = new java.util.concurrent.atomic.AtomicBoolean(false);
                    try {
                        java.util.concurrent.CompletableFuture<Boolean> future = new java.util.concurrent.CompletableFuture<>();
                        org.bukkit.Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
                            try {
                                future.complete(requirementService.canUnlock(player, perk));
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        });
                        canUnlock.set(future.get(1, java.util.concurrent.TimeUnit.SECONDS));
                    } catch (Exception e) {
                        // If check fails, assume cannot unlock
                        canUnlock.set(false);
                    }

                    final List<Component> lore = new ArrayList<>();
                    lore.add(Component.empty());

                    if (canUnlock.get()) {
                        lore.add(MINI_MESSAGE.deserialize("<green>Click to unlock this perk!</green>"));
                    } else {
                        lore.add(MINI_MESSAGE.deserialize("<red>Requirements not met</red>"));
                    }

                    return UnifiedBuilderFactory.item(canUnlock.get() ? Material.LIME_DYE : Material.RED_DYE)
                            .setName(MINI_MESSAGE.deserialize("<white>Unlock Perk</white>"))
                            .setLore(lore)
                            .setGlowing(canUnlock.get())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
                })
                .onClick(clickContext -> handleUnlockAttempt(clickContext, perk));
    }

    /**
     * Renders the enable button.
     */
    private void renderEnableButton(
            @NotNull final RenderContext render,
            @NotNull final Player player,
            @NotNull final Perk perk,
            @NotNull final RDQPlayer rdqPlayer,
            @NotNull final RDQ plugin
    ) {
        render.slot(TOGGLE_BUTTON_SLOT)
                .renderWith(() -> {
                    final PerkManagementService managementService = plugin.getPerkManagementService();
                    final boolean canEnable = managementService.canEnableAnotherPerk(rdqPlayer);

                    final List<Component> lore = new ArrayList<>();
                    lore.add(Component.empty());

                    if (canEnable) {
                        lore.add(MINI_MESSAGE.deserialize("<green>Click to enable this perk</green>"));
                    } else {
                        lore.add(MINI_MESSAGE.deserialize("<red>Maximum enabled perks reached</red>"));
                    }

                    return UnifiedBuilderFactory.item(canEnable ? Material.LIME_DYE : Material.RED_DYE)
                            .setName(MINI_MESSAGE.deserialize("<white>Enable Perk</white>"))
                            .setLore(lore)
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
                })
                .onClick(clickContext -> handleToggleEnable(clickContext, perk));
    }

    /**
     * Renders the disable button.
     */
    private void renderDisableButton(
            @NotNull final RenderContext render,
            @NotNull final Player player,
            @NotNull final Perk perk,
            @NotNull final RDQPlayer rdqPlayer,
            @NotNull final RDQ plugin
    ) {
        render.slot(DISABLE_BUTTON_SLOT)
                .renderWith(() -> {
                    final List<Component> lore = new ArrayList<>();
                    lore.add(Component.empty());
                    lore.add(MINI_MESSAGE.deserialize("<yellow>Click to disable this perk</yellow>"));

                    return UnifiedBuilderFactory.item(Material.ORANGE_DYE)
                            .setName(MINI_MESSAGE.deserialize("<white>Disable Perk</white>"))
                            .setLore(lore)
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
                })
                .onClick(clickContext -> handleToggleEnable(clickContext, perk));
    }

    /**
     * Handles unlock attempt.
     */
    private void handleUnlockAttempt(
            @NotNull final SlotClickContext clickContext,
            @NotNull final Perk perk
    ) {
        try {
            final Player player = clickContext.getPlayer();
            final RDQ plugin = rdq.get(clickContext);
            final RDQPlayer rdqPlayer = currentPlayer.get(clickContext);
            final PerkRequirementService requirementService = plugin.getPerkRequirementService();

            // Check if player can unlock
            if (!requirementService.canUnlock(player, perk)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(MINI_MESSAGE.deserialize("<red>You don't meet the requirements to unlock this perk!</red>"));
                return;
            }

            // Attempt to unlock
            requirementService.attemptUnlock(player, rdqPlayer, perk).thenAccept(result -> {
                if (result.isSuccess()) {
                    PlayerPerk playerPerk = result.getPlayerPerk();
                    
                    // Validate perk is properly enabled after unlock
                    if (playerPerk != null && playerPerk.isUnlocked() && playerPerk.isEnabled()) {
                        // Optionally auto-activate the perk
                        final PerkActivationService activationService = plugin.getPerkActivationService();
                        activationService.activate(player, playerPerk).thenAccept(activated -> {
                            if (activated) {
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                                player.sendMessage(MINI_MESSAGE.deserialize("<green>Successfully unlocked and enabled perk: " + perk.getIdentifier() + "!</green>"));
                            } else {
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                                player.sendMessage(MINI_MESSAGE.deserialize("<green>Successfully unlocked perk: " + perk.getIdentifier() + "!</green>"));
                            }
                            clickContext.update();
                        });
                    } else {
                        LOGGER.log(Level.SEVERE, "Perk {0} unlocked but not properly enabled (unlocked={1}, enabled={2})", 
                                new Object[]{perk.getIdentifier(), 
                                        playerPerk != null && playerPerk.isUnlocked(), 
                                        playerPerk != null && playerPerk.isEnabled()});
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage(MINI_MESSAGE.deserialize("<red>Perk unlocked but requires manual enabling. Please contact an administrator.</red>"));
                        clickContext.update();
                    }
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to unlock perk. Please try again.</red>"));
                }
            }).exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to unlock perk " + perk.getIdentifier(), throwable);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while unlocking the perk.</red>"));
                return null;
            });

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling unlock attempt", e);
            clickContext.getPlayer().playSound(clickContext.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    /**
     * Handles toggle enable/disable.
     */
    private void handleToggleEnable(
            @NotNull final SlotClickContext clickContext,
            @NotNull final Perk perk
    ) {
        try {
            final Player player = clickContext.getPlayer();
            final RDQ plugin = rdq.get(clickContext);
            final RDQPlayer rdqPlayer = currentPlayer.get(clickContext);
            final PerkManagementService managementService = plugin.getPerkManagementService();
            final PerkActivationService activationService = plugin.getPerkActivationService();

            // Get current player perk state
            final Optional<PlayerPerk> playerPerkOpt = managementService.getPlayerPerk(rdqPlayer, perk);
            
            if (playerPerkOpt.isEmpty() || !playerPerkOpt.get().isUnlocked()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(MINI_MESSAGE.deserialize("<red>You must unlock this perk first!</red>"));
                return;
            }

            final PlayerPerk playerPerk = playerPerkOpt.get();

            if (playerPerk.isEnabled()) {
                // Disable the perk
                managementService.disablePerk(rdqPlayer, perk).thenAccept(success -> {
                    if (success) {
                        // Deactivate if currently active
                        if (playerPerk.isActive()) {
                            activationService.deactivate(player, playerPerk);
                        }
                        
                        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 0.8f);
                        player.sendMessage(MINI_MESSAGE.deserialize("<yellow>Disabled perk: " + perk.getIdentifier() + "</yellow>"));
                        clickContext.update();
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to disable perk.</red>"));
                    }
                }).exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to disable perk " + perk.getIdentifier(), throwable);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return null;
                });
            } else {
                // Enable the perk
                managementService.enablePerk(rdqPlayer, perk).thenAccept(success -> {
                    if (success) {
                        // Activate the perk
                        activationService.activate(player, playerPerk);
                        
                        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.2f);
                        player.sendMessage(MINI_MESSAGE.deserialize("<green>Enabled perk: " + perk.getIdentifier() + "!</green>"));
                        clickContext.update();
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to enable perk. You may have reached the maximum enabled perk limit.</red>"));
                    }
                }).exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to enable perk " + perk.getIdentifier(), throwable);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return null;
                });
            }

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling toggle enable", e);
            clickContext.getPlayer().playSound(clickContext.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    /**
     * Formats a perk type for display.
     */
    private @NotNull String formatPerkType(@NotNull final com.raindropcentral.rdq.database.entity.perk.PerkType perkType) {
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
    private @NotNull String formatPerkCategory(@NotNull final com.raindropcentral.rdq.database.entity.perk.PerkCategory category) {
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
        final long hours = millis / 3600000;
        final long minutes = (millis % 3600000) / 60000;
        final long seconds = (millis % 60000) / 1000;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Renders an error state.
     */
    private void renderErrorState(
            @NotNull final RenderContext render,
            @NotNull final Player player
    ) {
        render.slot(22)
                .renderWith(() -> UnifiedBuilderFactory.item(Material.BARRIER)
                        .setName(Component.text("Error loading perk details"))
                        .build());
    }
}
