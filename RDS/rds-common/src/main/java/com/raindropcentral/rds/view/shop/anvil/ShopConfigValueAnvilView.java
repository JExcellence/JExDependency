package com.raindropcentral.rds.view.shop.anvil;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.service.config.ShopConfigEditSupport;
import com.raindropcentral.rds.view.shop.ShopConfigView;
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
import java.util.Map;

/**
 * Renders the config value editor anvil for a selected config path.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopConfigValueAnvilView extends AbstractAnvilView {

    private final State<RDS> rds = initialState("plugin");
    private final State<String> configPath = initialState("configPath");
    private final State<String> currentValue = initialState("currentValue");
    private final State<String> settingType = initialState("settingType");

    /**
     * Creates a new config value editor anvil view.
     */
    public ShopConfigValueAnvilView() {
        super(ShopConfigView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_config_value_anvil_ui";
    }

    @Override
    protected @NotNull Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final ShopConfigEditSupport.SettingType type = this.resolveSettingType(context);
        final Object parsedValue = ShopConfigEditSupport.parseInput(input, type);
        this.saveConfigValue(this.rds.get(context), this.resolveConfigPath(context), parsedValue);
        return parsedValue;
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        return Map.of(
                "config_path", this.resolveConfigPath(context),
                "current_value", this.resolveCurrentValue(context),
                "value_type", this.resolveSettingType(context).name().toLowerCase(java.util.Locale.ROOT)
        );
    }

    @Override
    protected @NotNull String getInitialInputText(
            final @NotNull OpenContext context
    ) {
        return this.resolveCurrentValue(context);
    }

    @Override
    protected boolean isValidInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        try {
            ShopConfigEditSupport.parseInput(input, this.resolveSettingType(context));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

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
                                "value_type", this.resolveSettingType(render).name().toLowerCase(java.util.Locale.ROOT)
                        ))
                        .build()
                        .children())
                .build();

        render.firstSlot(inputSlotItem);
    }

    @Override
    protected void onValidationFailed(
            final @Nullable String input,
            final @NotNull Context context
    ) {
        this.i18n("error.invalid_value", context.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "input", input == null ? "" : input,
                        "value_type", this.resolveSettingType(context).name().toLowerCase(java.util.Locale.ROOT)
                ))
                .build()
                .sendMessage();
    }

    @Override
    protected @NotNull Map<String, Object> prepareResultData(
            final @Nullable Object processingResult,
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final Map<String, Object> result = super.prepareResultData(processingResult, input, context);
        result.put("plugin", this.rds.get(context));
        result.put("updatedPath", this.resolveConfigPath(context));
        result.put("updatedValue", ShopConfigEditSupport.formatValue(processingResult));
        return result;
    }

    private void saveConfigValue(
            final @NotNull RDS plugin,
            final @NotNull String path,
            final @Nullable Object value
    ) {
        if (path.isBlank()) {
            throw new IllegalArgumentException("Config path cannot be empty");
        }

        final File configFile = new File(new File(plugin.getDataFolder(), "config"), "config.yml");
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

    private @NotNull ShopConfigEditSupport.SettingType resolveSettingType(
            final @NotNull Context context
    ) {
        final String rawType = this.settingType.get(context);
        if (rawType == null || rawType.isBlank()) {
            return ShopConfigEditSupport.SettingType.STRING;
        }

        try {
            return ShopConfigEditSupport.SettingType.valueOf(rawType);
        } catch (IllegalArgumentException ignored) {
            return ShopConfigEditSupport.SettingType.STRING;
        }
    }

    private @NotNull Material resolveMaterial(
            final @NotNull ShopConfigEditSupport.SettingType settingType
    ) {
        return switch (settingType) {
            case BOOLEAN -> Material.BLUE_STAINED_GLASS_PANE;
            case INTEGER, LONG, DOUBLE -> Material.YELLOW_STAINED_GLASS_PANE;
            case LIST -> Material.PURPLE_STAINED_GLASS_PANE;
            case STRING -> Material.ORANGE_STAINED_GLASS_PANE;
        };
    }
}
