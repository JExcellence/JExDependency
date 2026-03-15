package com.raindropcentral.core.service;

import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.repository.RStatisticRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for managing abstract statistics with comprehensive CRUD operations.
 *
 * <p>This service provides high-level operations for statistic management outside of aggregate
 * contexts. It's primarily used for administrative tooling or cross-profile maintenance tasks
 * that need direct access to statistic entities. For player-specific statistics, prefer using
 * {@link RPlayerStatisticService} which operates on the aggregate level.
 * </p>
 *
 * <p>All operations are asynchronous and return {@link CompletableFuture} to avoid blocking
 * the main thread. The service automatically injects the {@link RStatisticRepository} via the
 * {@link de.jexcellence.hibernate.repository.RepositoryManager} when instantiated through
 * {@code createInstance()}.
 * </p>
 *
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class RStatisticService {

    @InjectRepository
    private RStatisticRepository statisticRepository;

    /**
     * Constructs a new RStatisticService.
 *
 * <p>The repository will be automatically injected by the RepositoryManager when this service
     * is created via {@code RepositoryManager.getInstance().createInstance(RStatisticService.class)}.
     * </p>
     */
    public RStatisticService() {
        // Repository injected by RepositoryManager
    }

    /**
     * Finds a statistic by its database ID.
     *
     * @param id the statistic's database identifier
     * @return future containing an optional with the statistic if found
     */
    public CompletableFuture<Optional<RAbstractStatistic>> findById(final @NotNull Long id) {
        return statisticRepository.findByIdAsync(id);
    }

    /**
     * Finds all statistics in the database.
 *
 * <p><b>Warning:</b> This operation can be expensive if there are many statistics.
     * Consider using pagination or filtering in production environments.
     * </p>
     *
     * @return future containing a list of all statistics
     */
    public CompletableFuture<List<RAbstractStatistic>> findAll() {
        return statisticRepository.findAllAsync(0, Integer.MAX_VALUE);
    }

    /**
     * Creates a new statistic entity.
     *
     * @param statistic the statistic to create
     * @return future containing the created statistic with assigned ID
     */
    public CompletableFuture<RAbstractStatistic> create(final @NotNull RAbstractStatistic statistic) {
        return statisticRepository.createAsync(statistic);
    }

    /**
     * Updates an existing statistic entity.
 *
 * <p>The statistic must have a non-null ID to be updated.
     * </p>
     *
     * @param statistic the statistic to update
     * @return future containing the updated statistic
     */
    public CompletableFuture<RAbstractStatistic> update(final @NotNull RAbstractStatistic statistic) {
        return statisticRepository.updateAsync(statistic);
    }

    /**
     * Creates or updates a statistic entity.
 *
 * <p>If the statistic has a null ID, it will be created. Otherwise, it will be updated.
     * </p>
     *
     * @param statistic the statistic to save
     * @return future containing the saved statistic
     */
    public CompletableFuture<RAbstractStatistic> createOrUpdate(final @NotNull RAbstractStatistic statistic) {
        if (statistic.getId() == null) {
            return create(statistic);
        }
        return update(statistic);
    }

    /**
     * Deletes a statistic by its database ID.
     *
     * @param id the statistic's database identifier
     * @return future that completes when the deletion is finished
     */
    public CompletableFuture<Boolean> deleteById(final @NotNull Long id) {
        return statisticRepository.deleteAsync(id);
    }

    /**
     * Deletes a statistic entity.
     *
     * @param statistic the statistic to delete
     * @return future that completes when the deletion is finished
     */
    public CompletableFuture<Boolean> delete(final @NotNull RAbstractStatistic statistic) {
        if (statistic.getId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return deleteById(statistic.getId());
    }

    /**
     * Checks if a statistic exists by its database ID.
     *
     * @param id the statistic's database identifier
     * @return future containing true if the statistic exists
     */
    public CompletableFuture<Boolean> existsById(final @NotNull Long id) {
        return findById(id).thenApply(Optional::isPresent);
    }

    /**
     * Deletes multiple statistics by their IDs.
 *
 * <p>This is a convenience method for batch deletion operations.
     * </p>
     *
     * @param ids the list of statistic IDs to delete
     * @return future that completes when all deletions are finished
     */
    public CompletableFuture<Void> deleteByIds(final @NotNull List<Long> ids) {
        final CompletableFuture<?>[] deleteFutures = ids.stream()
                .map(this::deleteById)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(deleteFutures);
    }

    /**
     * Gets the injected repository instance.
 *
 * <p>This is primarily for testing purposes or advanced use cases.
     * </p>
     *
     * @return the statistic repository
     */
    public RStatisticRepository getRepository() {
        return statisticRepository;
    }
}
