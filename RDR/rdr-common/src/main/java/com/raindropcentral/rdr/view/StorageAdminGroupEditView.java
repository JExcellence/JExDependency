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
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Editor view for group-specific max-storage and discount overrides.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminGroupEditView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindroprdr.command.admin";

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the group override editor.
     */
    public StorageAdminGroupEditView() {
        super(StorageAdminPlayerView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_group_edit_ui";
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
        return Map.of(
            "group_name", this.resolveSelectedGroup(context) == null
                ? this.i18n("summary.none", context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder()
                : this.resolveSelectedGroup(context)
        );
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
            "   n m   ",
            "   d c   ",
            "         "
        };
    }

    /**
     * Renders the group override editor controls.
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

        final List<String> availableGroups = this.getAvailableLuckPermsGroups(render);
        final String selectedGroup = this.resolveSelectedGroup(render);
        final StorageAdminPlayerSettingsService settingsService = this.rdr.get(render).getStorageAdminPlayerSettingsService();
        final StorageAdminPlayerSettingsService.GroupOverride override = settingsService == null || selectedGroup == null
            ? null
            : settingsService.getGroupOverride(selectedGroup);

        render.layoutSlot('s', this.createSummaryItem(player, selectedGroup, override, availableGroups.size()));

        render.layoutSlot('n', this.createSelectGroupItem(player, selectedGroup, availableGroups.size()))
            .onClick(this::handleSelectGroupClick);

        render.layoutSlot('m', this.createMaximumStoragesItem(player, override))
            .onClick(clickContext -> this.handleGroupValueEdit(
                clickContext,
                StorageAdminOverrideEditMode.GROUP_MAX_STORAGES,
                selectedGroup,
                override != null && override.maximumStorages() != null ? Integer.toString(override.maximumStorages()) : ""
            ));

        render.layoutSlot('d', this.createDiscountItem(player, override))
            .onClick(clickContext -> this.handleGroupValueEdit(
                clickContext,
                StorageAdminOverrideEditMode.GROUP_DISCOUNT,
                selectedGroup,
                override != null && override.discountPercent() != null ? formatPercent(override.discountPercent()) : ""
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

    private void handleGroupValueEdit(
        final @NotNull SlotClickContext clickContext,
        final @NotNull StorageAdminOverrideEditMode editMode,
        final @Nullable String selectedGroup,
        final @NotNull String currentValue
    ) {
        clickContext.setCancelled(true);
        if (!this.hasAdminAccess(clickContext.getPlayer())) {
            this.i18n("feedback.access_denied_message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final List<String> availableGroups = this.getAvailableLuckPermsGroups(clickContext);
        if (availableGroups.isEmpty()) {
            this.i18n("feedback.luckperms_groups_unavailable", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (selectedGroup == null || selectedGroup.isBlank()) {
            this.i18n("feedback.group_required", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (!availableGroups.contains(selectedGroup)) {
            this.i18n("feedback.group_unknown", clickContext.getPlayer())
                .withPlaceholder("group_name", selectedGroup)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        clickContext.openForPlayer(
            StorageAdminOverrideValueAnvilView.class,
            this.buildEditData(clickContext, editMode, selectedGroup, currentValue)
        );
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

        final String selectedGroup = this.resolveSelectedGroup(clickContext);
        if (selectedGroup == null || selectedGroup.isBlank()) {
            this.i18n("feedback.group_required", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final List<String> availableGroups = this.getAvailableLuckPermsGroups(clickContext);
        if (!availableGroups.isEmpty() && !availableGroups.contains(selectedGroup)) {
            this.i18n("feedback.group_unknown", clickContext.getPlayer())
                .withPlaceholder("group_name", selectedGroup)
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

        settingsService.clearGroupOverrides(selectedGroup);
        this.i18n("feedback.group_cleared", clickContext.getPlayer())
            .withPlaceholder("group_name", selectedGroup)
            .includePrefix()
            .build()
            .sendMessage();

        clickContext.openForPlayer(
            StorageAdminGroupEditView.class,
            Map.of(
                "plugin", this.rdr.get(clickContext),
                "groupName", selectedGroup
            )
        );
    }

    private void handleSelectGroupClick(
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

        final List<String> availableGroups = this.getAvailableLuckPermsGroups(clickContext);
        if (availableGroups.isEmpty()) {
            this.i18n("feedback.luckperms_groups_unavailable", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final String currentGroup = this.resolveSelectedGroup(clickContext);
        final int currentIndex = currentGroup == null ? -1 : availableGroups.indexOf(currentGroup);
        final boolean reverse = clickContext.isRightClick() || clickContext.isShiftClick();
        final int nextIndex;
        if (currentIndex < 0) {
            nextIndex = 0;
        } else if (reverse) {
            nextIndex = (currentIndex - 1 + availableGroups.size()) % availableGroups.size();
        } else {
            nextIndex = (currentIndex + 1) % availableGroups.size();
        }

        final String selectedGroup = availableGroups.get(nextIndex);
        this.i18n("feedback.group_selected", clickContext.getPlayer())
            .withPlaceholders(Map.of(
                "group_name", selectedGroup,
                "group_count", availableGroups.size()
            ))
            .includePrefix()
            .build()
            .sendMessage();

        clickContext.openForPlayer(
            StorageAdminGroupEditView.class,
            Map.of(
                "plugin", this.rdr.get(clickContext),
                "groupName", selectedGroup
            )
        );
    }

    private @NotNull Map<String, Object> buildEditData(
        final @NotNull Context context,
        final @NotNull StorageAdminOverrideEditMode editMode,
        final @Nullable String selectedGroup,
        final @NotNull String currentValue
    ) {
        final Map<String, Object> data = new HashMap<>();
        data.put("plugin", this.rdr.get(context));
        data.put("editMode", editMode.name());
        data.put("groupName", selectedGroup == null ? "" : selectedGroup);
        data.put("currentValue", currentValue);
        return data;
    }

    private @Nullable String resolveSelectedGroup(
        final @NotNull Context context
    ) {
        final List<String> availableGroups = this.getAvailableLuckPermsGroups(context);
        final String explicitGroup = this.normalizeGroupName(this.readGroupNameFromInitialData(context));
        if (explicitGroup != null && (!availableGroups.isEmpty() ? availableGroups.contains(explicitGroup) : !explicitGroup.isBlank())) {
            return explicitGroup;
        }

        if (!availableGroups.isEmpty()) {
            return availableGroups.get(0);
        }

        final StorageAdminPlayerSettingsService settingsService = this.rdr.get(context).getStorageAdminPlayerSettingsService();
        if (settingsService == null || settingsService.getGroupOverrides().isEmpty()) {
            return null;
        }

        return settingsService.getGroupOverrides().keySet().stream()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .findFirst()
            .orElse(null);
    }

    private @NotNull List<String> getAvailableLuckPermsGroups(
        final @NotNull Context context
    ) {
        final StorageAdminPlayerSettingsService settingsService = this.rdr.get(context).getStorageAdminPlayerSettingsService();
        if (settingsService == null) {
            return List.of();
        }
        return settingsService.getLuckPermsGroupNames();
    }

    private @Nullable String readGroupNameFromInitialData(
        final @NotNull Context context
    ) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object rawGroupName = data.get("groupName");
        return rawGroupName instanceof String configuredGroupName ? configuredGroupName : null;
    }

    private @Nullable String normalizeGroupName(
        final @Nullable String rawGroup
    ) {
        if (rawGroup == null) {
            return null;
        }

        final String normalizedGroup = rawGroup.trim().toLowerCase(Locale.ROOT);
        return normalizedGroup.isBlank() ? null : normalizedGroup;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @Nullable String selectedGroup,
        final @Nullable StorageAdminPlayerSettingsService.GroupOverride override,
        final int availableGroupCount
    ) {
        final String groupNameDisplay = selectedGroup == null || selectedGroup.isBlank()
            ? this.i18n("summary.none", player).build().getI18nVersionWrapper().asPlaceholder()
            : selectedGroup;
        final String maxStoragesDisplay = override == null || override.maximumStorages() == null
            ? this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder()
            : this.formatMaxStorages(player, override.maximumStorages());
        final String discountDisplay = override == null || override.discountPercent() == null
            ? this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder()
            : formatPercent(override.discountPercent());

        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "group_name", groupNameDisplay,
                    "override_max_storages", maxStoragesDisplay,
                    "override_discount", discountDisplay,
                    "group_count", availableGroupCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSelectGroupItem(
        final @NotNull Player player,
        final @Nullable String selectedGroup,
        final int availableGroupCount
    ) {
        return UnifiedBuilderFactory.item(Material.NAME_TAG)
            .setName(this.i18n("actions.select_group.name", player).build().component())
            .setLore(this.i18n("actions.select_group.lore", player)
                .withPlaceholders(Map.of(
                    "group_name",
                    selectedGroup == null || selectedGroup.isBlank()
                        ? this.i18n("summary.none", player).build().getI18nVersionWrapper().asPlaceholder()
                        : selectedGroup,
                    "group_count", availableGroupCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMaximumStoragesItem(
        final @NotNull Player player,
        final @Nullable StorageAdminPlayerSettingsService.GroupOverride override
    ) {
        final String value = override == null || override.maximumStorages() == null
            ? this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder()
            : this.formatMaxStorages(player, override.maximumStorages());

        return UnifiedBuilderFactory.item(Material.BEACON)
            .setName(this.i18n("actions.maximum_storages.name", player).build().component())
            .setLore(this.i18n("actions.maximum_storages.lore", player)
                .withPlaceholder("override_max_storages", value)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createDiscountItem(
        final @NotNull Player player,
        final @Nullable StorageAdminPlayerSettingsService.GroupOverride override
    ) {
        final String value = override == null || override.discountPercent() == null
            ? this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder()
            : formatPercent(override.discountPercent());

        return UnifiedBuilderFactory.item(Material.EMERALD)
            .setName(this.i18n("actions.discount.name", player).build().component())
            .setLore(this.i18n("actions.discount.lore", player)
                .withPlaceholder("override_discount", value)
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
