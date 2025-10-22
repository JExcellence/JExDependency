package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.quest.RQuest;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RQuestRepositoryTest {

    private ExecutorService executor;
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        this.executor = Executors.newSingleThreadExecutor();
        this.entityManagerFactory = Mockito.mock(EntityManagerFactory.class);
        GenericCachedRepository.resetConstructorInvocation();
    }

    @AfterEach
    void tearDown() {
        this.executor.shutdownNow();
        GenericCachedRepository.resetConstructorInvocation();
    }

    @Test
    @DisplayName("Constructor forwards quest metadata and identifier extractor to GenericCachedRepository")
    void constructorConfiguresQuestIdentifierExtractor() {
        new RQuestRepository(this.executor, this.entityManagerFactory);

        GenericCachedRepository.ConstructorInvocation<RQuest, Long, String> invocation =
                GenericCachedRepository.getLastConstructorInvocation();

        assertNotNull(invocation, "Constructor invocation should be captured");
        assertSame(this.executor, invocation.executor(), "Executor should be forwarded to the parent constructor");
        assertSame(this.entityManagerFactory, invocation.entityManagerFactory(),
                "EntityManagerFactory should be forwarded to the parent constructor");
        assertSame(RQuest.class, invocation.entityType(),
                "Repository must advertise the RQuest entity class");

        Function<RQuest, ?> identifierExtractor = invocation.idExtractor();
        assertNotNull(identifierExtractor, "Identifier extractor should not be null");

        RecordingQuest quest = new RecordingQuest("quest.delegate", 0, 1);
        assertEquals("quest.delegate", identifierExtractor.apply(quest),
                "Identifier extractor should invoke RQuest::getIdentifier");
    }

    @Test
    @DisplayName("findByIdentifierAsync delegates to the identifier cache key")
    void findByIdentifierAsyncDelegatesToCacheKeyLookup() {
        RQuestRepository repository = spy(new RQuestRepository(this.executor, this.entityManagerFactory));
        RecordingQuest quest = new RecordingQuest("quest.lookup", 0, 1);
        CompletableFuture<RQuest> questFuture = CompletableFuture.completedFuture(quest);

        doReturn(questFuture).when(repository).findByCacheKeyAsync("identifier", "quest.lookup");

        CompletableFuture<Optional<RQuest>> result = repository.findByIdentifierAsync("quest.lookup");
        Optional<RQuest> resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), result::join);

        assertTrue(resolved.isPresent(), "Expected the optional to contain the quest");
        assertSame(quest, resolved.orElseThrow());
        verify(repository).findByCacheKeyAsync("identifier", "quest.lookup");
    }

    private static final class RecordingQuest extends RQuest {

        private RecordingQuest(final String identifier, final int initialUpgradeLevel, final int maximumUpgradeLevel) {
            super(identifier, initialUpgradeLevel, maximumUpgradeLevel);
        }

        @Override
        protected Material initializeShowcase() {
            return Material.BOOK;
        }
    }
}
