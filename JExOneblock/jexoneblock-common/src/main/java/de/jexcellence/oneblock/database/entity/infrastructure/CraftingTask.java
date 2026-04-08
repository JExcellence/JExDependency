package de.jexcellence.oneblock.database.entity.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Crafting Task - Represents a queued crafting operation
 * Used for automation modules and processor upgrades
 */
@Setter
@Getter
@Embeddable
public class CraftingTask {

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type")
    private CraftingTaskType taskType;
    
    @Column(name = "target_item")
    private String targetItem; // AutomationModule name, ProcessorType name, etc.
    
    @Column(name = "target_level")
    private int targetLevel; // For processor upgrades
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completion_time")
    private LocalDateTime completionTime;
    
    @Column(name = "energy_cost")
    private long energyCost;
    
    @Column(name = "is_completed")
    private boolean completed = false;
    
    @Column(name = "progress_percentage")
    private double progressPercentage = 0.0;
    
    // Constructors
    public CraftingTask() {}
    
    public CraftingTask(CraftingTaskType taskType, String targetItem, int targetLevel, 
                       LocalDateTime completionTime, long energyCost) {
        this.taskType = taskType;
        this.targetItem = targetItem;
        this.targetLevel = targetLevel;
        this.startedAt = LocalDateTime.now();
        this.completionTime = completionTime;
        this.energyCost = energyCost;
    }
    
    /**
     * Updates the progress of this crafting task
     */
    public void updateProgress() {
        if (completed) return;
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(completionTime)) {
            progressPercentage = 100.0;
            completed = true;
        } else {
            // Calculate progress based on time elapsed
            long totalDuration = java.time.Duration.between(startedAt, completionTime).toSeconds();
            long elapsed = java.time.Duration.between(startedAt, now).toSeconds();
            progressPercentage = Math.min(100.0, (double) elapsed / totalDuration * 100.0);
        }
    }
    
    /**
     * Gets the remaining time for this task
     */
    public long getRemainingSeconds() {
        if (completed) return 0L;
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(completionTime)) return 0L;
        
        return java.time.Duration.between(now, completionTime).toSeconds();
    }
    
    /**
     * Gets the total duration of this task in seconds
     */
    public long getTotalDurationSeconds() {
        return java.time.Duration.between(startedAt, completionTime).toSeconds();
    }
    
    /**
     * Checks if this task can be completed instantly (with enough energy)
     */
    public boolean canInstantComplete(long availableEnergy) {
        return !completed && availableEnergy >= energyCost;
    }
    
    /**
     * Instantly completes this task by consuming energy
     */
    public boolean instantComplete(long availableEnergy) {
        if (canInstantComplete(availableEnergy)) {
            completed = true;
            progressPercentage = 100.0;
            completionTime = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * Gets the progress as a decimal (0.0 to 1.0)
     */
    public double getProgress() { return progressPercentage / 100.0; }
    
    /**
     * Crafting task types
     */
    @Getter
    public enum CraftingTaskType {
        AUTOMATION_MODULE("Automation Module"),
        PROCESSOR_UPGRADE("Processor Upgrade"),
        GENERATOR_UPGRADE("Generator Upgrade"),
        STORAGE_UPGRADE("Storage Upgrade"),
        INFRASTRUCTURE_COMPONENT("Infrastructure Component");
        
        private final String displayName;
        
        CraftingTaskType(String displayName) {
            this.displayName = displayName;
        }

    }
}