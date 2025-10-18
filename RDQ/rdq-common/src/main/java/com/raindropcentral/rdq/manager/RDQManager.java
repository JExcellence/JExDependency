package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.manager.bounty.BountyManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import com.raindropcentral.rdq.manager.rank.RankManager;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import org.jetbrains.annotations.NotNull;

/**
 * Central manager for all RDQ functionalities.
 * <p>
 * This abstract class defines the contract for managing different aspects of RDQ.
 * Implementations differ between free and premium versions, providing different
 * levels of functionality.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public abstract class RDQManager {

    private final String edition;

    protected RDQManager(final @NotNull String edition) {
        this.edition = edition;
    }

    public abstract @NotNull BountyManager getBountyManager();

    public abstract @NotNull QuestManager getQuestManager();

    public abstract @NotNull RankManager getRankManager();

    public abstract @NotNull PerkManager getPerkManager();

    public final @NotNull String getEdition() {
        return this.edition;
    }

    public abstract boolean isPremium();

    public abstract void initialize();

    public abstract void shutdown();
}