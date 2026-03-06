package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.service.shop.ShopAdminPlayerSettingsService;
import com.raindropcentral.rds.view.shop.anvil.ShopAdminOverrideValueAnvilView;
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
 * Editor view for group-specific max-shop and discount overrides.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopAdminGroupEditView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the group override editor.
     */
    public ShopAdminGroupEditView() {
        super(ShopAdminPlayerView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_admin_group_edit_ui";
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
        final ShopAdminPlayerSettingsService settingsService = this.rds.get(render).getShopAdminPlayerSettingsService();
        final ShopAdminPlayerSettingsService.GroupOverride override = settingsService == null || selectedGroup == null
                ? null
                : settingsService.getGroupOverride(selectedGroup);

        render.layoutSlot('s', this.createSummaryItem(player, selectedGroup, override, availableGroups.size()));

        render.layoutSlot('n', this.createSelectGroupItem(player, selectedGroup, availableGroups.size()))
                .onClick(this::handleSelectGroupClick);

        render.layoutSlot('m', this.createMaximumShopsItem(player, override))
                .onClick(clickContext -> this.handleGroupValueEdit(
                        clickContext,
                        ShopAdminOverrideEditMode.GROUP_MAX_SHOPS,
                        selectedGroup,
                        override != null && override.maximumShops() != null ? Integer.toString(override.maximumShops()) : ""
                ));

        render.layoutSlot('d', this.createDiscountItem(player, override))
                .onClick(clickContext -> this.handleGroupValueEdit(
                        clickContext,
                        ShopAdminOverrideEditMode.GROUP_DISCOUNT,
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
            final @NotNull ShopAdminOverrideEditMode editMode,
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
                ShopAdminOverrideValueAnvilView.class,
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

        final ShopAdminPlayerSettingsService settingsService = this.rds.get(clickContext).getShopAdminPlayerSettingsService();
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
                ShopAdminGroupEditView.class,
                Map.of(
                        "plugin", this.rds.get(clickContext),
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
                ShopAdminGroupEditView.class,
                Map.of(
                        "plugin", this.rds.get(clickContext),
                        "groupName", selectedGroup
                )
        );
    }

    private @NotNull Map<String, Object> buildEditData(
            final @NotNull Context context,
            final @NotNull ShopAdminOverrideEditMode editMode,
            final @Nullable String selectedGroup,
            final @NotNull String currentValue
    ) {
        final Map<String, Object> data = new HashMap<>();
        data.put("plugin", this.rds.get(context));
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

        final ShopAdminPlayerSettingsService settingsService = this.rds.get(context).getShopAdminPlayerSettingsService();
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
        final ShopAdminPlayerSettingsService settingsService = this.rds.get(context).getShopAdminPlayerSettingsService();
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
            final @Nullable ShopAdminPlayerSettingsService.GroupOverride override,
            final int availableGroupCount
    ) {
        final String groupNameDisplay = selectedGroup == null || selectedGroup.isBlank()
                ? this.i18n("summary.none", player).build().getI18nVersionWrapper().asPlaceholder()
                : selectedGroup;
        final String maxShopsDisplay = override == null || override.maximumShops() == null
                ? this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder()
                : this.formatMaxShops(player, override.maximumShops());
        final String discountDisplay = override == null || override.discountPercent() == null
                ? this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder()
                : formatPercent(override.discountPercent());

        return UnifiedBuilderFactory.item(Material.BOOK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "group_name", groupNameDisplay,
                                "override_max_shops", maxShopsDisplay,
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

    private @NotNull ItemStack createMaximumShopsItem(
            final @NotNull Player player,
            final @Nullable ShopAdminPlayerSettingsService.GroupOverride override
    ) {
        final String value = override == null || override.maximumShops() == null
                ? this.i18n("summary.inherit", player).build().getI18nVersionWrapper().asPlaceholder()
                : this.formatMaxShops(player, override.maximumShops());

        return UnifiedBuilderFactory.item(Material.BEACON)
                .setName(this.i18n("actions.maximum_shops.name", player).build().component())
                .setLore(this.i18n("actions.maximum_shops.lore", player)
                        .withPlaceholder("override_max_shops", value)
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createDiscountItem(
            final @NotNull Player player,
            final @Nullable ShopAdminPlayerSettingsService.GroupOverride override
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
