package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.perk.PerkSection;
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
 * Abstract base for perks that apply potion effects (toggleable passives).
 * Provides safe application/removal, amplifier/duration via permissions, and correct tick conversions.
 */
@MappedSuperclass
public abstract class PotionEffectPerk extends RPerk {

    @Transient
    private static final Logger LOGGER = Logger.getLogger(PotionEffectPerk.class.getName());

    /** Stored as string for flexible mapping across versions. */
    @Column(name = "potion_effect_type")
    private String potionEffectTypeName;

    /** Tracks whether the effect is currently applied to a player. */
    @Transient
    private final Map<UUID, Boolean> appliedEffects = new ConcurrentHashMap<>();

    /** Tracks amplifier per player for reapplication checks. */
    @Transient
    private final Map<UUID, Integer> appliedAmplifiers = new ConcurrentHashMap<>();

    protected PotionEffectPerk() {
        super();
    }

    protected PotionEffectPerk(
            final @NotNull String identifier,
            final @NotNull PerkSection perkSection,
            final @NotNull EPerkType perkType,
            final @NotNull String potionEffectTypeName
    ) {
        super(identifier, perkSection, perkType);
        this.potionEffectTypeName = potionEffectTypeName.toUpperCase();
    }

    public String getPotionEffectTypeName() {
        return this.potionEffectTypeName;
    }

    public void setPotionEffectTypeName(final @NotNull String potionEffectTypeName) {
        this.potionEffectTypeName = potionEffectTypeName;
    }

    @Transient
    protected @Nullable PotionEffectType getPotionEffectType() {
        if (this.potionEffectTypeName == null) return null;
        try {
            return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.fromString(this.potionEffectTypeName));
        } catch (final Exception exception) {
            LOGGER.warning("Invalid potion effect type name: " + this.potionEffectTypeName);
            return null;
        }
    }

    @Override
    public boolean performActivation() {
        // Global readiness; per-player toggling applies effects
        return this.isEnabled() && this.getPotionEffectType() != null;
    }

    @Override
    public boolean performDeactivation() {
        // No global cleanup; per-player removal handled externally
        return true;
    }

    @Override
    public boolean canPerformActivation() {
        return this.isEnabled() && this.getPotionEffectType() != null;
    }

    @Override
    public void performTrigger() {
        // Toggleable passives do not use trigger
    }

    /** Applies the effect to the given RDQ player. */
    public boolean applyEffectToPlayer(final @NotNull com.raindropcentral.rdq.database.entity.player.RDQPlayer player) {
        final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return false;

        final PotionEffectType effectType = this.getPotionEffectType();
        if (effectType == null) {
            LOGGER.warning("Cannot apply effect - invalid type: " + this.potionEffectTypeName);
            return false;
        }

        if (this.appliedEffects.getOrDefault(player.getUniqueId(), false)) return false;

        final int amplifier = this.getEffectiveAmplifier(bukkitPlayer);
        final long durationSeconds = this.getEffectiveDurationSeconds(bukkitPlayer);
        final int durationTicks = durationSeconds > 0
                ? (int) Math.min(durationSeconds * 20L, Integer.MAX_VALUE)
                : Integer.MAX_VALUE;

        final PotionEffect effect = new PotionEffect(effectType, durationTicks, amplifier, false, true, true);
        bukkitPlayer.addPotionEffect(effect);
        this.appliedEffects.put(player.getUniqueId(), true);
        this.appliedAmplifiers.put(player.getUniqueId(), amplifier);
        LOGGER.info("Applied " + this.getPotionEffectTypeName() + " effect to player " + player.getPlayerName() + " with amplifier " + amplifier + " (Level " + (amplifier + 1) + ")");
        return true;
    }

    /** Removes the effect from the given RDQ player. */
    public boolean removeEffectFromPlayer(final @NotNull com.raindropcentral.rdq.database.entity.player.RDQPlayer player) {
        final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        final boolean wasApplied = this.appliedEffects.getOrDefault(player.getUniqueId(), false);
        this.appliedEffects.put(player.getUniqueId(), false);
        this.appliedAmplifiers.remove(player.getUniqueId());

        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return wasApplied;
        }
        if (!wasApplied) return false;

        final PotionEffectType effectType = this.getPotionEffectType();
        if (effectType != null) {
            bukkitPlayer.removePotionEffect(effectType);
        }
        LOGGER.info("Removed " + this.getPotionEffectTypeName() + " effect from player " + player.getPlayerName());
        return true;
    }

    /** Updates the effect for amplifier changes. */
    public boolean updateEffectForPlayer(final @NotNull com.raindropcentral.rdq.database.entity.player.RDQPlayer player) {
        final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return false;
        if (!this.appliedEffects.getOrDefault(player.getUniqueId(), false)) return false;

        final int currentAmplifier = this.appliedAmplifiers.getOrDefault(player.getUniqueId(), 0);
        final int newAmplifier = this.getEffectiveAmplifier(bukkitPlayer);
        if (currentAmplifier == newAmplifier) return true;

        this.removeEffectFromPlayer(player);
        return this.applyEffectToPlayer(player);
    }

    public int getEffectiveAmplifier(final @NotNull Player player) {
        return this.getPerkSection().getPermissionAmplifiers().getEffectiveAmplifier(player);
    }

    public long getEffectiveDurationSeconds(final @NotNull Player player) {
        return this.getPerkSection().getPermissionDurations().getEffectiveDuration(player);
    }
}
