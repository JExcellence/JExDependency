package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Requirement that checks if a player has specific permissions.
 * <p>
 * The {@code PermissionRequirement} is satisfied when the player has all the specified
 * permissions. This requirement is useful for rank-based systems where higher ranks
 * automatically get permissions that allow them to bypass or meet certain requirements.
 * When consumed, this requirement is a no-op since permissions are not consumed.
 * </p>
 *
 * <ul>
 *   <li>Supports both single permission and multiple permissions checking.</li>
 *   <li>All specified permissions must be present for the requirement to be met.</li>
 *   <li>Progress is calculated as the ratio of permissions held to permissions required.</li>
 *   <li>Consumption is not applicable (permissions are not consumed).</li>
 *   <li>Integrates with RequirementSection for flexible configuration.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class PermissionRequirement extends AbstractRequirement {

    /**
     * Enumeration of permission checking modes.
     */
    public enum PermissionMode {
        /**
         * All permissions must be present (default).
         */
        ALL,
        
        /**
         * At least one permission must be present.
         */
        ANY,
        
        /**
         * At least N permissions must be present (where N is specified by minimumRequired).
         */
        MINIMUM
    }

    /**
     * The list of required permissions.
     */
    @JsonProperty("requiredPermissions")
    private final List<String> requiredPermissions;

    /**
     * The permission checking mode.
     */
    @JsonProperty("permissionMode")
    private final PermissionMode permissionMode;

    /**
     * The minimum number of permissions required when using MINIMUM mode.
     */
    @JsonProperty("minimumRequired")
    private final int minimumRequired;

    /**
     * Optional description for this permission requirement.
     */
    @JsonProperty("description")
    private final String description;

    /**
     * Whether to check for negated permissions (permissions the player should NOT have).
     */
    @JsonProperty("checkNegated")
    private final boolean checkNegated;

    /**
     * Constructs a {@code PermissionRequirement} with a single permission.
     *
     * @param permission The required permission.
     */
    public PermissionRequirement(
            @NotNull final String permission
    ) {
        this(List.of(permission), PermissionMode.ALL, 1, null, false);
    }

    /**
     * Constructs a {@code PermissionRequirement} with multiple permissions.
     *
     * @param requiredPermissions A list of required permissions.
     */
    public PermissionRequirement(
            @NotNull final List<String> requiredPermissions
    ) {
        this(requiredPermissions, PermissionMode.ALL, requiredPermissions.size(), null, false);
    }

    /**
     * Constructs a {@code PermissionRequirement} with specific mode.
     *
     * @param requiredPermissions A list of required permissions.
     * @param permissionMode The permission checking mode.
     */
    public PermissionRequirement(
            @NotNull final List<String> requiredPermissions,
            @NotNull final PermissionMode permissionMode
    ) {
        this(requiredPermissions, permissionMode, 
             permissionMode == PermissionMode.ANY ? 1 : requiredPermissions.size(), 
             null, false);
    }

    /**
     * Constructs a {@code PermissionRequirement} with full configuration options.
     *
     * @param requiredPermissions A list of required permissions.
     * @param permissionMode The permission checking mode.
     * @param minimumRequired The minimum number of permissions required (for MINIMUM mode).
     * @param description Optional description for this requirement.
     * @param checkNegated Whether to check for negated permissions.
     */
    @JsonCreator
    public PermissionRequirement(
            @JsonProperty("requiredPermissions") @NotNull final List<String> requiredPermissions,
            @JsonProperty("permissionMode") @Nullable final PermissionMode permissionMode,
            @JsonProperty("minimumRequired") @Nullable final Integer minimumRequired,
            @JsonProperty("description") @Nullable final String description,
            @JsonProperty("checkNegated") @Nullable final Boolean checkNegated
    ) {
        super(Type.PERMISSION);

        if (requiredPermissions.isEmpty()) {
            throw new IllegalArgumentException("At least one permission must be specified.");
        }

        // Validate permissions
        for (final String permission : requiredPermissions) {
            if (permission == null || permission.trim().isEmpty()) {
                throw new IllegalArgumentException("Permission cannot be null or empty.");
            }
        }

        final PermissionMode mode = permissionMode != null ? permissionMode : PermissionMode.ALL;
        final int minRequired = minimumRequired != null ? minimumRequired : 
                               (mode == PermissionMode.ANY ? 1 : requiredPermissions.size());

        if (minRequired < 1) {
            throw new IllegalArgumentException("Minimum required must be at least 1.");
        }

        if (minRequired > requiredPermissions.size()) {
            throw new IllegalArgumentException(
                "Minimum required (" + minRequired + ") cannot exceed total permissions (" + requiredPermissions.size() + ")."
            );
        }

        this.requiredPermissions = new ArrayList<>(requiredPermissions);
        this.permissionMode = mode;
        this.minimumRequired = minRequired;
        this.description = description;
        this.checkNegated = checkNegated != null ? checkNegated : false;
    }

    /**
     * Checks if the player has the required permissions based on the configured mode.
     *
     * @param player The player whose permissions will be checked.
     * @return {@code true} if the player meets the permission requirements, {@code false} otherwise.
     */
    @Override
    public boolean isMet(
            @NotNull final Player player
    ) {
        return switch (this.permissionMode) {
            case ALL -> this.requiredPermissions.stream()
                    .allMatch(permission -> this.checkPermission(player, permission));
            case ANY -> this.requiredPermissions.stream()
                    .anyMatch(permission -> this.checkPermission(player, permission));
            case MINIMUM -> {
                final long matchingPermissions = this.requiredPermissions.stream()
                        .mapToLong(permission -> this.checkPermission(player, permission) ? 1 : 0)
                        .sum();
                yield matchingPermissions >= this.minimumRequired;
            }
        };
    }

    /**
     * Calculates the progress towards fulfilling the permission requirement.
     * <p>
     * Progress is calculated based on the permission mode:
     * <ul>
     *   <li><b>ALL:</b> Ratio of permissions held to total permissions required.</li>
     *   <li><b>ANY:</b> 1.0 if any permission is held, 0.0 otherwise.</li>
     *   <li><b>MINIMUM:</b> Ratio of permissions held to minimum required.</li>
     * </ul>
     * </p>
     *
     * @param player The player whose permissions will be evaluated.
     * @return A double between 0.0 and 1.0 representing progress.
     */
    @Override
    public double calculateProgress(
            @NotNull final Player player
    ) {
        if (this.requiredPermissions.isEmpty()) {
            return 1.0;
        }

        final long heldPermissions = this.requiredPermissions.stream()
                .mapToLong(permission -> this.checkPermission(player, permission) ? 1 : 0)
                .sum();

        return switch (this.permissionMode) {
            case ALL -> Math.min(1.0, (double) heldPermissions / this.requiredPermissions.size());
            case ANY -> heldPermissions > 0 ? 1.0 : 0.0;
            case MINIMUM -> Math.min(1.0, (double) heldPermissions / this.minimumRequired);
        };
    }

    /**
     * Consumes resources from the player to fulfill this requirement.
     * <p>
     * Not applicable for permission requirements; this method is a no-op.
     * </p>
     *
     * @param player The player from whom resources would be consumed.
     */
    @Override
    public void consume(
            @NotNull final Player player
    ) {
        // Permissions are not consumed
    }

    /**
     * Returns the translation key for this requirement's description.
     * <p>
     * This key can be used for localization and user-facing descriptions.
     * </p>
     *
     * @return The language key for this requirement's description.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.permission";
    }

    /**
     * Returns a defensive copy of the required permissions list.
     *
     * @return A new {@link List} containing the required permissions.
     */
    @NotNull
    public List<String> getRequiredPermissions() {
        return new ArrayList<>(this.requiredPermissions);
    }

    /**
     * Gets the permission checking mode.
     *
     * @return The permission mode.
     */
    @NotNull
    public PermissionMode getPermissionMode() {
        return this.permissionMode;
    }

    /**
     * Gets the minimum number of permissions required.
     *
     * @return The minimum required count.
     */
    public int getMinimumRequired() {
        return this.minimumRequired;
    }

    /**
     * Gets the optional description for this permission requirement.
     *
     * @return The description, or null if not provided.
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets whether negated permissions are checked.
     *
     * @return True if checking for negated permissions, false otherwise.
     */
    public boolean isCheckNegated() {
        return this.checkNegated;
    }

    /**
     * Gets detailed permission information for each required permission for the specified player.
     *
     * @param player The player whose permissions will be checked.
     * @return A list of {@link PermissionStatus} objects containing detailed permission information.
     */
    @JsonIgnore
    @NotNull
    public List<PermissionStatus> getDetailedPermissionStatus(
            @NotNull final Player player
    ) {
        return IntStream.range(0, this.requiredPermissions.size())
                .mapToObj(index -> {
                    final String permission = this.requiredPermissions.get(index);
                    final boolean hasPermission = this.checkPermission(player, permission);
                    return new PermissionStatus(index, permission, hasPermission);
                })
                .toList();
    }

    /**
     * Gets the permissions that the player currently has.
     *
     * @param player The player whose held permissions will be retrieved.
     * @return A list of permissions the player has from the required list.
     */
    @JsonIgnore
    @NotNull
    public List<String> getHeldPermissions(
            @NotNull final Player player
    ) {
        return this.requiredPermissions.stream()
                .filter(permission -> this.checkPermission(player, permission))
                .toList();
    }

    /**
     * Gets the permissions that the player is missing.
     *
     * @param player The player whose missing permissions will be calculated.
     * @return A list of permissions the player is missing from the required list.
     */
    @JsonIgnore
    @NotNull
    public List<String> getMissingPermissions(
            @NotNull final Player player
    ) {
        return this.requiredPermissions.stream()
                .filter(permission -> !this.checkPermission(player, permission))
                .toList();
    }

    /**
     * Checks if this requirement uses ALL mode (all permissions must be held).
     *
     * @return True if using ALL mode, false otherwise.
     */
    @JsonIgnore
    public boolean isAllMode() {
        return this.permissionMode == PermissionMode.ALL;
    }

    /**
     * Checks if this requirement uses ANY mode (at least one permission must be held).
     *
     * @return True if using ANY mode, false otherwise.
     */
    @JsonIgnore
    public boolean isAnyMode() {
        return this.permissionMode == PermissionMode.ANY;
    }

    /**
     * Checks if this requirement uses MINIMUM mode (at least N permissions must be held).
     *
     * @return True if using MINIMUM mode, false otherwise.
     */
    @JsonIgnore
    public boolean isMinimumMode() {
        return this.permissionMode == PermissionMode.MINIMUM;
    }

    /**
     * Validates the internal state of this permission requirement.
     *
     * @throws IllegalStateException If the requirement is in an invalid state.
     */
    @JsonIgnore
    public void validate() {
        if (this.requiredPermissions.isEmpty()) {
            throw new IllegalStateException("PermissionRequirement must have at least one permission.");
        }

        if (this.minimumRequired < 1 || this.minimumRequired > this.requiredPermissions.size()) {
            throw new IllegalStateException(
                "Invalid minimumRequired: " + this.minimumRequired + 
                " (must be between 1 and " + this.requiredPermissions.size() + ")."
            );
        }

        // Validate each permission
        for (int i = 0; i < this.requiredPermissions.size(); i++) {
            final String permission = this.requiredPermissions.get(i);
            if (permission == null || permission.trim().isEmpty()) {
                throw new IllegalStateException("Permission at index " + i + " is null or empty.");
            }
        }
    }

    /**
     * Creates a PermissionRequirement from a string permission mode.
     * Useful for configuration parsing.
     *
     * @param requiredPermissions The list of required permissions.
     * @param modeString The mode as a string ("ALL", "ANY", "MINIMUM").
     * @param minimumRequired The minimum required count (for MINIMUM mode).
     * @return A new PermissionRequirement instance.
     * @throws IllegalArgumentException If the mode string is invalid.
     */
    @JsonIgnore
    @NotNull
    public static PermissionRequirement fromString(
            @NotNull final List<String> requiredPermissions,
            @NotNull final String modeString,
            final int minimumRequired
    ) {
        final PermissionMode mode;
        try {
            mode = PermissionMode.valueOf(modeString.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid permission mode: " + modeString + ". Valid modes are: ALL, ANY, MINIMUM.");
        }

        return new PermissionRequirement(requiredPermissions, mode, minimumRequired, null, false);
    }

    /**
     * Checks if a player has a specific permission, considering negation.
     *
     * @param player The player to check.
     * @param permission The permission to check.
     * @return True if the permission check passes, false otherwise.
     */
    private boolean checkPermission(
            @NotNull final Player player,
            @NotNull final String permission
    ) {
        final boolean hasPermission = player.hasPermission(permission);
        return this.checkNegated != hasPermission;
    }

    /**
     * Represents detailed permission status information for a single permission.
     */
    public static class PermissionStatus {
        private final int index;
        private final String permission;
        private final boolean hasPermission;

        /**
         * Constructs a new PermissionStatus instance.
         *
         * @param index The index of the permission in the requirements list.
         * @param permission The permission string.
         * @param hasPermission Whether the player has this permission.
         */
        public PermissionStatus(
                final int index,
                @NotNull final String permission,
                final boolean hasPermission
        ) {
            this.index = index;
            this.permission = permission;
            this.hasPermission = hasPermission;
        }

        /**
         * Gets the index of this permission in the requirements list.
         *
         * @return The permission index.
         */
        public int getIndex() {
            return this.index;
        }

        /**
         * Gets the permission string.
         *
         * @return The permission.
         */
        @NotNull
        public String getPermission() {
            return this.permission;
        }

        /**
         * Gets whether the player has this permission.
         *
         * @return True if the player has the permission, false otherwise.
         */
        public boolean hasPermission() {
            return this.hasPermission;
        }
    }
}