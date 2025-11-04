package com.raindropcentral.rdq.utility.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.bounty.BountySection;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.type.EBountyClaimMode;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
 * This class is responsible for creating, tracking, and resolving bounties on players,
 * including reward distribution, damage tracking, and updating player displays.
 * It also handles configuration for bounty claim modes and integrates with the
 * plugin's messaging systems.
 * </p>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public class BountyManager {

    private final static Logger LOGGER = CentralLogger.getLogger(BountyManager.class.getName());
    private final static String BOUNTY_FOLDER = "bounty";
    private final static String BOUNTY_FILE   = "bounty.yml";

    private final Map<UUID, RBounty>           activeBounties = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Double>> damageTracker  = new ConcurrentHashMap<>();

    private final RDQ rdq;
    private final BountyService bountyService;

    private EBountyClaimMode claimMode;

    /**
     * Constructs a new BountyManager using dependency injection.
     *
     * @param rdq           The main JavaPlugin instance for config loading.
     * @param bountyService    The service for bounty data operations.
     */
    public BountyManager(
            @NotNull RDQ rdq,
            @NotNull BountyService bountyService
    ) {
        this.rdq = rdq;
        this.bountyService = bountyService;
        this.loadConfig();
    }

    private void loadConfig() {
        try {
            final ConfigManager cfgManager = new ConfigManager(this.rdq.getPlugin(), BOUNTY_FOLDER);
            final ConfigKeeper<BountySection> cfgKeeper = new ConfigKeeper<>(cfgManager, BOUNTY_FILE, BountySection.class);
            this.claimMode = cfgKeeper.rootSection.getClaimMode();
        } catch (final Exception exception) {
            this.claimMode = EBountyClaimMode.LAST_HIT;
            LOGGER.log(Level.WARNING, "Error while loading bounty configuration, loading fallback", exception);
        }
    }

    public void createBounty(
            final @NotNull RDQPlayer targetPlayer,
            final @NotNull Player commissioner,
            final @NotNull Set<RewardItem> rewardItems,
            final @NotNull Map<String, Double> rewardCurrencies
    ) {
        this.bountyService.createBounty(targetPlayer, commissioner, rewardItems, rewardCurrencies)
                .whenCompleteAsync((rBounty, throwable) -> {
                    if (throwable != null) {
                        LOGGER.warning("Error occurred while creating bounty: " + throwable.getMessage());
                        return;
                    }
                    activeBounties.put(targetPlayer.getUniqueId(), rBounty);
                    damageTracker.put(targetPlayer.getUniqueId(), new HashMap<>());
                    Bukkit.getOnlinePlayers().forEach(player -> TranslationService.create(TranslationKey.of("bounty.created.broadcast"), player)
                            .withAll(Map.of("target_name", targetPlayer.getPlayerName(), "commissioner_name", commissioner.getName()))
                            .sendTitle());
                    this.updateBountyPlayerDisplay(targetPlayer.getUniqueId());
                }, this.rdq.getExecutor());
    }

    public void removeBounty(final @NotNull UUID targetUniqueId) {
        activeBounties.remove(targetUniqueId);
        damageTracker.remove(targetUniqueId);
        this.updateBountyPlayerDisplay(targetUniqueId);
    }

    public void trackDamage(final @NotNull UUID targetUniqueId, final @NotNull UUID attackerUniqueId, final double damage) {
        if (this.activeBounties.containsKey(targetUniqueId)) {
            this.damageTracker.computeIfAbsent(targetUniqueId, k -> new HashMap<>()).merge(attackerUniqueId, damage, Double::sum);
        }
    }

    public void handleBountyKill(final @NotNull Player killedPlayer) {
        final UUID targetUniqueId = killedPlayer.getUniqueId();
        RBounty bounty = activeBounties.get(targetUniqueId);
        if (bounty == null) return;

        final UUID receiverUniqueId = this.determineBountyWinner(targetUniqueId);
        if (receiverUniqueId == null) return;

        final Player winner = Bukkit.getPlayer(receiverUniqueId);
        final String winnerName = winner != null ? winner.getName() : "An unknown player";

        if (winner != null) {
            this.giveRewardItemsToPlayer(winner, bounty.getRewardItems());
        }

        RDQPlayer targetPlayer = bounty.getPlayer();
        targetPlayer.setBounty(null);

        this.rdq.getPlayerRepository().updateAsync(targetPlayer)
                .thenCompose(updatedPlayer -> this.bountyService.deleteBounty(bounty.getId()))
                .whenCompleteAsync((deleted, throwable) -> {
                    if (throwable != null) {
                        LOGGER.warning("Error occurred while processing bounty claim: " + throwable.getMessage());
                        return;
                    }
                    this.removeBounty(targetUniqueId);
                    Bukkit.getOnlinePlayers().forEach(player -> TranslationService.create(TranslationKey.of("bounty.claimed.broadcast"), player)
                            .withAll(Map.of("target_name", targetPlayer.getPlayerName(), "receiver_name", winnerName))
                            .withPrefix().send());
                }, this.rdq.getExecutor());
    }

    public void updateBountyPlayerDisplay(final @NotNull UUID playerUniqueId) {
        final Player player = Bukkit.getPlayer(playerUniqueId);
        if (player == null) return;
        if (this.activeBounties.containsKey(playerUniqueId)) {
            updateBountyDisplay(player);
        } else {
            resetPlayerDisplay(player);
        }
    }

    private void updateBountyDisplay(final @NotNull Player player) {
        this.rdq.getPlatform().getPlatformAPI().setDisplayName(player, TranslationService.create(TranslationKey.of("bounty.display.player_list_name"), player)
                .withAll(Map.of("bounty_symbol", "☠", "player_name", player.getName()))
                .build().component());
        LOGGER.fine("Updated bounty display for " + player.getName());
    }

    private void resetPlayerDisplay(final @NotNull Player player) {
        this.rdq.getPlatform().getPlatformAPI().setDisplayName(player, Component.text(player.getName()));
        LOGGER.fine("Reset display names for " + player.getName());
    }

    public boolean hasActiveBounty(final @NotNull UUID playerUniqueId) {
        return this.activeBounties.containsKey(playerUniqueId);
    }

    public RBounty getBounty(final @NotNull UUID playerUniqueId) {
        return activeBounties.get(playerUniqueId);
    }

    private UUID determineBountyWinner(final @NotNull UUID targetUniqueId) {
        Map<UUID, Double> damages = this.damageTracker.get(targetUniqueId);
        if (damages == null || damages.isEmpty()) return null;
        return (claimMode == EBountyClaimMode.LAST_HIT) ? this.getLastDamager(targetUniqueId) : this.getTopDamager(damages);
    }

    private UUID getLastDamager(final @NotNull UUID targetUniqueId) {
        return this.getTopDamager(this.damageTracker.get(targetUniqueId)); // Fallback
    }

    private @Nullable UUID getTopDamager(final @NotNull Map<UUID, Double> damages) {
        return damages.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    public void giveRewardItemsToPlayer(@NotNull Player player, @NotNull Set<RewardItem> rewardItems) {
        List<ItemStack> leftovers = new ArrayList<>();
        for (RewardItem rewardItem : rewardItems) {
            ItemStack item = rewardItem.getItem();
            int amount = rewardItem.getAmount();
            int maxStack = item.getMaxStackSize();
            ItemStack singleStack = item.clone();
            singleStack.setAmount(1);
            while (amount > 0) {
                int giveAmount = Math.min(amount, maxStack);
                ItemStack stackToGive = singleStack.clone();
                stackToGive.setAmount(giveAmount);
                HashMap<Integer, ItemStack> notFit = player.getInventory().addItem(stackToGive);
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