package com.raindropcentral.rdq.manager.bounty;

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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Free version of the BountyManager with in-memory storage and limited features.
 * <p>
 * This manager handles the business logic for bounties but does not persist any data.
 * Core features like bounty creation are disabled and gated as premium-only.
 * </p>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since 3.0.0
 */
public final class FreeBountyManager implements BountyManager {

    private static final Logger LOGGER = CentralLogger.getLogger(FreeBountyManager.class.getName());
    private static final String BOUNTY_FOLDER = "bounty";
    private static final String BOUNTY_FILE = "bounty.yml";

    private final Map<UUID, RBounty> activeBounties = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Double>> damageTracker = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastHitTracker = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;
    private final RPlatform platform;
    private final EBountyClaimMode claimMode;

    public FreeBountyManager(@NotNull JavaPlugin plugin, @NotNull RPlatform platform) {
        this.plugin = plugin;
        this.platform = platform;

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
        // Feature Gate: Inform the user this is a premium feature.
        TranslationService.create(TranslationKey.of("general.premium-feature"), commissioner)
                .withPrefix()
                .send();
    }

    @Override
    public void removeBounty(@NotNull UUID targetUniqueId) {
        activeBounties.remove(targetUniqueId);
        damageTracker.remove(targetUniqueId);
        lastHitTracker.remove(targetUniqueId);
        updateBountyPlayerDisplay(targetUniqueId);
    }

    @Override
    public void trackDamage(@NotNull UUID targetUniqueId, @NotNull UUID attackerUniqueId, double damage) {
        // In-memory tracking, same as premium, but data is ephemeral.
        if (damage <= 0.0d || !this.activeBounties.containsKey(targetUniqueId)) {
            return;
        }
        this.damageTracker.computeIfAbsent(targetUniqueId, k -> new ConcurrentHashMap<>()).merge(attackerUniqueId, damage, Double::sum);
        this.lastHitTracker.put(targetUniqueId, attackerUniqueId);
    }

    @Override
    public void handleBountyKill(@NotNull Player killedPlayer) {
        // This logic can remain to handle bounties created by admins, for example.
        final UUID targetId = killedPlayer.getUniqueId();
        final RBounty bounty = activeBounties.get(targetId);
        if (bounty == null) return;

        final UUID winnerId = determineBountyWinner(targetId);
        if (winnerId == null) return;

        final Player winner = Bukkit.getPlayer(winnerId);
        if (winner != null) {
            giveRewardItemsToPlayer(winner, bounty.getRewardItems());
        }

        // Since there's no persistence, we just remove it from memory.
        this.platform.getScheduler().runSync(() -> removeBounty(targetId));
    }

    @Override
    public @NotNull RBounty addItemRewards(@NotNull RBounty bounty, @NotNull List<ItemStack> items) {
        // No-op for free version
        return bounty;
    }

    @Override
    public @NotNull RBounty addCurrencyReward(@NotNull RBounty bounty, @NotNull String currencyName, double amount) {
        // No-op for free version
        return bounty;
    }

    @Override
    public void updateBountyPlayerDisplay(@NotNull UUID playerUniqueId) {
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
    public boolean hasActiveBounty(@NotNull UUID playerUniqueId) {
        return this.activeBounties.containsKey(playerUniqueId);
    }

    @Override
    public @Nullable RBounty getBounty(@NotNull UUID playerUniqueId) {
        return this.activeBounties.get(playerUniqueId);
    }

    @Override
    public void giveRewardItemsToPlayer(@NotNull Player player, @NotNull Set<RewardItem> rewardItems) {
        // This logic can be shared between versions.
        if (!Bukkit.isPrimaryThread()) {
            this.platform.getScheduler().runSync(() -> giveRewardItemsToPlayer(player, rewardItems));
            return;
        }
        final List<ItemStack> leftovers = new ArrayList<>();
        for (final RewardItem rewardItem : rewardItems) {
            final ItemStack item = rewardItem.getItem().clone();
            if (!player.getInventory().addItem(item).isEmpty()) {
                leftovers.add(item);
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
    }



    private void resetPlayerDisplay(final @NotNull Player player) {
        this.platform.getPlatformAPI().setDisplayName(player, Component.text(player.getName()));
    }

    private @Nullable UUID determineBountyWinner(final @NotNull UUID targetUniqueId) {
        if (this.claimMode == EBountyClaimMode.LAST_HIT) {
            return this.lastHitTracker.get(targetUniqueId);
        }
        final Map<UUID, Double> damages = this.damageTracker.get(targetUniqueId);
        if (damages == null || damages.isEmpty()) {
            return null;
        }
        return damages.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }
}