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
 * The {@code PermissionRequirement} is satisfied when the player meets the configured
 * permission criteria (all, any, or a minimum count). This requirement is useful for
 * rank-based systems where higher ranks automatically get permissions that allow them to
 * bypass or meet certain requirements.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public final class PermissionRequirement extends AbstractRequirement {

    /**
     * Determines how the required permissions must be satisfied.
     */
    public enum PermissionMode {
        ALL,
        ANY,
        MINIMUM
    }

    @JsonProperty("requiredPermissions")
    private final List<String> requiredPermissions;

    @JsonProperty("permissionMode")
    private final PermissionMode permissionMode;

    @JsonProperty("minimumRequired")
    private final int minimumRequired;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("checkNegated")
    private final boolean checkNegated;

    /**
     * Creates a requirement that must match a single permission.
     *
     * @param permission the permission that must be held by the player
     */
    public PermissionRequirement(final @NotNull String permission) {
        this(List.of(permission), PermissionMode.ALL, 1, null, false);
    }

    /**
     * Creates a requirement that must match all provided permissions.
     *
     * @param requiredPermissions the permissions the player must hold
     */
    public PermissionRequirement(final @NotNull List<String> requiredPermissions) {
        this(requiredPermissions, PermissionMode.ALL, requiredPermissions.size(), null, false);
    }

    /**
     * Creates a requirement using the provided permission evaluation mode.
     *
     * @param requiredPermissions the permissions relevant to the requirement
     * @param permissionMode      how the permissions must be satisfied
     */
    public PermissionRequirement(
            final @NotNull List<String> requiredPermissions,
            final @NotNull PermissionMode permissionMode
    ) {
        this(requiredPermissions, permissionMode,
                permissionMode == PermissionMode.ANY ? 1 : requiredPermissions.size(), null, false);
    }

    /**
     * Creates a requirement from a JSON payload.
     *
     * @param requiredPermissions the permissions the player must satisfy
     * @param permissionMode      the evaluation mode used when checking the permissions
     * @param minimumRequired     the minimum number of permissions required when using
     *                            {@link PermissionMode#MINIMUM}
     * @param description         optional description for UI or logging contexts
     * @param checkNegated        whether the permission check should be negated
     */
    @JsonCreator
    public PermissionRequirement(
            @JsonProperty("requiredPermissions") final @NotNull List<String> requiredPermissions,
            @JsonProperty("permissionMode") final @Nullable PermissionMode permissionMode,
            @JsonProperty("minimumRequired") final @Nullable Integer minimumRequired,
            @JsonProperty("description") final @Nullable String description,
            @JsonProperty("checkNegated") final @Nullable Boolean checkNegated
    ) {
        super(Type.PERMISSION);

        if (requiredPermissions.isEmpty()) {
            throw new IllegalArgumentException("At least one permission must be specified.");
        }

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
                    "Minimum required (" + minRequired + ") cannot exceed total permissions (" +
                    requiredPermissions.size() + ")."
            );
        }

        this.requiredPermissions = new ArrayList<>(requiredPermissions);
        this.permissionMode = mode;
        this.minimumRequired = minRequired;
        this.description = description;
        this.checkNegated = checkNegated != null ? checkNegated : false;
    }

    /**
     * Evaluates whether the player satisfies the requirement.
     *
     * @param player the player being evaluated
     * @return {@code true} if the player meets the configured permission mode; otherwise {@code false}
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        return switch (this.permissionMode) {
            case ALL -> this.requiredPermissions.stream()
                    .allMatch(permission -> this.checkPermission(player, permission));
            case ANY -> this.requiredPermissions.stream()
                    .anyMatch(permission -> this.checkPermission(player, permission));
            case MINIMUM -> {
                final long matchingPermissions = this.requiredPermissions.stream()
                        .filter(permission -> this.checkPermission(player, permission))
                        .count();
                yield matchingPermissions >= this.minimumRequired;
            }
        };
    }

    /**
     * Calculates progress toward fulfilling the requirement for the provided player.
     *
     * @param player the player being evaluated
     * @return the completion ratio, ranging between {@code 0.0} and {@code 1.0}
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.requiredPermissions.isEmpty()) {
            return 1.0;
        }

        final long heldPermissions = this.requiredPermissions.stream()
                .filter(permission -> this.checkPermission(player, permission))
                .count();

        return switch (this.permissionMode) {
            case ALL -> Math.min(1.0, (double) heldPermissions / this.requiredPermissions.size());
            case ANY -> heldPermissions > 0 ? 1.0 : 0.0;
            case MINIMUM -> Math.min(1.0, (double) heldPermissions / this.minimumRequired);
        };
    }

    /**
     * Permission requirements do not consume any state and therefore this method is a no-op.
     *
     * @param player the player being evaluated
     */
    @Override
    public void consume(final @NotNull Player player) {
    }

    /**
     * Provides the translation key for describing this requirement.
     *
     * @return the translation key used for localized descriptions
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.permission";
    }

    /**
     * Retrieves the permissions required by this requirement.
     *
     * @return a copy of the required permissions list
     */
    @NotNull
    public List<String> getRequiredPermissions() {
        return new ArrayList<>(this.requiredPermissions);
    }

    /**
     * Retrieves the mode controlling how permissions are evaluated.
     *
     * @return the configured {@link PermissionMode}
     */
    @NotNull
    public PermissionMode getPermissionMode() {
        return this.permissionMode;
    }

    /**
     * Retrieves the minimum number of permissions a player must have when using
     * {@link PermissionMode#MINIMUM}.
     *
     * @return the minimum required permission count
     */
    public int getMinimumRequired() {
        return this.minimumRequired;
    }

    /**
     * Retrieves the optional description associated with this requirement.
     *
     * @return the optional description, or {@code null} if not provided
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Indicates whether the permission check result should be negated.
     *
     * @return {@code true} if permission results are negated; otherwise {@code false}
     */
    public boolean isCheckNegated() {
        return this.checkNegated;
    }

    /**
     * Builds a detailed status breakdown for each required permission.
     *
     * @param player the player whose permissions are evaluated
     * @return a list containing the permission status entries in declaration order
     */
    @JsonIgnore
    @NotNull
    public List<PermissionStatus> getDetailedPermissionStatus(final @NotNull Player player) {
        return IntStream.range(0, this.requiredPermissions.size())
                .mapToObj(index -> {
                    final String permission = this.requiredPermissions.get(index);
                    final boolean hasPermission = this.checkPermission(player, permission);
                    return new PermissionStatus(index, permission, hasPermission);
                })
                .toList();
    }

    /**
     * Retrieves the subset of required permissions the player currently holds.
     *
     * @param player the player whose permissions are being inspected
     * @return the permissions possessed by the player that satisfy this requirement
     */
    @JsonIgnore
    @NotNull
    public List<String> getHeldPermissions(final @NotNull Player player) {
        return this.requiredPermissions.stream()
                .filter(permission -> this.checkPermission(player, permission))
                .toList();
    }

    /**
     * Retrieves the subset of required permissions the player is missing.
     *
     * @param player the player whose permissions are being inspected
     * @return the permissions the player still needs to satisfy this requirement
     */
    @JsonIgnore
    @NotNull
    public List<String> getMissingPermissions(final @NotNull Player player) {
        return this.requiredPermissions.stream()
                .filter(permission -> !this.checkPermission(player, permission))
                .toList();
    }

    /**
     * Determines if the requirement is configured in {@link PermissionMode#ALL} mode.
     *
     * @return {@code true} when the requirement expects all permissions to be present
     */
    @JsonIgnore
    public boolean isAllMode() {
        return this.permissionMode == PermissionMode.ALL;
    }

    /**
     * Determines if the requirement is configured in {@link PermissionMode#ANY} mode.
     *
     * @return {@code true} when the requirement expects any permission to be present
     */
    @JsonIgnore
    public boolean isAnyMode() {
        return this.permissionMode == PermissionMode.ANY;
    }

    /**
     * Determines if the requirement is configured in {@link PermissionMode#MINIMUM} mode.
     *
     * @return {@code true} when the requirement expects a minimum number of permissions
     */
    @JsonIgnore
    public boolean isMinimumMode() {
        return this.permissionMode == PermissionMode.MINIMUM;
    }

    /**
     * Validates that the requirement configuration is internally consistent.
     *
     * @throws IllegalStateException if the configuration violates the expected invariants
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

        for (int i = 0; i < this.requiredPermissions.size(); i++) {
            final String permission = this.requiredPermissions.get(i);
            if (permission == null || permission.trim().isEmpty()) {
                throw new IllegalStateException("Permission at index " + i + " is null or empty.");
            }
        }
    }

    /**
     * Creates a requirement from primitive inputs typically sourced from configuration.
     *
     * @param requiredPermissions the permissions the player must satisfy
     * @param modeString          the textual representation of the permission mode
     * @param minimumRequired     the minimum number of permissions required for {@code MINIMUM} mode
     * @return a new {@link PermissionRequirement} configured with the provided parameters
     * @throws IllegalArgumentException if the mode string does not map to a valid {@link PermissionMode}
     */
    @JsonIgnore
    @NotNull
    public static PermissionRequirement fromString(
            final @NotNull List<String> requiredPermissions,
            final @NotNull String modeString,
            final int minimumRequired
    ) {
        final PermissionMode mode;
        try {
            mode = PermissionMode.valueOf(modeString.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid permission mode: " + modeString + ". Valid modes are: ALL, ANY, MINIMUM."
            );
        }
        return new PermissionRequirement(requiredPermissions, mode, minimumRequired, null, false);
    }

    /**
     * Checks whether the player holds the given permission, accounting for negation rules.
     *
     * @param player     the player being evaluated
     * @param permission the permission identifier to check
     * @return {@code true} if the player satisfies the check; otherwise {@code false}
     */
    private boolean checkPermission(final @NotNull Player player, final @NotNull String permission) {
        final boolean hasPermission = player.hasPermission(permission);
        return this.checkNegated != hasPermission;
    }

    /**
     * Captures the evaluation state for a single permission within the requirement.
     */
    public static final class PermissionStatus {
        private final int index;
        private final String permission;
        private final boolean hasPermission;

        /**
         * Creates a permission status entry.
         *
         * @param index        the position of the permission within the requirement list
         * @param permission   the permission identifier
         * @param hasPermission whether the player satisfies the permission check
         */
        public PermissionStatus(
                final int index,
                final @NotNull String permission,
                final boolean hasPermission
        ) {
            this.index = index;
            this.permission = permission;
            this.hasPermission = hasPermission;
        }

        /**
         * Retrieves the index of the permission within the requirement definition.
         *
         * @return the permission index
         */
        public int getIndex() {
            return this.index;
        }

        /**
         * Retrieves the permission identifier.
         *
         * @return the permission string
         */
        @NotNull
        public String getPermission() {
            return this.permission;
        }

        /**
         * Indicates whether the evaluated player holds the permission.
         *
         * @return {@code true} if the permission is satisfied; otherwise {@code false}
         */
        public boolean hasPermission() {
            return this.hasPermission;
        }
    }
}