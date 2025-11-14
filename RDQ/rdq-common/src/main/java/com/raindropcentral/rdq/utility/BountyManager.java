package com.raindropcentral.rdq.utility;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.bounty.BountySection;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.type.EBountyClaimMode;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all bounty-related operations in the RaindropQuests system.
 * <p>
 * Responsible for creating, tracking, and resolving bounties on players,
 * including reward distribution, damage tracking, and updating player displays.
 * Ensures thread-safety and schedules Bukkit API calls on the main thread while coordinating
 * repository writes on the shared asynchronous executor.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class BountyManager {

    private static final Logger LOGGER = CentralLogger.getLogger(BountyManager.class.getName());

    /**
     * The folder where bounty configuration files are stored.
     */
    private static final String BOUNTY_FOLDER = "bounty";

    /**
     * The name of the bounty configuration file.
     */
    private static final String BOUNTY_FILE = "bounty.yml";

    /**
     * Map of active bounties, keyed by the target player's UUID.
     */
    private final Map<UUID, RBounty> activeBounties = new ConcurrentHashMap<>();

    /**
     * Tracks damage dealt to each bounty target, mapping target UUIDs to a map of attacker UUIDs and their damage.
     */
    private final Map<UUID, Map<UUID, Double>> damageTracker = new ConcurrentHashMap<>();

    /**
     * Tracks the last attacker for each bounty target (used for LAST_HIT claim mode).
     */
    private final Map<UUID, UUID> lastHitTracker = new ConcurrentHashMap<>();

    /**
     * Reference to the plugin implementation and the Bukkit plugin handle for scheduling.
     */
    private final RDQ rdq;

    /**
     * Platform API for cross-platform compatibility.
     */
    private final RPlatform platform;

    /**
     * The current bounty claim mode (e.g., last hit, most damage).
     */
    private final EBountyClaimMode claimMode;

    /**
     * Constructs a new {@code BountyManager}, loading the persisted configuration to determine the
     * bounty claim mode and defaulting to {@link EBountyClaimMode#LAST_HIT} when loading fails.
     */
    public BountyManager(
            @NotNull RDQ rdq
    ) {
        this.rdq = rdq;
        this.platform = this.rdq.getPlatform();

        EBountyClaimMode loadedMode;
        try {
            final ConfigManager cfgManager = new ConfigManager(this.rdq.getPlugin(), BOUNTY_FOLDER);
            final ConfigKeeper<BountySection> cfgKeeper = new ConfigKeeper<>(cfgManager, BOUNTY_FILE, BountySection.class);
            loadedMode = cfgKeeper.rootSection.getClaimMode();
        } catch (final Exception exception) {
            loadedMode = EBountyClaimMode.LAST_HIT;
            LOGGER.log(Level.WARNING, "Error while loading bounty configuration, using fallback claim mode.", exception);
        }
        this.claimMode = loadedMode;
    }

    /**
     * Creates a new bounty for the specified target player, with the given commissioner, item rewards,
     * and currency rewards.
     * <p>
     * The bounty is written to the repository asynchronously. Once persisted, the in-memory caches are
     * updated and any Bukkit API invocations (such as broadcasting messages) are rescheduled on the
     * primary thread.
     * </p>
     *
     * @param targetPlayer     the player who is the target of the bounty
     * @param commissioner     the player who places the bounty
     * @param rewardItems      the set of item rewards for the bounty
     * @param rewardCurrencies the map of currency rewards for the bounty
     */
    public void createBounty(
            @NotNull RDQPlayer targetPlayer,
            @NotNull Player commissioner,
            @NotNull Set<RewardItem> rewardItems,
            @NotNull Map<String, Double> rewardCurrencies
    ) {
        final RBounty bounty = new RBounty(targetPlayer, commissioner, rewardItems, rewardCurrencies);

        this.rdq.getBountyRepository().createAsync(bounty)
                .whenCompleteAsync((createdBounty, throwable) -> {
                    if (throwable != null) {
                        LOGGER.log(Level.WARNING, "Error occurred while creating bounty.", throwable);
                        return;
                    }

                    activeBounties.put(targetPlayer.getUniqueId(), createdBounty);
                    damageTracker.put(targetPlayer.getUniqueId(), new ConcurrentHashMap<>());
                    lastHitTracker.remove(targetPlayer.getUniqueId());

                    this.rdq.getPlatform().getScheduler().runSync(() -> {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            TranslationService.create(TranslationKey.of("bounty.created.broadcast"), online)
                                    .withAll(Map.of(
                                            "target_name", targetPlayer.getPlayerName(),
                                            "commissioner_name", commissioner.getName())
                                    ).sendTitle();
                        }
                        updateBountyPlayerDisplay(targetPlayer.getUniqueId());
                    });
                }, this.rdq.getExecutor());
    }

    /**
     * Removes an active bounty and its associated damage tracking for the specified player while
     * refreshing the player's display to remove bounty indicators.
     *
     * @param targetUniqueId the UUID of the player whose bounty should be removed
     */
    public void removeBounty(final @NotNull UUID targetUniqueId) {
        activeBounties.remove(targetUniqueId);
        damageTracker.remove(targetUniqueId);
        lastHitTracker.remove(targetUniqueId);
        updateBountyPlayerDisplay(targetUniqueId);
    }

    /**
     * Tracks damage dealt by an attacker to a bounty target, accumulating damage per attacker and
     * maintaining last-hit information when using {@link EBountyClaimMode#LAST_HIT}.
     *
     * @param targetUniqueId   the UUID of the bounty target
     * @param attackerUniqueId the UUID of the attacker
     * @param damage           the amount of damage dealt
     */
    public void trackDamage(
            final @NotNull UUID targetUniqueId,
            final @NotNull UUID attackerUniqueId,
            final double damage
    ) {
        if (damage <= 0.0d) {
            return;
        }
        if (!this.activeBounties.containsKey(targetUniqueId)) {
            return;
        }

        final Map<UUID, Double> attackerDamages = this.damageTracker.computeIfAbsent(
                targetUniqueId, k -> new ConcurrentHashMap<>()
        );
        attackerDamages.merge(attackerUniqueId, damage, Double::sum);

        // Track last hitter for LAST_HIT mode
        this.lastHitTracker.put(targetUniqueId, attackerUniqueId);
    }

    /**
     * Handles the event when a player with an active bounty is killed.
     * <p>
     * Determines the winner based on the configured claim mode, distributes rewards, updates the
     * player and bounty repositories asynchronously, and notifies all connected players from the
     * main thread once the bounty has been finalized.
     * </p>
     *
     * @param killedPlayer the player who was killed
     */
    public void handleBountyKill(@NotNull Player killedPlayer) {
        final UUID targetId = killedPlayer.getUniqueId();
        final RBounty bounty = activeBounties.get(targetId);
        if (bounty == null) return;

        final UUID winnerId = determineBountyWinner(targetId);
        if (winnerId == null) return;

        final Player winner = Bukkit.getPlayer(winnerId);
        if (winner != null) {
            if (!Bukkit.isPrimaryThread()) this.rdq.getPlatform().getScheduler().runSync(() -> giveRewardItemsToPlayer(winner, bounty.getRewardItems()));
            else giveRewardItemsToPlayer(winner, bounty.getRewardItems());

            this.rdq.getPlatform().getScheduler().runSync(() -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    TranslationService.create(
                            TranslationKey.of("bounty.claimed.player"), online
                    ).withAll(Map.of(
                            "target_name", killedPlayer.getName(),
                            "receiver_name", winner.getName()
                    )).withPrefix().send();
                }
            });
        }

        final RDQPlayer targetPlayer = bounty.getPlayer();
        targetPlayer.setBounty(null);

        this.rdq.getPlayerRepository().updateAsync(targetPlayer)
                .thenComposeAsync(unused -> this.rdq.getBountyRepository().deleteAsync(bounty.getId()), this.rdq.getExecutor())
                .whenCompleteAsync((unused2, throwable) -> {
                    if (throwable != null) {
                        LOGGER.log(Level.WARNING, "Error finalizing bounty claim.", throwable);
                        return;
                    }
                    this.rdq.getPlatform().getScheduler().runSync(() -> {
                        removeBounty(targetId);
                        final OfflinePlayer winnerOnline = Bukkit.getOfflinePlayer(winnerId);
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            TranslationService.create(TranslationKey.of("bounty.claimed.broadcast"), online)
                                    .withAll(Map.of(
                                            "target_name", targetPlayer.getPlayerName(),
                                            "receiver_name", winnerOnline.getName() == null ? "Unknown" : winnerOnline.getName()
                                    )).send();
                        }
                    });
                }, this.rdq.getExecutor());
    }

    /**
     * Adds item rewards to an existing bounty.
     * <p>
     * This is currently a stub and returns the provided bounty unchanged until persistence and
     * mutation logic are implemented.
     * </p>
     *
     * @param bounty the bounty to add rewards to
     * @param items  the list of item stacks to add as rewards
     * @return the updated bounty
     */
    public @NotNull RBounty addItemRewards(
            final @NotNull RBounty bounty,
            final @NotNull List<ItemStack> items
    ) {
        Objects.requireNonNull(bounty, "bounty cannot be null");
        Objects.requireNonNull(items, "items cannot be null");

        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                final RewardItem rewardItem = new RewardItem(item);
                bounty.addRewardItem(rewardItem);
            }
        }

        return bounty;
    }

    /**
     * Adds a currency reward to an existing bounty.
     * <p>
     * This is currently a stub and returns the provided bounty unchanged until persistence and
     * mutation logic are implemented.
     * </p>
     *
     * @param bounty       the bounty to add the currency reward to
     * @param currencyName the name of the currency
     * @param amount       the amount to add
     * @return the updated bounty
     */
    public @NotNull RBounty addCurrencyReward(
            final @NotNull RBounty bounty,
            final @NotNull String currencyName,
            final double amount
    ) {
        Objects.requireNonNull(bounty, "bounty cannot be null");
        Objects.requireNonNull(currencyName, "currencyName cannot be null");

        if (amount <= 0) {
            throw new IllegalArgumentException("Currency amount must be positive");
        }

        bounty.addRewardCurrency(currencyName, amount);
        return bounty;
    }

    /**
     * Updates the display name of a player to indicate bounty status, scheduling the mutation on the
     * main thread when necessary.
     *
     * @param playerUniqueId the UUID of the player to update
     */
    public void updateBountyPlayerDisplay(final @NotNull UUID playerUniqueId) {
        final Runnable task = () -> {
            final Player player = Bukkit.getPlayer(playerUniqueId);
            if (player == null) {
                return;
            }

            if (this.activeBounties.containsKey(playerUniqueId)) {
                updateBountyDisplay(player);
            } else {
                resetPlayerDisplay(player);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            this.rdq.getPlatform().getScheduler().runSync(task);
        }
    }

    private void updateBountyDisplay(final @NotNull Player player) {
        this.platform.getPlatformAPI().setDisplayName(
                player,
                TranslationService.create(
                        TranslationKey.of("bounty.display.player_list_name"), player
                ).withAll(
                        Map.of(
                                "bounty_symbol", "☠",
                                "player_name", player.getName()
                        )
                ).build().component()
        );

        LOGGER.fine("Updated bounty display for " + player.getName() + " using " + this.platform.getPlatformAPI().getType());
    }

    private void resetPlayerDisplay(final @NotNull Player player) {
        this.platform.getPlatformAPI().setDisplayName(
                player,
                Component.text(player.getName())
        );
        LOGGER.fine("Reset display names for " + player.getName());
    }

    /**
     * Checks if a player currently has an active bounty.
     *
     * @param playerUniqueId the UUID of the player to check
     * @return {@code true} if the player has an active bounty, {@code false} otherwise
     */
    public boolean hasActiveBounty(final @NotNull UUID playerUniqueId) {
        return this.activeBounties.containsKey(playerUniqueId);
    }

    /**
     * Retrieves the active bounty for a player, if any.
     *
     * @param playerUniqueId the UUID of the player
     * @return the {@link RBounty} instance, or {@code null} if none exists
     */
    public @Nullable RBounty getBounty(final @NotNull UUID playerUniqueId) {
        return activeBounties.get(playerUniqueId);
    }

    /**
     * Determines the winner of a bounty based on the configured claim mode.
     *
     * @param targetUniqueId the UUID of the bounty target
     * @return the UUID of the winning player, or {@code null} if no winner can be determined
     */
    private @Nullable UUID determineBountyWinner(final @NotNull UUID targetUniqueId) {
        if (this.claimMode == EBountyClaimMode.LAST_HIT) {
            return this.lastHitTracker.get(targetUniqueId);
        }

        final Map<UUID, Double> damages = this.damageTracker.get(targetUniqueId);
        if (damages == null || damages.isEmpty()) {
            return null;
        }
        return getTopDamager(damages);
    }

    /**
     * Gets the UUID of the player who dealt the most damage to a bounty target.
     *
     * @param damages a map of attacker UUIDs to damage dealt
     * @return the UUID of the top damager, or {@code null} if not found
     */
    private @Nullable UUID getTopDamager(final @NotNull Map<UUID, Double> damages) {
        return damages
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Gives the specified reward items to the player, splitting stacks as needed and ensuring the
     * operation completes on the main thread.
     * <p>
     * If the player's inventory is full, excess items are dropped at their location and the player
     * receives a notification about leftover items.
     * </p>
     *
     * @param player      the player to receive the items
     * @param rewardItems the set of {@link RewardItem} to give
     */
    public void giveRewardItemsToPlayer(
            final @NotNull Player player,
            final @NotNull Set<RewardItem> rewardItems
    ) {
        if (!Bukkit.isPrimaryThread()) {
            this.rdq.getPlatform().getScheduler().runSync(() -> giveRewardItemsToPlayer(player, rewardItems));
            return;
        }

        final List<ItemStack> leftovers = new ArrayList<>();
        for (final RewardItem rewardItem : rewardItems) {
            final ItemStack item = rewardItem.getItem();
            int amount = rewardItem.getAmount();
            final int maxStack = item.getMaxStackSize();

            final ItemStack singleStack = item.clone();
            singleStack.setAmount(1);

            while (amount > 0) {
                final int giveAmount = Math.min(amount, maxStack);
                final ItemStack stackToGive = singleStack.clone();
                stackToGive.setAmount(giveAmount);

                final Map<Integer, ItemStack> notFit = player.getInventory().addItem(stackToGive);
                if (!notFit.isEmpty()) {
                    leftovers.addAll(notFit.values());
                }
                amount -= giveAmount;
            }
        }

        if (!leftovers.isEmpty()) {
            leftovers.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            TranslationService.create(TranslationKey.of("bounty_reward_ui.left_overs"), player).withPrefix().send();
        }
    }
}
