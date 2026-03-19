package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure;
import de.jexcellence.oneblock.database.repository.IslandInfrastructureRepository;
import de.jexcellence.oneblock.manager.infrastructure.InfrastructureManager;
import de.jexcellence.oneblock.manager.infrastructure.InfrastructureTickProcessor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class InfrastructureServiceImpl implements IInfrastructureService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final Plugin plugin;
    private final InfrastructureManager manager;
    private final InfrastructureTickProcessor tickProcessor;
    private final IslandInfrastructureRepository repository;
    private final Map<Long, IslandInfrastructure> cache = new ConcurrentHashMap<>();
    
    private boolean initialized = false;
    
    public InfrastructureServiceImpl(
            @NotNull Plugin plugin,
            @NotNull IslandInfrastructureRepository repository
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.manager = new InfrastructureManager();
        this.tickProcessor = new InfrastructureTickProcessor(plugin, manager, repository);
    }
    
    public void initialize() {
        if (initialized) {
            LOGGER.warning("[Infrastructure] Service already initialized");
            return;
        }
        
        LOGGER.info("[Infrastructure] Initializing service...");
        
        preloadInfrastructure();
        
        tickProcessor.start();
        
        initialized = true;
        LOGGER.info("[Infrastructure] Service initialized successfully with " + cache.size() + " infrastructure records");
    }
    
    public void shutdown() {
        if (!initialized) {
            return;
        }
        
        LOGGER.info("[Infrastructure] Shutting down service...");
        
        tickProcessor.stop();
        
        cache.clear();
        
        initialized = false;
        LOGGER.info("[Infrastructure] Service shut down successfully");
    }
    
    @Override
    public @Nullable IslandInfrastructure getInfrastructure(@NotNull Long islandId, @NotNull UUID playerId) {
        var cached = cache.get(islandId);
        if (cached != null) {
            return cached;
        }
        
        var infrastructure = repository.findByIslandId(islandId);
        
        if (infrastructure == null) {
            infrastructure = new IslandInfrastructure(islandId, playerId);
            repository.saveAsync(infrastructure);
        }
        
        cache.put(islandId, infrastructure);
        
        return infrastructure;
    }
    
    @Override
    public @NotNull CompletableFuture<Optional<IslandInfrastructure>> getInfrastructureAsync(@NotNull Long islandId) {
        var cached = cache.get(islandId);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        
        return repository.findByIslandIdAsync(islandId)
                .thenApply(opt -> {
                    opt.ifPresent(infra -> cache.put(islandId, infra));
                    return opt;
                });
    }
    
    @Override
    public @NotNull InfrastructureManager getManager() {
        return manager;
    }
    
    @Override
    public @NotNull InfrastructureTickProcessor getTickProcessor() {
        return tickProcessor;
    }
    
    private void preloadInfrastructure() {
        LOGGER.info("[Infrastructure] Pre-loading infrastructure from database...");
        
        try {
            var allInfrastructure = repository.findAll();
            for (var infra : allInfrastructure) {
                cache.put(infra.getIslandId(), infra);
            }
            LOGGER.info("[Infrastructure] Pre-loaded " + allInfrastructure.size() + " infrastructure records");
        } catch (Exception e) {
            LOGGER.severe("[Infrastructure] Failed to pre-load infrastructure: " + e.getMessage());
        }
    }
    
    public void registerInfrastructure(@NotNull Long islandId) {
        if (!initialized) return;
        
        var infrastructure = cache.get(islandId);
        if (infrastructure != null) {
            tickProcessor.register(infrastructure);
        }
    }
    
    public void unregisterInfrastructure(@NotNull Long islandId) {
        if (!initialized) return;
        
        tickProcessor.unregister(islandId);
    }
    
    public int getActiveCount() {
        return tickProcessor.getActiveCount();
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isHealthy() {
        if (!initialized) {
            return false;
        }
        
        try {
            repository.findAll();
        } catch (Exception e) {
            LOGGER.severe("[Infrastructure] Health check failed: Repository not accessible - " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    public Map<String, Object> getMetrics() {
        return Map.of(
            "initialized", initialized,
            "cache_size", cache.size(),
            "active_count", tickProcessor.getActiveCount()
        );
    }
    
    public boolean saveInfrastructure(@NotNull IslandInfrastructure infrastructure) {
        try {
            repository.save(infrastructure);
            cache.put(infrastructure.getIslandId(), infrastructure);
            return true;
        } catch (Exception e) {
            LOGGER.severe("[Infrastructure] Failed to save infrastructure for island " + 
                infrastructure.getIslandId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public CompletableFuture<Boolean> saveInfrastructureAsync(@NotNull IslandInfrastructure infrastructure) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                repository.saveAsync(infrastructure).join();
                cache.put(infrastructure.getIslandId(), infrastructure);
                return true;
            } catch (Exception e) {
                LOGGER.severe("[Infrastructure] Failed to save infrastructure asynchronously for island " + 
                    infrastructure.getIslandId() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
}