package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link QuestCategory} entities.
 * <p>
 * Uses {@link CachedRepository} built-in methods wherever possible.
 * The cache key is the category {@code identifier} string.
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public class QuestCategoryRepository extends CachedRepository<QuestCategory, Long, String> {

    /**
     * Constructs a new {@code QuestCategoryRepository}.
     *
     * @param executor             the executor service for async operations
     * @param entityManagerFactory the entity manager factory
     * @param entityClass          the entity class type
     * @param keyExtractor         function to extract the cache key from the entity
     */
    public QuestCategoryRepository(
            @NotNull final ExecutorService executor,
            @NotNull final EntityManagerFactory entityManagerFactory,
            @NotNull final Class<QuestCategory> entityClass,
            @NotNull final Function<QuestCategory, String> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds all enabled quest categories asynchronously, ordered by display order.
     *
     * @return a future containing the list of enabled categories
     */
    @NotNull
    public CompletableFuture<List<QuestCategory>> findAllEnabled() {
        return findAllByAttributesAsync(Map.of("enabled", true))
                .thenApply(categories -> categories.stream()
                        .sorted(java.util.Comparator.comparingInt(QuestCategory::getDisplayOrder))
                        .toList());
    }

    /**
     * Finds a quest category by its unique identifier asynchronously.
     *
     * @param identifier the category identifier
     * @return a future containing the optional category
     */
    @NotNull
    public CompletableFuture<Optional<QuestCategory>> findByIdentifier(@NotNull final String identifier) {
        return findAllByAttributesAsync(Map.of("identifier", identifier))
                .thenApply(list -> list.stream().findFirst());
    }
}
