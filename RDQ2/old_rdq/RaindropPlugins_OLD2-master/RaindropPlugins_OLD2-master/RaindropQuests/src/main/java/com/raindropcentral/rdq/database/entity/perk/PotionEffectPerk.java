package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Abstract base class for perks that apply potion effects.
 * <p>
 * This class provides common functionality for all potion effect-based perks,
 * including effect application, removal, tracking, and permission-based
 * amplifier/duration handling. The potion effect type is stored as a string
 * in the database for flexibility and JPA compatibility.
 * </p>
 * <p>
 * All concrete potion effect perks will be stored in the same table as other perks
 * using single table inheritance with discriminator values.
 * </p>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since TBD
 */
@MappedSuperclass
public abstract class PotionEffectPerk extends RPerk {
    
    @Transient
    private static final Logger LOGGER = Logger.getLogger(PotionEffectPerk.class.getName());
    
    /**
     * The name of the potion effect type to apply.
     * This is stored as a string for JPA compatibility and flexibility.
     */
    @Column(
        name = "potion_effect_type",
        nullable = false
    )
    private String potionEffectTypeName;
    
    /**
     * Tracks which players currently have the potion effect applied.
     * This is used to prevent duplicate applications and for cleanup.
     */
    @Transient
    private final Map<UUID, Boolean> appliedEffects = new ConcurrentHashMap<>();
    
    /**
     * Tracks the current amplifier level applied to each player.
     * This helps with proper cleanup when amplifiers change.
     */
    @Transient
    private final Map<UUID, Integer> appliedAmplifiers = new ConcurrentHashMap<>();
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected PotionEffectPerk() {
        
        super();
    }
    
    /**
     * Constructs a new PotionEffectPerk.
     *
     * @param identifier           the unique identifier for this perk
     * @param perkSection          the perk configuration section
     * @param perkType             the type of this perk
     * @param potionEffectTypeName the name of the potion effect type to apply
     */
    protected PotionEffectPerk(
        final @NotNull String identifier,
        final @NotNull PerkSection perkSection,
        final @NotNull EPerkType perkType,
        final @NotNull String potionEffectTypeName
    ) {
        
        super(
            identifier,
            perkSection,
            perkType
        );
        this.potionEffectTypeName = potionEffectTypeName.toUpperCase();
    }
    
    /**
     * Gets the potion effect type name stored in the database.
     *
     * @return the potion effect type name
     */
    public String getPotionEffectTypeName() {
        
        return this.potionEffectTypeName;
    }
    
    /**
     * Sets the potion effect type name.
     *
     * @param potionEffectTypeName the potion effect type name to set
     */
    public void setPotionEffectTypeName(final @NotNull String potionEffectTypeName) {
        
        this.potionEffectTypeName = potionEffectTypeName;
    }
    
    /**
     * Gets the actual PotionEffectType from the stored name.
     * This method handles the conversion from string to PotionEffectType.
     *
     * @return the PotionEffectType, or null if the name is invalid
     */
    @Transient
    protected @Nullable PotionEffectType getPotionEffectType() {
        
        if (this.potionEffectTypeName == null) {
            return null;
        }
        
        try {
            return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.fromString(this.potionEffectTypeName));
        } catch (final Exception exception) {
            LOGGER.warning("Invalid potion effect type name: " + this.potionEffectTypeName);
            return null;
        }
    }
    
    @Override
    public boolean performActivation() {
        // For toggleable perks, the actual player-specific activation
        // is handled by the activation service. This method just validates
        // that the perk can be activated in general.
        return this.isEnabled() && this.getPotionEffectType() != null;
    }
    
    @Override
    public boolean performDeactivation() {
        // For toggleable perks, the actual player-specific deactivation
        // is handled by the activation service. This method just validates
        // that the perk can be deactivated in general.
        return true;
    }
    
    @Override
    public boolean canPerformActivation() {
        
        return this.isEnabled() && this.getPotionEffectType() != null;
    }
    
    @Override
    public void performTrigger() {
        // Toggleable passive perks don't use the trigger mechanism
        // The effect is applied/removed through activation/deactivation
    }
    
    /**
     * Applies the potion effect to a specific player.
     * This method is called by the activation service when a player activates the perk.
     *
     * @param player the player to apply the effect to
     *
     * @return true if the effect was applied successfully
     */
    public boolean applyEffectToPlayer(final @NotNull RDQPlayer player) {
        
        final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        if (bukkitPlayer == null || ! bukkitPlayer.isOnline()) {
            return false;
        }
        
        final PotionEffectType effectType = this.getPotionEffectType();
        if (effectType == null) {
            LOGGER.warning("Cannot apply effect - invalid potion effect type: " + this.potionEffectTypeName);
            return false;
        }
        
        if (this.appliedEffects.getOrDefault(
            player.getUniqueId(),
            false
        )) {
            return false;
        }
        
        final int  amplifier         = this.getEffectiveAmplifier(bukkitPlayer);
        final long durationInSeconds = this.getEffectiveDurationSeconds(bukkitPlayer);
        
        final int duration = durationInSeconds > 0 ?
                             (int) Math.min(
                                 durationInSeconds * 20L,
                                 Integer.MAX_VALUE
                             ) :
                             Integer.MAX_VALUE;
        
        final PotionEffect effect = new PotionEffect(
            effectType,
            duration,
            amplifier,
            false,
            true,
            true
        );
        
        bukkitPlayer.addPotionEffect(effect);
        
        this.appliedEffects.put(
            player.getUniqueId(),
            true
        );
        this.appliedAmplifiers.put(
            player.getUniqueId(),
            amplifier
        );
        
        LOGGER.info(
            "Applied " + this.getPotionEffectTypeName() + " effect to player " +
            player.getPlayerName() + " with amplifier " + amplifier +
            " (Level " + (amplifier + 1) + ")"
        );
        
        return true;
    }
    
    /**
     * Removes the potion effect from a specific player.
     * This method is called by the activation service when a player deactivates the perk.
     *
     * @param player the player to remove the effect from
     *
     * @return true if the effect was removed successfully
     */
    public boolean removeEffectFromPlayer(final @NotNull RDQPlayer player) {
        
        final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        
        final boolean wasApplied = this.appliedEffects.getOrDefault(
            player.getUniqueId(),
            false
        );
        this.appliedEffects.put(
            player.getUniqueId(),
            false
        );
        this.appliedAmplifiers.remove(player.getUniqueId());
        
        if (bukkitPlayer == null || ! bukkitPlayer.isOnline()) {
            return wasApplied;
        }
        
        if (! wasApplied) {
            return false;
        }
        
        final PotionEffectType effectType = this.getPotionEffectType();
        if (effectType != null) {
            bukkitPlayer.removePotionEffect(effectType);
        }
        
        LOGGER.info("Removed " + this.getPotionEffectTypeName() + " effect from player " + player.getPlayerName());
        
        return true;
    }
    
    /**
     * Updates the potion effect for a player if their permissions have changed.
     * This allows for dynamic amplifier changes based on permission updates.
     *
     * @param player the player to update the effect for
     *
     * @return true if the effect was updated successfully
     */
    public boolean updateEffectFoRDQPlayer(final @NotNull RDQPlayer player) {
        
        final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        if (bukkitPlayer == null || ! bukkitPlayer.isOnline()) {
            return false;
        }
        
        if (! this.appliedEffects.getOrDefault(
            player.getUniqueId(),
            false
        )) {
            return false;
        }
        
        final int currentAmplifier = this.appliedAmplifiers.getOrDefault(
            player.getUniqueId(),
            0
        );
        final int newAmplifier     = this.getEffectiveAmplifier(bukkitPlayer);
        
        if (currentAmplifier == newAmplifier) {
            return true;
        }
        
        this.removeEffectFromPlayer(player);
        return this.applyEffectToPlayer(player);
    }
    
    /**
     * Gets the effective amplifier for this perk for a specific player.
     * This integrates with the permission-based amplifier system.
     *
     * @param player the Bukkit player to get the amplifier for
     *
     * @return the effective amplifier level
     */
    public int getEffectiveAmplifier(final @NotNull Player player) {
        
        return this.getPerkSection().getPermissionAmplifiers().getEffectiveAmplifier(player);
    }
    
    /**
     * Gets the effective duration for this perk in seconds.
     * Returns the database value if set, otherwise falls back to the default.
     *
     * @param player the Bukkit player to get the duration for
     *
     * @return the duration in seconds, or 0 for permanent effects
     */
    public long getEffectiveDurationSeconds(final @NotNull Player player) {
        
        return this.getPerkSection().getPermissionDurations().getEffectiveDuration(player);
    }
    
}