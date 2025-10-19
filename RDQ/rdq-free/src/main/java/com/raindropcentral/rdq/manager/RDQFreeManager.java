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

    /**
     * Creates a free edition manager and wires the lightweight feature managers used by the
     * edition.
     */
    public RDQFreeManager() {
        super("Free");
        this.bountyManager = new FreeBountyManager();
        this.questManager = new FreeQuestManager();
        this.rankManager = new FreeRankManager();
        this.perkManager = new FreePerkManager();
    }

    /**
     * {@inheritDoc}
     *
     * @return the free edition bounty manager that exposes view-only data.
     */
    @Override
    public @NotNull BountyManager getBountyManager() {
        return this.bountyManager;
    }

    /**
     * {@inheritDoc}
     *
     * @return the quest manager providing limited quest functionality for the free edition.
     */
    @Override
    public @NotNull QuestManager getQuestManager() {
        return this.questManager;
    }

    /**
     * {@inheritDoc}
     *
     * @return the rank manager that offers non-persistent rank data for demonstrations.
     */
    @Override
    public @NotNull RankManager getRankManager() {
        return this.rankManager;
    }

    /**
     * {@inheritDoc}
     *
     * @return the perk manager exposing showcase-only perks in the free edition.
     */
    @Override
    public @NotNull PerkManager getPerkManager() {
        return this.perkManager;
    }

    /**
     * Indicates that this manager represents the free edition.
     *
     * @return {@code false} because the free manager never grants premium capabilities.
     */
    @Override
    public boolean isPremium() {
        return false;
    }

    /**
     * Initializes the free edition by logging the available subsystems. All subsystems operate in
     * a limited, demonstration-only capacity.
     */
    @Override
    public void initialize() {
        LOGGER.info("Initializing RDQ Free Manager...");
        LOGGER.info("Bounty System: Limited (View Only)");
        LOGGER.info("Quest System: Limited");
        LOGGER.info("Rank System: Limited");
        LOGGER.info("Perk System: Limited");
    }

    /**
     * Shuts down the free edition manager and logs the event for observability.
     */
    @Override
    public void shutdown() {
        LOGGER.info("Shutting down RDQ Free Manager...");
    }

    /**
     * Free edition implementation of {@link QuestManager} that only exposes limited quest data.
     */
    private static class FreeQuestManager implements QuestManager {
    }

    /**
     * Free edition implementation of {@link RankManager} without persistence.
     */
    private static class FreeRankManager implements RankManager {
    }

    /**
     * Free edition implementation of {@link PerkManager} that surfaces showcase-only perks.
     */
    private static class FreePerkManager implements PerkManager {
    }
}