package de.jexcellence.oneblock.manager.infrastructure;

import de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure;
import de.jexcellence.oneblock.database.repository.IslandInfrastructureRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background tick processor for all infrastructure systems
 * Handles energy, automation, crafting, and passive rewards
 * Modern Java - efficient and clean
 */
public class InfrastructureTickProcessor {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    private static final int ENERGY_TICK_INTERVAL = 20; // 1 second
    private static final int AUTOMATION_TICK_INTERVAL = 100; // 5 seconds
    private static final int CRAFTING_TICK_INTERVAL = 20; // 1 second
    private static final int PASSIVE_REWARD_INTERVAL = 1200; // 1 minute
    private static final int SAVE_INTERVAL = 6000; // 5 minutes
    
    private final Plugin plugin;
    private final InfrastructureManager manager;
    private final IslandInfrastructureRepository repository;
    private final Map<Long, IslandInfrastructure> activeInfrastructure = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private int energyTaskId = -1;
    private int automationTaskId = -1;
    private int craftingTaskId = -1;
    private int passiveTaskId = -1;
    private int saveTaskId = -1;
    
    public InfrastructureTickProcessor(Plugin plugin, InfrastructureManager manager, IslandInfrastructureRepository repository) {
        this.plugin = plugin;
        this.manager = manager;
        this.repository = repository;
    }
    
    /**
     * Starts all background processing tasks
     */
    public void start() {
        if (running.getAndSet(true)) return;
        
        LOGGER.info("[Infrastructure] Starting background processors...");
        
        // Energy tick - every second
        energyTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processEnergyTick, 
            ENERGY_TICK_INTERVAL, ENERGY_TICK_INTERVAL).getTaskId();
        
        // Automation tick - every 5 seconds
        automationTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processAutomationTick,
            AUTOMATION_TICK_INTERVAL, AUTOMATION_TICK_INTERVAL).getTaskId();
        
        // Crafting tick - every second
        craftingTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processCraftingTick,
            CRAFTING_TICK_INTERVAL, CRAFTING_TICK_INTERVAL).getTaskId();
        
        // Passive rewards - every minute
        passiveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processPassiveRewards,
            PASSIVE_REWARD_INTERVAL, PASSIVE_REWARD_INTERVAL).getTaskId();
        
        // Auto-save - every 5 minutes
        saveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processSave,
            SAVE_INTERVAL, SAVE_INTERVAL).getTaskId();
        
        LOGGER.info("[Infrastructure] Background processors started successfully");
    }
    
    /**
     * Stops all background processing tasks
     */
    public void stop() {
        if (!running.getAndSet(false)) return;
        
        LOGGER.info("[Infrastructure] Stopping background processors...");
        
        cancelTask(energyTaskId);
        cancelTask(automationTaskId);
        cancelTask(craftingTaskId);
        cancelTask(passiveTaskId);
        cancelTask(saveTaskId);
        
        // Final save
        saveAll();
        
        LOGGER.info("[Infrastructure] Background processors stopped");
    }
    
    /**
     * Registers infrastructure for processing
     */
    public void register(IslandInfrastructure infrastructure) {
        activeInfrastructure.put(infrastructure.getIslandId(), infrastructure);
    }
    
    /**
     * Unregisters infrastructure from processing
     */
    public void unregister(Long islandId) {
        var infra = activeInfrastructure.remove(islandId);
        if (infra != null) {
            repository.saveAsync(infra);
        }
    }
    
    /**
     * Gets active infrastructure count
     */
    public int getActiveCount() {
        return activeInfrastructure.size();
    }
    
    // Processing methods
    private void processEnergyTick() {
        activeInfrastructure.values().parallelStream().forEach(infra -> {
            try {
                manager.getEnergyService().processEnergyGeneration(infra);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[Infrastructure] Energy tick failed for " + infra.getIslandId(), e);
            }
        });
    }
    
    private void processAutomationTick() {
        activeInfrastructure.values().parallelStream().forEach(infra -> {
            try {
                manager.getAutomationService().processAutomation(infra);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[Infrastructure] Automation tick failed for " + infra.getIslandId(), e);
            }
        });
    }
    
    private void processCraftingTick() {
        activeInfrastructure.values().parallelStream().forEach(infra -> {
            try {
                manager.getCraftingService().processCraftingQueue(infra);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[Infrastructure] Crafting tick failed for " + infra.getIslandId(), e);
            }
        });
    }
    
    private void processPassiveRewards() {
        var now = LocalDateTime.now();
        
        activeInfrastructure.values().parallelStream().forEach(infra -> {
            try {
                var lastReward = infra.getLastPassiveReward();
                var minutesSinceReward = java.time.Duration.between(lastReward, now).toMinutes();
                
                if (minutesSinceReward >= 1) {
                    // Calculate passive XP based on infrastructure
                    var baseXp = infra.getStorageTier().getMaxPassiveXpPerMinute();
                    var multiplier = infra.getPassiveXpMultiplier();
                    var passiveXp = (long) (baseXp * multiplier * minutesSinceReward);
                    
                    // Store XP for player to claim (or auto-apply)
                    // This would integrate with your XP system
                    
                    infra.setLastPassiveReward(now);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[Infrastructure] Passive reward failed for " + infra.getIslandId(), e);
            }
        });
    }
    
    private void processSave() {
        saveAll();
    }
    
    private void saveAll() {
        var count = 0;
        for (var infra : activeInfrastructure.values()) {
            try {
                repository.saveAsync(infra);
                count++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[Infrastructure] Save failed for " + infra.getIslandId(), e);
            }
        }
        LOGGER.fine("[Infrastructure] Saved " + count + " infrastructure records");
    }
    
    private void cancelTask(int taskId) {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
