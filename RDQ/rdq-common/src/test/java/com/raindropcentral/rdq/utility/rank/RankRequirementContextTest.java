package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankRequirementContextTest {

    @Test
    void itPropagatesContextAndLogsFailuresDuringApply() {
        final CountingRequirementSection successful = new CountingRequirementSection(false);
        final CountingRequirementSection failing = new CountingRequirementSection(true);

        final Map<String, BaseRequirementSection> requirements = new LinkedHashMap<>();
        requirements.put("alpha", successful);
        requirements.put("beta", failing);

        final RankSection rankSection = mock(RankSection.class);
        when(rankSection.getRequirements()).thenReturn(requirements);

        final Logger logger = Logger.getLogger("RankRequirementContextTest#apply");
        final TestLogHandler handler = new TestLogHandler();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);

        try {
            RankRequirementContext.apply(rankSection, "warrior", "novice", logger);
        } finally {
            logger.removeHandler(handler);
        }

        assertEquals(1, successful.setContextCalls, "apply should set context on each requirement");
        assertEquals("warrior", successful.lastTreeId);
        assertEquals("novice", successful.lastRankId);
        assertEquals("alpha", successful.lastRequirementKey);
        assertEquals(1, successful.afterParsingCalls, "apply should invoke afterParsing on successful requirements");

        assertEquals(1, failing.setContextCalls, "apply should attempt to set context even when afterParsing fails");
        assertEquals(1, failing.afterParsingCalls, "apply should attempt afterParsing even when it throws");

        assertEquals(1, handler.records.size(), "apply should log a warning for parsing failures");
        final LogRecord record = handler.records.get(0);
        assertEquals(Level.WARNING, record.getLevel());
        assertEquals("Failed to process requirement {0} for rank {1} in tree {2}: {3}", record.getMessage());
        final Object[] parameters = record.getParameters();
        assertNotNull(parameters, "logger should provide context parameters");
        assertEquals("beta", parameters[0]);
        assertEquals("novice", parameters[1]);
        assertEquals("warrior", parameters[2]);
        assertEquals("boom", parameters[3]);
    }

    @Test
    void itSkipsNullRequirementEntries() {
        final CountingRequirementSection first = new CountingRequirementSection(false);
        final CountingRequirementSection third = new CountingRequirementSection(false);

        final Map<String, BaseRequirementSection> requirements = new LinkedHashMap<>();
        requirements.put("alpha", first);
        requirements.put("beta", null);
        requirements.put("gamma", third);

        final RankSection rankSection = mock(RankSection.class);
        when(rankSection.getRequirements()).thenReturn(requirements);

        assertDoesNotThrow(() -> RankRequirementContext.apply(rankSection, "tree", "rank", Logger.getAnonymousLogger()),
                "apply should ignore null requirement entries without throwing");

        assertEquals(1, first.setContextCalls, "non-null requirements before null entries should still be processed");
        assertEquals(1, first.afterParsingCalls, "non-null requirements should invoke afterParsing");
        assertEquals(1, third.setContextCalls, "non-null requirements after null entries should still be processed");
        assertEquals(1, third.afterParsingCalls, "non-null requirements after null entries should invoke afterParsing");
    }

    @Test
    void itDelegatesApplyAsyncToProvidedExecutor() {
        final RankSection rankSection = mock(RankSection.class);
        when(rankSection.getRequirements()).thenReturn(Collections.emptyMap());

        final ExecutorService singleThread = Executors.newSingleThreadExecutor(r -> new Thread(r, "rank-context-executor"));
        final AtomicReference<String> executingThreadName = new AtomicReference<>();
        final Executor executor = command -> singleThread.submit(() -> {
            executingThreadName.set(Thread.currentThread().getName());
            command.run();
        });

        try {
            final CompletableFuture<Void> future = RankRequirementContext.applyAsync(
                    rankSection,
                    "tree",
                    "rank",
                    Logger.getAnonymousLogger(),
                    executor
            );

            future.join();
            assertTrue(future.isDone(), "applyAsync should complete even when requirements are empty");
            assertEquals("rank-context-executor", executingThreadName.get(),
                    "applyAsync should delegate to the provided executor");
        } finally {
            singleThread.shutdownNow();
        }
    }

    private static final class CountingRequirementSection extends BaseRequirementSection {

        private final boolean throwOnAfterParsing;
        private int setContextCalls;
        private int afterParsingCalls;
        private String lastTreeId;
        private String lastRankId;
        private String lastRequirementKey;

        private CountingRequirementSection(final boolean throwOnAfterParsing) {
            super(new EvaluationEnvironmentBuilder());
            this.throwOnAfterParsing = throwOnAfterParsing;
        }

        @Override
        public void setContext(final String rankTreeName, final String rankName, final String requirementKey) {
            this.setContextCalls++;
            this.lastTreeId = rankTreeName;
            this.lastRankId = rankName;
            this.lastRequirementKey = requirementKey;
            super.setContext(rankTreeName, rankName, requirementKey);
        }

        @Override
        public void afterParsing(final List<Field> fields) throws Exception {
            this.afterParsingCalls++;
            if (this.throwOnAfterParsing) {
                throw new IllegalStateException("boom");
            }
        }
    }

    private static final class TestLogHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            this.records.add(record);
        }

        @Override
        public void flush() {
            // No-op for test handler.
        }

        @Override
        public void close() {
            this.records.clear();
        }
    }
}
