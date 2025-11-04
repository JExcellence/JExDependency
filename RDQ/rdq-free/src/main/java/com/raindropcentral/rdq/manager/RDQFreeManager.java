package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.manager.bounty.BountyManager;
import com.raindropcentral.rdq.manager.bounty.FreeBountyManager;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import com.raindropcentral.rplatform.RPlatform;
import org.bukkit.plugin.java.JavaPlugin;
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
    private final PerkManager perkManager;

    public RDQFreeManager(
            JavaPlugin plugin, RPlatform platform
    ) {
        super("Free");
        this.bountyManager = new FreeBountyManager(plugin, platform);
        this.questManager = new FreeQuestManager();
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

    private static class FreeRankManager {
    }

    private static class FreePerkManager implements PerkManager {

        @Override
        public com.raindropcentral.rdq.perk.runtime.PerkRegistry getPerkRegistry() {
            return null; // Not implemented in free edition
        }

        @Override
        public com.raindropcentral.rdq.perk.runtime.PerkStateService getPerkStateService() {
            return null; // Not implemented in free edition
        }

        @Override
        public com.raindropcentral.rdq.perk.runtime.PerkTriggerService getPerkTriggerService() {
            return null; // Not implemented in free edition
        }

        @Override
        public void initialize() {
            // No-op for free edition
        }

        @Override
        public void shutdown() {
            // No-op for free edition
        }
    }
}