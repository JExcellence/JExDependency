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
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Renders the config value editor anvil for a selected storage config path.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageConfigValueAnvilView extends AbstractAnvilView {

    private final State<RDR> rdr = initialState("plugin");
    private final State<String> configPath = initialState("configPath");
    private final State<String> currentValue = initialState("currentValue");
    private final State<String> settingType = initialState("settingType");

    /**
     * Creates a new config value editor anvil view.
     */
    public StorageConfigValueAnvilView() {
        super(StorageConfigView.class);
    }

    /**
     * Returns the translation namespace used by this anvil view.
     *
     * @return storage config anvil translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_config_value_anvil_ui";
    }

    /**
     * Parses and persists the entered config value.
     *
     * @param input user-entered value
     * @param context current anvil interaction context
     * @return parsed typed config value
     */
    @Override
    protected @NotNull Object processInput(
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final StorageConfigEditSupport.SettingType type = this.resolveSettingType(context);
        final Object parsedValue = StorageConfigEditSupport.parseInput(input, type);
        this.saveConfigValue(this.rdr.get(context), this.resolveConfigPath(context), parsedValue);
        return parsedValue;
    }

    /**
     * Supplies title placeholders for the targeted config path.
     *
     * @param context open context for the current player
     * @return placeholder map with path, current value, and type
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
        final @NotNull OpenContext context
    ) {
        return Map.of(
            "config_path", this.resolveConfigPath(context),
            "current_value", this.resolveCurrentValue(context),
            "value_type", this.resolveSettingType(context).name().toLowerCase(Locale.ROOT)
        );
    }

    /**
     * Returns the current value as the initial editable text.
     *
     * @param context open context for the current player
     * @return initial anvil input text
     */
    @Override
    protected @NotNull String getInitialInputText(
        final @NotNull OpenContext context
    ) {
        return this.resolveCurrentValue(context);
    }

    /**
     * Validates the entered value against the selected config type.
     *
     * @param input user-entered text
     * @param context current interaction context
     * @return {@code true} when the input can be parsed for the setting type
     */
    @Override
    protected boolean isValidInput(
        final @NotNull String input,
        final @NotNull Context context
    ) {
        try {
            StorageConfigEditSupport.parseInput(input, this.resolveSettingType(context));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Renders the anvil input slot with a type-colored pane and current value prompt.
     *
     * @param render render context
     * @param player player viewing the anvil
     */
    @Override
    protected void setupFirstSlot(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(this.resolveMaterial(this.resolveSettingType(render)))
            .setName(Component.text(this.resolveCurrentValue(render).isBlank() ? " " : this.resolveCurrentValue(render)))
            .setLore(this.i18n("input.lore", player)
                .withPlaceholders(Map.of(
                    "config_path", this.resolveConfigPath(render),
                    "current_value", this.resolveCurrentValue(render),
                    "value_type", this.resolveSettingType(render).name().toLowerCase(Locale.ROOT)
                ))
                .build()
                .children())
            .build();

        render.firstSlot(inputSlotItem);
    }

    /**
     * Sends a type-specific validation error for invalid input.
     *
     * @param input invalid user input
     * @param context current interaction context
     */
    @Override
    protected void onValidationFailed(
        final @Nullable String input,
        final @NotNull Context context
    ) {
        this.i18n("error.invalid_value", context.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of(
                "input", input == null ? "" : input,
                "value_type", this.resolveSettingType(context).name().toLowerCase(Locale.ROOT)
            ))
            .build()
            .sendMessage();
    }

    /**
     * Preserves plugin state and update metadata for the parent config view.
     *
     * @param processingResult parsed typed value
     * @param input submitted input text
     * @param context current interaction context
     * @return result data payload for the parent view
     */
    @Override
    protected @NotNull Map<String, Object> prepareResultData(
        final @Nullable Object processingResult,
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final Map<String, Object> result = super.prepareResultData(processingResult, input, context);
        result.put("plugin", this.rdr.get(context));
        result.put("updatedPath", this.resolveConfigPath(context));
        result.put("updatedValue", StorageConfigEditSupport.formatValue(processingResult));
        return result;
    }

    private void saveConfigValue(
        final @NotNull RDR plugin,
        final @NotNull String path,
        final @Nullable Object value
    ) {
        if (path.isBlank()) {
            throw new IllegalArgumentException("Config path cannot be empty");
        }

        final File configFile = new File(new File(plugin.getPlugin().getDataFolder(), "config"), "config.yml");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        configuration.set(path, value);
        try {
            configuration.save(configFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save config path '" + path + "'", exception);
        }
    }

    private @NotNull String resolveCurrentValue(
        final @NotNull Context context
    ) {
        final String value = this.currentValue.get(context);
        return value == null ? "" : value;
    }

    private @NotNull String resolveConfigPath(
        final @NotNull Context context
    ) {
        final String path = this.configPath.get(context);
        return path == null ? "" : path;
    }

    private @NotNull StorageConfigEditSupport.SettingType resolveSettingType(
        final @NotNull Context context
    ) {
        final String rawType = this.settingType.get(context);
        if (rawType == null || rawType.isBlank()) {
            return StorageConfigEditSupport.SettingType.STRING;
        }

        try {
            return StorageConfigEditSupport.SettingType.valueOf(rawType);
        } catch (IllegalArgumentException ignored) {
            return StorageConfigEditSupport.SettingType.STRING;
        }
    }

    private @NotNull Material resolveMaterial(
        final @NotNull StorageConfigEditSupport.SettingType settingType
    ) {
        return switch (settingType) {
            case BOOLEAN -> Material.BLUE_STAINED_GLASS_PANE;
            case INTEGER, LONG, DOUBLE -> Material.YELLOW_STAINED_GLASS_PANE;
            case LIST -> Material.PURPLE_STAINED_GLASS_PANE;
            case STRING -> Material.ORANGE_STAINED_GLASS_PANE;
        };
    }
}
