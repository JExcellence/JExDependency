package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.service.shop.ShopAdminPlayerSettingsService;
import com.raindropcentral.rds.view.shop.anvil.ShopAdminOverrideValueAnvilView;
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Editor view for a specific player's max-shop and discount overrides.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopAdminPlayerEditView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private final State<RDS> rds = initialState("plugin");
    private final State<UUID> targetUuid = initialState("targetUuid");
    private final State<String> targetName = initialState("targetName");

    /**
     * Creates the player override editor.
     */
    public ShopAdminPlayerEditView() {
        super(ShopAdminPlayerSelectView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_admin_player_edit_ui";
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
                "player_name", this.resolveTargetName(context)
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

        final RDS plugin = this.rds.get(render);
        final ShopAdminPlayerSettingsService settingsService = plugin.getShopAdminPlayerSettingsService();
        final ShopAdminPlayerSettingsService.PlayerOverride override = settingsService == null
                ? null
                : settingsService.getPlayerOverride(selectedPlayerId);
        final ConfigSection config = plugin.getDefaultConfig();

        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(selectedPlayerId);
        final Player onlinePlayer = offlinePlayer.getPlayer();
        final int effectiveMaxShops = onlinePlayer == null
                ? plugin.getMaximumShops(selectedPlayerId, config)
                : plugin.getMaximumShops(onlinePlayer, config);
        final double effectiveDiscount = onlinePlayer == null
                ? (override != null && override.discountPercent() != null ? override.discountPercent() : 0.0D)
                : plugin.getShopDiscountPercent(onlinePlayer);

        render.layoutSlot('s', this.createSummaryItem(
                player,
                this.resolveTargetName(render),
                selectedPlayerId,
                override,
                effectiveMaxShops,
                effectiveDiscount
        ));

        render.layoutSlot('m', this.createMaximumShopsItem(player, override, effectiveMaxShops))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopAdminOverrideValueAnvilView.class,
                        this.buildEditData(
                                clickContext,
                                ShopAdminOverrideEditMode.PLAYER_MAX_SHOPS,
                                selectedPlayerId,
                                this.resolveTargetName(clickContext),
                                override != null && override.maximumShops() != null
                                        ? Integer.toString(override.maximumShops())
                                        : Integer.toString(effectiveMaxShops)
                        )
                ));

        render.layoutSlot('d', this.createDiscountItem(player, override, effectiveDiscount))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopAdminOverrideValueAnvilView.class,
                        this.buildEditData(
                                clickContext,
                                ShopAdminOverrideEditMode.PLAYER_DISCOUNT,
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

        final ShopAdminPlayerSettingsService settingsService = this.rds.get(clickContext).getShopAdminPlayerSettingsService();
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
                ShopAdminPlayerEditView.class,
                Map.of(
                        "plugin", this.rds.get(clickContext),
                        "targetUuid", selectedPlayerId,
                        "targetName", this.resolveTargetName(clickContext)
                )
        );
    }

    private @NotNull Map<String, Object> buildEditData(
            final @NotNull Context context,
            final @NotNull ShopAdminOverrideEditMode editMode,
            final @NotNull UUID playerId,
            final @NotNull String playerName,
            final @NotNull String currentValue
    ) {
        final Map<String, Object> data = new HashMap<>();
        data.put("plugin", this.rds.get(context));
        data.put("editMode", editMode.name());
        data.put("targetUuid", playerId);
        data.put("targetName", playerName);
        data.put("currentValue", currentValue);
        return data;
    }

    private @Nullable UUID resolveTargetUuid(
            final @NotNull Context context
    ) {
        final UUID configuredPlayerId = this.targetUuid.get(context);
        return configuredPlayerId == null ? null : configuredPlayerId;
    }

    private @NotNull String resolveTargetName(
            final @NotNull Context context
    ) {
        final String configuredName = this.targetName.get(context);
        if (configuredName != null && !configuredName.isBlank()) {
            return configuredName;
        }

        final UUID playerId = this.resolveTargetUuid(context);
        return playerId == null ? "Unknown" : playerId.toString();
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull String selectedName,
            final @NotNull UUID selectedPlayerId,
            final @Nullable ShopAdminPlayerSettingsService.PlayerOverride override,
            final int effectiveMaxShops,
            final double effectiveDiscount
    ) {
        final String overrideMaxDisplay = override != null && override.maximumShops() != null
                ? this.formatMaxShops(player, override.maximumShops())
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
                                "override_max_shops", overrideMaxDisplay,
                                "effective_max_shops", this.formatMaxShops(player, effectiveMaxShops),
                                "override_discount", overrideDiscountDisplay,
                                "effective_discount", formatPercent(effectiveDiscount)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createMaximumShopsItem(
            final @NotNull Player player,
            final @Nullable ShopAdminPlayerSettingsService.PlayerOverride override,
            final int effectiveMaxShops
    ) {
        final String overrideValue = override != null && override.maximumShops() != null
                ? this.formatMaxShops(player, override.maximumShops())
                : this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder();

        return UnifiedBuilderFactory.item(Material.BEACON)
                .setName(this.i18n("actions.maximum_shops.name", player).build().component())
                .setLore(this.i18n("actions.maximum_shops.lore", player)
                        .withPlaceholders(Map.of(
                                "override_max_shops", overrideValue,
                                "effective_max_shops", this.formatMaxShops(player, effectiveMaxShops)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createDiscountItem(
            final @NotNull Player player,
            final @Nullable ShopAdminPlayerSettingsService.PlayerOverride override,
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

    private @NotNull String formatMaxShops(
            final @NotNull Player player,
            final int maxShops
    ) {
        if (maxShops > 0) {
            return Integer.toString(maxShops);
        }
        return this.i18n("summary.unlimited", player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
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
