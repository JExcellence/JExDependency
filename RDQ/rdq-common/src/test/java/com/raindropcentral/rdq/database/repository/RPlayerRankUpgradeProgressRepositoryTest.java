package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class RPlayerRankUpgradeProgressRepositoryTest {

    @AfterEach
    void resetConstructorCapture() {
        GenericCachedRepository.resetConstructorInvocation();
    }

    @Test
    void constructorSuppliesEntityClassAndIdExtractor() throws Exception {
        GenericCachedRepository.resetConstructorInvocation();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

        try {
            new RPlayerRankUpgradeProgressRepository(executor, entityManagerFactory);

            final GenericCachedRepository.ConstructorInvocation<RPlayerRankUpgradeProgress, Long, Long> invocation =
                    GenericCachedRepository.getLastConstructorInvocation();

            assertNotNull(invocation, "Constructor invocation should be captured");
            assertSame(executor, invocation.executor(),
                    "Executor should be forwarded to the parent constructor for async operations");
            assertSame(entityManagerFactory, invocation.entityManagerFactory(),
                    "EntityManagerFactory should be forwarded to the parent constructor");
            assertSame(RPlayerRankUpgradeProgress.class, invocation.entityType(),
                    "Repository must advertise the RPlayerRankUpgradeProgress entity class");

            final Function<RPlayerRankUpgradeProgress, Long> idExtractor = invocation.idExtractor();
            assertNotNull(idExtractor, "ID extractor should not be null");

            final RPlayerRankUpgradeProgress entity = instantiateProgressWithId(77L);
            assertEquals(77L, idExtractor.apply(entity),
                    "ID extractor should resolve the entity identifier via RPlayerRankUpgradeProgress::getId");
        } finally {
            executor.shutdownNow();
        }
    }

    private static RPlayerRankUpgradeProgress instantiateProgressWithId(final long id)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException,
            NoSuchFieldException {
        final Constructor<RPlayerRankUpgradeProgress> constructor = RPlayerRankUpgradeProgress.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final RPlayerRankUpgradeProgress progress = constructor.newInstance();

        try {
            final Method setId = AbstractEntity.class.getDeclaredMethod("setId", Long.class);
            setId.setAccessible(true);
            setId.invoke(progress, id);
        } catch (final NoSuchMethodException ignored) {
            final Field idField = AbstractEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, id);
        }

        return progress;
    }
}
