package de.jexcellence.quests.perk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Intent blob parsed from a perk YAML's {@code behaviour:} section —
 * the runtime listener reads these fields to decide what to apply at
 * tick / event time. Every field is optional; a perk that declares
 * only {@code specialType: FLIGHT} ignores the potion-effect path,
 * and one that declares only {@code cancelDamageCause: FALL} ignores
 * the scheduled refresh loop entirely.
 *
 * @param effect Bukkit {@code PotionEffectType} name (e.g. {@code SPEED},
 *               {@code HASTE}) — {@code null} means no potion effect
 * @param amplifier potion amplifier (0 = level I); ignored when effect is null
 * @param durationTicks how long each applied potion effect lasts
 * @param refreshEveryTicks how often the scheduler re-applies it;
 *                          should be &lt;= durationTicks to avoid gaps
 * @param ambient / particles cosmetic flags passed straight to PotionEffect
 * @param specialType runtime-level hook. Known values:
 *                    {@code FLIGHT} — setAllowFlight(true) while active;
 *                    {@code KEEP_INVENTORY} — suppresses death-drop
 * @param cancelDamageCause Bukkit {@code DamageCause} name to null out
 *                          on owned players (e.g. {@code FALL}); {@code null} = no op
 * @param multiplyXp multiplier to apply to {@code PlayerExpChangeEvent}
 *                   for the duration-ticks window after activation; zero/unset = off
 */
public record PerkBehaviour(
        @Nullable String effect,
        int amplifier,
        int durationTicks,
        int refreshEveryTicks,
        boolean ambient,
        boolean particles,
        @Nullable String specialType,
        @Nullable String cancelDamageCause,
        double multiplyXp
) {

    public static final @NotNull PerkBehaviour EMPTY = new PerkBehaviour(
            null, 0, 0, 0, false, false, null, null, 0.0);

    public boolean hasPotionEffect() {
        return this.effect != null && !this.effect.isBlank() && this.durationTicks > 0;
    }

    public boolean isFlight() {
        return "FLIGHT".equalsIgnoreCase(this.specialType);
    }

    public boolean isKeepInventory() {
        return "KEEP_INVENTORY".equalsIgnoreCase(this.specialType);
    }
}
