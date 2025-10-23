package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.manager.bounty.DefaultBountyManager;
import com.raindropcentral.rplatform.RPlatform;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void constructorInjectsDependenciesAndProvidesPremiumManagers() {
        AtomicReference<List<Object>> constructorArguments = new AtomicReference<>();

        try (MockedConstruction<DefaultBountyManager> construction = mockConstruction(DefaultBountyManager.class,
                (mock, context) -> constructorArguments.set(context.arguments()))) {

            PremiumRDQManager manager = new PremiumRDQManager(
                    this.plugin,
                    this.platform,
                    this.executor,
                    this.bountyRepository,
                    this.playerRepository
            );

            List<DefaultBountyManager> constructedManagers = construction.constructed();
            assertEquals(1, constructedManagers.size(), "Expected a single DefaultBountyManager instance");

            List<Object> arguments = constructorArguments.get();
            assertNotNull(arguments, "Constructor arguments should be captured");
            assertEquals(5, arguments.size(), "DefaultBountyManager should receive five arguments");
            assertSame(this.plugin, arguments.get(0), "JavaPlugin must be provided to DefaultBountyManager");
            assertSame(this.platform, arguments.get(1), "RPlatform must be provided to DefaultBountyManager");
            assertSame(this.executor, arguments.get(2), "Executor must be provided to DefaultBountyManager");
            assertSame(this.bountyRepository, arguments.get(3), "RBountyRepository must be provided to DefaultBountyManager");
            assertSame(this.playerRepository, arguments.get(4), "RDQPlayerRepository must be provided to DefaultBountyManager");

            DefaultBountyManager bountyManager = constructedManagers.get(0);
            assertSame(bountyManager, manager.getBountyManager(), "Bounty manager accessor should return the constructed instance");

            assertNull(manager.getQuestManager(), "Premium quests are not yet implemented");
            assertNull(manager.getRankManager(), "Premium ranks are not yet implemented");
            assertNull(manager.getPerkManager(), "Premium perks are not yet implemented");
            assertTrue(manager.isPremium(), "Premium manager must report premium availability");

            manager.initialize();
            manager.shutdown();

            assertSame(bountyManager, manager.getBountyManager(), "Lifecycle methods must not replace the bounty manager");
            assertNull(manager.getQuestManager(), "Quest manager should remain uninitialized after lifecycle calls");
            assertNull(manager.getRankManager(), "Rank manager should remain uninitialized after lifecycle calls");
            assertNull(manager.getPerkManager(), "Perk manager should remain uninitialized after lifecycle calls");
        }
    }
}
