package de.jexcellence.jexplatform.config.permission;

import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Permission-gated amplifier configuration section.
 *
 * <p>Resolves an integer amplifier value for a player based on their permissions.
 * When multiple permissions match and best-value resolution is active, the
 * <strong>highest</strong> amplifier wins. Optional min/max bounds clamp the
 * resolved value.
 *
 * <pre>{@code
 * # YAML example
 * amplifier:
 *   enabled: true
 *   useBestValue: true
 *   defaultAmplifier: 1
 *   maxAmplifier: 10
 *   minAmplifier: 0
 *   permissionAmplifiers:
 *     "myplugin.amplifier.vip": 3
 *     "myplugin.amplifier.elite": 5
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
@CSAlways
public class PermissionAmplifierSection extends PermissionBasedSection<Integer> {

    private Integer defaultAmplifier;
    private Map<String, Integer> permissionAmplifiers;
    private Integer maxAmplifier;
    private Integer minAmplifier;

    /**
     * Creates an amplifier section with the provided evaluation environment.
     *
     * @param evaluationEnvironmentBuilder shared expression context
     */
    public PermissionAmplifierSection(
            @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    // ── Domain accessors ──────────────────────────────────────────────────────

    /**
     * Returns the effective amplifier for the given player.
     *
     * <p>Convenience alias for {@link #getEffectiveValue(org.bukkit.entity.Player)}.
     *
     * @param player the player to evaluate (may be {@code null})
     * @return the resolved amplifier value
     */
    public int getEffectiveAmplifier(@Nullable org.bukkit.entity.Player player) {
        return getEffectiveValue(player);
    }

    /**
     * Returns the configured maximum amplifier, or {@code null} when unbounded.
     *
     * @return the upper bound, or {@code null}
     */
    public @Nullable Integer getMaxAmplifier() {
        return maxAmplifier;
    }

    /**
     * Returns the configured minimum amplifier, or {@code null} when unbounded.
     *
     * @return the lower bound, or {@code null}
     */
    public @Nullable Integer getMinAmplifier() {
        return minAmplifier;
    }

    /**
     * Checks whether an amplifier value falls within the configured bounds.
     *
     * @param amplifier the value to check
     * @return {@code true} when the value is within bounds (or no bounds are set)
     */
    public boolean isAmplifierWithinBounds(int amplifier) {
        if (minAmplifier != null && amplifier < minAmplifier) {
            return false;
        }
        return maxAmplifier == null || amplifier <= maxAmplifier;
    }

    // ── Abstract hook implementations ─────────────────────────────────────────

    @Override
    protected @NotNull Integer getDefaultValue() {
        return defaultAmplifier != null ? defaultAmplifier : 1;
    }

    @Override
    protected @NotNull Map<String, Integer> getPermissionValues() {
        return permissionAmplifiers != null ? permissionAmplifiers : new HashMap<>();
    }

    @Override
    protected boolean getDefaultUseBestValue() {
        return true;
    }

    @Override
    protected @NotNull Integer chooseBestValue(@NotNull Integer current,
                                               @NotNull Integer candidate) {
        return Math.max(current, candidate);
    }

    @Override
    protected boolean isBetterValue(@NotNull Integer candidate,
                                    @NotNull Integer current) {
        return candidate > current;
    }

    // ── Overridable hooks ─────────────────────────────────────────────────────

    @Override
    protected @NotNull Integer applyBounds(@NotNull Integer value) {
        var result = value;
        if (minAmplifier != null && result < minAmplifier) {
            result = minAmplifier;
        }
        if (maxAmplifier != null && result > maxAmplifier) {
            result = maxAmplifier;
        }
        return result;
    }

    @Override
    protected boolean isValidValue(@Nullable Integer value) {
        return value != null;
    }

    @Override
    protected void performAdditionalValidation() {
        if (minAmplifier != null && maxAmplifier != null && minAmplifier > maxAmplifier) {
            throw new IllegalStateException(
                    "minAmplifier (" + minAmplifier + ") cannot exceed maxAmplifier ("
                            + maxAmplifier + ")");
        }
    }
}
