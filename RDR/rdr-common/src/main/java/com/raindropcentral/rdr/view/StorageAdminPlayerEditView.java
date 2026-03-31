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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.service.StorageAdminPlayerSettingsService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editor view for a specific player's max-storage and discount overrides.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminPlayerEditView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindroprdr.command.admin";

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the player override editor.
     */
    public StorageAdminPlayerEditView() {
        super(StorageAdminPlayerSelectView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_player_edit_ui";
    }

    /**
     * Returns title placeholders.
     *
     * @param context open context
     * @return placeholder map
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
        final @NotNull OpenContext context
    ) {
        return Map.of("player_name", this.resolveTargetName(context));
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "   m d   ",
            "    c    ",
            "         "
        };
    }

    /**
     * Renders the player override editor controls.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        if (!this.hasAdminAccess(player)) {
            render.slot(13).renderWith(() -> this.createLockedItem(player));
            return;
        }

        final UUID selectedPlayerId = this.resolveTargetUuid(render);
        if (selectedPlayerId == null) {
            render.slot(13).renderWith(() -> this.createMissingTargetItem(player));
            return;
        }

        final RDR plugin = this.rdr.get(render);
        final StorageAdminPlayerSettingsService settingsService = plugin.getStorageAdminPlayerSettingsService();
        final StorageAdminPlayerSettingsService.PlayerOverride override = settingsService == null
            ? null
            : settingsService.getPlayerOverride(selectedPlayerId);
        final ConfigSection config = plugin.getDefaultConfig();

        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(selectedPlayerId);
        final Player onlinePlayer = offlinePlayer.getPlayer();
        final int effectiveMaxStorages = onlinePlayer == null
            ? plugin.getMaximumStorages(selectedPlayerId, config)
            : plugin.getMaximumStorages(onlinePlayer, config);
        final double effectiveDiscount = onlinePlayer == null
            ? (override != null && override.discountPercent() != null ? override.discountPercent() : 0.0D)
            : plugin.getStorageDiscountPercent(onlinePlayer);

        render.layoutSlot('s', this.createSummaryItem(
            player,
            this.resolveTargetName(render),
            selectedPlayerId,
            override,
            effectiveMaxStorages,
            effectiveDiscount
        ));

        render.layoutSlot('m', this.createMaximumStoragesItem(player, override, effectiveMaxStorages))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminOverrideValueAnvilView.class,
                this.buildEditData(
                    clickContext,
                    StorageAdminOverrideEditMode.PLAYER_MAX_STORAGES,
                    selectedPlayerId,
                    this.resolveTargetName(clickContext),
                    override != null && override.maximumStorages() != null
                        ? Integer.toString(override.maximumStorages())
                        : Integer.toString(effectiveMaxStorages)
                )
            ));

        render.layoutSlot('d', this.createDiscountItem(player, override, effectiveDiscount))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminOverrideValueAnvilView.class,
                this.buildEditData(
                    clickContext,
                    StorageAdminOverrideEditMode.PLAYER_DISCOUNT,
                    selectedPlayerId,
                    this.resolveTargetName(clickContext),
                    override != null && override.discountPercent() != null
                        ? formatPercent(override.discountPercent())
                        : formatPercent(effectiveDiscount)
                )
            ));

        render.layoutSlot('c', this.createClearOverridesItem(player))
            .onClick(this::handleClearOverridesClick);
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context
     */
    @Override
    public void onClick(
        final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleClearOverridesClick(
        final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);
        if (!this.hasAdminAccess(clickContext.getPlayer())) {
            this.i18n("feedback.access_denied_message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final UUID selectedPlayerId = this.resolveTargetUuid(clickContext);
        if (selectedPlayerId == null) {
            this.i18n("feedback.player_missing_message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final StorageAdminPlayerSettingsService settingsService = this.rdr.get(clickContext).getStorageAdminPlayerSettingsService();
        if (settingsService == null) {
            this.i18n("feedback.settings_unavailable", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        settingsService.clearPlayerOverrides(selectedPlayerId);
        this.i18n("feedback.player_cleared", clickContext.getPlayer())
            .withPlaceholders(Map.of(
                "player_name", this.resolveTargetName(clickContext),
                "player_uuid", selectedPlayerId.toString()
            ))
            .includePrefix()
            .build()
            .sendMessage();

        clickContext.openForPlayer(
            StorageAdminPlayerEditView.class,
            Map.of(
                "plugin", this.rdr.get(clickContext),
                "targetUuid", selectedPlayerId,
                "targetName", this.resolveTargetName(clickContext)
            )
        );
    }

    private @NotNull Map<String, Object> buildEditData(
        final @NotNull Context context,
        final @NotNull StorageAdminOverrideEditMode editMode,
        final @NotNull UUID playerId,
        final @NotNull String playerName,
        final @NotNull String currentValue
    ) {
        final Map<String, Object> data = new HashMap<>();
        data.put("plugin", this.rdr.get(context));
        data.put("editMode", editMode.name());
        data.put("targetUuid", playerId);
        data.put("targetName", playerName);
        data.put("currentValue", currentValue);
        return data;
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
        final Object initialData = context.getInitialData();
        if (initialData instanceof Map<?, ?> data) {
            final Object name = data.get("targetName");
            if (name instanceof String text && !text.isBlank()) {
                return text;
            }
        }

        final UUID playerId = this.resolveTargetUuid(context);
        return playerId == null ? "Unknown" : playerId.toString();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull String selectedName,
        final @NotNull UUID selectedPlayerId,
        final @Nullable StorageAdminPlayerSettingsService.PlayerOverride override,
        final int effectiveMaxStorages,
        final double effectiveDiscount
    ) {
        final String overrideMaxDisplay = override != null && override.maximumStorages() != null
            ? this.formatMaxStorages(player, override.maximumStorages())
            : this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder();
        final String overrideDiscountDisplay = override != null && override.discountPercent() != null
            ? formatPercent(override.discountPercent())
            : this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder();

        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "player_name", selectedName,
                    "player_uuid", selectedPlayerId.toString(),
                    "override_max_storages", overrideMaxDisplay,
                    "effective_max_storages", this.formatMaxStorages(player, effectiveMaxStorages),
                    "override_discount", overrideDiscountDisplay,
                    "effective_discount", formatPercent(effectiveDiscount)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMaximumStoragesItem(
        final @NotNull Player player,
        final @Nullable StorageAdminPlayerSettingsService.PlayerOverride override,
        final int effectiveMaxStorages
    ) {
        final String overrideValue = override != null && override.maximumStorages() != null
            ? this.formatMaxStorages(player, override.maximumStorages())
            : this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder();

        return UnifiedBuilderFactory.item(Material.BEACON)
            .setName(this.i18n("actions.maximum_storages.name", player).build().component())
            .setLore(this.i18n("actions.maximum_storages.lore", player)
                .withPlaceholders(Map.of(
                    "override_max_storages", overrideValue,
                    "effective_max_storages", this.formatMaxStorages(player, effectiveMaxStorages)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createDiscountItem(
        final @NotNull Player player,
        final @Nullable StorageAdminPlayerSettingsService.PlayerOverride override,
        final double effectiveDiscount
    ) {
        final String overrideDiscount = override != null && override.discountPercent() != null
            ? formatPercent(override.discountPercent())
            : this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder();

        return UnifiedBuilderFactory.item(Material.EMERALD)
            .setName(this.i18n("actions.discount.name", player).build().component())
            .setLore(this.i18n("actions.discount.lore", player)
                .withPlaceholders(Map.of(
                    "override_discount", overrideDiscount,
                    "effective_discount", formatPercent(effectiveDiscount)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createClearOverridesItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("actions.clear.name", player).build().component())
            .setLore(this.i18n("actions.clear.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createLockedItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.access_denied.name", player).build().component())
            .setLore(this.i18n("feedback.access_denied.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingTargetItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.player_missing.name", player).build().component())
            .setLore(this.i18n("feedback.player_missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull String formatMaxStorages(
        final @NotNull Player player,
        final int maxStorages
    ) {
        if (maxStorages > 0) {
            return Integer.toString(maxStorages);
        }
        return this.i18n("summary.unlimited", player).build().getI18nVersionWrapper().asPlaceholder();
    }

    private static @NotNull String formatPercent(
        final double value
    ) {
        return String.format(Locale.US, "%.2f%%", value);
    }

    private boolean hasAdminAccess(
        final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }
}
