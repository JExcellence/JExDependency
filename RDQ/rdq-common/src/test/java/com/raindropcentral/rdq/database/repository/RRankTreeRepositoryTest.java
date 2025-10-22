package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RRankTreeRepositoryTest {

    private ExecutorService executor;
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        this.executor = Executors.newSingleThreadExecutor();
        this.entityManagerFactory = mock(EntityManagerFactory.class);
        GenericCachedRepository.resetConstructorInvocation();
    }

    @AfterEach
    void tearDown() {
        this.executor.shutdownNow();
        GenericCachedRepository.resetConstructorInvocation();
    }

    @Test
    @DisplayName("Constructor forwards entity metadata and identifier extractor to GenericCachedRepository")
    void constructorWiresIdentifierExtractor() {
        new RRankTreeRepository(this.executor, this.entityManagerFactory);

        GenericCachedRepository.ConstructorInvocation<RRankTree, Long, String> invocation =
                GenericCachedRepository.getLastConstructorInvocation();

        assertNotNull(invocation, "Constructor invocation should be recorded");
        assertSame(this.executor, invocation.executor(), "Executor should be forwarded to the parent constructor");
        assertSame(this.entityManagerFactory, invocation.entityManagerFactory(),
                "EntityManagerFactory should be forwarded to the parent constructor");
        assertSame(RRankTree.class, invocation.entityType(),
                "Repository must supply the RRankTree entity type to the parent constructor");

        Function<RRankTree, ?> idExtractor = invocation.idExtractor();
        assertNotNull(idExtractor, "Identifier extractor should be provided to the parent constructor");

        RRankTree tree = createRankTree("tree.savanna");
        Object identifier = idExtractor.apply(tree);
        assertEquals("tree.savanna", identifier,
                "Identifier extractor should delegate to RRankTree::getIdentifier");
    }

    @Test
    @DisplayName("findByIdentifierAsync delegates to cache key lookup with the identifier attribute")
    void findByIdentifierAsyncDelegatesToCacheKeyLookup() {
        RRankTreeRepository repository = spy(new RRankTreeRepository(this.executor, this.entityManagerFactory));
        RRankTree rankTree = createRankTree("tree.lotus");

        doReturn(CompletableFuture.completedFuture(rankTree)).when(repository)
                .findByCacheKeyAsync(eq("identifier"), eq("tree.lotus"));

        CompletableFuture<Optional<RRankTree>> future = repository.findByIdentifierAsync("tree.lotus");
        Optional<RRankTree> result = assertTimeoutPreemptively(Duration.ofSeconds(1), future::join);

        assertTrue(result.isPresent(), "Expected the optional to contain the resolved rank tree");
        assertSame(rankTree, result.orElseThrow());

        verify(repository).findByCacheKeyAsync(eq("identifier"), eq("tree.lotus"));
    }

    private static RRankTree createRankTree(final String identifier) {
        return new RRankTree(
                identifier,
                identifier + ".name",
                identifier + ".description",
                new IconSection(new EvaluationEnvironmentBuilder()),
                1,
                0,
                true,
                false
        );
    }
}
