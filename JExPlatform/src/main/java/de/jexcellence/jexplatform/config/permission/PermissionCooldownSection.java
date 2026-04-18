package de.jexcellence.jexplatform.config.permission;

import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Permission-gated cooldown configuration section.
 *
 * <p>Resolves a cooldown duration (in seconds) for a player based on their
 * permissions. When multiple permissions match and best-value resolution is
 * active, the <strong>shortest</strong> cooldown wins — a zero value
 * (no cooldown) takes priority over any positive value.
 *
 * <pre>{@code
 * # YAML example
 * cooldown:
 *   enabled: true
 *   useBestValue: true
 *   defaultCooldownSeconds: 60
 *   permissionCooldowns:
 *     "myplugin.cooldown.vip": 30
 *     "myplugin.cooldown.bypass": 0
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
@CSAlways
public class PermissionCooldownSection extends PermissionBasedSection<Long> {

    private Long defaultCooldownSeconds;
    private Long defaultCooldown;
    private Map<String, Long> permissionCooldowns;

    /**
     * Creates a cooldown section with the provided evaluation environment.
     *
     * @param evaluationEnvironmentBuilder shared expression context
     */
    public PermissionCooldownSection(
            @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    // ── Domain accessors ──────────────────────────────────────────────────────

    /**
     * Returns the effective cooldown in seconds for the given player.
     *
     * <p>Convenience alias for {@link #getEffectiveValue(org.bukkit.entity.Player)}.
     *
     * @param player the player to evaluate (may be {@code null})
     * @return the resolved cooldown in seconds
     */
    public long getEffectiveCooldown(@Nullable org.bukkit.entity.Player player) {
        return getEffectiveValue(player);
    }

    /**
     * Returns the effective cooldown in milliseconds for the given player.
     *
     * @param player the player to evaluate (may be {@code null})
     * @return the resolved cooldown in milliseconds
     */
    public long getEffectiveCooldownMillis(@Nullable org.bukkit.entity.Player player) {
        return getEffectiveValue(player) * 1000L;
    }

    /**
     * Checks whether the player has a zero cooldown (bypass).
     *
     * @param player the player to check
     * @return {@code true} when the effective cooldown is zero
     */
    public boolean isCooldownBypassed(@Nullable org.bukkit.entity.Player player) {
        return getEffectiveValue(player) == 0L;
    }

    // ── Abstract hook implementations ─────────────────────────────────────────

    @Override
    protected @NotNull Long getDefaultValue() {
        if (defaultCooldownSeconds != null) {
            return defaultCooldownSeconds;
        }
        return defaultCooldown != null ? defaultCooldown : 0L;
    }

    @Override
    protected @NotNull Map<String, Long> getPermissionValues() {
        return permissionCooldowns != null ? permissionCooldowns : new HashMap<>();
    }

    @Override
    protected boolean getDefaultUseBestValue() {
        return true;
    }

    @Override
    protected @NotNull Long chooseBestValue(@NotNull Long current, @NotNull Long candidate) {
        if (candidate == 0L) {
            return 0L;
        }
        if (current == 0L) {
            return 0L;
        }
        return Math.min(current, candidate);
    }

    @Override
    protected boolean isBetterValue(@NotNull Long candidate, @NotNull Long current) {
        if (candidate == 0L) {
            return true;
        }
        if (current == 0L) {
            return false;
        }
        return candidate < current;
    }

    // ── Overridable hooks ─────────────────────────────────────────────────────

    @Override
    protected boolean isValidValue(@Nullable Long value) {
        return value != null && value >= 0L;
    }
}
