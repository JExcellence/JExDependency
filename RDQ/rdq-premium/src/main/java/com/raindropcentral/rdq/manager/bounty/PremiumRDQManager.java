package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import com.raindropcentral.rplatform.RPlatform;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

/**
 * Concrete implementation of RDQManager for the Premium edition.
 * This class now correctly instantiates the DefaultBountyManager.
 */
public final class PremiumRDQManager extends RDQManager {

    private final BountyManager bountyManager;
    private final QuestManager questManager;
    private final PerkManager perkManager;

    public PremiumRDQManager(
            @NotNull JavaPlugin plugin,
            @NotNull RPlatform platform,
            @NotNull Executor executor,
            @NotNull RBountyRepository bountyRepository,
            @NotNull RDQPlayerRepository playerRepository
    ) {
        super("Premium");

        this.bountyManager = new DefaultBountyManager(plugin, platform, executor, bountyRepository, playerRepository);

        this.questManager = null;
        this.perkManager = null;
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
    public @NotNull PerkManager getPerkManager() {
        return this.perkManager;
    }

    @Override
    public boolean isPremium() {
        return true;
    }

    @Override
    public void initialize() {}

    @Override
    public void shutdown() {}
}