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

package com.raindropcentral.rdr.view;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.service.StorageAdminPlayerSettingsService;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Generic anvil editor for storage admin player/group override values.
 *
 * <p>This view supports editing player max storages, player discounts, group max storages,
 * and group discounts.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminOverrideValueAnvilView extends AbstractAnvilView {

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the storage admin override anvil editor.
     */
    public StorageAdminOverrideValueAnvilView() {
        super(StorageAdminPlayerView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_override_anvil_ui";
    }

    /**
     * Processes and persists the edited override value.
     *
     * @param input raw player input
     * @param context active context
     * @return normalized result payload
     */
    @Override
    protected @NotNull Object processInput(
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final StorageAdminOverrideEditMode mode = this.resolveEditMode(context);
        final StorageAdminPlayerSettingsService settingsService = this.resolveSettingsService(context);

        return switch (mode) {
            case PLAYER_MAX_STORAGES -> this.updatePlayerMaximumStorages(context, settingsService, input);
            case PLAYER_DISCOUNT -> this.updatePlayerDiscount(context, settingsService, input);
            case GROUP_MAX_STORAGES -> this.updateGroupMaximumStorages(context, settingsService, input);
            case GROUP_DISCOUNT -> this.updateGroupDiscount(context, settingsService, input);
        };
    }

    /**
     * Returns title placeholders for this anvil editor.
     *
     * @param context open context
     * @return placeholder map
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
        final @NotNull OpenContext context
    ) {
        return Map.of(
            "edit_mode", this.resolveEditMode(context).name().toLowerCase(Locale.ROOT),
            "target_name", this.resolveTargetName(context),
            "group_name", this.resolveGroupName(context)
        );
    }

    /**
     * Returns the initial anvil input text.
     *
     * @param context open context
     * @return initial text
     */
    @Override
    protected @NotNull String getInitialInputText(
        final @NotNull OpenContext context
    ) {
        return this.resolveCurrentValue(context);
    }

    /**
     * Validates player input for the active edit mode.
     *
     * @param input user input
     * @param context active context
     * @return {@code true} when input is valid
     */
    @Override
    protected boolean isValidInput(
        final @NotNull String input,
        final @NotNull Context context
    ) {
        try {
            return switch (this.resolveEditMode(context)) {
                case PLAYER_MAX_STORAGES -> this.resolveTargetUuid(context) != null
                    && this.parseMaximumStorages(input) != Integer.MIN_VALUE;
                case PLAYER_DISCOUNT -> this.resolveTargetUuid(context) != null
                    && this.parseDiscountPercent(input) >= 0.0D;
                case GROUP_MAX_STORAGES -> !this.resolveGroupName(context).isBlank()
                    && this.isKnownLuckPermsGroup(context, this.resolveGroupName(context))
                    && this.parseMaximumStorages(input) != Integer.MIN_VALUE;
                case GROUP_DISCOUNT -> !this.resolveGroupName(context).isBlank()
                    && this.isKnownLuckPermsGroup(context, this.resolveGroupName(context))
                    && this.parseDiscountPercent(input) >= 0.0D;
            };
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Configures the anvil first-slot item.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void setupFirstSlot(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final StorageAdminOverrideEditMode mode = this.resolveEditMode(render);
        final String initialValue = this.resolveCurrentValue(render);

        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(this.resolveInputMaterial(mode))
            .setName(Component.text(initialValue.isBlank() ? " " : initialValue))
            .setLore(this.i18n("input.lore", player)
                .withPlaceholders(Map.of(
                    "edit_mode", mode.name().toLowerCase(Locale.ROOT),
                    "target_name", this.resolveTargetName(render),
                    "group_name", this.resolveGroupName(render),
                    "current_value", initialValue.isBlank() ? "-" : initialValue
                ))
                .build()
                .children())
            .build();

        render.firstSlot(inputSlotItem);
    }

    /**
     * Handles validation failures with mode-specific feedback.
     *
     * @param input invalid input
     * @param context active context
     */
    @Override
    protected void onValidationFailed(
        final @Nullable String input,
        final @NotNull Context context
    ) {
        this.i18n("error.invalid_value", context.getPlayer())
            .withPlaceholders(Map.of(
                "input", input == null ? "" : input,
                "edit_mode", this.resolveEditMode(context).name().toLowerCase(Locale.ROOT)
            ))
            .includePrefix()
            .build()
            .sendMessage();
    }

    /**
     * Appends navigation data required by the parent editor views.
     *
     * @param processingResult processed result
     * @param input raw input
     * @param context active context
     * @return merged result data
     */
    @Override
    protected @NotNull Map<String, Object> prepareResultData(
        final @Nullable Object processingResult,
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final Map<String, Object> result = new HashMap<>(super.prepareResultData(processingResult, input, context));
        result.put("plugin", this.rdr.get(context));

        final StorageAdminOverrideEditMode mode = this.resolveEditMode(context);
        result.put("editMode", mode.name());

        final UUID selectedPlayerId = this.resolveTargetUuid(context);
        if (selectedPlayerId != null) {
            result.put("targetUuid", selectedPlayerId);
        }
        result.put("targetName", this.resolveTargetName(context));
        result.put("groupName", this.resolveGroupName(context));
        result.put("currentValue", processingResult == null ? "" : processingResult.toString());
        return result;
    }

    private @NotNull Integer updatePlayerMaximumStorages(
        final @NotNull Context context,
        final @NotNull StorageAdminPlayerSettingsService settingsService,
        final @NotNull String input
    ) {
        final UUID selectedPlayerId = this.resolveTargetUuid(context);
        if (selectedPlayerId == null) {
            throw new IllegalArgumentException("Player target is required.");
        }

        final int parsedMaximum = this.parseMaximumStorages(input);
        settingsService.setPlayerMaximumStorages(selectedPlayerId, this.resolveTargetName(context), parsedMaximum);
        this.i18n("feedback.player_max_updated", context.getPlayer())
            .withPlaceholders(Map.of(
                "player_name", this.resolveTargetName(context),
                "value", Integer.toString(parsedMaximum)
            ))
            .includePrefix()
            .build()
            .sendMessage();
        return parsedMaximum;
    }

    private @NotNull Double updatePlayerDiscount(
        final @NotNull Context context,
        final @NotNull StorageAdminPlayerSettingsService settingsService,
        final @NotNull String input
    ) {
        final UUID selectedPlayerId = this.resolveTargetUuid(context);
        if (selectedPlayerId == null) {
            throw new IllegalArgumentException("Player target is required.");
        }

        final double parsedDiscount = this.parseDiscountPercent(input);
        settingsService.setPlayerDiscountPercent(selectedPlayerId, this.resolveTargetName(context), parsedDiscount);
        this.i18n("feedback.player_discount_updated", context.getPlayer())
            .withPlaceholders(Map.of(
                "player_name", this.resolveTargetName(context),
                "value", formatPercent(parsedDiscount)
            ))
            .includePrefix()
            .build()
            .sendMessage();
        return parsedDiscount;
    }

    private @NotNull Integer updateGroupMaximumStorages(
        final @NotNull Context context,
        final @NotNull StorageAdminPlayerSettingsService settingsService,
        final @NotNull String input
    ) {
        final String selectedGroup = this.resolveGroupName(context);
        if (selectedGroup.isBlank()) {
            throw new IllegalArgumentException("Group name is required.");
        }
        if (!this.isKnownLuckPermsGroup(context, selectedGroup)) {
            throw new IllegalArgumentException("Group is not available in LuckPerms.");
        }

        final int parsedMaximum = this.parseMaximumStorages(input);
        settingsService.setGroupMaximumStorages(selectedGroup, parsedMaximum);
        this.i18n("feedback.group_max_updated", context.getPlayer())
            .withPlaceholders(Map.of(
                "group_name", selectedGroup,
                "value", Integer.toString(parsedMaximum)
            ))
            .includePrefix()
            .build()
            .sendMessage();
        return parsedMaximum;
    }

    private @NotNull Double updateGroupDiscount(
        final @NotNull Context context,
        final @NotNull StorageAdminPlayerSettingsService settingsService,
        final @NotNull String input
    ) {
        final String selectedGroup = this.resolveGroupName(context);
        if (selectedGroup.isBlank()) {
            throw new IllegalArgumentException("Group name is required.");
        }
        if (!this.isKnownLuckPermsGroup(context, selectedGroup)) {
            throw new IllegalArgumentException("Group is not available in LuckPerms.");
        }

        final double parsedDiscount = this.parseDiscountPercent(input);
        settingsService.setGroupDiscountPercent(selectedGroup, parsedDiscount);
        this.i18n("feedback.group_discount_updated", context.getPlayer())
            .withPlaceholders(Map.of(
                "group_name", selectedGroup,
                "value", formatPercent(parsedDiscount)
            ))
            .includePrefix()
            .build()
            .sendMessage();
        return parsedDiscount;
    }

    private int parseMaximumStorages(
        final @NotNull String input
    ) {
        final int parsed = Integer.parseInt(input.trim());
        if (parsed != -1 && parsed < 1) {
            throw new IllegalArgumentException("Maximum storages must be -1 or positive.");
        }
        return parsed;
    }

    private double parseDiscountPercent(
        final @NotNull String input
    ) {
        final String normalized = input.trim().replace("%", "");
        final double parsed = Double.parseDouble(normalized);
        if (parsed < 0.0D || parsed > 100.0D) {
            throw new IllegalArgumentException("Discount must be between 0 and 100.");
        }
        return parsed;
    }

    private @NotNull String resolveCurrentValue(
        final @NotNull Context context
    ) {
        final String value = this.readStringFromInitialData(context, "currentValue");
        return value == null ? "" : value;
    }

    private @Nullable UUID resolveTargetUuid(
        final @NotNull Context context
    ) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object targetId = data.get("targetUuid");
        if (targetId instanceof UUID uuid) {
            return uuid;
        }
        if (targetId instanceof String textValue) {
            try {
                return UUID.fromString(textValue);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private @NotNull String resolveTargetName(
        final @NotNull Context context
    ) {
        final String selectedPlayerName = this.readStringFromInitialData(context, "targetName");
        if (selectedPlayerName != null && !selectedPlayerName.isBlank()) {
            return selectedPlayerName;
        }

        final UUID selectedPlayerId = this.resolveTargetUuid(context);
        return selectedPlayerId == null ? "Unknown" : selectedPlayerId.toString();
    }

    private @NotNull String resolveGroupName(
        final @NotNull Context context
    ) {
        final String selectedGroup = this.readStringFromInitialData(context, "groupName");
        if (selectedGroup == null) {
            return "";
        }

        final String normalized = selectedGroup.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "" : normalized;
    }

    private @NotNull StorageAdminOverrideEditMode resolveEditMode(
        final @NotNull Context context
    ) {
        final String rawMode = this.readStringFromInitialData(context, "editMode");
        return rawMode == null
            ? StorageAdminOverrideEditMode.PLAYER_MAX_STORAGES
            : StorageAdminOverrideEditMode.fromRaw(rawMode);
    }

    private @NotNull StorageAdminPlayerSettingsService resolveSettingsService(
        final @NotNull Context context
    ) {
        final StorageAdminPlayerSettingsService settingsService = this.rdr.get(context).getStorageAdminPlayerSettingsService();
        if (settingsService == null) {
            throw new IllegalStateException("Admin settings service is unavailable.");
        }
        return settingsService;
    }

    private @Nullable String readStringFromInitialData(
        final @NotNull Context context,
        final @NotNull String key
    ) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object value = data.get(key);
        return value instanceof String textValue ? textValue : null;
    }

    private boolean isKnownLuckPermsGroup(
        final @NotNull Context context,
        final @NotNull String groupNameValue
    ) {
        final LuckPermsService luckPermsService = this.rdr.get(context).getLuckPermsService();
        if (luckPermsService == null) {
            return false;
        }

        try {
            return luckPermsService.groupExists(groupNameValue).join();
        } catch (Exception exception) {
            return false;
        }
    }

    private @NotNull Material resolveInputMaterial(
        final @NotNull StorageAdminOverrideEditMode editModeValue
    ) {
        return switch (editModeValue) {
            case PLAYER_MAX_STORAGES, GROUP_MAX_STORAGES -> Material.BEACON;
            case PLAYER_DISCOUNT, GROUP_DISCOUNT -> Material.EMERALD;
        };
    }

    private static @NotNull String formatPercent(
        final double value
    ) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
