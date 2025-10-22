package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.quest.RPlayerQuest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RPlayerQuestRepositoryTest {

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
            new RPlayerQuestRepository(executor, entityManagerFactory);

            final GenericCachedRepository.ConstructorInvocation<RPlayerQuest, Long, Long> invocation =
                    GenericCachedRepository.getLastConstructorInvocation();

            assertNotNull(invocation, "Constructor invocation should be captured");
            assertSame(executor, invocation.executor(), "Executor should be forwarded to the parent constructor");
            assertSame(entityManagerFactory, invocation.entityManagerFactory(),
                    "EntityManagerFactory should be forwarded to the parent constructor");
            assertSame(RPlayerQuest.class, invocation.entityType(),
                    "Repository must advertise the RPlayerQuest entity class");

            final Function<RPlayerQuest, Long> idExtractor = invocation.idExtractor();
            assertNotNull(idExtractor, "ID extractor should not be null");

            final RPlayerQuest entity = instantiateQuestWithId(42L);
            assertEquals(42L, idExtractor.apply(entity),
                    "ID extractor should resolve the entity identifier via AbstractEntity::getId");
        } finally {
            executor.shutdownNow();
        }
    }

    private static RPlayerQuest instantiateQuestWithId(final long id)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException,
            NoSuchFieldException {
        final Constructor<RPlayerQuest> constructor = RPlayerQuest.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final RPlayerQuest quest = constructor.newInstance();

        try {
            final Method setId = AbstractEntity.class.getDeclaredMethod("setId", Long.class);
            setId.setAccessible(true);
            setId.invoke(quest, id);
        } catch (final NoSuchMethodException ignored) {
            final Field idField = AbstractEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(quest, id);
        }

        return quest;
    }
}
