package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.manager.bounty.BountyManager;
import com.raindropcentral.rdq.manager.bounty.DefaultBountyManager;
import com.raindropcentral.rdq.manager.perk.DefaultPerkManager;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import com.raindropcentral.rdq.manager.rank.RankManager;
import com.raindropcentral.rplatform.RPlatform;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

/**
 * Concrete implementation of RDQManager for the Premium edition.
 * <p>
 * This class initializes and provides access to the full-featured,
 * persistent managers for bounties, quests, ranks, and perks.
 * </p>
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
public final class PremiumRDQManager extends RDQManager {

    private final BountyManager bountyManager;
    private final QuestManager questManager;
    private final RankManager rankManager;
    private final PerkManager perkManager;

    /**
     * Constructs the PremiumRDQManager and initializes all sub-managers
     * using dependency injection.
     *
     */
    public PremiumRDQManager(
            @NotNull RDQ rdq,
            @NotNull JavaPlugin plugin,
            @NotNull RPlatform platform,
            @NotNull Executor executor,
            @NotNull RBountyRepository bountyRepository,
            @NotNull RDQPlayerRepository playerRepository
    ) {
        super("Premium");

        // Initialize all premium managers with their required dependencies.
        this.bountyManager = new DefaultBountyManager(plugin, platform, executor, bountyRepository, playerRepository);
        this.questManager = null;
        this.rankManager = null;
        this.perkManager = new DefaultPerkManager(rdq, rdq.getPerkInitializationManager().getPerkEventBus());
    }

    @Override
    public @NotNull BountyManager getBountyManager() {
        return this.bountyManager;
    }

    @Override
    public @NotNull QuestManager getQuestManager() {
        return this.questManager;
    }

    @Override
    public @NotNull RankManager getRankManager() {
        return this.rankManager;
    }

    @Override
    public @NotNull PerkManager getPerkManager() {
        return this.perkManager;
    }

    @Override
    public boolean isPremium() {
        return true;
    }

    @Override
    public void initialize() {
        // Initialization is handled in the constructor to ensure managers are always available.
        // This method can be used for post-construction logic if needed.
    }

    @Override
    public void shutdown() {
        // Add shutdown logic for any managers that require it (e.g., saving caches).
    }
}