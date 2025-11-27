package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.manager.perk.DefaultPerkManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import org.jetbrains.annotations.NotNull;

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

    private final QuestManager questManager;
    private final DefaultPerkManager perkManager;

    /**
     * Constructs the PremiumRDQManager and initializes all sub-managers
     * using dependency injection.
     * Note: BountyService is now managed separately via RDQ.getBountyService()
     */
    public PremiumRDQManager(@NotNull RDQ rdq) {
        super("Premium");

        this.questManager = null;
        this.perkManager = new DefaultPerkManager(rdq, rdq.getPerkInitializationManager().getPerkEventBus());
    }

    @Override
    public @NotNull QuestManager getQuestManager() {
        return this.questManager;
    }

    @Override
    public @NotNull DefaultPerkManager getPerkManager() {
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