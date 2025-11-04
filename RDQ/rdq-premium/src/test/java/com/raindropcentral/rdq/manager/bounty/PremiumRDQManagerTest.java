package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rplatform.RPlatform;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;

@ExtendWith(MockitoExtension.class)
class PremiumRDQManagerTest {

    @Mock
    private JavaPlugin plugin;

    @Mock
    private RPlatform platform;

    @Mock
    private Executor executor;

    @Mock
    private RBountyRepository bountyRepository;

    @Mock
    private RDQPlayerRepository playerRepository;

    @Test
    void constructorCreatesDefaultBountyManagerAndExposesDependencies() {
        try (MockedConstruction<DefaultBountyManager> construction = mockConstruction(DefaultBountyManager.class,
                (mock, context) -> {
                    assertSame(this.plugin, context.arguments().get(0));
                    assertSame(this.platform, context.arguments().get(1));
                    assertSame(this.executor, context.arguments().get(2));
                    assertSame(this.bountyRepository, context.arguments().get(3));
                    assertSame(this.playerRepository, context.arguments().get(4));
                })) {
            final PremiumRDQManager manager = new PremiumRDQManager(
                    this.plugin,
                    this.platform,
                    this.executor,
                    this.bountyRepository,
                    this.playerRepository
            );

            final DefaultBountyManager constructedManager = construction.constructed().get(0);

            assertSame(constructedManager, manager.getBountyManager());
            assertNull(manager.getQuestManager());
            assertNull(manager.getRankManager());
            assertNull(manager.getPerkManager());
            assertTrue(manager.isPremium());

            assertDoesNotThrow(manager::initialize);
            assertDoesNotThrow(manager::shutdown);
        }
    }
}
