/*
package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.perk.RPlayerPerk;
import com.raindropcentral.rdq.type.EPerkType;
import com.raindropcentral.rdq.utility.perk.RPerkManagementService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

*/
/**
 * Service responsible for handling perk activation, deactivation, and effect management.
 * <p>
 * This service handles:
 * <ul>
 *     <li>Perk activation and deactivation logic</li>
 *     <li>Cooldown management and tracking</li>
 *     <li>Toggle state management for toggleable perks</li>
 *     <li>Effect duration and scheduling</li>
 *     <li>Concurrent usage limits</li>
 * </ul>
 * </p>
 * <p>
 * This service is separate from {@link com.raindropcentral.rdq2.utility.perk.RPerkManagementService} which handles
 * perk ownership and enablement.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 *//*

public class RPerkActivationService {
    
    private static final Logger LOGGER = Logger.getLogger(RPerkActivationService.class.getName());
    
    private final RDQImpl                    rdq;
    private final RPerkManagementService managementService;
    
    public RPerkActivationService(
        final @NotNull RDQImpl rdq
    ) {
        
        this.rdq = rdq;
        this.managementService = new RPerkManagementService(this.rdq);
    }
    
    */
/**
     * Tracks the last activation time for each player-perk combination for cooldown management.
     * Key format: "playerUUID:perkIdentifier"
     *//*

    private final Map<String, Long> cooldownTracker = new ConcurrentHashMap<>();
    
    */
/**
     * Tracks active sessions for usage time calculation.
     * Key format: "playerUUID:perkIdentifier", Value: session start time
     *//*

    private final Map<String, Long> activeSessionTracker = new ConcurrentHashMap<>();
    
    */
/**
     * Tracks toggle states for toggleable perks.
     * Key format: "playerUUID:perkIdentifier", Value: toggle state
     *//*

    private final Map<String, Boolean> toggleStateTracker = new ConcurrentHashMap<>();
    
    */
/**
     * Activates a perk for a player.
     *
     * @param playerPerk the player-perk association
     *
     * @return true if activation was successful, false otherwise
     *//*

    public boolean activatePerk(final @NotNull RPlayerPerk playerPerk) {
        
        if (! canActivatePerk(playerPerk)) {
            return false;
        }
        
        final EPerkType perkType = playerPerk.getPerk().getPerkType();
        
        return switch (perkType) {
            case TOGGLEABLE_PASSIVE -> handleToggleableActivation(playerPerk);
            case INSTANT_USE -> handleInstantUseActivation(playerPerk);
            case DURATION_BASED -> handleDurationBasedActivation(playerPerk);
            case EVENT_TRIGGERED -> false;
        };
    }
    
    */
/**
     * Deactivates a perk for a player.
     *
     * @param playerPerk the player-perk association
     *
     * @return true if deactivation was successful, false otherwise
     *//*

    public boolean deactivatePerk(final @NotNull RPlayerPerk playerPerk) {
        
        if (! playerPerk.isActive()) {
            return false;
        }
        
        final EPerkType perkType = playerPerk.getPerk().getPerkType();
        
        return switch (perkType) {
            case TOGGLEABLE_PASSIVE -> handleToggleableDeactivation(playerPerk);
            case INSTANT_USE,
                 DURATION_BASED,
                 EVENT_TRIGGERED -> handleStandardDeactivation(playerPerk);
        };
    }
    
    */
/**
     * Toggles a perk state (activate if inactive, deactivate if active).
     *
     * @param playerPerk the player-perk association
     *
     * @return true if toggle was successful, false otherwise
     *//*

    public boolean togglePerk(final @NotNull RPlayerPerk playerPerk) {
        
        if (! playerPerk.getPerk().getPerkType().isToggleable()) {
            return false;
        }
        
        if (playerPerk.isActive()) {
            return deactivatePerk(playerPerk);
        } else {
            return activatePerk(playerPerk);
        }
    }
    
    */
/**
     * Triggers a perk for event-based perks.
     *
     * @param playerPerk the player-perk association
     *
     * @return true if trigger was successful, false otherwise
     *//*

    public boolean triggerPerk(final @NotNull RPlayerPerk playerPerk) {
        
        if (! canActivatePerk(playerPerk)) {
            return false;
        }
        
        if (playerPerk.getPerk().getPerkType() != EPerkType.EVENT_TRIGGERED) {
            return false;
        }
        
        final String cooldownKey = getCooldownKey(playerPerk);
        
        if (isOnCooldown(playerPerk)) {
*/
/*             RPerkStatisticService.recordCooldownViolation(
                playerPerk.getPlayer(),
                playerPerk.getPerk()
            ); *//*

            return false;
        }
        
        if (playerPerk.getPerk().performActivation()) {
            cooldownTracker.put(
                cooldownKey,
                System.currentTimeMillis()
            );
            
            managementService.updateLastActivated(playerPerk);
            managementService.incrementActivationCount(playerPerk);
            
*/
/*             RPerkStatisticService.recordPerkActivation(
                playerPerk.getPlayer(),
                playerPerk.getPerk()
            ); *//*

            
            playerPerk.getPerk().performTrigger();
            
            return true;
        }
        
        return false;
    }
    
    */
/**
     * Checks if a perk can be activated for a player.
     *
     * @param playerPerk the player-perk association
     *
     * @return true if the perk can be activated, false otherwise
     *//*

    public boolean canActivatePerk(final @NotNull RPlayerPerk playerPerk) {
        
        if (! playerPerk.isEnabled()) {
            return false;
        }
        
        if (! playerPerk.getPerk().isEnabled()) {
            return false;
        }
        
        final Player bukkitPlayer = Bukkit.getPlayer(playerPerk.getPlayer().getUniqueId());
        if (bukkitPlayer == null) {
            return false;
        }
        
        final String requiredPermission = playerPerk.getPerk().getRequiredPermission();
        if (requiredPermission != null && ! bukkitPlayer.hasPermission(requiredPermission)) {
            return false;
        }
        
        if (playerPerk.getPerk().getPerkType().isToggleable() && playerPerk.isActive()) {
            return false;
        }
        
        if (isConcurrentLimitReached(playerPerk.getPerk())) {
            return false;
        }
        
        if (! playerPerk.getPerk().canPerformActivation()) {
            return false;
        }
        
        return true;
    }
    
    */
/**
     * Checks if a perk is currently on cooldown for a player.
     *
     * @param playerPerk the player-perk association
     *
     * @return true if on cooldown, false otherwise
     *//*

    public boolean isOnCooldown(final @NotNull RPlayerPerk playerPerk) {
        
        if (! playerPerk.getPerk().getPerkType().hasCooldown()) {
            return false;
        }
        
        final String cooldownKey    = getCooldownKey(playerPerk);
        final Long   lastActivation = cooldownTracker.get(cooldownKey);
        
        if (lastActivation == null) {
            return false;
        }
        
        final Player bukkitPlayer = Bukkit.getPlayer(playerPerk.getPlayer().getUniqueId());
        if (bukkitPlayer == null) {
            return false;
        }
        
        final long cooldownDuration = getCooldownDuration(
            playerPerk,
            bukkitPlayer
        );
        if (cooldownDuration <= 0) {
            return false;
        }
        
        final long elapsed = System.currentTimeMillis() - lastActivation;
        return elapsed < cooldownDuration;
    }
    
    */
/**
     * Gets the remaining cooldown time for a player-perk combination.
     *
     * @param playerPerk the player-perk association
     *
     * @return remaining cooldown time in milliseconds, or 0 if not on cooldown
     *//*

    public long getRemainingCooldown(final @NotNull RPlayerPerk playerPerk) {
        
        if (! isOnCooldown(playerPerk)) {
            return 0L;
        }
        
        final String cooldownKey    = getCooldownKey(playerPerk);
        final Long   lastActivation = cooldownTracker.get(cooldownKey);
        
        if (lastActivation == null) {
            return 0L;
        }
        
        final Player bukkitPlayer = Bukkit.getPlayer(playerPerk.getPlayer().getUniqueId());
        if (bukkitPlayer == null) {
            return 0L;
        }
        
        final long cooldownDuration = getCooldownDuration(
            playerPerk,
            bukkitPlayer
        );
        final long elapsed          = System.currentTimeMillis() - lastActivation;
        
        return Math.max(
            0L,
            cooldownDuration - elapsed
        );
    }
    
    */
/**
     * Checks if a toggleable perk is currently toggled on for a player.
     *
     * @param playerPerk the player-perk association
     *
     * @return true if toggled on, false otherwise
     *//*

    public boolean isToggledOn(final @NotNull RPlayerPerk playerPerk) {
        
        if (! playerPerk.getPerk().getPerkType().isToggleable()) {
            return false;
        }
        
        final String toggleKey = getToggleKey(playerPerk);
        return toggleStateTracker.getOrDefault(
            toggleKey,
            false
        );
    }
    
    */
/**
     * Handles activation for toggleable passive perks.
     *//*

    private boolean handleToggleableActivation(final @NotNull RPlayerPerk playerPerk) {
        
        final String toggleKey = getToggleKey(playerPerk);
        
        if (toggleStateTracker.getOrDefault(
            toggleKey,
            false
        )) {
            return false;
        }
        
        if (playerPerk.getPerk().performActivation()) {
            playerPerk.setActive(true);
            managementService.updateLastActivated(playerPerk);
            managementService.incrementActivationCount(playerPerk);
            
            toggleStateTracker.put(
                toggleKey,
                true
            );
            
            final String sessionKey = getSessionKey(playerPerk);
            activeSessionTracker.put(
                sessionKey,
                System.currentTimeMillis()
            );
            
*/
/*             RPerkStatisticService.recordPerkActivation(
                playerPerk.getPlayer(),
                playerPerk.getPerk()
            ); *//*

            
            return true;
        }
        
        return false;
    }
    
    */
/**
     * Handles deactivation for toggleable passive perks.
     *//*

    private boolean handleToggleableDeactivation(final @NotNull RPlayerPerk playerPerk) {
        
        final String toggleKey = getToggleKey(playerPerk);
        
        if (! toggleStateTracker.getOrDefault(
            toggleKey,
            false
        )) {
            return false;
        }
        
        if (playerPerk.getPerk().performDeactivation()) {
            final String sessionKey   = getSessionKey(playerPerk);
            final Long   sessionStart = activeSessionTracker.remove(sessionKey);
            if (sessionStart != null) {
                final long usageTime = System.currentTimeMillis() - sessionStart;
                managementService.addUsageTime(
                    playerPerk,
                    usageTime
                );
            }
            
            playerPerk.setActive(false);
            managementService.updateLastDeactivated(playerPerk);
            
            toggleStateTracker.put(
                toggleKey,
                false
            );
            
*/
/*             RPerkStatisticService.recordPerkDeactivation(
                playerPerk.getPlayer(),
                playerPerk.getPerk()
            ); *//*

            
            return true;
        }
        
        return false;
    }
    
    */
/**
     * Handles activation for instant-use perks.
     *//*

    private boolean handleInstantUseActivation(final @NotNull RPlayerPerk playerPerk) {
        
        final String cooldownKey = getCooldownKey(playerPerk);
        
        if (isOnCooldown(playerPerk)) {
*/
/*             RPerkStatisticService.recordCooldownViolation(
                playerPerk.getPlayer(),
                playerPerk.getPerk()
            ); *//*

            return false;
        }
        
        if (playerPerk.getPerk().performActivation()) {
            cooldownTracker.put(
                cooldownKey,
                System.currentTimeMillis()
            );
            
            playerPerk.setActive(true);
            managementService.updateLastActivated(playerPerk);
            managementService.incrementActivationCount(playerPerk);
            
*/
/*             RPerkStatisticService.recordPerkActivation(
                playerPerk.getPlayer(),
                playerPerk.getPerk()
            ); *//*

            
            playerPerk.getPerk().performTrigger();
            
            handleStandardDeactivation(playerPerk);
            
            return true;
        }
        
        return false;
    }
    
    */
/**
     * Handles activation for duration-based perks.
     *//*

    private boolean handleDurationBasedActivation(final @NotNull RPlayerPerk playerPerk) {
        
        final String cooldownKey = getCooldownKey(playerPerk);
        
        if (isOnCooldown(playerPerk)) {
 */
/*            RPerkStatisticService.recordCooldownViolation(
                playerPerk.getPlayer(),
                playerPerk.getPerk()
            ); *//*

            return false;
        }
        
        if (playerPerk.getPerk().performActivation()) {
            cooldownTracker.put(
                cooldownKey,
                System.currentTimeMillis()
            );
            
            playerPerk.setActive(true);
            managementService.updateLastActivated(playerPerk);
            managementService.incrementActivationCount(playerPerk);
            
            final String sessionKey = getSessionKey(playerPerk);
            activeSessionTracker.put(
                sessionKey,
                System.currentTimeMillis()
            );
            
*/
/*             RPerkStatisticService.recordPerkActivation(
                playerPerk.getPlayer(),
                playerPerk.getPerk()
            ); *//*

            
            playerPerk.getPerk().performTrigger();
            
            scheduleDurationBasedDeactivation(playerPerk);
            
            return true;
        }
        
        return false;
    }
    
    */
/**
     * Handles deactivation for standard perks (non-toggleable).
     *//*

    private boolean handleStandardDeactivation(final @NotNull RPlayerPerk playerPerk) {
        
        if (! playerPerk.isActive()) {
            return false;
        }
        
        if (playerPerk.getPerk().performDeactivation()) {
            final String sessionKey   = getSessionKey(playerPerk);
            final Long   sessionStart = activeSessionTracker.remove(sessionKey);
            if (sessionStart != null) {
                final long usageTime = System.currentTimeMillis() - sessionStart;
                managementService.addUsageTime(
                    playerPerk,
                    usageTime
                );
            }
            
            playerPerk.setActive(false);
            managementService.updateLastDeactivated(playerPerk);
*/
/*
            RPerkStatisticService.recordPerkDeactivation(
                playerPerk.getPlayer(),
                playerPerk.getPerk()
            );
             *//*

            return true;
        }
        
        return false;
    }
    
    */
/**
     * Schedules automatic deactivation for duration-based perks.
     *//*

    private void scheduleDurationBasedDeactivation(
        final @NotNull RPlayerPerk playerPerk
    ) {
        
        final Long durationSeconds = playerPerk.getPerk().getPerkSection().getPermissionDurations().getEffectiveDuration(Bukkit.getPlayer(playerPerk.getPlayer().getUniqueId()));
        if (durationSeconds != null && durationSeconds > 0) {
            Bukkit.getScheduler().runTaskLater(
                this.rdq.getImpl(),
                () -> deactivatePerk(playerPerk),
                durationSeconds * 20L
            );
        }
    }
    
    */
/**
     * Checks if the concurrent user limit has been reached for a perk.
     *//*

    private boolean isConcurrentLimitReached(final @NotNull RPerk perk) {
        
        final Integer maxUsers = perk.getMaxConcurrentUsers();
        if (maxUsers == null || maxUsers <= 0) {
            return false;
        }
        
        //TODO
        return 30 >= maxUsers;
    }
    
    */
/**
     * Gets the cooldown duration for a player-perk combination.
     *//*

    private long getCooldownDuration(
        final @NotNull RPlayerPerk playerPerk,
        final @NotNull Player player
    ) {
        
        if (playerPerk.getPerk().getPerkSection().getPermissionCooldowns() != null) {
            return playerPerk.getPerk().getPerkSection()
                             .getPermissionCooldowns()
                             .getEffectiveCooldown(player) * 1000L;
        }
        
        return 30000L;
    }
    
    */
/**
     * Generates a unique key for cooldown tracking.
     *//*

    private String getCooldownKey(final @NotNull RPlayerPerk playerPerk) {
        
        return playerPerk.getPlayer().getUniqueId() + ":" + playerPerk.getPerk().getIdentifier();
    }
    
    */
/**
     * Generates a unique key for session tracking.
     *//*

    private String getSessionKey(final @NotNull RPlayerPerk playerPerk) {
        
        return playerPerk.getPlayer().getUniqueId() + ":" + playerPerk.getPerk().getIdentifier();
    }
    
    */
/**
     * Generates a unique key for toggle state tracking.
     *//*

    private String getToggleKey(final @NotNull RPlayerPerk playerPerk) {
        
        return playerPerk.getPlayer().getUniqueId() + ":" + playerPerk.getPerk().getIdentifier();
    }
    
    */
/**
     * Cleans up tracking data for a player (call when player disconnects).
     *
     * @param playerUniqueId the player UUID to clean up
     *//*

    public void cleanupPlayer(final @NotNull UUID playerUniqueId) {
        
        final String playerIdStr = playerUniqueId.toString();
        
        cooldownTracker.entrySet().removeIf(entry -> entry.getKey().startsWith(playerIdStr + ":"));
        activeSessionTracker.entrySet().removeIf(entry -> entry.getKey().startsWith(playerIdStr + ":"));
        toggleStateTracker.entrySet().removeIf(entry -> entry.getKey().startsWith(playerIdStr + ":"));
        
        managementService.cleanupPlayerData(playerUniqueId);
    }
    
    */
/**
     * Cleans up tracking data for a specific player-perk combination.
     *
     * @param playerPerk the player-perk association to clean up
     *//*

    public void cleanupPlayerPerk(final @NotNull RPlayerPerk playerPerk) {
        
        final String cooldownKey = getCooldownKey(playerPerk);
        final String sessionKey  = getSessionKey(playerPerk);
        final String toggleKey   = getToggleKey(playerPerk);
        
        cooldownTracker.remove(cooldownKey);
        activeSessionTracker.remove(sessionKey);
        toggleStateTracker.remove(toggleKey);
    }
    
}*/
