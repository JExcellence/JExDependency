/*
 * StorageTrustedView.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
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

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.entity.StorageTrustStatus;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;

/**
 * Paginated owner management view for shared storage trust access.
 *
 * <p>Owners can cycle each known player's trust status between {@link StorageTrustStatus#PUBLIC},
 * {@link StorageTrustStatus#ASSOCIATE}, and {@link StorageTrustStatus#TRUSTED}. Associates may deposit
 * into the storage, while trusted players may both deposit and withdraw.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageTrustedView extends APaginatedView<StorageTrustedView.TrustedPlayerEntry> {

    private final State<RDR> rdr = initialState("plugin");
    private final State<Long> storageId = initialState("storage_id");
    private final State<String> storageKey = initialState("storage_key");

    /**
     * Creates the storage trusted-player management view.
     */
    public StorageTrustedView() {
        super(StorageSettingsView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return storage trusted view translation key prefix
     */
    @Override
    protected String getKey() {
        return "storage_trusted_ui";
    }

    /**
     * Returns the inventory layout for the trusted-player manager.
     *
     * @return six-row layout with summary, paginated entries, and navigation support
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
     * Supplies title placeholders for the targeted storage.
     *
     * @param context open context for the current viewer
     * @return placeholder map containing the storage key
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext context) {
        final String targetStorageKey = this.storageKey.get(context);
        return Map.of("storage_key", targetStorageKey == null ? "unknown" : targetStorageKey);
    }

    /**
     * Loads the set of known players whose storage trust status may be managed by the owner.
     *
     * @param context current view context
     * @return completed future containing trusted-player entries
     */
    @Override
    protected CompletableFuture<List<TrustedPlayerEntry>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDR plugin = this.rdr.get(context);
        final RStorage storage = this.findStorage(context);
        if (storage == null || !storage.isOwner(context.getPlayer().getUniqueId()) || !plugin.canChangeStorageSettings()) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<TrustedPlayerEntry> entries = new ArrayList<>();
        for (final OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer == null || offlinePlayer.getUniqueId() == null) {
                continue;
            }

            if (storage.isOwner(offlinePlayer.getUniqueId())) {
                continue;
            }

            entries.add(new TrustedPlayerEntry(
                offlinePlayer.getUniqueId(),
                this.resolvePlayerName(offlinePlayer),
                storage.getTrustStatus(offlinePlayer.getUniqueId())
            ));
        }

        entries.sort(Comparator.comparing(TrustedPlayerEntry::playerName, String.CASE_INSENSITIVE_ORDER));
        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders a single trusted-player entry in the paginated grid.
     *
     * @param context current view context
     * @param builder item component builder used for the entry slot
     * @param index zero-based entry index in the source list
     * @param entry trusted-player entry being rendered
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull TrustedPlayerEntry entry
    ) {
        builder.withItem(this.createEntryItem(context.getPlayer(), entry))
            .onClick(clickContext -> this.handleEntryClick(clickContext, entry));
    }

    /**
     * Renders the storage summary and fallback state items when needed.
     *
     * @param render render context for slot registration
     * @param player player viewing the trusted-player manager
     */
    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RStorage storage = this.findStorage(render);

        if (storage == null) {
            render.slot(4).renderWith(() -> this.createMissingStorageItem(player));
            return;
        }

        if (!storage.isOwner(player.getUniqueId())) {
            render.slot(4).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot('s').renderWith(() -> this.createSummaryItem(player, storage));

        if (!this.rdr.get(render).canChangeStorageSettings()) {
            render.slot(22).renderWith(() -> this.createEditionLockedItem(player));
            return;
        }

        if (Bukkit.getOfflinePlayers().length <= 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels item interaction so GUI entries cannot be moved.
     *
     * @param click slot click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleEntryClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TrustedPlayerEntry entry
    ) {
        final RDR plugin = this.rdr.get(clickContext);
        final RStorage storage = this.findStorage(clickContext);
        if (storage == null) {
            this.i18n("feedback.storage_missing.message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        if (!storage.isOwner(clickContext.getPlayer().getUniqueId())) {
            this.i18n("feedback.not_owner", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        if (!plugin.canChangeStorageSettings()) {
            this.sendConfigEditDisabled(clickContext.getPlayer());
            this.openFreshView(clickContext);
            return;
        }

        final StorageTrustStatus updatedStatus = storage.getTrustStatus(entry.playerId()).next();
        final boolean updated = plugin.getStorageRepository() != null
            && plugin.getStorageRepository().updateTrustStatus(
                storage.getId(),
                clickContext.getPlayer().getUniqueId(),
                entry.playerId(),
                updatedStatus
            );
        if (!updated) {
            this.i18n("feedback.storage_missing.message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        this.i18n("feedback.updated." + updatedStatus.name().toLowerCase(Locale.ROOT), clickContext.getPlayer())
            .withPlaceholder("player", entry.playerName())
            .includePrefix()
            .build()
            .sendMessage();
        this.openFreshView(clickContext);
    }

    private void openFreshView(final @NotNull Context context) {
        final Long targetStorageId = this.storageId.get(context);
        final String targetStorageKey = this.storageKey.get(context);
        if (targetStorageId == null || targetStorageKey == null) {
            return;
        }

        context.openForPlayer(
            StorageTrustedView.class,
            Map.of(
                "plugin", this.rdr.get(context),
                "storage_id", targetStorageId,
                "storage_key", targetStorageKey
            )
        );
    }

    private @Nullable RStorage findStorage(final @NotNull Context context) {
        final RDR plugin = this.rdr.get(context);
        final Long targetStorageId = this.storageId.get(context);
        return plugin.getStorageRepository() == null || targetStorageId == null
            ? null
            : plugin.getStorageRepository().findWithPlayerById(targetStorageId);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        return UnifiedBuilderFactory.item(Material.BOOKSHELF)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "storage_key", storage.getStorageKey(),
                    "owner_name", this.resolveOwnerName(storage),
                    "associate_count", storage.getTrustedPlayerCount(StorageTrustStatus.ASSOCIATE),
                    "trusted_count", storage.getTrustedPlayerCount(StorageTrustStatus.TRUSTED)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEntryItem(
        final @NotNull Player player,
        final @NotNull TrustedPlayerEntry entry
    ) {
        final String statusKey = entry.status().name().toLowerCase(Locale.ROOT);
        return UnifiedBuilderFactory.item(this.resolveMaterial(entry.status()))
            .setName(this.i18n("entry." + statusKey + ".name", player)
                .withPlaceholder("player", entry.playerName())
                .build()
                .component())
            .setLore(this.i18n("entry." + statusKey + ".lore", player)
                .withPlaceholder("player", entry.playerName())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull Material resolveMaterial(final @NotNull StorageTrustStatus status) {
        return switch (status) {
            case PUBLIC -> Material.GRAY_DYE;
            case ASSOCIATE -> Material.YELLOW_DYE;
            case TRUSTED -> Material.LIME_DYE;
        };
    }

    private @NotNull ItemStack createLockedItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.locked.name", player).build().component())
            .setLore(this.i18n("feedback.locked.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingStorageItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.storage_missing.name", player).build().component())
            .setLore(this.i18n("feedback.storage_missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.empty.name", player).build().component())
            .setLore(this.i18n("feedback.empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEditionLockedItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.edition_locked.name", player).build().component())
            .setLore(this.i18n("feedback.edition_locked.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull String resolvePlayerName(final @NotNull OfflinePlayer player) {
        return player.getName() == null || player.getName().isBlank()
            ? player.getUniqueId().toString()
            : player.getName();
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

    private void sendConfigEditDisabled(final @NotNull Player player) {
        new I18n.Builder("storage.message.config_edit_disabled", player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    record TrustedPlayerEntry(
        @NotNull UUID playerId,
        @NotNull String playerName,
        @NotNull StorageTrustStatus status
    ) {}
}
