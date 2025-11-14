package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.config.bounty.BountySection;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of the {@link BountyManager} interface.
 * <p>
 * Responsible for creating, tracking, and resolving bounties on players,
 * including reward distribution, damage tracking, and updating player displays.
 * Ensures thread-safety and schedules Bukkit API calls on the main thread.
 * </p>
 */
public class DefaultBountyManager implements BountyManager {

    private static final Logger LOGGER = CentralLogger.getLogger(DefaultBountyManager.class.getName());
    private static final String BOUNTY_FOLDER = "bounty";
    private static final String BOUNTY_FILE = "bounty.yml";

    private final Map<UUID, RBounty> activeBounties = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Double>> damageTracker = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastHitTracker = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;
    private final RPlatform platform;
    private final Executor executor;
    private final RBountyRepository bountyRepository;
    private final RDQPlayerRepository playerRepository;
    private final EBountyClaimMode claimMode;

    /**
     * Constructs a new BountyManager using dependency injection.
     *
     * @param plugin           The main plugin instance for config loading.
     * @param platform         The RPlatform instance for scheduling and platform API access.
     * @param executor         The executor service for async operations.
     * @param bountyRepository The repository for bounty data.
     * @param playerRepository The repository for player data.
     */
    public DefaultBountyManager(
            @NotNull JavaPlugin plugin,
            @NotNull RPlatform platform,
            @NotNull Executor executor,
            @NotNull RBountyRepository bountyRepository,
            @NotNull RDQPlayerRepository playerRepository
    ) {
        this.plugin = plugin;
        this.platform = platform;
        this.executor = executor;
        this.bountyRepository = bountyRepository;
        this.playerRepository = playerRepository;

        EBountyClaimMode loadedMode;
        try {
            final ConfigManager cfgManager = new ConfigManager(this.plugin, BOUNTY_FOLDER);
            final ConfigKeeper<BountySection> cfgKeeper = new ConfigKeeper<>(cfgManager, BOUNTY_FILE, BountySection.class);
            loadedMode = cfgKeeper.rootSection.getClaimMode();
        } catch (final Exception exception) {
            loadedMode = EBountyClaimMode.LAST_HIT;
            LOGGER.log(Level.WARNING, "Error loading bounty config, using fallback.", exception);
        }
        this.claimMode = loadedMode;
    }

    @Override
    public void createBounty(
            @NotNull RDQPlayer targetPlayer,
            @NotNull Player commissioner,
            @NotNull Set<RewardItem> rewardItems,
            @NotNull Map<String, Double> rewardCurrencies
    ) {
        final RBounty bounty = new RBounty(targetPlayer, commissioner, rewardItems, rewardCurrencies);

        this.bountyRepository.createAsync(bounty)
                .whenCompleteAsync((createdBounty, throwable) -> {
                    if (throwable != null) {
                        LOGGER.log(Level.WARNING, "Error creating bounty.", throwable);
                        return;
                    }
                    activeBounties.put(targetPlayer.getUniqueId(), createdBounty);
                    damageTracker.put(targetPlayer.getUniqueId(), new ConcurrentHashMap<>());
                    lastHitTracker.remove(targetPlayer.getUniqueId());

                    this.platform.getScheduler().runSync(() -> {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            TranslationService.create(TranslationKey.of("bounty.created.broadcast"), online)
                                    .withAll(Map.of("target_name", targetPlayer.getPlayerName(), "commissioner_name", commissioner.getName()))
                                    .sendTitle();
                        }
                        updateBountyPlayerDisplay(targetPlayer.getUniqueId());
                    });
                }, this.executor);
    }

    @Override
    public void removeBounty(final @NotNull UUID targetUniqueId) {
        activeBounties.remove(targetUniqueId);
        damageTracker.remove(targetUniqueId);
        lastHitTracker.remove(targetUniqueId);
        updateBountyPlayerDisplay(targetUniqueId);
    }

    @Override
    public void trackDamage(
            final @NotNull UUID targetUniqueId,
            final @NotNull UUID attackerUniqueId,
            final double damage
    ) {
        if (damage <= 0.0d || !this.activeBounties.containsKey(targetUniqueId)) {
            return;
        }
        this.damageTracker.computeIfAbsent(targetUniqueId, k -> new ConcurrentHashMap<>()).merge(attackerUniqueId, damage, Double::sum);
        this.lastHitTracker.put(targetUniqueId, attackerUniqueId);
    }

    @Override
    public void handleBountyKill(@NotNull Player killedPlayer) {
        final UUID targetId = killedPlayer.getUniqueId();
        final RBounty bounty = activeBounties.get(targetId);
        if (bounty == null) return;

        final UUID winnerId = determineBountyWinner(targetId);
        if (winnerId == null) return;

        final Player winner = Bukkit.getPlayer(winnerId);
        if (winner != null) {
            giveRewardItemsToPlayer(winner, bounty.getRewardItems());
            this.platform.getScheduler().runSync(() -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    TranslationService.create(TranslationKey.of("bounty.claimed.player"), online)
                            .withAll(Map.of("target_name", killedPlayer.getName(), "receiver_name", winner.getName()))
                            .withPrefix().send();
                }
            });
        }

        final RDQPlayer targetPlayer = bounty.getPlayer();
        targetPlayer.setBounty(null);

        this.playerRepository.updateAsync(targetPlayer)
                .thenComposeAsync(unused -> this.bountyRepository.deleteAsync(bounty.getId()), this.executor)
                .whenCompleteAsync((unused2, throwable) -> {
                    if (throwable != null) {
                        LOGGER.log(Level.WARNING, "Error finalizing bounty claim.", throwable);
                        return;
                    }
                    this.platform.getScheduler().runSync(() -> {
                        removeBounty(targetId);
                        final OfflinePlayer winnerOffline = Bukkit.getOfflinePlayer(winnerId);
                        String winnerName = winnerOffline.getName() == null ? "Unknown" : winnerOffline.getName();
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            TranslationService.create(TranslationKey.of("bounty.claimed.broadcast"), online)
                                    .withAll(Map.of("target_name", targetPlayer.getPlayerName(), "receiver_name", winnerName))
                                    .send();
                        }
                    });
                }, this.executor);
    }

    @Override
    public @NotNull RBounty addItemRewards(@NotNull RBounty bounty, @NotNull List<ItemStack> items) {
        Objects.requireNonNull(bounty, "bounty cannot be null");
        Objects.requireNonNull(items, "items cannot be null");

        try {
            for (ItemStack item : items) {
                if (item != null && !item.getType().isAir()) {
                    final RewardItem rewardItem = new RewardItem(item);
                    bounty.addRewardItem(rewardItem);
                }
            }

            this.bountyRepository.create(bounty);
            LOGGER.log(Level.FINE, () -> "Added " + items.size() + " item rewards to bounty for player " + bounty.getPlayer().getPlayerName());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to add item rewards to bounty", exception);
        }

        return bounty;
    }
    @Override
    public @NotNull RBounty addCurrencyReward(@NotNull RBounty bounty, @NotNull String currencyName, double amount) {
        Objects.requireNonNull(bounty, "bounty cannot be null");
        Objects.requireNonNull(currencyName, "currencyName cannot be null");

        if (amount <= 0) {
            throw new IllegalArgumentException("Currency amount must be positive");
        }

        try {
            bounty.addRewardCurrency(currencyName, amount);
            this.bountyRepository.create(bounty);
            LOGGER.log(Level.FINE, () -> "Added " + amount + " of currency '" + currencyName + "' to bounty for player " + bounty.getPlayer().getPlayerName());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to add currency reward to bounty", exception);
        }

        return bounty;
    }
    @Override
    public void updateBountyPlayerDisplay(final @NotNull UUID playerUniqueId) {
        final Runnable task = () -> {
            final Player player = Bukkit.getPlayer(playerUniqueId);
            if (player == null) return;
            if (this.activeBounties.containsKey(playerUniqueId)) {
                updateBountyDisplay(player);
            } else {
                resetPlayerDisplay(player);
            }
        };
        if (Bukkit.isPrimaryThread()) task.run();
        else this.platform.getScheduler().runSync(task);
    }

    @Override
    public boolean hasActiveBounty(final @NotNull UUID playerUniqueId) {
        return this.activeBounties.containsKey(playerUniqueId);
    }

    @Override
    public @Nullable RBounty getBounty(final @NotNull UUID playerUniqueId) {
        return activeBounties.get(playerUniqueId);
    }

    @Override
    public void giveRewardItemsToPlayer(@NotNull Player player, @NotNull Set<RewardItem> rewardItems) {
        if (!Bukkit.isPrimaryThread()) {
            this.platform.getScheduler().runSync(() -> giveRewardItemsToPlayer(player, rewardItems));
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
                if (!player.getInventory().addItem(stackToGive).isEmpty()) {
                    leftovers.add(stackToGive);
                }
                amount -= giveAmount;
            }
        }
        if (!leftovers.isEmpty()) {
            leftovers.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            TranslationService.create(TranslationKey.of("bounty_reward_ui.left_overs"), player).withPrefix().send();
        }
    }

    private void updateBountyDisplay(final @NotNull Player player) {
        this.platform.getPlatformAPI().setDisplayName(player,
                TranslationService.create(TranslationKey.of("bounty.display.player_list_name"), player)
                        .withAll(Map.of("bounty_symbol", "☠", "player_name", player.getName()))
                        .build().component()
        );
        LOGGER.fine("Updated bounty display for " + player.getName());
    }

    private void resetPlayerDisplay(final @NotNull Player player) {
        this.platform.getPlatformAPI().setDisplayName(player, Component.text(player.getName()));
        LOGGER.fine("Reset display names for " + player.getName());
    }

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

    private @Nullable UUID getTopDamager(final @NotNull Map<UUID, Double> damages) {
        return damages.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }
}
