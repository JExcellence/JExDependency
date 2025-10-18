package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.manager.bounty.BountyManager;
import com.raindropcentral.rdq.manager.bounty.FreeBountyManager;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import com.raindropcentral.rdq.manager.rank.RankManager;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Free edition manager implementation.
 * <p>
 * Provides limited functionality:
 * <ul>
 * <li>In-memory storage (no database)</li>
 * <li>View-only features</li>
 * <li>Limited capacity</li>
 * <li>Mock data for demonstration</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class RDQFreeManager extends RDQManager {

    private static final Logger LOGGER = Logger.getLogger(RDQFreeManager.class.getName());

    private final BountyManager bountyManager;
    private final QuestManager questManager;
    private final RankManager rankManager;
    private final PerkManager perkManager;

    public RDQFreeManager() {
        super("Free");
        this.bountyManager = new FreeBountyManager();
        this.questManager = new FreeQuestManager();
        this.rankManager = new FreeRankManager();
        this.perkManager = new FreePerkManager();
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
        return false;
    }

    @Override
    public void initialize() {
        LOGGER.info("Initializing RDQ Free Manager...");
        LOGGER.info("Bounty System: Limited (View Only)");
        LOGGER.info("Quest System: Limited");
        LOGGER.info("Rank System: Limited");
        LOGGER.info("Perk System: Limited");
    }

    @Override
    public void shutdown() {
        LOGGER.info("Shutting down RDQ Free Manager...");
    }

    private static class FreeQuestManager implements QuestManager {
    }

    private static class FreeRankManager implements RankManager {
    }

    private static class FreePerkManager implements PerkManager {
    }
}