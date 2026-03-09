package com.raindropcentral.rdr.database.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.raindropcentral.rdr.database.entity.RTradeDelivery;
import com.raindropcentral.rdr.database.entity.TradeDeliveryStatus;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Repository for persisted trade-delivery payouts and claim transitions.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class RRTradeDelivery extends BaseRepository<RTradeDelivery, Long> {

    /**
     * Delivery claim status.
     */
    public enum ClaimStatus {
        /**
         * Delivery was claimed and payload was returned.
         */
        CLAIMED,
        /**
         * Delivery row was not found.
         */
        MISSING,
        /**
         * Delivery belongs to a different recipient.
         */
        FORBIDDEN,
        /**
         * Delivery was already claimed.
         */
        ALREADY_CLAIMED
    }

    /**
     * Result payload of a delivery-claim attempt.
     *
     * @param status claim status
     * @param deliveryId claimed delivery row identifier, or {@code null} when not claimed
     * @param itemPayload claimed item payload map
     * @param currencyPayload claimed currency payload map
     */
    public record ClaimResult(
        @NotNull ClaimStatus status,
        @Nullable Long deliveryId,
        @NotNull Map<Integer, ItemStack> itemPayload,
        @NotNull Map<String, Double> currencyPayload
    ) {
    }

    /**
     * Creates a repository for {@link RTradeDelivery} entities.
     *
     * @param executorService executor used for async repository operations
     * @param entityManagerFactory entity manager factory used for persistence operations
     * @throws NullPointerException if any argument is {@code null}
     */
    public RRTradeDelivery(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RTradeDelivery.class);
    }

    /**
     * Creates a pending delivery row.
     *
     * @param tradeUuid trade UUID this delivery belongs to
     * @param recipientUuid recipient UUID
     * @param itemPayload item payload map
     * @param currencyPayload currency payload map
     * @return async created entity
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<RTradeDelivery> createPendingAsync(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID recipientUuid,
        final @NotNull Map<Integer, ItemStack> itemPayload,
        final @NotNull Map<String, Double> currencyPayload
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.create(new RTradeDelivery(tradeUuid, recipientUuid, itemPayload, currencyPayload)),
            getExecutorService()
        );
    }

    /**
     * Finds pending deliveries for one recipient.
     *
     * @param recipientUuid recipient UUID
     * @return immutable list of pending deliveries ordered by creation timestamp
     * @throws NullPointerException if {@code recipientUuid} is {@code null}
     */
    public @NotNull List<RTradeDelivery> findPendingByRecipient(final @NotNull UUID recipientUuid) {
        final UUID validatedRecipientUuid = Objects.requireNonNull(recipientUuid, "recipientUuid cannot be null");
        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select delivery from RTradeDelivery delivery "
                    + "where delivery.recipientUuid = :recipientUuid and delivery.status = :status "
                    + "order by delivery.createdAt asc",
                RTradeDelivery.class
            )
            .setParameter("recipientUuid", validatedRecipientUuid)
            .setParameter("status", TradeDeliveryStatus.PENDING)
            .getResultList()));
    }

    /**
     * Finds pending deliveries for multiple recipients.
     *
     * @param recipientUuids recipient UUID list
     * @return immutable list of pending deliveries ordered by creation timestamp
     * @throws NullPointerException if {@code recipientUuids} is {@code null}
     */
    public @NotNull List<RTradeDelivery> findPendingByRecipients(final @NotNull List<UUID> recipientUuids) {
        final List<UUID> validatedRecipients = List.copyOf(Objects.requireNonNull(recipientUuids, "recipientUuids cannot be null"));
        if (validatedRecipients.isEmpty()) {
            return List.of();
        }

        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select delivery from RTradeDelivery delivery "
                    + "where delivery.recipientUuid in :recipientUuids and delivery.status = :status "
                    + "order by delivery.createdAt asc",
                RTradeDelivery.class
            )
            .setParameter("recipientUuids", validatedRecipients)
            .setParameter("status", TradeDeliveryStatus.PENDING)
            .getResultList()));
    }

    /**
     * Claims a pending delivery for the supplied recipient UUID.
     *
     * @param deliveryId target delivery row identifier
     * @param recipientUuid claiming recipient UUID
     * @return async claim result with payload when successful
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<ClaimResult> claimDeliveryAsync(
        final @NotNull Long deliveryId,
        final @NotNull UUID recipientUuid
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.claimDelivery(deliveryId, recipientUuid),
            getExecutorService()
        );
    }

    private @NotNull ClaimResult claimDelivery(
        final @NotNull Long deliveryId,
        final @NotNull UUID recipientUuid
    ) {
        final Long validatedDeliveryId = Objects.requireNonNull(deliveryId, "deliveryId cannot be null");
        final UUID validatedRecipientUuid = Objects.requireNonNull(recipientUuid, "recipientUuid cannot be null");
        return this.executeInTransaction(entityManager -> {
            final RTradeDelivery delivery = entityManager.find(
                RTradeDelivery.class,
                validatedDeliveryId,
                LockModeType.PESSIMISTIC_WRITE
            );
            if (delivery == null) {
                return new ClaimResult(ClaimStatus.MISSING, null, Map.of(), Map.of());
            }
            if (!validatedRecipientUuid.equals(delivery.getRecipientUuid())) {
                return new ClaimResult(ClaimStatus.FORBIDDEN, null, Map.of(), Map.of());
            }
            if (delivery.getStatus() != TradeDeliveryStatus.PENDING) {
                return new ClaimResult(ClaimStatus.ALREADY_CLAIMED, null, Map.of(), Map.of());
            }

            final Map<Integer, ItemStack> itemPayload = delivery.getItemPayload();
            final Map<String, Double> currencyPayload = delivery.getCurrencyPayload();
            delivery.markClaimed(LocalDateTime.now());
            return new ClaimResult(ClaimStatus.CLAIMED, delivery.getId(), itemPayload, currencyPayload);
        });
    }
}
