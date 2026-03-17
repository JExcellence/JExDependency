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

package com.raindropcentral.rds.view.shop.anvil;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.service.shop.ShopAdminPlayerSettingsService;
import com.raindropcentral.rds.view.shop.ShopAdminOverrideEditMode;
import com.raindropcentral.rds.view.shop.ShopAdminPlayerView;
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
 * Generic anvil editor for player/group admin override values.
 *
 * <p>This view supports editing player max shops, player discounts, group names, group max shops,
 * and group discounts.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopAdminOverrideValueAnvilView extends AbstractAnvilView {

    private final State<RDS> rds = initialState("plugin");
    private final State<String> editMode = initialState("editMode");
    private final State<String> currentValue = initialState("currentValue");
    private final State<UUID> targetUuid = initialState("targetUuid");
    private final State<String> targetName = initialState("targetName");
    private final State<String> groupName = initialState("groupName");

    /**
     * Creates the admin override anvil editor.
     */
    public ShopAdminOverrideValueAnvilView() {
        super(ShopAdminPlayerView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_admin_override_anvil_ui";
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
        final ShopAdminOverrideEditMode mode = this.resolveEditMode(context);
        final ShopAdminPlayerSettingsService settingsService = this.resolveSettingsService(context);

        return switch (mode) {
            case PLAYER_MAX_SHOPS -> this.updatePlayerMaximumShops(context, settingsService, input);
            case PLAYER_DISCOUNT -> this.updatePlayerDiscount(context, settingsService, input);
            case GROUP_NAME -> this.selectGroup(context, input);
            case GROUP_MAX_SHOPS -> this.updateGroupMaximumShops(context, settingsService, input);
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
        final String value = this.currentValue.get(context);
        return value == null ? "" : value;
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
                case PLAYER_MAX_SHOPS, GROUP_MAX_SHOPS -> this.parseMaximumShops(input) != Integer.MIN_VALUE;
                case PLAYER_DISCOUNT, GROUP_DISCOUNT -> this.parseDiscountPercent(input) >= 0.0D;
                case GROUP_NAME -> this.isKnownLuckPermsGroup(context, this.normalizeGroupName(input));
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
        final ShopAdminOverrideEditMode mode = this.resolveEditMode(render);
        final String initialValue = this.currentValue.get(render) == null ? "" : this.currentValue.get(render);

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
        result.put("plugin", this.rds.get(context));

        final ShopAdminOverrideEditMode mode = this.resolveEditMode(context);
        result.put("editMode", mode.name());

        final UUID selectedPlayerId = this.targetUuid.get(context);
        if (selectedPlayerId != null) {
            result.put("targetUuid", selectedPlayerId);
        }
        result.put("targetName", this.resolveTargetName(context));

        if (mode == ShopAdminOverrideEditMode.GROUP_NAME && processingResult instanceof String selectedGroupName) {
            result.put("groupName", selectedGroupName);
        } else {
            result.put("groupName", this.resolveGroupName(context));
        }
        result.put("currentValue", processingResult == null ? "" : processingResult.toString());
        return result;
    }

    private @NotNull Integer updatePlayerMaximumShops(
            final @NotNull Context context,
            final @NotNull ShopAdminPlayerSettingsService settingsService,
            final @NotNull String input
    ) {
        final UUID selectedPlayerId = this.resolveTargetUuid(context);
        final int parsedMaximum = this.parseMaximumShops(input);
        settingsService.setPlayerMaximumShops(selectedPlayerId, this.resolveTargetName(context), parsedMaximum);
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
            final @NotNull ShopAdminPlayerSettingsService settingsService,
            final @NotNull String input
    ) {
        final UUID selectedPlayerId = this.resolveTargetUuid(context);
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

    private @NotNull String selectGroup(
            final @NotNull Context context,
            final @NotNull String input
    ) {
        final String selectedGroup = this.normalizeGroupName(input);
        if (!this.isKnownLuckPermsGroup(context, selectedGroup)) {
            throw new IllegalArgumentException("Group is not available in LuckPerms.");
        }

        this.i18n("feedback.group_selected", context.getPlayer())
                .withPlaceholder("group_name", selectedGroup)
                .includePrefix()
                .build()
                .sendMessage();
        return selectedGroup;
    }

    private @NotNull Integer updateGroupMaximumShops(
            final @NotNull Context context,
            final @NotNull ShopAdminPlayerSettingsService settingsService,
            final @NotNull String input
    ) {
        final String selectedGroup = this.resolveGroupName(context);
        if (selectedGroup.isBlank()) {
            throw new IllegalArgumentException("Group name is required.");
        }

        final int parsedMaximum = this.parseMaximumShops(input);
        settingsService.setGroupMaximumShops(selectedGroup, parsedMaximum);
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
            final @NotNull ShopAdminPlayerSettingsService settingsService,
            final @NotNull String input
    ) {
        final String selectedGroup = this.resolveGroupName(context);
        if (selectedGroup.isBlank()) {
            throw new IllegalArgumentException("Group name is required.");
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

    private int parseMaximumShops(
            final @NotNull String input
    ) {
        final int parsed = Integer.parseInt(input.trim());
        if (parsed != -1 && parsed < 1) {
            throw new IllegalArgumentException("Maximum shops must be -1 or positive.");
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

    private @NotNull UUID resolveTargetUuid(
            final @NotNull Context context
    ) {
        final UUID selectedPlayerId = this.targetUuid.get(context);
        if (selectedPlayerId == null) {
            throw new IllegalArgumentException("Player target is required.");
        }
        return selectedPlayerId;
    }

    private @NotNull String resolveTargetName(
            final @NotNull Context context
    ) {
        final String selectedPlayerName = this.targetName.get(context);
        if (selectedPlayerName != null && !selectedPlayerName.isBlank()) {
            return selectedPlayerName;
        }
        final UUID selectedPlayerId = this.targetUuid.get(context);
        return selectedPlayerId == null ? "Unknown" : selectedPlayerId.toString();
    }

    private @NotNull String resolveGroupName(
            final @NotNull Context context
    ) {
        final String selectedGroup = this.groupName.get(context);
        if (selectedGroup == null) {
            return "";
        }
        return this.normalizeGroupName(selectedGroup);
    }

    private @NotNull ShopAdminOverrideEditMode resolveEditMode(
            final @NotNull Context context
    ) {
        final String rawMode = this.editMode.get(context);
        return rawMode == null
                ? ShopAdminOverrideEditMode.GROUP_NAME
                : ShopAdminOverrideEditMode.fromRaw(rawMode);
    }

    private @NotNull ShopAdminPlayerSettingsService resolveSettingsService(
            final @NotNull Context context
    ) {
        final ShopAdminPlayerSettingsService settingsService = this.rds.get(context).getShopAdminPlayerSettingsService();
        if (settingsService == null) {
            throw new IllegalStateException("Admin settings service is unavailable.");
        }
        return settingsService;
    }

    private @NotNull String normalizeGroupName(
            final @NotNull String input
    ) {
        final String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be blank.");
        }
        return normalized;
    }

    private boolean isKnownLuckPermsGroup(
            final @NotNull Context context,
            final @NotNull String groupNameValue
    ) {
        final LuckPermsService luckPermsService = this.rds.get(context).getLuckPermsService();
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
            final @NotNull ShopAdminOverrideEditMode editModeValue
    ) {
        return switch (editModeValue) {
            case PLAYER_MAX_SHOPS, GROUP_MAX_SHOPS -> Material.BEACON;
            case PLAYER_DISCOUNT, GROUP_DISCOUNT -> Material.EMERALD;
            case GROUP_NAME -> Material.NAME_TAG;
        };
    }

    private static @NotNull String formatPercent(
            final double value
    ) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
