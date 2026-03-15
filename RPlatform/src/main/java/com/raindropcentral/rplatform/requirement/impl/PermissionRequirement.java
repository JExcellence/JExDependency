package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Represents the PermissionRequirement API type.
 */
public final class PermissionRequirement extends AbstractRequirement {

    /**
     * Represents the PermissionMode API type.
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
     * Executes PermissionRequirement.
     */
    public PermissionRequirement(@NotNull String permission) {
        this(List.of(permission), PermissionMode.ALL, 1, null, false);
    }

    /**
     * Executes PermissionRequirement.
     */
    public PermissionRequirement(@NotNull List<String> requiredPermissions) {
        this(requiredPermissions, PermissionMode.ALL, requiredPermissions.size(), null, false);
    }

    /**
     * Executes PermissionRequirement.
     */
    public PermissionRequirement(@NotNull List<String> requiredPermissions, @NotNull PermissionMode permissionMode) {
        this(requiredPermissions, permissionMode,
                permissionMode == PermissionMode.ANY ? 1 : requiredPermissions.size(), null, false);
    }

    /**
     * Executes PermissionRequirement.
     */
    @JsonCreator
    public PermissionRequirement(@JsonProperty("requiredPermissions") @NotNull List<String> requiredPermissions,
                                @JsonProperty("permissionMode") @Nullable PermissionMode permissionMode,
                                @JsonProperty("minimumRequired") @Nullable Integer minimumRequired,
                                @JsonProperty("description") @Nullable String description,
                                @JsonProperty("checkNegated") @Nullable Boolean checkNegated) {
        super("PERMISSION");

        if (requiredPermissions.isEmpty()) {
            throw new IllegalArgumentException("At least one permission must be specified.");
        }

        for (var permission : requiredPermissions) {
            if (permission == null || permission.trim().isEmpty()) {
                throw new IllegalArgumentException("Permission cannot be null or empty.");
            }
        }

        var mode = permissionMode != null ? permissionMode : PermissionMode.ALL;
        var minRequired = minimumRequired != null ? minimumRequired :
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
     * Returns whether met.
     */
    @Override
    public boolean isMet(@NotNull Player player) {
        return switch (permissionMode) {
            case ALL -> requiredPermissions.stream()
                    .allMatch(permission -> checkPermission(player, permission));
            case ANY -> requiredPermissions.stream()
                    .anyMatch(permission -> checkPermission(player, permission));
            case MINIMUM -> {
                var matchingPermissions = requiredPermissions.stream()
                        .filter(permission -> checkPermission(player, permission))
                        .count();
                yield matchingPermissions >= minimumRequired;
            }
        };
    }

    /**
     * Executes calculateProgress.
     */
    @Override
    public double calculateProgress(@NotNull Player player) {
        if (requiredPermissions.isEmpty()) {
            return 1.0;
        }

        var heldPermissions = requiredPermissions.stream()
                .filter(permission -> checkPermission(player, permission))
                .count();

        return switch (permissionMode) {
            case ALL -> Math.min(1.0, (double) heldPermissions / requiredPermissions.size());
            case ANY -> heldPermissions > 0 ? 1.0 : 0.0;
            case MINIMUM -> Math.min(1.0, (double) heldPermissions / minimumRequired);
        };
    }

    /**
     * Executes consume.
     */
    @Override
    public void consume(@NotNull Player player) {
    }

    /**
     * Gets descriptionKey.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.permission";
    }

    /**
     * Gets minimumRequired.
     */
    @NotNull public List<String> getRequiredPermissions() { return new ArrayList<>(requiredPermissions); }
    @NotNull public PermissionMode getPermissionMode() { return permissionMode; }
    public int getMinimumRequired() { return minimumRequired; }
    /**
     * Returns whether checkNegated.
     */
    @Nullable public String getDescription() { return description; }
    public boolean isCheckNegated() { return checkNegated; }

    /**
     * Gets detailedPermissionStatus.
     */
    @JsonIgnore
    @NotNull
    public List<PermissionStatus> getDetailedPermissionStatus(@NotNull Player player) {
        return IntStream.range(0, requiredPermissions.size())
                .mapToObj(index -> {
                    var permission = requiredPermissions.get(index);
                    var hasPermission = checkPermission(player, permission);
                    return new PermissionStatus(index, permission, hasPermission);
                })
                .toList();
    }

    /**
     * Gets heldPermissions.
     */
    @JsonIgnore
    @NotNull
    public List<String> getHeldPermissions(@NotNull Player player) {
        return requiredPermissions.stream()
                .filter(permission -> checkPermission(player, permission))
                .toList();
    }

    /**
     * Gets missingPermissions.
     */
    @JsonIgnore
    @NotNull
    public List<String> getMissingPermissions(@NotNull Player player) {
        return requiredPermissions.stream()
                .filter(permission -> !checkPermission(player, permission))
                .toList();
    }

    @JsonIgnore public boolean isAllMode() { return permissionMode == PermissionMode.ALL; }
    @JsonIgnore public boolean isAnyMode() { return permissionMode == PermissionMode.ANY; }
    @JsonIgnore public boolean isMinimumMode() { return permissionMode == PermissionMode.MINIMUM; }

    /**
     * Executes validate.
     */
    @JsonIgnore
    public void validate() {
        if (requiredPermissions.isEmpty()) {
            throw new IllegalStateException("PermissionRequirement must have at least one permission.");
        }
        if (minimumRequired < 1 || minimumRequired > requiredPermissions.size()) {
            throw new IllegalStateException(
                    "Invalid minimumRequired: " + minimumRequired +
                    " (must be between 1 and " + requiredPermissions.size() + ")."
            );
        }

        for (int i = 0; i < requiredPermissions.size(); i++) {
            var permission = requiredPermissions.get(i);
            if (permission == null || permission.trim().isEmpty()) {
                throw new IllegalStateException("Permission at index " + i + " is null or empty.");
            }
        }
    }

    /**
     * Executes fromString.
     */
    @JsonIgnore
    @NotNull
    public static PermissionRequirement fromString(@NotNull List<String> requiredPermissions,
                                                   @NotNull String modeString,
                                                   int minimumRequired) {
        PermissionMode mode;
        try {
            mode = PermissionMode.valueOf(modeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid permission mode: " + modeString + ". Valid modes are: ALL, ANY, MINIMUM."
            );
        }
        return new PermissionRequirement(requiredPermissions, mode, minimumRequired, null, false);
    }

    private boolean checkPermission(@NotNull Player player, @NotNull String permission) {
        var hasPermission = player.hasPermission(permission);
        return checkNegated != hasPermission;
    }

    /**
     * Represents the PermissionStatus API type.
     */
    public static final class PermissionStatus {
        private final int index;
        private final String permission;
        private final boolean hasPermission;

        /**
         * Executes PermissionStatus.
         */
        public PermissionStatus(int index, @NotNull String permission, boolean hasPermission) {
            this.index = index;
            this.permission = permission;
            this.hasPermission = hasPermission;
        }

        /**
         * Gets index.
         */
        public int getIndex() { return index; }
        /**
         * Returns whether permission.
         */
        @NotNull public String getPermission() { return permission; }
        public boolean hasPermission() { return hasPermission; }
    }
}
