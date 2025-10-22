package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RRequirementRepositoryTest {

    private ExecutorService executor;
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        this.executor = Executors.newSingleThreadExecutor();
        this.entityManagerFactory = Mockito.mock(EntityManagerFactory.class);
    }

    @AfterEach
    void tearDown() {
        this.executor.shutdownNow();
    }

    @Test
    @DisplayName("Constructor forwards entity metadata and identifier extractor to GenericCachedRepository")
    void constructorConfiguresSuperclassMetadata() {
        RRequirementRepository repository = new RRequirementRepository(this.executor, this.entityManagerFactory);

        Class<?> entityClass = extractEntityClass(repository);
        Function<RRequirement, Long> identifierExtractor = extractIdentifierExtractor(repository);

        assertSame(RRequirement.class, entityClass, "Repository must declare the RRequirement aggregate type");
        assertNotNull(identifierExtractor, "Identifier extractor should be present on the repository");

        RRequirement entity = requirementWithId(99L);
        assertEquals(99L, identifierExtractor.apply(entity), "Identifier extractor should resolve AbstractEntity#getId");
    }

    private static Class<?> extractEntityClass(RRequirementRepository repository) {
        return Arrays.stream(GenericCachedRepository.class.getDeclaredFields())
                .filter(field -> Class.class.equals(field.getType()))
                .map(field -> (Class<?>) readField(field, repository))
                .filter(Objects::nonNull)
                .filter(RRequirement.class::equals)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Entity class field not found on GenericCachedRepository"));
    }

    @SuppressWarnings("unchecked")
    private static Function<RRequirement, Long> extractIdentifierExtractor(RRequirementRepository repository) {
        return Arrays.stream(GenericCachedRepository.class.getDeclaredFields())
                .filter(field -> Function.class.isAssignableFrom(field.getType()))
                .map(field -> (Function<RRequirement, Long>) readField(field, repository))
                .filter(Objects::nonNull)
                .filter(function -> {
                    RRequirement probe = requirementWithId(123L);
                    return Long.valueOf(123L).equals(function.apply(probe));
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Identifier extractor field not found on GenericCachedRepository"));
    }

    private static RRequirement requirementWithId(long id) {
        AbstractRequirement requirement = Mockito.mock(AbstractRequirement.class);
        IconSection icon = Mockito.mock(IconSection.class);
        RRequirement entity = new RRequirement(requirement, icon);
        setEntityId(entity, id);
        return entity;
    }

    private static void setEntityId(AbstractEntity entity, long id) {
        try {
            Field idField = AbstractEntity.class.getDeclaredField("id");
            boolean accessible = idField.canAccess(entity);
            idField.setAccessible(true);
            idField.set(entity, id);
            idField.setAccessible(accessible);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new AssertionError("Unable to configure entity identifier", ex);
        }
    }

    private static Object readField(Field field, RRequirementRepository repository) {
        boolean accessible = field.canAccess(repository);
        field.setAccessible(true);
        try {
            return field.get(repository);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Unable to read field " + field.getName(), ex);
        } finally {
            field.setAccessible(accessible);
        }
    }
}
