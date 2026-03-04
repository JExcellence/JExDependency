/*
 * StorageSettingsView.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import java.util.HashMap;
import java.util.Map;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.entity.StorageTrustStatus;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-storage settings hub for owner-only storage management actions.
 *
 * <p>This view exposes the trusted-player configuration screen and the hotkey binding entry point for a
 * specific storage. Non-owners may view the storage summary but cannot modify settings.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageSettingsView extends BaseView {

    private final State<RDR> rdr = initialState("plugin");
    private final State<Long> storageId = initialState("storage_id");
    private final State<String> storageKey = initialState("storage_key");

    /**
     * Creates the storage settings view.
     */
    public StorageSettingsView() {
        super(StoragePlayerView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return storage settings translation key prefix
     */
    @Override
    protected String getKey() {
        return "storage_settings_ui";
    }

    /**
     * Supplies title placeholders for the targeted storage.
     *
     * @param context open context for the current player
     * @return placeholder map containing the storage key
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext context) {
        final String targetStorageKey = this.storageKey.get(context);
        return Map.of("storage_key", targetStorageKey == null ? "unknown" : targetStorageKey);
    }

    /**
     * Returns the compact settings layout.
     *
     * @return three-row layout with summary and settings actions
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "   t h   ",
            "         "
        };
    }

    /**
     * Disables automatic filler item placement for this compact settings menu.
     *
     * @return {@code false} so only explicit controls render
     */
    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    /**
     * Renders the storage summary and owner-only settings actions.
     *
     * @param render render context for slot registration
     * @param player player viewing the storage settings
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RStorage storage = this.findStorage(render);
        if (storage == null) {
            render.layoutSlot('s')
                .renderWith(() -> this.createMissingItem(player));
            return;
        }

        final boolean owner = storage.isOwner(player.getUniqueId());
        render.layoutSlot('s')
            .renderWith(() -> this.createSummaryItem(player, storage));

        render.layoutSlot('t')
            .withItem(owner ? this.createTrustedItem(player, storage) : this.createLockedActionItem(player, "trusted"))
            .onClick(clickContext -> {
                if (owner) {
                    clickContext.openForPlayer(StorageTrustedView.class, this.buildStorageInitialData(clickContext, storage));
                }
            });

        render.layoutSlot('h')
            .withItem(owner ? this.createHotkeyItem(player, storage) : this.createLockedActionItem(player, "hotkey"))
            .onClick(clickContext -> {
                if (owner) {
                    clickContext.openForPlayer(StorageHotkeyAnvilView.class, this.buildHotkeyInitialData(clickContext, storage));
                }
            });
    }

    /**
     * Cancels item interaction so settings items cannot be moved.
     *
     * @param click slot click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @Nullable RStorage findStorage(final @NotNull Context context) {
        final RDR plugin = this.rdr.get(context);
        final Long targetStorageId = this.storageId.get(context);
        return plugin.getStorageRepository() == null || targetStorageId == null
            ? null
            : plugin.getStorageRepository().findWithPlayerById(targetStorageId);
    }

    private @NotNull Map<String, Object> buildStorageInitialData(
        final @NotNull Context context,
        final @NotNull RStorage storage
    ) {
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("plugin", this.rdr.get(context));
        initialData.put("storage_id", storage.getId());
        initialData.put("storage_key", storage.getStorageKey());
        return initialData;
    }

    private @NotNull Map<String, Object> buildHotkeyInitialData(
        final @NotNull Context context,
        final @NotNull RStorage storage
    ) {
        final Map<String, Object> initialData = new HashMap<>(this.buildStorageInitialData(context, storage));
        initialData.put("max_hotkeys", this.rdr.get(context).getDefaultConfig().getMaxHotkeys());
        initialData.put("storage_hotkey", storage.getHotkey() == null ? 0 : storage.getHotkey());
        initialData.put("return_to_settings", true);
        return initialData;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        return UnifiedBuilderFactory.item(Material.BARREL)
            .setName(this.i18n("summary.name", player)
                .withPlaceholder("storage_key", storage.getStorageKey())
                .build()
                .component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "storage_key", storage.getStorageKey(),
                    "owner_name", this.resolveOwnerName(storage),
                    "hotkey_display", storage.getHotkey() == null ? "-" : storage.getHotkey(),
                    "associate_count", storage.getTrustedPlayerCount(StorageTrustStatus.ASSOCIATE),
                    "trusted_count", storage.getTrustedPlayerCount(StorageTrustStatus.TRUSTED),
                    "access_level", storage.isOwner(player.getUniqueId())
                        ? this.i18n("summary.access.owner", player).build().getI18nVersionWrapper().asPlaceholder()
                        : this.i18n(
                            "summary.access." + storage.getTrustStatus(player.getUniqueId()).name().toLowerCase(),
                            player
                        ).build().getI18nVersionWrapper().asPlaceholder()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createTrustedItem(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        return UnifiedBuilderFactory.item(Material.BOOKSHELF)
            .setName(this.i18n("trusted.name", player).build().component())
            .setLore(this.i18n("trusted.lore", player)
                .withPlaceholders(Map.of(
                    "associate_count", storage.getTrustedPlayerCount(StorageTrustStatus.ASSOCIATE),
                    "trusted_count", storage.getTrustedPlayerCount(StorageTrustStatus.TRUSTED)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createHotkeyItem(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        return UnifiedBuilderFactory.item(Material.TRIPWIRE_HOOK)
            .setName(this.i18n("hotkey.name", player).build().component())
            .setLore(this.i18n("hotkey.lore", player)
                .withPlaceholder("hotkey_display", storage.getHotkey() == null ? "-" : storage.getHotkey())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createLockedActionItem(
        final @NotNull Player player,
        final @NotNull String action
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n(action + "_locked.name", player).build().component())
            .setLore(this.i18n(action + "_locked.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull String resolveOwnerName(final @NotNull RStorage storage) {
        if (storage.getPlayer() == null) {
            return "Unknown";
        }

        final String ownerName = Bukkit.getOfflinePlayer(storage.getPlayer().getIdentifier()).getName();
        return ownerName == null || ownerName.isBlank()
            ? storage.getPlayer().getIdentifier().toString()
            : ownerName;
    }
}