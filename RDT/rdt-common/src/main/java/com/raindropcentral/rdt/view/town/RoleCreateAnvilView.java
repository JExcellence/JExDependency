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
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * First-step role creation anvil that captures and validates the role ID.
 *
 * <p>On successful validation, this view passes the captured role ID into
 * {@link RoleCreateNameAnvilView}.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RoleCreateAnvilView extends AbstractAnvilView {

    private static final Pattern ROLE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int MIN_ROLE_ID_LENGTH = 2;
    private static final int MAX_ROLE_ID_LENGTH = 24;

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the role ID anvil and configures chaining into role name input.
     */
    public RoleCreateAnvilView() {
        super(RoleCreateNameAnvilView.class);
    }

    /**
     * Returns the translation key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "role_create_id_anvil_ui";
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
     * Provides starter text for role ID input.
     *
     * @param context open context
     * @return initial input text
     */
    @Override
    protected @NotNull String getInitialInputText(final @NotNull OpenContext context) {
        return "ROLE_ID";
    }

    /**
     * Validates role ID input and creation prerequisites.
     *
     * @param input user input
     * @param context interaction context
     * @return {@code true} when valid
     */
    @Override
    protected boolean isValidInput(final @NotNull String input, final @NotNull Context context) {
        return this.validateRoleId(input, context) == ValidationResult.VALID;
    }

    /**
     * Normalizes and returns the role ID for handoff to the second anvil step.
     *
     * @param input validated role ID input
     * @param context interaction context
     * @return normalized role ID
     */
    @Override
    protected @Nullable Object processInput(final @NotNull String input, final @NotNull Context context) {
        return RTown.normalizeRoleId(input);
    }

    /**
     * Preserves plugin and town context for the role-name step.
     *
     * @param processingResult normalized role ID
     * @param input original input
     * @param context interaction context
     * @return handoff data for the next view
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
        final String normalizedRoleId = RTown.normalizeRoleId(input);

        if (plugin != null) {
            data.put("plugin", plugin);
        }
        if (resolvedTownUuid != null) {
            data.put("town_uuid", resolvedTownUuid);
        }
        data.put("role_id", normalizedRoleId);
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
        final ValidationResult validationResult = this.validateRoleId(normalizedInput, context);

        this.i18n(this.toValidationMessageKey(validationResult), context.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "input", normalizedInput,
                        "min_length", MIN_ROLE_ID_LENGTH,
                        "max_length", MAX_ROLE_ID_LENGTH,
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

    private @NotNull ValidationResult validateRoleId(
            final @NotNull String rawInput,
            final @NotNull Context context
    ) {
        final String roleId = RTown.normalizeRoleId(rawInput);
        if (roleId.isEmpty()) {
            return ValidationResult.EMPTY;
        }

        if (roleId.length() < MIN_ROLE_ID_LENGTH || roleId.length() > MAX_ROLE_ID_LENGTH) {
            return ValidationResult.INVALID_LENGTH;
        }

        if (!ROLE_ID_PATTERN.matcher(roleId).matches()) {
            return ValidationResult.INVALID_PATTERN;
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

        if (town.findRoleById(roleId) != null) {
            return ValidationResult.ALREADY_EXISTS;
        }

        return ValidationResult.VALID;
    }

    private @NotNull String toValidationMessageKey(final @NotNull ValidationResult result) {
        return switch (result) {
            case VALID -> "error.invalid.generic";
            case EMPTY -> "error.invalid.empty";
            case INVALID_LENGTH -> "error.invalid.length";
            case INVALID_PATTERN -> "error.invalid.pattern";
            case ALREADY_EXISTS -> "error.invalid.exists";
            case SYSTEM_UNAVAILABLE -> "error.system_unavailable";
            case TOWN_UNAVAILABLE -> "error.town_unavailable";
            case NO_PERMISSION -> "error.no_permission";
        };
    }

    private enum ValidationResult {
        VALID,
        EMPTY,
        INVALID_LENGTH,
        INVALID_PATTERN,
        ALREADY_EXISTS,
        SYSTEM_UNAVAILABLE,
        TOWN_UNAVAILABLE,
        NO_PERMISSION
    }
}
