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

package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modal confirmation dialog implementing a two-column accept/decline pattern with localized glass.
 * panes and shared navigation from {@link BaseView}.
 *
 * <p>The view derives its translations from dynamically provided keys, enabling per-use messaging
 * while leveraging head utilities for back navigation.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class ConfirmationView extends BaseView {

    /**
     * Holds the translation key namespace used to resolve confirm and cancel strings.
     */
    private final State<String> customKey = initialState("key");
    /**
     * Captures the initial data map supplied when opening the dialog so it can be merged back on.
     * completion.
     */
    private final State<Map<String, Object>> initialData = initialState("initialData");
    /**
     * Optional callback executed with the user's decision after navigation occurs.
     */
    private final State<Consumer<Boolean>> callback = initialState("callback");

    /**
     * Executes ConfirmationView.
     */
    public ConfirmationView() {
        super();
    }

    /**
     * Provides a default translation namespace that callers replace via {@link Builder}.
     *
     * @return the base translation key for fallback scenarios
     */
    @Override
    protected String getKey() {
        return "";
    }

    /**
     * Declares a six-row layout with dedicated columns for confirm and cancel buttons.
     *
     * @return the template rows representing the dialog layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "         ",
                " ccc xxx ",
                " ccc xxx ",
                " ccc xxx ",
                "         ",
                "b        "
        };
    }

    /**
     * Applies the localized title based on the configured key, falling back to the superclass.
     * when none is supplied.
     *
     * @param open the open context containing player details and initial state
     */
    @Override
    public void onOpen(@NotNull OpenContext open) {
        String key = this.customKey.get(open);
        if (
                key == null
        ) {
            key = super.getTitleKey();
        }

        Component titleComponent = new I18n.Builder(key + ".title", open.getPlayer())
                .withPlaceholders(this.getTitlePlaceholders(open)).build().component();
        
        String titleString = LegacyComponentSerializer.legacySection().serialize(titleComponent);
        
        open.modifyConfig().title(titleString);
    }

    /**
     * Returns {@code false} confirmation to the caller and triggers the optional callback when.
     * the navigation head is selected.
     *
     * @param clickContext the click context provided by Inventory Framework
     */
    @Override
    protected void handleBackButtonClick(@NotNull SlotClickContext clickContext) {
        clickContext.back(
                this.mergeWithInitialData(
                        Map.of("confirmed", false),
                        clickContext
                )
        );

        final Consumer<Boolean> callback = this.callback.get(clickContext);
        if (
                callback != null
        ) {
            callback.accept(false);
        }
    }

    /**
     * Lays out the confirm (green) and cancel (red) panes with localized lore prior to user.
     * interaction.
     *
     * @param render the render context used to register slot handlers
     * @param player the player viewing the dialog
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {

        render.layoutSlot(
                'c',
                UnifiedBuilderFactory
                        .item(Material.LIME_STAINED_GLASS_PANE)
                        .setName(
                                new I18n.Builder(this.customKey.get(render) + ".confirm.name", player).build().component()
                        )
                        .setLore(
                                new I18n.Builder(this.customKey.get(render) + ".confirm.lore", player)
                                        .withPlaceholders(this.initialData.get(render)).build().children()
                        )
                        .build()
        ).onClick(this::handleConfirm);

        render.layoutSlot(
                'x',
                UnifiedBuilderFactory
                        .item(Material.RED_STAINED_GLASS_PANE)
                        .setName(
                                new I18n.Builder(this.customKey.get(render) + ".cancel.name", player).build().component()
                        )
                        .setLore(
                                new I18n.Builder(this.customKey.get(render) + ".cancel.lore", player)
                                        .withPlaceholders(this.initialData.get(render)).build().children()
                        )
                        .build()
        ).onClick(this::handleCancel);
    }

    /**
     * Handles the confirm button click.
     *
     * @param clickContext the context for the confirm click event
     */
    private void handleConfirm(@NotNull Context clickContext) {
        clickContext.back(
                this.mergeWithInitialData(
                        Map.of("confirmed", true),
                        clickContext
                )
        );

        final Consumer<Boolean> callback = this.callback.get(clickContext);
        if (callback != null) {
            callback.accept(true);
        }
    }

    /**
     * Handles the cancel button click.
     *
     * @param clickContext the context for the cancel click event
     */
    private void handleCancel(@NotNull Context clickContext) {
        clickContext.back(
                this.mergeWithInitialData(
                        Map.of("confirmed", false),
                        clickContext
                )
        );

        final Consumer<Boolean> callback = this.callback.get(clickContext);
        if (callback != null) {
            callback.accept(false);
        }
    }

    /**
     * Utility to merge initial data with confirmation result.
     *
     * @param result  the result map describing confirmation outcome
     * @param context the Inventory Framework context carrying the initial data
     * @return a merged data map suitable for {@link SlotClickContext#back(Object)}
     */
    private Map<String, Object> mergeWithInitialData(
            final @NotNull Map<String, Object> result,
            final @NotNull Context context
    ) {

        final Map<String, Object> initial = this.initialData.get(context);

        if (
                initial == null
        ) {
            return result;
        }

        final Map<String, Object> merged = new HashMap<>(initial);
        merged.putAll(result);
        return merged;
    }

    /**
     * Builder class for creating ConfirmationView instances with custom configuration.
     */
    public static class Builder {

        private String key;
        private String messageKey;
        private Map<String, Object> initialData;
        private Consumer<Boolean> callback;
        private Class<? extends View> parentViewClass;

        /**
         * Sets a custom title key for the confirmation dialog.
         *
         * @param key the translation key namespace to use for dialog strings
         * @return this builder for chaining
         */
        public Builder withKey(@NotNull String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets a custom message key for the confirm button lore.
         *
         * @param messageKey the translation key providing confirm/cancel lore entries
         * @return this builder for chaining
         */
        public Builder withMessageKey(@NotNull String messageKey) {
            this.messageKey = messageKey;
            return this;
        }

        /**
         * Sets initial data to be passed back to the parent view.
         *
         * @param initialData the data payload merged into the confirmation result
         * @return this builder for chaining
         */
        public Builder withInitialData(@Nullable Map<String, Object> initialData) {
            this.initialData = initialData;
            return this;
        }

        /**
         * Sets the callback to be executed when the user confirms or cancels.
         *
         * @param callback the consumer invoked with the confirmation result
         * @return this builder for chaining
         */
        public Builder withCallback(@Nullable Consumer<Boolean> callback) {
            this.callback = callback;
            return this;
        }

        /**
         * Sets the parent view class for proper navigation.
         *
         * @param parentViewClass the parent view to reopen after the decision is made
         * @return this builder for chaining
         */
        public Builder withParentView(@Nullable Class<? extends View> parentViewClass) {
            this.parentViewClass = parentViewClass;
            return this;
        }

        /**
         * Opens the confirmation view for the given player.
         *
         * @param context the Inventory Framework context used to open the view
         * @param player  the player who should see the confirmation dialog
         */
        public void openFor(
                final @NotNull Context context,
                final @NotNull Player player
        ) {
            Map<String, Object> data = new HashMap<>();

            if (
                    this.key != null
            ) {
                data.put("key", this.key);
            }
            if (
                    this.messageKey != null
            ) {
                data.put("messageKey", this.messageKey);
            }
            if (
                    this.initialData != null
            ) {
                data.put("initialData", this.initialData);
            }
            if (
                    this.callback != null
            ) {
                data.put("callback", this.callback);
            }
            if (
                    this.parentViewClass != null
            ) {
                data.put("parentViewClass", this.parentViewClass);
            }
            try {
                context.openForPlayer(
                        ConfirmationView.class,
                        data
                );
            } catch (
                    final Exception exception
            ) {
                Logger.getLogger(ConfirmationView.class.getName()).log(
                        Level.WARNING,
                        "Failed to open confirmation dialog",
                        exception
                );
                context.back(data);
            }
        }
    }

}
