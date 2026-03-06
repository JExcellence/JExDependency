package com.raindropcentral.rdr.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.repository.RRStorage;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
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
 * Paginated admin storage-control browser.
 *
 * <p>Depending on mode, this browser force-drains or force-resets selected storages.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminStorageControlView extends APaginatedView<StorageAdminStorageControlView.StorageControlEntry> {

    private static final String ADMIN_COMMAND_PERMISSION = "raindroprdr.command.admin";

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the storage-control browser view.
     */
    public StorageAdminStorageControlView() {
        super(StorageAdminPlayerView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_storage_control_ui";
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
        final String modeLabelKey = this.resolveMode(context) == StorageAdminStorageControlMode.FORCE_RESET_STORAGE
            ? "mode.force_reset"
            : "mode.force_drain";
        return Map.of(
            "action_mode", this.i18n(modeLabelKey, context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder()
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
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  < p >  "
        };
    }

    /**
     * Loads all storages for pagination.
     *
     * @param context current context
     * @return async storage list
     */
    @Override
    protected @NotNull CompletableFuture<List<StorageControlEntry>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        if (!this.hasAdminAccess(context.getPlayer())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final RDR plugin = this.rdr.get(context);
        final RRStorage storageRepository = plugin.getStorageRepository();
        if (storageRepository == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<StorageControlEntry> entries = new ArrayList<>();
        for (final RStorage storage : storageRepository.findAllWithPlayer()) {
            if (storage.getId() == null || storage.getPlayer() == null) {
                continue;
            }
            entries.add(new StorageControlEntry(
                storage.getId(),
                storage.getStorageKey(),
                storage.getPlayer().getIdentifier(),
                storage.getStoredSlotCount(),
                storage.getInventorySize()
            ));
        }

        entries.sort(Comparator
            .comparing((StorageControlEntry entry) -> this.getOwnerName(entry.ownerId()))
            .thenComparing(StorageControlEntry::storageKey));
        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders one storage control entry.
     *
     * @param context current context
     * @param builder slot builder
     * @param index zero-based index
     * @param entry entry payload
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull StorageControlEntry entry
    ) {
        builder.withItem(this.createEntryItem(context, entry))
            .onClick(clickContext -> this.handleStorageClick(clickContext, entry));
    }

    /**
     * Renders summary/empty cards.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        if (!this.hasAdminAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        final int storageCount = this.getPagination(render).source() == null
            ? 0
            : this.getPagination(render).source().size();
        render.layoutSlot('s', this.createSummaryItem(player, storageCount, this.resolveMode(render)));
        if (storageCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
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

    private void handleStorageClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull StorageControlEntry entry
    ) {
        clickContext.setCancelled(true);

        if (!this.hasAdminAccess(clickContext.getPlayer())) {
            this.i18n("feedback.access_denied_message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final RDR plugin = this.rdr.get(clickContext);
        final RRStorage storageRepository = plugin.getStorageRepository();
        if (storageRepository == null) {
            this.i18n("feedback.storage_missing_message", clickContext.getPlayer())
                .withPlaceholder("storage_key", entry.storageKey())
                .includePrefix()
                .build()
                .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final RStorage liveStorage = storageRepository.findWithPlayerById(entry.storageId());
        if (liveStorage == null) {
            this.i18n("feedback.storage_missing_message", clickContext.getPlayer())
                .withPlaceholder("storage_key", entry.storageKey())
                .includePrefix()
                .build()
                .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final StorageAdminStorageControlMode mode = this.resolveMode(clickContext);
        if (mode == StorageAdminStorageControlMode.FORCE_DRAIN_STORAGE) {
            liveStorage.setInventory(Map.of());
            liveStorage.clearLease();
            storageRepository.update(liveStorage);
            final int closedViews = StorageAdminForceCloseSupport.closeStorageViews(plugin, liveStorage.getId());
            this.i18n("feedback.drained", clickContext.getPlayer())
                .withPlaceholders(Map.of(
                    "owner", this.getOwnerName(liveStorage.getPlayer() == null ? entry.ownerId() : liveStorage.getPlayer().getIdentifier()),
                    "storage_key", liveStorage.getStorageKey(),
                    "closed_views", closedViews
                ))
                .includePrefix()
                .build()
                .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        liveStorage.setInventory(Map.of());
        liveStorage.setTaxDebtEntries(Map.of());
        liveStorage.setTrustedPlayers(Map.of());
        liveStorage.clearHotkey();
        liveStorage.clearLease();
        storageRepository.update(liveStorage);
        final int closedViews = StorageAdminForceCloseSupport.closeStorageViews(plugin, liveStorage.getId());
        this.i18n("feedback.reset", clickContext.getPlayer())
            .withPlaceholders(Map.of(
                "owner", this.getOwnerName(liveStorage.getPlayer() == null ? entry.ownerId() : liveStorage.getPlayer().getIdentifier()),
                "storage_key", liveStorage.getStorageKey(),
                "closed_views", closedViews
            ))
            .includePrefix()
            .build()
            .sendMessage();
        this.openFreshView(clickContext);
    }

    private void openFreshView(
        final @NotNull Context context
    ) {
        context.openForPlayer(
            StorageAdminStorageControlView.class,
            Map.of(
                "plugin", this.rdr.get(context),
                "actionMode", this.resolveMode(context).name()
            )
        );
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int storageCount,
        final @NotNull StorageAdminStorageControlMode mode
    ) {
        final String modeLabel = this.i18n(
            mode == StorageAdminStorageControlMode.FORCE_RESET_STORAGE ? "mode.force_reset" : "mode.force_drain",
            player
        ).build().getI18nVersionWrapper().asPlaceholder();

        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "storage_count", storageCount,
                    "action_mode", modeLabel
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEntryItem(
        final @NotNull Context context,
        final @NotNull StorageControlEntry entry
    ) {
        final Player player = context.getPlayer();
        final StorageAdminStorageControlMode mode = this.resolveMode(context);
        final String actionLabel = this.i18n(
            mode == StorageAdminStorageControlMode.FORCE_RESET_STORAGE ? "entry.action.force_reset" : "entry.action.force_drain",
            player
        ).build().getI18nVersionWrapper().asPlaceholder();

        return UnifiedBuilderFactory.item(mode == StorageAdminStorageControlMode.FORCE_RESET_STORAGE ? Material.BARRIER : Material.BUCKET)
            .setName(this.i18n("entry.name", player)
                .withPlaceholders(Map.of(
                    "owner", this.getOwnerName(entry.ownerId()),
                    "storage_key", entry.storageKey()
                ))
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "owner", this.getOwnerName(entry.ownerId()),
                    "storage_key", entry.storageKey(),
                    "storage_id", entry.storageId(),
                    "stored_slots", entry.storedSlots(),
                    "inventory_size", entry.inventorySize(),
                    "action", actionLabel
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.empty.name", player).build().component())
            .setLore(this.i18n("feedback.empty.lore", player).build().children())
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

    private @NotNull StorageAdminStorageControlMode resolveMode(
        final @NotNull Context context
    ) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return StorageAdminStorageControlMode.FORCE_DRAIN_STORAGE;
        }

        final Object rawMode = data.get("actionMode");
        return rawMode instanceof String textValue
            ? StorageAdminStorageControlMode.fromRaw(textValue)
            : StorageAdminStorageControlMode.FORCE_DRAIN_STORAGE;
    }

    private @NotNull String getOwnerName(
        final @NotNull UUID ownerId
    ) {
        final String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        return ownerName == null || ownerName.isBlank() ? ownerId.toString() : ownerName;
    }

    private boolean hasAdminAccess(
        final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }

    /**
     * Immutable storage-control list entry.
     *
     * @param storageId storage identifier
     * @param storageKey storage key
     * @param ownerId owner UUID
     * @param storedSlots occupied slot count
     * @param inventorySize storage slot capacity
     */
    protected record StorageControlEntry(
        @NotNull Long storageId,
        @NotNull String storageKey,
        @NotNull UUID ownerId,
        int storedSlots,
        int inventorySize
    ) {
    }
}
