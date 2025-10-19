package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.manager.bounty.BountyManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import com.raindropcentral.rdq.manager.rank.RankManager;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import org.jetbrains.annotations.NotNull;

/**
 * Central manager for all RDQ functionalities.
 * <p>
 * Implementations encapsulate the concrete managers for bounties, quests, ranks, and perks
 * associated with a specific RDQ edition. The manager also coordinates lifecycle hooks for
 * initializing and shutting down managed subsystems when the plugin state changes.
 * </p>
 * <p>
 * Each edition participates in the staged enable pipeline orchestrated by {@link com.raindropcentral.rdq.RDQ}:
 * asynchronous platform initialization (stage&nbsp;1) prepares the shared executor by preferring
 * virtual threads and falling back to the fixed pool when necessary, component and view wiring
 * (stage&nbsp;2) runs inside the {@link com.raindropcentral.rdq.RDQ#runSync(Runnable) runSync}
 * boundary, and repository wiring (stage&nbsp;3) hydrates shared stores such as
 * {@link com.raindropcentral.rdq.database.repository.RBountyRepository},
 * {@link com.raindropcentral.rdq.database.repository.RRankRepository}, and
 * {@link com.raindropcentral.rdq.database.repository.RPerkRepository}. Manager implementations
 * must ensure the concrete edition services they expose remain compatible with those shared
 * repositories so free and premium modules stay aligned.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public abstract class RDQManager {

    private final String edition;

    protected RDQManager(final @NotNull String edition) {
        this.edition = edition;
    }

    /**
     * Provides access to the bounty manager responsible for tracking and rewarding bounties.
     *
     * @return the manager handling bounty data and operations
     */
    public abstract @NotNull BountyManager getBountyManager();

    /**
     * Retrieves the quest manager used to create, load, and administer quests.
     *
     * @return the manager coordinating quest definitions and progress
     */
    public abstract @NotNull QuestManager getQuestManager();

    /**
     * Supplies the rank manager that maintains player rank definitions and transitions.
     *
     * @return the manager orchestrating rank data and promotion logic
     */
    public abstract @NotNull RankManager getRankManager();

    /**
     * Exposes the perk manager responsible for unlocking and applying perk effects.
     *
     * @return the manager supervising perk catalogues and activation
     */
    public abstract @NotNull PerkManager getPerkManager();

    /**
     * Obtains the edition identifier that determines the feature set available to this manager.
     *
     * @return the configured edition name
     */
    public final @NotNull String getEdition() {
        return this.edition;
    }

    /**
     * Indicates whether the current manager instance exposes premium-exclusive features.
     *
     * @return {@code true} if the manager serves the premium edition; {@code false} for free
     */
    public abstract boolean isPremium();

    /**
     * Initializes all managed subsystems, preparing them for runtime use.
     */
    public abstract void initialize();

    /**
     * Performs orderly shutdown for all managed subsystems, releasing any allocated resources.
     */
    public abstract void shutdown();
}