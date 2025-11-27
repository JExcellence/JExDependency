package com.raindropcentral.rdq.rank.integration;

import com.raindropcentral.rdq.rank.Rank;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LuckPermsRankAdapter {

    private static final Logger LOGGER = Logger.getLogger(LuckPermsRankAdapter.class.getName());
    private static final int DEFAULT_PREFIX_PRIORITY = 100;

    private final LuckPerms luckPerms;

    private LuckPermsRankAdapter(@NotNull LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    @NotNull
    public static Optional<LuckPermsRankAdapter> create() {
        try {
            var luckPerms = LuckPermsProvider.get();
            LOGGER.info("LuckPerms integration enabled");
            return Optional.of(new LuckPermsRankAdapter(luckPerms));
        } catch (IllegalStateException e) {
            LOGGER.info("LuckPerms not available, rank permission integration disabled");
            return Optional.empty();
        }
    }

    @NotNull
    public CompletableFuture<Boolean> assignRankGroup(@NotNull UUID playerId, @NotNull Rank rank) {
        if (!rank.hasLuckPermsGroup()) {
            return CompletableFuture.completedFuture(true);
        }

        return loadUser(playerId)
            .thenCompose(userOpt -> {
                if (userOpt.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }

                var user = userOpt.get();
                var node = InheritanceNode.builder(rank.luckPermsGroup()).build();
                user.data().add(node);

                return luckPerms.getUserManager().saveUser(user)
                    .thenApply(v -> {
                        LOGGER.fine("Assigned group " + rank.luckPermsGroup() + " to player " + playerId);
                        return true;
                    });
            })
            .exceptionally(e -> {
                LOGGER.log(Level.WARNING, "Failed to assign rank group", e);
                return false;
            });
    }


    @NotNull
    public CompletableFuture<Boolean> removeRankGroup(@NotNull UUID playerId, @NotNull Rank rank) {
        if (!rank.hasLuckPermsGroup()) {
            return CompletableFuture.completedFuture(true);
        }

        return loadUser(playerId)
            .thenCompose(userOpt -> {
                if (userOpt.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }

                var user = userOpt.get();
                var node = InheritanceNode.builder(rank.luckPermsGroup()).build();
                user.data().remove(node);

                return luckPerms.getUserManager().saveUser(user)
                    .thenApply(v -> {
                        LOGGER.fine("Removed group " + rank.luckPermsGroup() + " from player " + playerId);
                        return true;
                    });
            })
            .exceptionally(e -> {
                LOGGER.log(Level.WARNING, "Failed to remove rank group", e);
                return false;
            });
    }

    @NotNull
    public CompletableFuture<Boolean> applyPrefix(@NotNull UUID playerId, @NotNull String prefix, int priority) {
        return loadUser(playerId)
            .thenCompose(userOpt -> {
                if (userOpt.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }

                var user = userOpt.get();
                clearPrefixes(user, priority);

                var node = PrefixNode.builder(prefix, priority).build();
                user.data().add(node);

                return luckPerms.getUserManager().saveUser(user)
                    .thenApply(v -> {
                        LOGGER.fine("Applied prefix to player " + playerId);
                        return true;
                    });
            })
            .exceptionally(e -> {
                LOGGER.log(Level.WARNING, "Failed to apply prefix", e);
                return false;
            });
    }

    @NotNull
    public CompletableFuture<Boolean> applySuffix(@NotNull UUID playerId, @NotNull String suffix, int priority) {
        return loadUser(playerId)
            .thenCompose(userOpt -> {
                if (userOpt.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }

                var user = userOpt.get();
                clearSuffixes(user, priority);

                var node = SuffixNode.builder(suffix, priority).build();
                user.data().add(node);

                return luckPerms.getUserManager().saveUser(user)
                    .thenApply(v -> {
                        LOGGER.fine("Applied suffix to player " + playerId);
                        return true;
                    });
            })
            .exceptionally(e -> {
                LOGGER.log(Level.WARNING, "Failed to apply suffix", e);
                return false;
            });
    }


    @NotNull
    public CompletableFuture<Boolean> onRankUnlock(@NotNull UUID playerId, @NotNull Rank rank, @Nullable String prefix, @Nullable String suffix) {
        return assignRankGroup(playerId, rank)
            .thenCompose(groupAssigned -> {
                if (prefix != null && !prefix.isBlank()) {
                    return applyPrefix(playerId, prefix, DEFAULT_PREFIX_PRIORITY);
                }
                return CompletableFuture.completedFuture(true);
            })
            .thenCompose(prefixApplied -> {
                if (suffix != null && !suffix.isBlank()) {
                    return applySuffix(playerId, suffix, DEFAULT_PREFIX_PRIORITY);
                }
                return CompletableFuture.completedFuture(true);
            });
    }

    private void clearPrefixes(@NotNull User user, int priority) {
        user.data().toCollection().stream()
            .filter(node -> node instanceof PrefixNode pn && pn.getPriority() == priority)
            .forEach(node -> user.data().remove(node));
    }

    private void clearSuffixes(@NotNull User user, int priority) {
        user.data().toCollection().stream()
            .filter(node -> node instanceof SuffixNode sn && sn.getPriority() == priority)
            .forEach(node -> user.data().remove(node));
    }

    @NotNull
    private CompletableFuture<Optional<User>> loadUser(@NotNull UUID playerId) {
        return luckPerms.getUserManager().loadUser(playerId)
            .thenApply(Optional::ofNullable);
    }

    public boolean isAvailable() {
        return true;
    }
}
