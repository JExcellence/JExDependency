package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.manager.bounty.FreeBountyManager;
import com.raindropcentral.rplatform.RPlatform;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;

@ExtendWith(MockitoExtension.class)
class RDQFreeManagerTest {

    @Mock
    private JavaPlugin plugin;

    @Mock
    private RPlatform platform;

    private Logger logger;
    private RecordingHandler handler;
    private Level originalLevel;
    private boolean originalUseParentHandlers;

    @BeforeEach
    void setUpLogger() {
        this.logger = Logger.getLogger(RDQFreeManager.class.getName());
        this.handler = new RecordingHandler();
        this.originalLevel = this.logger.getLevel();
        this.originalUseParentHandlers = this.logger.getUseParentHandlers();
        this.logger.setLevel(Level.ALL);
        this.logger.setUseParentHandlers(false);
        this.logger.addHandler(this.handler);
    }

    @AfterEach
    void tearDownLogger() {
        if (this.logger != null && this.handler != null) {
            this.logger.removeHandler(this.handler);
            this.logger.setLevel(this.originalLevel);
            this.logger.setUseParentHandlers(this.originalUseParentHandlers);
        }
    }

    @Test
    void constructorWiresFreeManagersAndDependencies() {
        AtomicReference<List<Object>> constructorArguments = new AtomicReference<>();

        try (MockedConstruction<FreeBountyManager> construction = mockConstruction(FreeBountyManager.class,
                (mock, context) -> constructorArguments.set(context.arguments()))) {

            RDQFreeManager manager = new RDQFreeManager(this.plugin, this.platform);

            List<FreeBountyManager> constructedManagers = construction.constructed();
            assertEquals(1, constructedManagers.size(), "Expected a single FreeBountyManager instance");

            List<Object> arguments = constructorArguments.get();
            assertNotNull(arguments, "Constructor arguments should be captured");
            assertEquals(2, arguments.size(), "FreeBountyManager should receive two arguments");
            assertSame(this.plugin, arguments.get(0), "JavaPlugin must be provided to FreeBountyManager");
            assertSame(this.platform, arguments.get(1), "RPlatform must be provided to FreeBountyManager");

            FreeBountyManager bountyManager = constructedManagers.getFirst();
            assertSame(bountyManager, manager.getBountyManager(), "Bounty manager accessor should return the constructed instance");

            assertNotNull(manager.getQuestManager(), "Quest manager should be initialized");
            assertEquals("FreeQuestManager", manager.getQuestManager().getClass().getSimpleName());

            assertNotNull(manager.getRankManager(), "Rank manager should be initialized");
            assertEquals("FreeRankManager", manager.getRankManager().getClass().getSimpleName());

            assertNotNull(manager.getPerkManager(), "Perk manager should be initialized");
            assertEquals("FreePerkManager", manager.getPerkManager().getClass().getSimpleName());

            assertFalse(manager.isPremium(), "Free manager must not be marked as premium");
        }
    }

    @Test
    void lifecycleLogsLimitedFeatureMessages() {
        try (MockedConstruction<FreeBountyManager> ignored = mockConstruction(FreeBountyManager.class)) {
            RDQFreeManager manager = new RDQFreeManager(this.plugin, this.platform);

            manager.initialize();
            manager.shutdown();
        }

        List<String> messages = this.handler.getMessages();
        assertTrue(messages.stream().anyMatch(message -> message.contains("Initializing RDQ Free Manager")),
                "Initialization message should be logged");
        assertTrue(messages.stream().anyMatch(message -> message.contains("Limited (View Only)")),
                "Limited bounty message should be logged");
        assertTrue(messages.stream().anyMatch(message -> message.contains("Quest System: Limited")),
                "Quest limitation message should be logged");
        assertTrue(messages.stream().anyMatch(message -> message.contains("Rank System: Limited")),
                "Rank limitation message should be logged");
        assertTrue(messages.stream().anyMatch(message -> message.contains("Perk System: Limited")),
                "Perk limitation message should be logged");
        assertTrue(messages.stream().anyMatch(message -> message.contains("Shutting down RDQ Free Manager")),
                "Shutdown message should be logged");
    }

    private static final class RecordingHandler extends Handler {

        private final List<LogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record != null) {
                this.records.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        private List<String> getMessages() {
            return this.records.stream().map(LogRecord::getMessage).toList();
        }
    }
}
