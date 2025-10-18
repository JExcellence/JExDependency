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
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public final class PermissionRequirement extends AbstractRequirement {

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

    public PermissionRequirement(final @NotNull String permission) {
        this(List.of(permission), PermissionMode.ALL, 1, null, false);
    }

    public PermissionRequirement(final @NotNull List<String> requiredPermissions) {
        this(requiredPermissions, PermissionMode.ALL, requiredPermissions.size(), null, false);
    }

    public PermissionRequirement(
            final @NotNull List<String> requiredPermissions,
            final @NotNull PermissionMode permissionMode
    ) {
        this(requiredPermissions, permissionMode,
                permissionMode == PermissionMode.ANY ? 1 : requiredPermissions.size(), null, false);
    }

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

    @Override
    public void consume(final @NotNull Player player) {
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.permission";
    }

    @NotNull
    public List<String> getRequiredPermissions() {
        return new ArrayList<>(this.requiredPermissions);
    }

    @NotNull
    public PermissionMode getPermissionMode() {
        return this.permissionMode;
    }

    public int getMinimumRequired() {
        return this.minimumRequired;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public boolean isCheckNegated() {
        return this.checkNegated;
    }

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

    @JsonIgnore
    @NotNull
    public List<String> getHeldPermissions(final @NotNull Player player) {
        return this.requiredPermissions.stream()
                .filter(permission -> this.checkPermission(player, permission))
                .toList();
    }

    @JsonIgnore
    @NotNull
    public List<String> getMissingPermissions(final @NotNull Player player) {
        return this.requiredPermissions.stream()
                .filter(permission -> !this.checkPermission(player, permission))
                .toList();
    }

    @JsonIgnore
    public boolean isAllMode() {
        return this.permissionMode == PermissionMode.ALL;
    }

    @JsonIgnore
    public boolean isAnyMode() {
        return this.permissionMode == PermissionMode.ANY;
    }

    @JsonIgnore
    public boolean isMinimumMode() {
        return this.permissionMode == PermissionMode.MINIMUM;
    }

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

    private boolean checkPermission(final @NotNull Player player, final @NotNull String permission) {
        final boolean hasPermission = player.hasPermission(permission);
        return this.checkNegated != hasPermission;
    }

    public static final class PermissionStatus {
        private final int index;
        private final String permission;
        private final boolean hasPermission;

        public PermissionStatus(
                final int index,
                final @NotNull String permission,
                final boolean hasPermission
        ) {
            this.index = index;
            this.permission = permission;
            this.hasPermission = hasPermission;
        }

        public int getIndex() {
            return this.index;
        }

        @NotNull
        public String getPermission() {
            return this.permission;
        }

        public boolean hasPermission() {
            return this.hasPermission;
        }
    }
}