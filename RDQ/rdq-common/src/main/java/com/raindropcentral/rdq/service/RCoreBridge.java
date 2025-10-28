package com.raindropcentral.rdq.service;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridge to the external RCoreAdapter without requiring the upstream interface at compile time.
 *
 * <p>RCore registers its adapter instance ({@code com.raindropcentral.core.api.RCoreAdapter}) under the
 * service interface FQCN: {@code com.raindropcentral.core.service.RCoreService}. This class resolves the
 * service lazily, proxies asynchronous calls by reflection, and introduces optional timeouts paired with
 * structured logging so RDQ can interact with the upstream API safely.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * // Immediate resolve (pass the provider plugin name, typically "RCore")
 * RCoreBridge bridge = RCoreBridge.fromBukkit(this, "RCore");
 * if (bridge != null) {
 *     bridge.findPlayerAsync(uuid).thenAccept(opt -> ...);
 * }
 *
 * // Or with retries via ServiceRegistry
 * ServiceRegistry reg = new ServiceRegistry();
 * RCoreBridge.awaitService(reg, "RCore", true, 20, 500)
 *     .thenAccept(opt -> opt.ifPresent(b -> this.rcore = b.withDefaultTimeout(Duration.ofSeconds(5))));
 * </pre>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@SuppressWarnings({"unused", "unchecked"})
public final class RCoreBridge {

    private static final Logger LOGGER = CentralLogger.getLogger(RCoreBridge.class);

    // Upstream interface FQCN registered by the RCore plugin
    private static final String SERVICE_FQCN = "com.raindropcentral.core.service.RCoreService";

    // Resolved upstream service instance (actual class: com.raindropcentral.core.api.RCoreAdapter)
    private final Object delegate;

    // Optional default timeout for operations; set to zero/negative to disable
    private volatile Duration defaultTimeout = Duration.ofSeconds(10);

    private RCoreBridge(@NotNull final Object delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        LOGGER.info("RCoreBridge initialized (Upstream API " + safeApiVersion() + ")");
    }

    // --------------------------------------------------------------------------------------------
    // Resolution
    // --------------------------------------------------------------------------------------------

    /**
     * Resolve upstream service immediately from Bukkit ServicesManager.
     *
     * @param plugin               Current plugin (RDQ)
     * @param providerPluginName   Optional plugin name that owns the service classes (e.g., "RCore")
     * @return bridge or null if not available
     */
    public static @Nullable RCoreBridge fromBukkit(@NotNull JavaPlugin plugin, @Nullable String providerPluginName) {
        Objects.requireNonNull(plugin, "plugin");
        final Class<?> svcClass = loadClass(SERVICE_FQCN, providerPluginName);
        if (svcClass == null) {
            LOGGER.warning("Cannot load service class " + SERVICE_FQCN + " (providerPlugin=" + providerPluginName + ")");
            return null;
        }
        final RegisteredServiceProvider<?> rsp = plugin.getServer().getServicesManager().getRegistration(svcClass);
        if (rsp == null) {
            LOGGER.warning("Service " + SERVICE_FQCN + " not registered in Bukkit ServicesManager");
            return null;
        }
        return new RCoreBridge(rsp.getProvider());
    }

    /**
     * Await upstream service via ServiceRegistry with retries.
     *
     * @param registry           service registry used for discovery
     * @param providerPluginName optional plugin name to load the class from (e.g., "RCore")
     * @param required           {@code true} to log a warning if the service is not discovered, otherwise optional
     * @param maxAttempts        number of attempts before giving up
     * @param retryDelayMs       delay between attempts in milliseconds
     * @return future resolving to the discovered bridge if the service becomes available
     */
    public static @NotNull CompletableFuture<Optional<RCoreBridge>> awaitService(
            @NotNull ServiceRegistry registry,
            @Nullable String providerPluginName,
            boolean required,
            int maxAttempts,
            long retryDelayMs
    ) {
        Objects.requireNonNull(registry, "registry");
        final ServiceRegistry.ServiceRegistrationBuilder<?> b =
                registry.register(SERVICE_FQCN, providerPluginName);
        if (required) b.required(); else b.optional();
        b.maxAttempts(maxAttempts)
                .retryDelay(retryDelayMs)
                .onSuccess(svc -> LOGGER.info("RCore service discovered via ServiceRegistry"))
                .onFailure(() -> LOGGER.warning("RCore service not discovered within the retry window"));
        return b.load().thenApply(opt -> opt.map(RCoreBridge::new));
    }

    /**
     * Check availability in ServicesManager (best-effort).
     *
     * @param providerPluginName optional plugin name to load the class from (e.g., "RCore")
     * @return {@code true} if the upstream service is registered and the interface could be resolved
     */
    public static boolean isAvailable(@Nullable String providerPluginName) {
        final Class<?> svcClass = loadClass(SERVICE_FQCN, providerPluginName);
        if (svcClass == null) return false;
        return Bukkit.getServer().getServicesManager().isProvidedFor(svcClass);
    }

    // --------------------------------------------------------------------------------------------
    // Configuration
    // --------------------------------------------------------------------------------------------

    /**
     * Configure a default timeout that wraps every asynchronous invocation proxied by this bridge.
     *
     * @param timeout duration applied to future completions; non-positive values disable the timeout
     * @return this bridge instance for chaining
     */
    public @NotNull RCoreBridge withDefaultTimeout(@NotNull Duration timeout) {
        this.defaultTimeout = Objects.requireNonNull(timeout, "timeout");
        return this;
    }

    /**
     * Retrieve the currently configured default timeout.
     *
     * @return configured timeout duration
     */
    public @NotNull Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    // --------------------------------------------------------------------------------------------
    // Player lookups
    // --------------------------------------------------------------------------------------------

    /**
     * Resolve a player using the supplied unique identifier.
     *
     * @param uniqueId player UUID being searched for
     * @return future yielding an optional containing the {@link RPlayer} if present
     */
    public @NotNull CompletableFuture<Optional<RPlayer>> findPlayerAsync(@NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return this.<Optional<RPlayer>>invokeCf("findPlayerAsync", new Class[]{UUID.class}, uniqueId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findPlayerAsync(UUID) failed for " + uniqueId, ex);
                    return Optional.<RPlayer>empty();
                });
    }

    /**
     * Resolve a player using a Bukkit {@link OfflinePlayer} reference.
     *
     * @param offlinePlayer offline player handle used for lookup
     * @return future yielding an optional containing the {@link RPlayer} if present
     */
    public @NotNull CompletableFuture<Optional<RPlayer>> findPlayerAsync(@NotNull OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        return this.<Optional<RPlayer>>invokeCf("findPlayerAsync", new Class[]{OfflinePlayer.class}, offlinePlayer)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findPlayerAsync(OfflinePlayer) failed for " + offlinePlayer.getUniqueId(), ex);
                    return Optional.<RPlayer>empty();
                });
    }

    /**
     * Resolve a player by their last known username.
     *
     * @param playerName case-insensitive player name used for lookup
     * @return future yielding an optional containing the {@link RPlayer} if found
     */
    public @NotNull CompletableFuture<Optional<RPlayer>> findPlayerByNameAsync(@NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        return this.<Optional<RPlayer>>invokeCf("findPlayerByNameAsync", new Class[]{String.class}, playerName)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findPlayerByNameAsync failed for name=" + playerName, ex);
                    return Optional.<RPlayer>empty();
                });
    }

    /**
     * Determine if an upstream {@link RPlayer} entry exists for the supplied UUID.
     *
     * @param uniqueId player UUID to verify
     * @return future resolving to {@code true} if the player exists
     */
    public @NotNull CompletableFuture<Boolean> playerExistsAsync(@NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return this.<Boolean>invokeCf("playerExistsAsync", new Class[]{UUID.class}, uniqueId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "playerExistsAsync(UUID) failed for " + uniqueId, ex);
                    return false;
                });
    }

    /**
     * Determine if an upstream {@link RPlayer} entry exists for the supplied offline player.
     *
     * @param offlinePlayer offline player handle to verify
     * @return future resolving to {@code true} if the player exists
     */
    public @NotNull CompletableFuture<Boolean> playerExistsAsync(@NotNull OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        return this.<Boolean>invokeCf("playerExistsAsync", new Class[]{OfflinePlayer.class}, offlinePlayer)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "playerExistsAsync(OfflinePlayer) failed for " + offlinePlayer.getUniqueId(), ex);
                    return false;
                });
    }

    // --------------------------------------------------------------------------------------------
    // Create / Update
    // --------------------------------------------------------------------------------------------

    /**
     * Create a new upstream {@link RPlayer} record.
     *
     * @param uniqueId   player UUID to create
     * @param playerName player name stored alongside the record
     * @return future yielding the created {@link RPlayer} if successful
     */
    public @NotNull CompletableFuture<Optional<RPlayer>> createPlayerAsync(@NotNull UUID uniqueId, @NotNull String playerName) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(playerName, "playerName");
        return this.<Optional<RPlayer>>invokeCf("createPlayerAsync", new Class[]{UUID.class, String.class}, uniqueId, playerName)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "createPlayerAsync failed for " + uniqueId + " name=" + playerName, ex);
                    return Optional.<RPlayer>empty();
                });
    }

    /**
     * Persist changes to an existing upstream {@link RPlayer} record.
     *
     * @param player player entity containing updated state
     * @return future yielding the updated {@link RPlayer} if the operation succeeds
     */
    public @NotNull CompletableFuture<Optional<RPlayer>> updatePlayerAsync(@NotNull RPlayer player) {
        Objects.requireNonNull(player, "player");
        return this.<Optional<RPlayer>>invokeCf("updatePlayerAsync", new Class[]{RPlayer.class}, player)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "updatePlayerAsync failed for " + player.getUniqueId(), ex);
                    return Optional.<RPlayer>empty();
                });
    }

    // --------------------------------------------------------------------------------------------
    // Statistics containers
    // --------------------------------------------------------------------------------------------

    /**
     * Fetch statistics for the player identified by the supplied UUID.
     *
     * @param uniqueId player UUID whose statistics will be retrieved
     * @return future yielding an optional containing the {@link RPlayerStatistic} when present
     */
    public @NotNull CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(@NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return this.<Optional<RPlayerStatistic>>invokeCf("findPlayerStatisticsAsync", new Class[]{UUID.class}, uniqueId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findPlayerStatisticsAsync(UUID) failed for " + uniqueId, ex);
                    return Optional.<RPlayerStatistic>empty();
                });
    }

    /**
     * Fetch statistics for the supplied offline player.
     *
     * @param offlinePlayer offline player handle whose statistics will be retrieved
     * @return future yielding an optional containing the {@link RPlayerStatistic} when present
     */
    public @NotNull CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(@NotNull OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        return this.<Optional<RPlayerStatistic>>invokeCf("findPlayerStatisticsAsync", new Class[]{OfflinePlayer.class}, offlinePlayer)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findPlayerStatisticsAsync(OfflinePlayer) failed for " + offlinePlayer.getUniqueId(), ex);
                    return Optional.<RPlayerStatistic>empty();
                });
    }

    // --------------------------------------------------------------------------------------------
    // Statistic values / presence
    // --------------------------------------------------------------------------------------------

    /**
     * Retrieve the stored value for a specific statistic belonging to the supplied player UUID.
     *
     * @param uniqueId   player UUID that owns the statistic
     * @param identifier statistic identifier defined by the provider
     * @param plugin     plugin namespace that manages the statistic
     * @return future yielding an optional containing the statistic value when available
     */
    public @NotNull CompletableFuture<Optional<Object>> findStatisticValueAsync(
            @NotNull UUID uniqueId, @NotNull String identifier, @NotNull String plugin) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(plugin, "plugin");
        return this.<Optional<Object>>invokeCf("findStatisticValueAsync", new Class[]{UUID.class, String.class, String.class},
                        uniqueId, identifier, plugin)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findStatisticValueAsync failed for " + uniqueId + " id=" + identifier + " plugin=" + plugin, ex);
                    return Optional.<Object>empty();
                });
    }

    /**
     * Retrieve the stored value for a specific statistic belonging to the supplied offline player.
     *
     * @param offlinePlayer offline player handle that owns the statistic
     * @param identifier    statistic identifier defined by the provider
     * @param plugin        plugin namespace that manages the statistic
     * @return future yielding an optional containing the statistic value when available
     */
    public @NotNull CompletableFuture<Optional<Object>> findStatisticValueAsync(
            @NotNull OfflinePlayer offlinePlayer, @NotNull String identifier, @NotNull String plugin) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(plugin, "plugin");
        return this.<Optional<Object>>invokeCf("findStatisticValueAsync", new Class[]{OfflinePlayer.class, String.class, String.class},
                        offlinePlayer, identifier, plugin)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findStatisticValueAsync failed for " + offlinePlayer.getUniqueId()
                            + " id=" + identifier + " plugin=" + plugin, ex);
                    return Optional.<Object>empty();
                });
    }

    /**
     * Determine whether a player UUID has a statistic with the supplied identifier and plugin namespace.
     *
     * @param uniqueId   player UUID to inspect
     * @param identifier statistic identifier defined by the provider
     * @param plugin     plugin namespace that manages the statistic
     * @return future resolving to {@code true} when the statistic exists
     */
    public @NotNull CompletableFuture<Boolean> hasStatisticAsync(
            @NotNull UUID uniqueId, @NotNull String identifier, @NotNull String plugin) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(plugin, "plugin");
        return this.<Boolean>invokeCf("hasStatisticAsync", new Class[]{UUID.class, String.class, String.class},
                        uniqueId, identifier, plugin)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "hasStatisticAsync failed for " + uniqueId + " id=" + identifier + " plugin=" + plugin, ex);
                    return false;
                });
    }

    /**
     * Determine whether an offline player has a statistic with the supplied identifier and plugin namespace.
     *
     * @param offlinePlayer offline player handle to inspect
     * @param identifier    statistic identifier defined by the provider
     * @param plugin        plugin namespace that manages the statistic
     * @return future resolving to {@code true} when the statistic exists
     */
    public @NotNull CompletableFuture<Boolean> hasStatisticAsync(
            @NotNull OfflinePlayer offlinePlayer, @NotNull String identifier, @NotNull String plugin) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(plugin, "plugin");
        return this.<Boolean>invokeCf("hasStatisticAsync", new Class[]{OfflinePlayer.class, String.class, String.class},
                        offlinePlayer, identifier, plugin)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "hasStatisticAsync failed for " + offlinePlayer.getUniqueId()
                            + " id=" + identifier + " plugin=" + plugin, ex);
                    return false;
                });
    }

    // --------------------------------------------------------------------------------------------
    // Mutations
    // --------------------------------------------------------------------------------------------

    /**
     * Remove the targeted statistic from the player identified by the supplied UUID.
     *
     * @param uniqueId   player UUID whose statistic should be removed
     * @param identifier statistic identifier defined by the provider
     * @param plugin     plugin namespace that manages the statistic
     * @return future resolving to {@code true} when the statistic is removed
     */
    public @NotNull CompletableFuture<Boolean> removeStatisticAsync(
            @NotNull UUID uniqueId, @NotNull String identifier, @NotNull String plugin) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(plugin, "plugin");
        return this.<Boolean>invokeCf("removeStatisticAsync", new Class[]{UUID.class, String.class, String.class},
                        uniqueId, identifier, plugin)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "removeStatisticAsync failed for " + uniqueId
                            + " id=" + identifier + " plugin=" + plugin, ex);
                    return false;
                });
    }

    /**
     * Remove the targeted statistic from the supplied offline player.
     *
     * @param offlinePlayer offline player handle whose statistic should be removed
     * @param identifier    statistic identifier defined by the provider
     * @param plugin        plugin namespace that manages the statistic
     * @return future resolving to {@code true} when the statistic is removed
     */
    public @NotNull CompletableFuture<Boolean> removeStatisticAsync(
            @NotNull OfflinePlayer offlinePlayer, @NotNull String identifier, @NotNull String plugin) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(plugin, "plugin");
        return this.<Boolean>invokeCf("removeStatisticAsync", new Class[]{OfflinePlayer.class, String.class, String.class},
                        offlinePlayer, identifier, plugin)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "removeStatisticAsync failed for " + offlinePlayer.getUniqueId()
                            + " id=" + identifier + " plugin=" + plugin, ex);
                    return false;
                });
    }

    /**
     * Insert or update a statistic for the supplied player UUID.
     *
     * @param uniqueId  player UUID that owns the statistic
     * @param statistic statistic payload to persist
     * @return future resolving to {@code true} when the statistic is stored
     */
    public @NotNull CompletableFuture<Boolean> addOrReplaceStatisticAsync(
            @NotNull UUID uniqueId, @NotNull RAbstractStatistic statistic) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(statistic, "statistic");
        return this.<Boolean>invokeCf("addOrReplaceStatisticAsync", new Class[]{UUID.class, RAbstractStatistic.class},
                        uniqueId, statistic)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "addOrReplaceStatisticAsync failed for " + uniqueId
                            + " id=" + statistic.getIdentifier() + " plugin=" + statistic.getPlugin(), ex);
                    return false;
                });
    }

    /**
     * Insert or update a statistic for the supplied offline player.
     *
     * @param offlinePlayer offline player handle that owns the statistic
     * @param statistic     statistic payload to persist
     * @return future resolving to {@code true} when the statistic is stored
     */
    public @NotNull CompletableFuture<Boolean> addOrReplaceStatisticAsync(
            @NotNull OfflinePlayer offlinePlayer, @NotNull RAbstractStatistic statistic) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        Objects.requireNonNull(statistic, "statistic");
        return this.<Boolean>invokeCf("addOrReplaceStatisticAsync", new Class[]{OfflinePlayer.class, RAbstractStatistic.class},
                        offlinePlayer, statistic)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "addOrReplaceStatisticAsync failed for " + offlinePlayer.getUniqueId()
                            + " id=" + statistic.getIdentifier() + " plugin=" + statistic.getPlugin(), ex);
                    return false;
                });
    }

    // --------------------------------------------------------------------------------------------
    // Counts
    // --------------------------------------------------------------------------------------------

    /**
     * Count how many statistics exist for the supplied player UUID scoped to the provided plugin namespace.
     *
     * @param uniqueId player UUID whose statistics should be counted
     * @param plugin   plugin namespace that owns the statistics
     * @return future resolving to the number of stored statistics
     */
    public @NotNull CompletableFuture<Long> getStatisticCountForPluginAsync(@NotNull UUID uniqueId, @NotNull String plugin) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(plugin, "plugin");
        return this.<Long>invokeCf("getStatisticCountForPluginAsync", new Class[]{UUID.class, String.class}, uniqueId, plugin)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "getStatisticCountForPluginAsync failed for " + uniqueId + " plugin=" + plugin, ex);
                    return 0L;
                });
    }

    /**
     * Count how many statistics exist for the supplied offline player scoped to the provided plugin namespace.
     *
     * @param offlinePlayer offline player handle whose statistics should be counted
     * @param plugin        plugin namespace that owns the statistics
     * @return future resolving to the number of stored statistics
     */
    public @NotNull CompletableFuture<Long> getStatisticCountForPluginAsync(@NotNull OfflinePlayer offlinePlayer, @NotNull String plugin) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        Objects.requireNonNull(plugin, "plugin");
        return this.<Long>invokeCf("getStatisticCountForPluginAsync", new Class[]{OfflinePlayer.class, String.class},
                        offlinePlayer, plugin)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "getStatisticCountForPluginAsync failed for " + offlinePlayer.getUniqueId()
                            + " plugin=" + plugin, ex);
                    return 0L;
                });
    }

    // --------------------------------------------------------------------------------------------
    // Meta
    // --------------------------------------------------------------------------------------------

    /**
     * Retrieve the upstream API version if available.
     *
     * @return API version string or {@code "unknown"} when unavailable
     */
    public @NotNull String getApiVersion() {
        try {
            final Object res = invoke("getApiVersion", new Class[0]);
            return res instanceof String s ? s : "unknown";
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "getApiVersion failed", t);
            return "unknown";
        }
    }

    /**
     * Expose underlying provider object for advanced use (avoid holding long-term references).
     *
     * @return raw upstream delegate instance
     */
    public @NotNull Object getDelegateObject() {
        return delegate;
    }

    // --------------------------------------------------------------------------------------------
    // Reflection helpers
    // --------------------------------------------------------------------------------------------

    private static @Nullable Class<?> loadClass(@NotNull String fqcn, @Nullable String pluginName) {
        Objects.requireNonNull(fqcn, "fqcn");
        // Try provider plugin classloader first if specified
        if (pluginName != null && !pluginName.isBlank()) {
            final Plugin p = Bukkit.getPluginManager().getPlugin(pluginName);
            if (p != null) {
                try {
                    return Class.forName(fqcn, false, p.getClass().getClassLoader());
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        try {
            return Class.forName(fqcn);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private <T> CompletableFuture<T> withTimeout(CompletableFuture<T> future) {
        final Duration timeout = this.defaultTimeout;
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return future;
        }
        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private Object invoke(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        final Method m = delegate.getClass().getMethod(methodName, paramTypes);
        return m.invoke(delegate, args);
    }

    private <T> CompletableFuture<T> invokeCf(String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Object res = invoke(methodName, paramTypes, args);
            if (!(res instanceof CompletableFuture<?> cf)) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Method " + methodName + " did not return CompletableFuture"));
            }
            return withTimeout((CompletableFuture<T>) cf);
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    private String safeApiVersion() {
        try {
            return getApiVersion();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }
}