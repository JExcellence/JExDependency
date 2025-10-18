package com.raindropcentral.rdq.service;

import com.raindropcentral.rcore.database.entity.RPlayer;
import com.raindropcentral.rcore.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.rcore.database.entity.statistic.RPlayerStatistic;
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
 * RCore registers its adapter instance (com.raindropcentral.core.api.RCoreAdapter) under the
 * service interface FQCN: com.raindropcentral.core.service.RCoreService.
 *
 * This class:
 * - Resolves the service via Bukkit ServicesManager using the upstream interface name (by reflection)
 * - Invokes all async methods reflectively while keeping RDQ's entity types in signatures
 * - Adds optional default timeouts and consistent logging
 *
 * Usage:
 *   // Immediate resolve (pass the provider plugin name, typically "RCore")
 *   RCoreBridge bridge = RCoreBridge.fromBukkit(this, "RCore");
 *   if (bridge != null) {
 *       bridge.findPlayerAsync(uuid).thenAccept(opt -> ...);
 *   }
 *
 *   // Or with retries via ServiceRegistry
 *   ServiceRegistry reg = new ServiceRegistry();
 *   RCoreBridge.awaitService(reg, "RCore", true, 20, 500)
 *       .thenAccept(opt -> opt.ifPresent(b -> this.rcore = b.withDefaultTimeout(Duration.ofSeconds(5))));
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
     * @param registry             ServiceRegistry
     * @param providerPluginName   Optional plugin name to load the class from (e.g., "RCore")
     * @param required             true: log warning on failure after retries, false: optional
     * @param maxAttempts          number of attempts
     * @param retryDelayMs         delay between attempts (ms)
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
     * Check availability in ServicesManager (best-effort). Returns false if the upstream interface cannot be loaded.
     */
    public static boolean isAvailable(@Nullable String providerPluginName) {
        final Class<?> svcClass = loadClass(SERVICE_FQCN, providerPluginName);
        if (svcClass == null) return false;
        return Bukkit.getServer().getServicesManager().isProvidedFor(svcClass);
    }

    // --------------------------------------------------------------------------------------------
    // Configuration
    // --------------------------------------------------------------------------------------------

    public @NotNull RCoreBridge withDefaultTimeout(@NotNull Duration timeout) {
        this.defaultTimeout = Objects.requireNonNull(timeout, "timeout");
        return this;
    }

    public @NotNull Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    // --------------------------------------------------------------------------------------------
    // Player lookups
    // --------------------------------------------------------------------------------------------

    public @NotNull CompletableFuture<Optional<RPlayer>> findPlayerAsync(@NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return this.<Optional<RPlayer>>invokeCf("findPlayerAsync", new Class[]{UUID.class}, uniqueId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findPlayerAsync(UUID) failed for " + uniqueId, ex);
                    return Optional.<RPlayer>empty();
                });
    }

    public @NotNull CompletableFuture<Optional<RPlayer>> findPlayerAsync(@NotNull OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer");
        return this.<Optional<RPlayer>>invokeCf("findPlayerAsync", new Class[]{OfflinePlayer.class}, offlinePlayer)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findPlayerAsync(OfflinePlayer) failed for " + offlinePlayer.getUniqueId(), ex);
                    return Optional.<RPlayer>empty();
                });
    }

    public @NotNull CompletableFuture<Optional<RPlayer>> findPlayerByNameAsync(@NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        return this.<Optional<RPlayer>>invokeCf("findPlayerByNameAsync", new Class[]{String.class}, playerName)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findPlayerByNameAsync failed for name=" + playerName, ex);
                    return Optional.<RPlayer>empty();
                });
    }

    public @NotNull CompletableFuture<Boolean> playerExistsAsync(@NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return this.<Boolean>invokeCf("playerExistsAsync", new Class[]{UUID.class}, uniqueId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "playerExistsAsync(UUID) failed for " + uniqueId, ex);
                    return false;
                });
    }

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

    public @NotNull CompletableFuture<Optional<RPlayer>> createPlayerAsync(@NotNull UUID uniqueId, @NotNull String playerName) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(playerName, "playerName");
        return this.<Optional<RPlayer>>invokeCf("createPlayerAsync", new Class[]{UUID.class, String.class}, uniqueId, playerName)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "createPlayerAsync failed for " + uniqueId + " name=" + playerName, ex);
                    return Optional.<RPlayer>empty();
                });
    }

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

    public @NotNull CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(@NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return this.<Optional<RPlayerStatistic>>invokeCf("findPlayerStatisticsAsync", new Class[]{UUID.class}, uniqueId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "findPlayerStatisticsAsync(UUID) failed for " + uniqueId, ex);
                    return Optional.<RPlayerStatistic>empty();
                });
    }

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

    public @NotNull CompletableFuture<Long> getStatisticCountForPluginAsync(@NotNull UUID uniqueId, @NotNull String plugin) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(plugin, "plugin");
        return this.<Long>invokeCf("getStatisticCountForPluginAsync", new Class[]{UUID.class, String.class}, uniqueId, plugin)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "getStatisticCountForPluginAsync failed for " + uniqueId + " plugin=" + plugin, ex);
                    return 0L;
                });
    }

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