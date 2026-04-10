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
import com.raindropcentral.rdr.commands.EPRRAction;
import com.raindropcentral.rdr.database.repository.RRStorage;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Anvil input view used to bind a numeric quick-access hotkey to a storage.
 *
 * <p>Players open this view from {@link StoragePlayerView} with a right click, enter a numeric value,
 * and persist a per-player hotkey that can later be used through {@code /rr <hotkey>}.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageHotkeyAnvilView extends AbstractAnvilView {

    private final State<RDR> rdr = initialState("plugin");
    private final State<Long> storageId = initialState("storage_id");
    private final State<String> storageKey = initialState("storage_key");
    private final State<Integer> storageHotkey = initialState("storage_hotkey");
    private final State<Integer> maxHotkeys = initialState("max_hotkeys");
    private final State<Boolean> returnToSettings = initialState("return_to_settings");

    /**
     * Creates the storage hotkey assignment view.
     */
    public StorageHotkeyAnvilView() {
        super(StoragePlayerView.class);
    }

    /**
     * Returns the translation namespace used by this anvil view.
     *
     * @return storage hotkey anvil translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_hotkey_anvil_ui";
    }

    /**
     * Persists the entered hotkey on the targeted storage.
     *
     * @param input user-entered hotkey text
     * @param context current anvil interaction context
     * @return assigned hotkey value, or {@code null} when persistence could not be completed
     */
    @Override
    protected Object processInput(
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final RDR plugin = this.rdr.get(context);
        final RRStorage storageRepository = plugin.getStorageRepository();
        final Long targetStorageId = this.storageId.get(context);
        final String targetStorageKey = this.storageKey.get(context);
        final Player player = context.getPlayer();
        final int hotkey = Integer.parseInt(input.trim());

        if (!plugin.canChangeStorageSettings()) {
            new I18n.Builder("storage.message.config_edit_disabled", player)
                .includePrefix()
                .build()
                .sendMessage();
            return null;
        }

        if (storageRepository == null || targetStorageId == null || targetStorageKey == null) {
            new I18n.Builder("storage.message.unavailable", player)
                .withPlaceholder("storage_key", targetStorageKey == null ? "unknown" : targetStorageKey)
                .build()
                .sendMessage();
            return null;
        }

        final boolean assigned = storageRepository.assignHotkey(
            targetStorageId,
            player.getUniqueId(),
            hotkey
        );
        if (!assigned) {
            new I18n.Builder("storage.message.unavailable", player)
                .withPlaceholder("storage_key", targetStorageKey)
                .build()
                .sendMessage();
            return null;
        }

        new I18n.Builder("storage.message.hotkey_saved", player)
            .withPlaceholders(Map.of(
                "storage_key", targetStorageKey,
                "hotkey", hotkey
            ))
            .build()
            .sendMessage();
        this.returnToParentView(plugin, player, context);
        return hotkey;
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
     * Returns the current hotkey as the initial editable value, falling back to {@code 1}.
     *
     * @param context open context for the current player
     * @return initial anvil input text
     */
    @Override
    protected @NotNull String getInitialInputText(final @NotNull OpenContext context) {
        final Integer currentHotkey = this.normalizeHotkey(this.storageHotkey.get(context));
        return currentHotkey == null ? "hotkey" : Integer.toString(currentHotkey);
    }

    /**
     * Validates the entered value against the configured hotkey range.
     *
     * @param input user-entered text
     * @param context current interaction context
     * @return {@code true} when the input is numeric and within range
     */
    @Override
    protected boolean isValidInput(
        final @NotNull String input,
        final @NotNull Context context
    ) {
        try {
            final int hotkey = Integer.parseInt(input.trim());
            return hotkey >= 1 && hotkey <= this.resolveMaxHotkeys(context);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Renders the anvil input slot with the current hotkey prompt and configured range.
     *
     * @param render render context
     * @param player player viewing the anvil
     */
    @Override
    protected void setupFirstSlot(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final Integer currentHotkey = this.normalizeHotkey(this.storageHotkey.get(render));
        final ItemStack inputItem = UnifiedBuilderFactory.item(Material.NAME_TAG)
            .setName(Component.text(currentHotkey == null ? "hotkey" : Integer.toString(currentHotkey)))
            .setLore(this.i18n("input.lore", player)
                .withPlaceholder("max_hotkeys", this.resolveMaxHotkeys(render))
                .build()
                .children())
            .build();

        render.firstSlot(inputItem);
    }

    /**
     * Sends a range-specific validation message for invalid hotkey input.
     *
     * @param input invalid user input
     * @param context current interaction context
     */
    @Override
    protected void onValidationFailed(
        final @Nullable String input,
        final @NotNull Context context
    ) {
        final String normalizedInput = input == null ? "" : input.trim();
        if (EPRRAction.isReservedSubcommandLabel(normalizedInput)) {
            this.i18n("error.reserved_hotkey_name", context.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                    "input", normalizedInput,
                    "max_hotkeys", this.resolveMaxHotkeys(context)
                ))
                .build()
                .sendMessage();
            return;
        }

        this.i18n("error.invalid_hotkey", context.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of(
                "input", normalizedInput,
                "max_hotkeys", this.resolveMaxHotkeys(context)
            ))
            .build()
            .sendMessage();
    }

    /**
     * Preserves the plugin initial data required to reopen the parent storage list.
     *
     * @param processingResult hotkey assignment result
     * @param input submitted input text
     * @param context current interaction context
     * @return initial data payload for the parent view
     */
    @Override
    protected @NotNull Map<String, Object> prepareResultData(
        final @Nullable Object processingResult,
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final Map<String, Object> resultData = super.prepareResultData(processingResult, input, context);
        resultData.put("plugin", this.rdr.get(context));
        resultData.put("storage_id", this.storageId.get(context));
        resultData.put("storage_key", this.storageKey.get(context));
        resultData.put("return_to_settings", this.returnToSettings.get(context));
        return resultData;
    }

    private int resolveMaxHotkeys(final @NotNull Context context) {
        final Integer configuredMaxHotkeys = this.maxHotkeys.get(context);
        if (configuredMaxHotkeys != null && configuredMaxHotkeys > 0) {
            return configuredMaxHotkeys;
        }

        final RDR plugin = this.rdr.get(context);
        return plugin == null ? 9 : plugin.getDefaultConfig().getMaxHotkeys();
    }

    private @Nullable Integer normalizeHotkey(final @Nullable Integer hotkey) {
        if (hotkey == null || hotkey < 1) {
            return null;
        }
        return hotkey;
    }

    private void returnToParentView(
        final @NotNull RDR plugin,
        final @NotNull Player player,
        final @NotNull Context context
    ) {
        plugin.getScheduler().runDelayed(() -> {
            if (!player.isOnline()) {
                return;
            }

            player.closeInventory();
            plugin.getScheduler().runDelayed(() -> {
                if (!player.isOnline()) {
                    return;
                }

                if (Boolean.TRUE.equals(this.returnToSettings.get(context))) {
                    final Long targetStorageId = this.storageId.get(context);
                    final String targetStorageKey = this.storageKey.get(context);
                    if (targetStorageId != null && targetStorageKey != null) {
                        plugin.getViewFrame().open(
                            StorageSettingsView.class,
                            player,
                            Map.of(
                                "plugin", plugin,
                                "storage_id", targetStorageId,
                                "storage_key", targetStorageKey
                            )
                        );
                        return;
                    }
                }

                plugin.getViewFrame().open(StoragePlayerView.class, player, Map.of("plugin", plugin));
            }, 1L);
        }, 1L);
    }
}
