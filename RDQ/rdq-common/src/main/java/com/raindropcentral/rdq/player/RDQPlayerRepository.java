package com.raindropcentral.rdq.player;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RDQPlayerRepository implements PlayerRepository {

    private static final Logger LOGGER = Logger.getLogger(RDQPlayerRepository.class.getName());

    private final ExecutorService executor;
    private final EntityManagerFactory emf;

    public RDQPlayerRepository(@NotNull ExecutorService executor, @NotNull EntityManagerFactory emf) {
        this.executor = executor;
        this.emf = emf;
    }

    @NotNull
    public CompletableFuture<Optional<RDQPlayer>> findById(@NotNull UUID id) {
        return supplyAsync(em -> {
            var player = em.find(RDQPlayer.class, id);
            return Optional.ofNullable(player);
        });
    }

    @NotNull
    public CompletableFuture<RDQPlayer> save(@NotNull RDQPlayer player) {
        return supplyAsync(em -> {
            if (em.find(RDQPlayer.class, player.id()) == null) {
                em.persist(player);
            } else {
                em.merge(player);
            }
            return player;
        });
    }

    @NotNull
    public CompletableFuture<Void> delete(@NotNull UUID id) {
        return runAsync(em -> {
            var player = em.find(RDQPlayer.class, id);
            if (player != null) {
                em.remove(player);
            }
        });
    }

    @NotNull
    public CompletableFuture<List<RDQPlayer>> findAll() {
        return supplyAsync(em -> {
            var query = em.createQuery("SELECT p FROM RDQPlayer p", RDQPlayer.class);
            return query.getResultList();
        });
    }

    @NotNull
    public CompletableFuture<Optional<RDQPlayer>> findByName(@NotNull String name) {
        return supplyAsync(em -> {
            var query = em.createQuery("SELECT p FROM RDQPlayer p WHERE p.name = :name", RDQPlayer.class);
            query.setParameter("name", name);
            var results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
        });
    }

    @NotNull
    public CompletableFuture<Long> count() {
        return supplyAsync(em -> {
            var query = em.createQuery("SELECT COUNT(p) FROM RDQPlayer p", Long.class);
            return query.getSingleResult();
        });
    }

    @NotNull
    public CompletableFuture<Boolean> exists(@NotNull UUID id) {
        return supplyAsync(em -> {
            var query = em.createQuery("SELECT COUNT(p) FROM RDQPlayer p WHERE p.id = :id", Long.class);
            query.setParameter("id", id);
            return query.getSingleResult() > 0;
        });
    }

    private <T> CompletableFuture<T> supplyAsync(@NotNull Function<EntityManager, T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            var em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                var result = operation.apply(em);
                em.getTransaction().commit();
                return result;
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                LOGGER.log(Level.SEVERE, "Database operation failed", e);
                throw new RuntimeException("Database operation failed", e);
            } finally {
                em.close();
            }
        }, executor);
    }

    private CompletableFuture<Void> runAsync(@NotNull java.util.function.Consumer<EntityManager> operation) {
        return CompletableFuture.runAsync(() -> {
            var em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                operation.accept(em);
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                LOGGER.log(Level.SEVERE, "Database operation failed", e);
                throw new RuntimeException("Database operation failed", e);
            } finally {
                em.close();
            }
        }, executor);
    }
}
