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

package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.service.config.ShopConfigEditSupport;
import com.raindropcentral.rds.view.shop.anvil.ShopConfigValueAnvilView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Renders a paginated editor for configurable values in {@code config/config.yml}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopConfigView extends APaginatedView<ShopConfigView.ConfigEntry> {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";
    private static final int MAX_PREVIEW_LENGTH = 80;

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the shop config editor view.
     */
    public ShopConfigView() {
        super(ShopAdminView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_config_ui";
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
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                "  < p >  "
        };
    }

    /**
     * Re-opens this view after anvil edits so pagination data is refreshed from disk.
     *
     * @param origin previous context
     * @param target active context
     */
    @Override
    public void onResume(
            final @NotNull Context origin,
            final @NotNull Context target
    ) {
        final UpdateResult result = this.extractUpdateResult(target) != null
                ? this.extractUpdateResult(target)
                : this.extractUpdateResult(origin);
        if (result != null) {
            this.i18n("feedback.updated", target.getPlayer())
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "config_path", result.configPath(),
                            "updated_value", result.updatedValue()
                    ))
                    .build()
                    .sendMessage();

            target.openForPlayer(
                    ShopConfigView.class,
                    Map.of("plugin", this.rds.get(target))
            );
            return;
        }

        target.update();
    }

    /**
     * Resolves config entries for pagination.
     *
     * @param context active menu context
     * @return async list of editable config entries
     */
    @Override
    protected @NotNull CompletableFuture<List<ConfigEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        if (!this.canAccessConfigEditor(context)) {
            return CompletableFuture.completedFuture(List.of());
        }

        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(this.getConfigFile(this.rds.get(context)));
        final List<ConfigEntry> entries = new ArrayList<>();

        for (final Map.Entry<String, Object> valueEntry : configuration.getValues(true).entrySet()) {
            final String path = valueEntry.getKey();
            final Object value = valueEntry.getValue();
            if (path == null || path.isBlank()) {
                continue;
            }
            if (value instanceof ConfigurationSection || value instanceof MemorySection) {
                continue;
            }
            if (!ShopConfigEditSupport.isEditableValue(value)) {
                continue;
            }

            entries.add(new ConfigEntry(path, value, ShopConfigEditSupport.determineType(value)));
        }

        entries.sort(Comparator.comparing(ConfigEntry::path, String.CASE_INSENSITIVE_ORDER));
        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders a single editable config entry.
     *
     * @param context menu context
     * @param builder item component builder
     * @param index rendered index
     * @param entry entry payload
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull ConfigEntry entry
    ) {
        builder.withItem(this.createEntryItem(context.getPlayer(), entry))
                .onClick(clickContext -> this.handleEntryEditClick(clickContext, entry));
    }

    /**
     * Renders static controls for the paginated config view.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        if (!player.hasPermission(ADMIN_COMMAND_PERMISSION)) {
            render.slot(22).renderWith(() -> this.createPermissionLockedItem(player));
            return;
        }

        if (!this.rds.get(render).canChangeConfigs()) {
            render.slot(22).renderWith(() -> this.createConfigLockedItem(player));
            return;
        }

        final int entryCount = this.countEntries(this.rds.get(render));
        final int protectionEntryCount = this.countProtectionEntries(this.rds.get(render));
        render.layoutSlot('s', this.createSummaryItem(player, entryCount, protectionEntryCount));
        if (entryCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context for the current inventory interaction
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleEntryEditClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull ConfigEntry entry
    ) {
        clickContext.setCancelled(true);

        if (!clickContext.getPlayer().hasPermission(ADMIN_COMMAND_PERMISSION)) {
            this.i18n("feedback.permission_locked_message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        if (!this.rds.get(clickContext).canChangeConfigs()) {
            this.i18n("feedback.config_locked_message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        clickContext.openForPlayer(
                ShopConfigValueAnvilView.class,
                Map.of(
                        "plugin", this.rds.get(clickContext),
                        "configPath", entry.path(),
                        "currentValue", ShopConfigEditSupport.formatValue(entry.value()),
                        "settingType", entry.settingType().name()
                )
        );
    }

    private @Nullable UpdateResult extractUpdateResult(
            final @NotNull Context context
    ) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object pathObject = data.get("updatedPath");
        final Object valueObject = data.get("updatedValue");
        if (!(pathObject instanceof String path) || !(valueObject instanceof String value)) {
            return null;
        }

        return new UpdateResult(path, value);
    }

    private boolean canAccessConfigEditor(
            final @NotNull Context context
    ) {
        return context.getPlayer().hasPermission(ADMIN_COMMAND_PERMISSION)
                && this.rds.get(context).canChangeConfigs();
    }

    private int countEntries(
            final @NotNull RDS plugin
    ) {
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(this.getConfigFile(plugin));
        int count = 0;
        for (final Object value : configuration.getValues(true).values()) {
            if (value instanceof ConfigurationSection || value instanceof MemorySection) {
                continue;
            }
            if (!ShopConfigEditSupport.isEditableValue(value)) {
                continue;
            }
            count++;
        }
        return count;
    }

    private int countProtectionEntries(
            final @NotNull RDS plugin
    ) {
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(this.getConfigFile(plugin));
        int count = 0;
        for (final Map.Entry<String, Object> valueEntry : configuration.getValues(true).entrySet()) {
            final String path = valueEntry.getKey();
            final Object value = valueEntry.getValue();
            if (path == null || !path.startsWith("protection.")) {
                continue;
            }
            if (value instanceof ConfigurationSection || value instanceof MemorySection) {
                continue;
            }
            if (!ShopConfigEditSupport.isEditableValue(value)) {
                continue;
            }
            count++;
        }
        return count;
    }

    private @NotNull File getConfigFile(
            final @NotNull RDS plugin
    ) {
        return new File(new File(plugin.getDataFolder(), "config"), "config.yml");
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final int settingCount,
            final int protectionSettingCount
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "setting_count", settingCount,
                                "protection_setting_count", protectionSettingCount
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEntryItem(
            final @NotNull Player player,
            final @NotNull ConfigEntry entry
    ) {
        return UnifiedBuilderFactory.item(this.resolveEntryMaterial(entry.settingType()))
                .setName(this.i18n("entry.name", player)
                        .withPlaceholder("config_path", entry.path())
                        .build()
                        .component())
                .setLore(this.i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "config_path", entry.path(),
                                "value_type", this.getValueTypeLabel(player, entry.settingType()),
                                "current_value", this.previewValue(entry.value())
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createPermissionLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.permission_locked.name", player).build().component())
                .setLore(this.i18n("feedback.permission_locked.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createConfigLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.config_locked.name", player).build().component())
                .setLore(this.i18n("feedback.config_locked.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEmptyItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.PAPER)
                .setName(this.i18n("feedback.empty.name", player).build().component())
                .setLore(this.i18n("feedback.empty.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull Material resolveEntryMaterial(
            final @NotNull ShopConfigEditSupport.SettingType settingType
    ) {
        return switch (settingType) {
            case BOOLEAN -> Material.LEVER;
            case INTEGER, LONG, DOUBLE -> Material.REPEATER;
            case LIST -> Material.BOOK;
            case STRING -> Material.NAME_TAG;
        };
    }

    private @NotNull String getValueTypeLabel(
            final @NotNull Player player,
            final @NotNull ShopConfigEditSupport.SettingType settingType
    ) {
        final String key = switch (settingType) {
            case BOOLEAN -> "type.boolean";
            case INTEGER -> "type.integer";
            case LONG -> "type.long";
            case DOUBLE -> "type.double";
            case LIST -> "type.list";
            case STRING -> "type.string";
        };
        return this.i18n(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String previewValue(
            final @Nullable Object value
    ) {
        final String formatted = ShopConfigEditSupport.formatValue(value).replace('\n', ' ');
        if (formatted.length() <= MAX_PREVIEW_LENGTH) {
            return formatted.isBlank() ? "<empty>" : formatted;
        }
        return formatted.substring(0, MAX_PREVIEW_LENGTH - 3) + "...";
    }

    private void openFreshView(
            final @NotNull Context context
    ) {
        context.openForPlayer(
                ShopConfigView.class,
                Map.of("plugin", this.rds.get(context))
        );
    }

    /**
     * Represents one editable config value entry.
     *
     * @param path full config path
     * @param value current value
     * @param settingType setting type
     */
    protected record ConfigEntry(
            @NotNull String path,
            @Nullable Object value,
            @NotNull ShopConfigEditSupport.SettingType settingType
    ) {
    }

    /**
     * Represents a config update result returned from the edit anvil.
     *
     * @param configPath updated config path
     * @param updatedValue formatted updated value
     */
    protected record UpdateResult(
            @NotNull String configPath,
            @NotNull String updatedValue
    ) {
    }
}
