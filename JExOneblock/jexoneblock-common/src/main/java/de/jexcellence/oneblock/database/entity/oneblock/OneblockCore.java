package de.jexcellence.oneblock.database.entity.oneblock;

import com.raindropcentral.rplatform.database.converter.LocationConverter;
import de.jexcellence.oneblock.service.DynamicEvolutionService;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Embeddable OneblockCore component for evolution progression tracking with modern optimization
 * Replaces JEIslandOneblock with modern structure, Lombok optimization, and clean methods
 */
@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class OneblockCore {
    
    @Column(name = "current_evolution", nullable = false, length = 100)
    @Builder.Default
    private String currentEvolution = getDefaultStartingEvolution();
    
    @Column(name = "evolution_level", nullable = false)
    @Builder.Default
    private int evolutionLevel = 1;
    
    @Column(name = "evolution_experience", nullable = false)
    @Builder.Default
    private double evolutionExperience = 0.0;
    
    @Column(name = "total_blocks_broken", nullable = false)
    @Builder.Default
    private long totalBlocksBroken = 0L;
    
    @Column(name = "prestige_level", nullable = false)
    @Builder.Default
    private int prestigeLevel = 0;
    
    @Column(name = "prestige_points", nullable = false)
    @Builder.Default
    private long prestigePoints = 0L;
    
    @Column(name = "oneblock_location", nullable = false)
    @Convert(converter = LocationConverter.class)
    private Location oneblockLocation;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
    
    @Column(name = "last_break_timestamp")
    private Long lastBreakTimestamp;
    
    @Column(name = "break_streak", nullable = false)
    @Builder.Default
    private int breakStreak = 0;
    
    @Column(name = "max_break_streak", nullable = false)
    @Builder.Default
    private int maxBreakStreak = 0;
    
    /**
     * Constructor for creating with location
     * @param oneblockLocation the location of the oneblock
     */
    public OneblockCore(@NotNull Location oneblockLocation) {
        this.oneblockLocation = oneblockLocation.clone();
        this.currentEvolution = getDefaultStartingEvolution();
        this.evolutionLevel = 1;
        this.evolutionExperience = 0.0;
        this.totalBlocksBroken = 0L;
        this.prestigeLevel = 0;
        this.prestigePoints = 0L;
        this.isActive = true;
        this.breakStreak = 0;
        this.maxBreakStreak = 0;
    }
    
    /**
     * Gets the default starting evolution.
     * Tries to get it from the dynamic evolution service, falls back to "Genesis".
     */
    private static String getDefaultStartingEvolution() {
        try {
            var dynamicService = new DynamicEvolutionService();
            return dynamicService.getStartingEvolution();
        } catch (Exception e) {
            // Fallback to Genesis if service fails
            return "Genesis";
        }
    }
    
    /**
     * Constructor for creating with evolution details
     * @param oneblockLocation the location of the oneblock
     * @param currentEvolution the current evolution name
     * @param evolutionLevel the evolution level
     */
    public OneblockCore(@NotNull Location oneblockLocation, @NotNull String currentEvolution, int evolutionLevel) {
        this.oneblockLocation = oneblockLocation.clone();
        this.currentEvolution = currentEvolution;
        this.evolutionLevel = evolutionLevel;
        this.evolutionExperience = 0.0;
        this.totalBlocksBroken = 0L;
        this.prestigeLevel = 0;
        this.prestigePoints = 0L;
        this.isActive = true;
        this.breakStreak = 0;
        this.maxBreakStreak = 0;
    }
    
    /**
     * Adds evolution experience and increments blocks broken counter
     * @param exp the experience to add
     */
    public void addEvolutionExperience(double exp) {
        this.evolutionExperience += exp;
        this.totalBlocksBroken++;
        this.lastBreakTimestamp = System.currentTimeMillis();
        
        // Update break streak
        this.breakStreak++;
        if (this.breakStreak > this.maxBreakStreak) {
            this.maxBreakStreak = this.breakStreak;
        }
    }
    
    /**
     * Adds evolution experience (integer version)
     * @param exp the experience to add
     */
    public void addEvolutionExperience(int exp) {
        this.addEvolutionExperience((double) exp);
    }
    
    /**
     * Legacy method for compatibility
     * @param experience the experience to add
     */
    public void addExperience(double experience) {
        addEvolutionExperience(experience);
    }
    
    /**
     * Legacy method for compatibility
     * @param experience the experience to add
     */
    public void addExperience(int experience) {
        addEvolutionExperience(experience);
    }
    
    /**
     * Advances to the next evolution level and resets experience
     */
    public void nextEvolutionLevel() {
        this.evolutionLevel++;
        this.evolutionExperience = 0.0;
    }
    
    /**
     * Legacy method for compatibility
     */
    public void nextEvolution() {
        nextEvolutionLevel();
    }
    
    /**
     * Advances multiple evolution levels
     * @param levels the number of levels to advance
     */
    public void addEvolutionLevels(int levels) {
        this.evolutionLevel += levels;
    }
    
    /**
     * Legacy method for compatibility
     * @param evolutions the number of evolutions to advance
     */
    public void addEvolution(int evolutions) {
        addEvolutionLevels(evolutions);
    }
    
    /**
     * Changes to a different evolution
     * @param evolutionName the new evolution name
     * @param resetLevel whether to reset the level to 1
     */
    public void changeEvolution(@NotNull String evolutionName, boolean resetLevel) {
        this.currentEvolution = evolutionName;
        if (resetLevel) {
            this.evolutionLevel = 1;
            this.evolutionExperience = 0.0;
        }
    }
    
    /**
     * Performs a prestige, resetting evolution progress but gaining prestige points
     * @param pointsGained the prestige points gained
     */
    public void prestige(long pointsGained) {
        this.prestigeLevel++;
        this.prestigePoints += pointsGained;
        this.evolutionLevel = 1;
        this.evolutionExperience = 0.0;
        this.currentEvolution = "Genesis"; // Reset to starting evolution
    }
    
    /**
     * Performs a prestige with default points calculation
     */
    public void prestige() {
        long pointsGained = calculatePrestigePoints();
        prestige(pointsGained);
    }
    
    /**
     * Calculates prestige points based on current progress
     * @return calculated prestige points
     */
    public long calculatePrestigePoints() {
        return (long) (evolutionLevel * 100 + evolutionExperience / 10);
    }
    
    /**
     * Gets the current evolution level (alias for compatibility)
     * @return current evolution level
     */
    public int getLevel() {
        return this.evolutionLevel;
    }
    
    /**
     * Legacy method for compatibility
     * @return current evolution level
     */
    public int getStage() {
        return this.evolutionLevel;
    }
    
    /**
     * Gets the experience as integer (alias for compatibility)
     * @return experience as integer
     */
    public int getExperienceAsInt() {
        return (int) this.evolutionExperience;
    }
    
    /**
     * Legacy method for compatibility
     * @return experience as integer
     */
    public int getExperience() {
        return getExperienceAsInt();
    }
    
    /**
     * Legacy method for compatibility
     * @return total blocks broken
     */
    public long getBlocksBroken() {
        return this.totalBlocksBroken;
    }
    
    /**
     * Legacy method for compatibility
     */
    public void incrementBlocksBroken() {
        this.totalBlocksBroken++;
    }
    
    /**
     * Legacy method for compatibility
     * @return prestige level
     */
    public int getCurrentPrestige() {
        return this.prestigeLevel;
    }
    
    /**
     * Legacy method for compatibility
     * @return current evolution as integer (level)
     */
    public int getCurrentEvolutionAsInt() {
        return this.evolutionLevel;
    }
    
    /**
     * Legacy method for compatibility
     * @return current experience
     */
    public double getCurrentExperience() {
        return this.evolutionExperience;
    }
    
    /**
     * Legacy method for compatibility
     * @return generator location
     */
    public Location getGeneratorLocation() {
        return this.oneblockLocation;
    }
    
    /**
     * Legacy method for compatibility
     * @param location the generator location
     */
    public void setGeneratorLocation(@NotNull Location location) {
        this.oneblockLocation = location.clone();
    }
    
    /**
     * Checks if the oneblock has reached a specific evolution level
     * @param level the evolution level to check
     * @return true if current level is at or above the specified level
     */
    public boolean hasReachedLevel(int level) {
        return this.evolutionLevel >= level;
    }
    
    /**
     * Checks if the oneblock is in a specific evolution
     * @param evolutionName the evolution name to check
     * @return true if current evolution matches
     */
    public boolean isInEvolution(@NotNull String evolutionName) {
        return this.currentEvolution.equalsIgnoreCase(evolutionName);
    }
    
    /**
     * Calculates total progress including prestige
     * @return total progress score
     */
    public long getTotalProgress() {
        return (long) this.prestigeLevel * 10000000L + 
               this.evolutionLevel * 10000L + 
               (long) this.evolutionExperience;
    }
    
    /**
     * Gets blocks broken per evolution level (average)
     * @return average blocks broken per level
     */
    public double getAverageBlocksPerLevel() {
        return this.evolutionLevel > 0 ? (double) this.totalBlocksBroken / this.evolutionLevel : 0.0;
    }
    
    /**
     * Gets experience per block broken (efficiency)
     * @return average experience per block
     */
    public double getExperiencePerBlock() {
        return this.totalBlocksBroken > 0 ? this.evolutionExperience / this.totalBlocksBroken : 0.0;
    }
    
    /**
     * Checks if the specified block matches this oneblock's location
     * @param block the block to check
     * @return true if the block is at the same location as this oneblock
     */
    public boolean isBlockAtLocation(@NotNull Block block) {
        return block.getLocation().toVector().equals(this.oneblockLocation.toVector());
    }
    
    /**
     * Checks if the specified location matches this oneblock's location
     * @param location the location to check
     * @return true if the location matches this oneblock's location
     */
    public boolean isAtLocation(@NotNull Location location) {
        return location.toVector().equals(this.oneblockLocation.toVector());
    }
    
    /**
     * Resets break streak (called when player stops breaking blocks)
     */
    public void resetBreakStreak() {
        this.breakStreak = 0;
    }
    
    /**
     * Checks if the player has been breaking blocks recently
     * @param timeoutMillis timeout in milliseconds
     * @return true if last break was within timeout
     */
    public boolean isRecentlyActive(long timeoutMillis) {
        return lastBreakTimestamp != null && 
               (System.currentTimeMillis() - lastBreakTimestamp) < timeoutMillis;
    }
    
    /**
     * Gets the time since last block break in milliseconds
     * @return milliseconds since last break, or -1 if never broken
     */
    public long getTimeSinceLastBreak() {
        return lastBreakTimestamp != null ? 
               System.currentTimeMillis() - lastBreakTimestamp : -1;
    }
    
    /**
     * Resets all progress (for admin purposes)
     */
    public void reset() {
        this.currentEvolution = "Genesis";
        this.evolutionLevel = 1;
        this.evolutionExperience = 0.0;
        this.totalBlocksBroken = 0L;
        this.prestigeLevel = 0;
        this.prestigePoints = 0L;
        this.breakStreak = 0;
        this.maxBreakStreak = 0;
        this.lastBreakTimestamp = null;
    }
    
    /**
     * Resets only evolution progress, keeping prestige
     */
    public void resetEvolution() {
        this.currentEvolution = "Genesis";
        this.evolutionLevel = 1;
        this.evolutionExperience = 0.0;
        this.breakStreak = 0;
    }
    
    /**
     * Gets a summary of the oneblock's progress
     * @return formatted progress string
     */
    public @NotNull String getProgressSummary() {
        return String.format("%s Level %d (%.1f XP) - %d blocks broken - Prestige %d (%d points)",
                currentEvolution, evolutionLevel, evolutionExperience, 
                totalBlocksBroken, prestigeLevel, prestigePoints);
    }
    
    @Override
    public String toString() {
        return "OneblockCore{" +
                "evolution='" + currentEvolution + '\'' +
                ", level=" + evolutionLevel +
                ", experience=" + String.format("%.1f", evolutionExperience) +
                ", totalBlocks=" + totalBlocksBroken +
                ", prestige=" + prestigeLevel +
                ", prestigePoints=" + prestigePoints +
                ", isActive=" + isActive +
                ", streak=" + breakStreak + "/" + maxBreakStreak +
                '}';
    }
}