/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Second-step role creation anvil that captures the role display name and persists the role.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RoleCreateNameAnvilView extends AbstractAnvilView {

    private static final int MIN_ROLE_NAME_LENGTH = 2;
    private static final int MAX_ROLE_NAME_LENGTH = 32;

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<String> roleId = initialState("role_id");

    /**
     * Creates the role-name anvil and configures return navigation to roles overview.
     */
    public RoleCreateNameAnvilView() {
        super(RolesOverviewView.class);
    }

    /**
     * Returns the translation key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "role_create_name_anvil_ui";
    }

    /**
     * Uses the root title key under this view namespace.
     *
     * @return title suffix key
     */
    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    /**
     * Cancels default inventory movement behavior in this view.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Provides starter text for role name input.
     *
     * @param context open context
     * @return initial input text
     */
    @Override
    protected @NotNull String getInitialInputText(final @NotNull OpenContext context) {
        return "Role Name";
    }

    /**
     * Validates role name input and creation prerequisites.
     *
     * @param input user input
     * @param context interaction context
     * @return {@code true} when valid
     */
    @Override
    protected boolean isValidInput(final @NotNull String input, final @NotNull Context context) {
        return this.validateRoleName(input, context) == ValidationResult.VALID;
    }

    /**
     * Persists the new town role with an empty permission set.
     *
     * @param input validated role name input
     * @param context interaction context
     * @return created role name
     */
    @Override
    protected @Nullable Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final RDT plugin = this.resolvePlugin(context);
        final UUID resolvedTownUuid = this.resolveTownUuid(context);
        final String resolvedRoleId = this.resolveRoleId(context);
        if (plugin == null || plugin.getTownRepository() == null || resolvedTownUuid == null || resolvedRoleId == null) {
            throw new IllegalStateException("Role creation context is unavailable.");
        }

        final RRTown townRepository = plugin.getTownRepository();
        if (townRepository == null) {
            throw new IllegalStateException("Town repository is unavailable.");
        }

        final RTown town = townRepository.findByTownUUID(resolvedTownUuid);
        if (town == null) {
            throw new IllegalStateException("Town no longer exists.");
        }

        final String roleName = input.trim();
        if (!town.addRole(resolvedRoleId, roleName, Set.of())) {
            throw new IllegalStateException("Role could not be created.");
        }

        townRepository.update(town);
        this.i18n("message.created", context.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "role_id", resolvedRoleId,
                        "role_name", roleName
                ))
                .build()
                .sendMessage();
        return roleName;
    }

    /**
     * Preserves plugin and town context for return to the parent view.
     *
     * @param processingResult processing result
     * @param input user input
     * @param context interaction context
     * @return result payload
     */
    @Override
    protected @NotNull Map<String, Object> prepareResultData(
            final @Nullable Object processingResult,
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final Map<String, Object> data = super.prepareResultData(processingResult, input, context);
        final RDT plugin = this.resolvePlugin(context);
        final UUID resolvedTownUuid = this.resolveTownUuid(context);
        if (plugin != null) {
            data.put("plugin", plugin);
        }
        if (resolvedTownUuid != null) {
            data.put("town_uuid", resolvedTownUuid);
        }
        return data;
    }

    /**
     * Sends a specific validation message and cancels invalid submits.
     *
     * @param input invalid input
     * @param context interaction context
     */
    @Override
    protected void onValidationFailed(
            final @Nullable String input,
            final @NotNull Context context
    ) {
        if (context instanceof final SlotClickContext clickContext) {
            clickContext.setCancelled(true);
        }

        final String normalizedInput = input == null ? "" : input.trim();
        final ValidationResult validationResult = this.validateRoleName(normalizedInput, context);
        final String roleIdValue = this.resolveRoleId(context) == null ? "-" : this.resolveRoleId(context);

        this.i18n(this.toValidationMessageKey(validationResult), context.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "input", normalizedInput,
                        "role_id", roleIdValue,
                        "min_length", MIN_ROLE_NAME_LENGTH,
                        "max_length", MAX_ROLE_NAME_LENGTH,
                        "permission", TownPermissions.CREATE_ROLES.getPermissionKey()
                ))
                .build()
                .sendMessage();
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable UUID resolveTownUuid(final @NotNull Context context) {
        try {
            return this.townUuid.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable String resolveRoleId(final @NotNull Context context) {
        try {
            final String value = this.roleId.get(context);
            if (value == null || value.isBlank()) {
                return null;
            }
            return RTown.normalizeRoleId(value);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @NotNull ValidationResult validateRoleName(
            final @NotNull String rawInput,
            final @NotNull Context context
    ) {
        final String roleName = rawInput.trim();
        if (roleName.isEmpty()) {
            return ValidationResult.EMPTY;
        }

        if (roleName.length() < MIN_ROLE_NAME_LENGTH || roleName.length() > MAX_ROLE_NAME_LENGTH) {
            return ValidationResult.INVALID_LENGTH;
        }

        final String resolvedRoleId = this.resolveRoleId(context);
        if (resolvedRoleId == null) {
            return ValidationResult.ROLE_ID_MISSING;
        }

        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getTownRepository() == null) {
            return ValidationResult.SYSTEM_UNAVAILABLE;
        }

        final UUID resolvedTownUuid = this.resolveTownUuid(context);
        if (resolvedTownUuid == null) {
            return ValidationResult.TOWN_UNAVAILABLE;
        }

        final RRTown townRepository = plugin.getTownRepository();
        if (townRepository == null) {
            return ValidationResult.SYSTEM_UNAVAILABLE;
        }

        final RTown town = townRepository.findByTownUUID(resolvedTownUuid);
        if (town == null) {
            return ValidationResult.TOWN_UNAVAILABLE;
        }

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return ValidationResult.SYSTEM_UNAVAILABLE;
        }

        final RDTPlayer townPlayer = playerRepository.findByPlayer(context.getPlayer().getUniqueId());
        if (!town.hasTownPermission(townPlayer, TownPermissions.CREATE_ROLES)) {
            return ValidationResult.NO_PERMISSION;
        }

        if (town.findRoleById(resolvedRoleId) != null) {
            return ValidationResult.ALREADY_EXISTS;
        }

        return ValidationResult.VALID;
    }

    private @NotNull String toValidationMessageKey(final @NotNull ValidationResult result) {
        return switch (result) {
            case VALID -> "error.invalid.generic";
            case EMPTY -> "error.invalid.empty";
            case INVALID_LENGTH -> "error.invalid.length";
            case ALREADY_EXISTS -> "error.invalid.exists";
            case ROLE_ID_MISSING -> "error.role_id_missing";
            case SYSTEM_UNAVAILABLE -> "error.system_unavailable";
            case TOWN_UNAVAILABLE -> "error.town_unavailable";
            case NO_PERMISSION -> "error.no_permission";
        };
    }

    private enum ValidationResult {
        VALID,
        EMPTY,
        INVALID_LENGTH,
        ALREADY_EXISTS,
        ROLE_ID_MISSING,
        SYSTEM_UNAVAILABLE,
        TOWN_UNAVAILABLE,
        NO_PERMISSION
    }
}
