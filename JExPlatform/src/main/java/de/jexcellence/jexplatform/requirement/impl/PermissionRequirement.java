package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Requires the player to hold one or more permissions.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class PermissionRequirement extends AbstractRequirement {

    /** How multiple permissions are evaluated. */
    public enum PermissionMode { ALL, ANY }

    /** Status of a single permission check. */
    public record PermissionStatus(@NotNull String permission, boolean held) {
    }

    @JsonProperty("permissions")
    private final List<String> permissions;

    @JsonProperty("mode")
    private final PermissionMode mode;

    @JsonProperty("negated")
    private final boolean negated;

    /**
     * Creates a permission requirement.
     *
     * @param permissions the required permissions
     * @param mode        the evaluation mode
     * @param negated     whether to invert the check
     */
    public PermissionRequirement(@JsonProperty("permissions") @NotNull List<String> permissions,
                                 @JsonProperty("mode") PermissionMode mode,
                                 @JsonProperty("negated") boolean negated) {
        super("PERMISSION");
        this.permissions = List.copyOf(permissions);
        this.mode = mode != null ? mode : PermissionMode.ALL;
        this.negated = negated;
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        var result = switch (mode) {
            case ALL -> permissions.stream().allMatch(player::hasPermission);
            case ANY -> permissions.stream().anyMatch(player::hasPermission);
        };
        return negated != result;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (permissions.isEmpty()) {
            return 1.0;
        }
        var held = permissions.stream().filter(player::hasPermission).count();
        return (double) held / permissions.size();
    }

    @Override
    public void consume(@NotNull Player player) {
        // Permissions are not consumable
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.permission";
    }

    /**
     * Returns detailed status for each permission.
     *
     * @param player the player
     * @return list of permission statuses
     */
    public @NotNull List<PermissionStatus> getDetailedStatus(@NotNull Player player) {
        return permissions.stream()
                .map(perm -> new PermissionStatus(perm, player.hasPermission(perm)))
                .toList();
    }
}
