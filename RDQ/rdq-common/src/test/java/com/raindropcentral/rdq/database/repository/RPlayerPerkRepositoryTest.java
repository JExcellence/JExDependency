package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.perk.RPlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RPlayerPerkRepositoryTest {

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
    void constructorWiresEntityMetadata() {
        new RPlayerPerkRepository(this.executor, this.entityManagerFactory);

        GenericCachedRepository.ConstructorInvocation<RPlayerPerk, Long, Long> invocation =
                GenericCachedRepository.getLastConstructorInvocation();

        assertNotNull(invocation, "Constructor invocation should be recorded");
        assertSame(this.executor, invocation.executor(), "Executor should be forwarded to the parent constructor");
        assertSame(this.entityManagerFactory, invocation.entityManagerFactory(),
                "EntityManagerFactory should be forwarded to the parent constructor");
        assertSame(RPlayerPerk.class, invocation.entityType(),
                "Repository must supply the RPlayerPerk entity type to the parent constructor");

        Function<RPlayerPerk, Long> idExtractor = invocation.idExtractor();
        assertNotNull(idExtractor, "Identifier extractor should be provided to the parent constructor");

        RPlayerPerk entity = instantiatePlayerPerk(73L);
        assertEquals(73L, idExtractor.apply(entity),
                "Identifier extractor should resolve AbstractEntity::getId");
    }

    @Test
    @DisplayName("findByPlayerAndPerkAsync delegates to attribute lookup with player and perk keys")
    void findByPlayerAndPerkAsyncDelegatesToAttributeLookup() {
        RPlayerPerkRepository repository = spy(new RPlayerPerkRepository(this.executor, this.entityManagerFactory));
        RDQPlayer player = mock(RDQPlayer.class);
        RPerk perk = mock(RPerk.class);
        RPlayerPerk association = mock(RPlayerPerk.class);

        doReturn(CompletableFuture.completedFuture(association)).when(repository).findByAttributesAsync(anyMap());

        CompletableFuture<Optional<RPlayerPerk>> result = repository.findByPlayerAndPerkAsync(player, perk);
        Optional<RPlayerPerk> resolved = assertTimeoutPreemptively(Duration.ofSeconds(1), result::join);

        assertTrue(resolved.isPresent(), "Expected the optional to contain the resolved association");
        assertSame(association, resolved.orElseThrow());

        ArgumentCaptor<Map<String, Object>> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findByAttributesAsync(attributeCaptor.capture());

        Map<String, Object> attributes = attributeCaptor.getValue();
        assertEquals(2, attributes.size(), "Attribute lookup should be performed with player and perk keys");
        assertSame(player, attributes.get("player"));
        assertSame(perk, attributes.get("perk"));
    }

    private static RPlayerPerk instantiatePlayerPerk(long id) {
        try {
            Constructor<RPlayerPerk> constructor = RPlayerPerk.class.getDeclaredConstructor();
            boolean accessible = constructor.canAccess(null);
            constructor.setAccessible(true);
            RPlayerPerk perk = constructor.newInstance();
            constructor.setAccessible(accessible);

            Field idField = AbstractEntity.class.getDeclaredField("id");
            boolean fieldAccessible = idField.canAccess(perk);
            idField.setAccessible(true);
            idField.set(perk, id);
            idField.setAccessible(fieldAccessible);

            return perk;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to instantiate RPlayerPerk for testing", exception);
        }
    }
}
