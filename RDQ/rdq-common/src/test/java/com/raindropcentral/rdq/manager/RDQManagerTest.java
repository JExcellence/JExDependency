package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.manager.bounty.BountyManager;
import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import com.raindropcentral.rdq.manager.rank.RankManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RDQManagerTest {

    private static final String EDITION = "TestEdition";

    @Mock
    private BountyManager bountyManager;

    @Mock
    private QuestManager questManager;

    @Mock
    private RankManager rankManager;

    @Mock
    private PerkManager perkManager;

    @Test
    void stubReturnsProvidedManagersAndEdition() {
        final StubRDQManager manager = new StubRDQManager(
                EDITION,
                false,
                bountyManager,
                questManager,
                rankManager,
                perkManager
        );

        assertEquals(EDITION, manager.getEdition());
        assertSame(bountyManager, manager.getBountyManager());
        assertSame(questManager, manager.getQuestManager());
        assertSame(rankManager, manager.getRankManager());
        assertSame(perkManager, manager.getPerkManager());
        assertFalse(manager.isPremium());
    }

    @Test
    void lifecycleHooksFlipFlags() {
        final StubRDQManager manager = new StubRDQManager(
                EDITION,
                true,
                bountyManager,
                questManager,
                rankManager,
                perkManager
        );

        assertFalse(manager.isInitialized());
        assertFalse(manager.isShutdown());

        manager.initialize();
        assertTrue(manager.isInitialized());

        manager.shutdown();
        assertTrue(manager.isShutdown());
    }

    private static final class StubRDQManager extends RDQManager {

        private final boolean premium;
        private final BountyManager bountyManager;
        private final QuestManager questManager;
        private final RankManager rankManager;
        private final PerkManager perkManager;
        private boolean initialized;
        private boolean shutdown;

        private StubRDQManager(
                final String edition,
                final boolean premium,
                final BountyManager bountyManager,
                final QuestManager questManager,
                final RankManager rankManager,
                final PerkManager perkManager
        ) {
            super(edition);
            this.premium = premium;
            this.bountyManager = bountyManager;
            this.questManager = questManager;
            this.rankManager = rankManager;
            this.perkManager = perkManager;
        }

        @Override
        public BountyManager getBountyManager() {
            return bountyManager;
        }

        @Override
        public QuestManager getQuestManager() {
            return questManager;
        }

        @Override
        public RankManager getRankManager() {
            return rankManager;
        }

        @Override
        public PerkManager getPerkManager() {
            return perkManager;
        }

        @Override
        public boolean isPremium() {
            return premium;
        }

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        private boolean isInitialized() {
            return initialized;
        }

        private boolean isShutdown() {
            return shutdown;
        }
    }
}
