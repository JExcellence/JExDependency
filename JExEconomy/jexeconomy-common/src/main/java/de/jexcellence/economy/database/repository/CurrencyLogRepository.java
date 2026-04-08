package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.CurrencyLog;
import de.jexcellence.economy.type.EChangeType;
import de.jexcellence.economy.type.ELogLevel;
import de.jexcellence.economy.type.ELogType;
import de.jexcellence.hibernate.entity.BaseEntity;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository interface for CurrencyLog entity operations.
 *
 * <p>Provides specialized methods for querying, filtering, and analyzing
 * currency operation logs. Supports both synchronous and asynchronous
 * operations for different use cases.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class CurrencyLogRepository extends CachedRepository<CurrencyLog, Long, Long> {
    
    private final ExecutorService asyncExecutorService;
    private final EntityManagerFactory entityManagerFactory;
    
    /**
     * Executes CurrencyLogRepository.
     */
    public CurrencyLogRepository(
        final @NotNull ExecutorService asyncExecutorService,
        final @NotNull EntityManagerFactory jpaEntityManagerFactory
    ) {
        
        super(
            asyncExecutorService,
            jpaEntityManagerFactory,
            CurrencyLog.class,
            BaseEntity::getId
        );
        
        this.asyncExecutorService = asyncExecutorService;
        this.entityManagerFactory = jpaEntityManagerFactory;
    }
    
    /**
     * Finds currency logs by player UUID and time range.
     *
     * @param playerUuid the player UUID
     * @param startTime the start time (inclusive)
     * @param endTime the end time (exclusive), null for no end limit
     * @return list of matching currency logs
     */
    @NotNull
    public List<CurrencyLog> findByPlayerAndTimeRange(
        @NotNull UUID playerUuid,
        @NotNull LocalDateTime startTime,
        @Nullable LocalDateTime endTime
    ) {
	    
	    try (EntityManager em = entityManagerFactory.createEntityManager()) {
		    CriteriaBuilder            cb    = em.getCriteriaBuilder();
		    CriteriaQuery<CurrencyLog> query = cb.createQuery(CurrencyLog.class);
		    Root<CurrencyLog>          root  = query.from(CurrencyLog.class);
		    
		    List<Predicate> predicates = new ArrayList<>();
		    predicates.add(cb.equal(
			    root.get("playerUuid"),
			    playerUuid
		    ));
		    predicates.add(cb.greaterThanOrEqualTo(
			    root.get("timestamp"),
			    startTime
		    ));
		    
		    if (endTime != null) {
			    predicates.add(cb.lessThan(
				    root.get("timestamp"),
				    endTime
			    ));
		    }
		    
		    query.where(predicates.toArray(new Predicate[0]));
		    query.orderBy(cb.desc(root.get("timestamp")));
		    
		    return em.createQuery(query).getResultList();
	    }
    }
    
    /**
     * Asynchronously finds currency logs by player UUID and time range.
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> findByPlayerAndTimeRangeAsync(
        @NotNull UUID playerUuid,
        @NotNull LocalDateTime startTime,
        @Nullable LocalDateTime endTime
    ) {
        return CompletableFuture.supplyAsync(() ->
                                                 findByPlayerAndTimeRange(playerUuid, startTime, endTime), asyncExecutorService);
    }
    
    /**
     * Calculates the total transaction volume for a currency within a time range.
     *
     * @param currencyId the currency ID
     * @param changeType the type of change (null for all types)
     * @param startTime the start time
     * @param endTime the end time (null for no end limit)
     * @return the total volume
     */
    public double calculateTransactionVolume(
        @NotNull Long currencyId,
        @Nullable EChangeType changeType,
        @NotNull LocalDateTime startTime,
        @Nullable LocalDateTime endTime
    ) {
	    
	    try (EntityManager em = entityManagerFactory.createEntityManager()) {
		    CriteriaBuilder       cb    = em.getCriteriaBuilder();
		    CriteriaQuery<Double> query = cb.createQuery(Double.class);
		    Root<CurrencyLog>     root  = query.from(CurrencyLog.class);
		    
		    List<Predicate> predicates = new ArrayList<>();
		    predicates.add(cb.equal(
			    root.get("currency").get("id"),
			    currencyId
		    ));
		    predicates.add(cb.equal(
			    root.get("logType"),
			    ELogType.TRANSACTION
		    ));
		    predicates.add(cb.equal(
			    root.get("success"),
			    true
		    ));
		    predicates.add(cb.greaterThanOrEqualTo(
			    root.get("timestamp"),
			    startTime
		    ));
		    predicates.add(cb.isNotNull(root.get("amount")));
		    
		    if (changeType != null) {
			    predicates.add(cb.equal(
				    root.get("operationType"),
				    changeType
			    ));
		    }
		    
		    if (endTime != null) {
			    predicates.add(cb.lessThan(
				    root.get("timestamp"),
				    endTime
			    ));
		    }
		    
		    query.select(cb.sum(root.get("amount")));
		    query.where(predicates.toArray(new Predicate[0]));
		    
		    Double result = em.createQuery(query).getSingleResult();
		    return result != null ?
		           result :
		           0.0;
	    }
    }
    
    /**
     * Counts log entries by type and time range.
     *
     * @param logType the log type
     * @param startTime the start time
     * @param endTime the end time (null for no end limit)
     * @return the count of matching entries
     */
    public long countByTypeAndTimeRange(
        @NotNull ELogType logType,
        @NotNull LocalDateTime startTime,
        @Nullable LocalDateTime endTime
    ) {
	    
	    try (EntityManager em = entityManagerFactory.createEntityManager()) {
		    CriteriaBuilder     cb    = em.getCriteriaBuilder();
		    CriteriaQuery<Long> query = cb.createQuery(Long.class);
		    Root<CurrencyLog>   root  = query.from(CurrencyLog.class);
		    
		    List<Predicate> predicates = new ArrayList<>();
		    predicates.add(cb.equal(
			    root.get("logType"),
			    logType
		    ));
		    predicates.add(cb.greaterThanOrEqualTo(
			    root.get("timestamp"),
			    startTime
		    ));
		    
		    if (endTime != null) {
			    predicates.add(cb.lessThan(
				    root.get("timestamp"),
				    endTime
			    ));
		    }
		    
		    query.select(cb.count(root));
		    query.where(predicates.toArray(new Predicate[0]));
		    
		    return em.createQuery(query).getSingleResult();
	    }
    }
    
    /**
     * Finds the most active players by transaction count for a currency.
     *
     * @param currencyId the currency ID
     * @param startTime the start time
     * @param endTime the end time (null for no end limit)
     * @param limit the maximum number of players to return
     * @return list of player UUIDs ordered by transaction count
     */
    @NotNull
    public List<UUID> findMostActivePlayersByTransactionCount(
        @NotNull Long currencyId,
        @NotNull LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int limit
    ) {
	    
	    try (EntityManager em = entityManagerFactory.createEntityManager()) {
		    CriteriaBuilder     cb    = em.getCriteriaBuilder();
		    CriteriaQuery<UUID> query = cb.createQuery(UUID.class);
		    Root<CurrencyLog>   root  = query.from(CurrencyLog.class);
		    
		    List<Predicate> predicates = new ArrayList<>();
		    predicates.add(cb.equal(
			    root.get("currency").get("id"),
			    currencyId
		    ));
		    predicates.add(cb.equal(
			    root.get("logType"),
			    ELogType.TRANSACTION
		    ));
		    predicates.add(cb.isNotNull(root.get("playerUuid")));
		    predicates.add(cb.greaterThanOrEqualTo(
			    root.get("timestamp"),
			    startTime
		    ));
		    
		    if (endTime != null) {
			    predicates.add(cb.lessThan(
				    root.get("timestamp"),
				    endTime
			    ));
		    }
		    
		    query.select(root.get("playerUuid"));
		    query.where(predicates.toArray(new Predicate[0]));
		    query.groupBy(root.get("playerUuid"));
		    query.orderBy(cb.desc(cb.count(root)));
		    
		    return em.createQuery(query)
		             .setMaxResults(limit)
		             .getResultList();
	    }
    }
    
    /**
     * Deletes old log entries to maintain database performance.
     *
     * @param cutoffDate the date before which logs should be deleted
     * @param keepErrorLogs whether to preserve error logs regardless of age
     * @return the number of deleted entries
     */
    public int deleteOldLogs(@NotNull LocalDateTime cutoffDate, boolean keepErrorLogs) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaDelete<CurrencyLog> delete = cb.createCriteriaDelete(CurrencyLog.class);
            Root<CurrencyLog> root = delete.from(CurrencyLog.class);
            
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.lessThan(root.get("timestamp"), cutoffDate));
            
            if (keepErrorLogs) {
                predicates.add(cb.notEqual(root.get("logLevel"), ELogLevel.ERROR));
                predicates.add(cb.notEqual(root.get("logLevel"), ELogLevel.CRITICAL));
                predicates.add(cb.equal(root.get("success"), true));
            }
            
            delete.where(predicates.toArray(new Predicate[0]));
            
            int deletedCount = em.createQuery(delete).executeUpdate();
            em.getTransaction().commit();
            
            return deletedCount;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    /**
     * Finds recent error logs for monitoring and debugging.
     *
     * @param hours the number of hours to look back
     * @param limit the maximum number of entries to return
     * @return list of recent error logs
     */
    @NotNull
    public List<CurrencyLog> findRecentErrors(int hours, int limit) {
	    
	    try (EntityManager em = entityManagerFactory.createEntityManager()) {
		    LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
		    
		    CriteriaBuilder            cb    = em.getCriteriaBuilder();
		    CriteriaQuery<CurrencyLog> query = cb.createQuery(CurrencyLog.class);
		    Root<CurrencyLog>          root  = query.from(CurrencyLog.class);
		    
		    List<Predicate> predicates = new ArrayList<>();
		    predicates.add(cb.greaterThanOrEqualTo(
			    root.get("timestamp"),
			    startTime
		    ));
		    
		    // Include error levels and failed operations
		    Predicate errorLevels = cb.or(
			    cb.equal(
				    root.get("logLevel"),
				    ELogLevel.ERROR
			    ),
			    cb.equal(
				    root.get("logLevel"),
				    ELogLevel.CRITICAL
			    )
		    );
		    Predicate failedOperations = cb.equal(
			    root.get("success"),
			    false
		    );
		    
		    predicates.add(cb.or(
			    errorLevels,
			    failedOperations
		    ));
		    
		    query.where(predicates.toArray(new Predicate[0]));
		    query.orderBy(cb.desc(root.get("timestamp")));
		    
		    return em.createQuery(query)
		             .setMaxResults(limit)
		             .getResultList();
	    }
    }
    
    /**
     * Asynchronously finds recent error logs.
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> findRecentErrorsAsync(int hours, int limit) {
        return CompletableFuture.supplyAsync(() ->
                                                 findRecentErrors(hours, limit), asyncExecutorService);
    }
    
    /**
     * Finds logs by multiple criteria using a flexible predicate-based approach.
     *
     * @param logType the log type (null for any)
     * @param logLevel the log level (null for any)
     * @param playerUuid the player UUID (null for any)
     * @param currencyId the currency ID (null for any)
     * @param startTime the start time (null for no start limit)
     * @param endTime the end time (null for no end limit)
     * @param limit the maximum number of results
     * @return list of matching logs
     */
    @NotNull
    public List<CurrencyLog> findByCriteria(
        @Nullable ELogType logType,
        @Nullable ELogLevel logLevel,
        @Nullable UUID playerUuid,
        @Nullable Long currencyId,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int limit
    ) {
	    
	    try (EntityManager em = entityManagerFactory.createEntityManager()) {
		    CriteriaBuilder            cb    = em.getCriteriaBuilder();
		    CriteriaQuery<CurrencyLog> query = cb.createQuery(CurrencyLog.class);
		    Root<CurrencyLog>          root  = query.from(CurrencyLog.class);
		    
		    List<Predicate> predicates = new ArrayList<>();
		    
		    if (logType != null) {
			    predicates.add(cb.equal(
				    root.get("logType"),
				    logType
			    ));
		    }
		    if (logLevel != null) {
			    predicates.add(cb.equal(
				    root.get("logLevel"),
				    logLevel
			    ));
		    }
		    if (playerUuid != null) {
			    predicates.add(cb.equal(
				    root.get("playerUuid"),
				    playerUuid
			    ));
		    }
		    if (currencyId != null) {
			    predicates.add(cb.equal(
				    root.get("currency").get("id"),
				    currencyId
			    ));
		    }
		    if (startTime != null) {
			    predicates.add(cb.greaterThanOrEqualTo(
				    root.get("timestamp"),
				    startTime
			    ));
		    }
		    if (endTime != null) {
			    predicates.add(cb.lessThan(
				    root.get("timestamp"),
				    endTime
			    ));
		    }
		    
		    if (! predicates.isEmpty()) {
			    query.where(predicates.toArray(new Predicate[0]));
		    }
		    
		    query.orderBy(cb.desc(root.get("timestamp")));
		    
		    return em.createQuery(query)
		             .setMaxResults(limit)
		             .getResultList();
	    }
    }
    
    /**
     * Asynchronously finds logs by multiple criteria.
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> findByCriteriaAsync(
        @Nullable ELogType logType,
        @Nullable ELogLevel logLevel,
        @Nullable UUID playerUuid,
        @Nullable Long currencyId,
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        int limit
    ) {
        return CompletableFuture.supplyAsync(() -> findByCriteria(logType, logLevel, playerUuid, currencyId, startTime, endTime, limit), asyncExecutorService);
    }
}
