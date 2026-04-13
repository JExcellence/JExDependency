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

package com.raindropcentral.core.view;

import com.raindropcentral.core.RCoreImpl;
import com.raindropcentral.core.service.RCoreBossBarService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic provider-detail settings view driven entirely by the registered provider descriptor.
 *
 * @author Codex
 * @since 2.1.0
 * @version 2.1.0
 */
public final class RCoreBossBarProviderView extends BaseView {

    private static final List<Integer> OPTION_SLOTS = List.of(20, 21, 22, 23, 24, 31);

    private final State<RCoreImpl> plugin = initialState("plugin");
    private final State<String> providerKey = initialState("providerKey");

    /**
     * Creates the provider-detail view.
     */
    public RCoreBossBarProviderView() {
        super(RCoreBossBarOverviewView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "rc_boss_bar_detail_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "    S    ",
            "  OOOOO  ",
            "    T    ",
            "         ",
            "         "
        };
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        final ProviderContext providerContext = this.resolveProviderContext(
            open.getPlayer(),
            this.plugin.get(open),
            this.providerKey.get(open)
        );
        return Map.of(
            "provider_name",
            providerContext == null ? "Unknown" : providerContext.providerNamePlaceholder()
        );
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final ProviderContext providerContext = this.resolveProviderContext(
            player,
            this.plugin.get(render),
            this.providerKey.get(render)
        );
        if (providerContext == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        render.layoutSlot(
            'S',
            UnifiedBuilderFactory.item(providerContext.providerDefinition().iconMaterial())
                .setName(this.translate(providerContext.providerDefinition().nameTranslationKey(), player))
                .setLore(List.of(
                    this.translate(providerContext.providerDefinition().descriptionTranslationKey(), player),
                    this.i18n(
                        providerContext.preferenceSnapshot().enabled() ? "summary.enabled" : "summary.disabled",
                        player
                    ).build().component(),
                    this.i18n("summary.options", player)
                        .withPlaceholder("option_count", providerContext.providerDefinition().options().size())
                        .build()
                        .component()
                ))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build()
        );

        render.layoutSlot(
            'T',
            this.createToggleItem(player, providerContext)
        ).onClick(clickContext -> {
            providerContext.bossBarService().toggleEnabled(player.getUniqueId(), providerContext.providerDefinition().key());
            clickContext.update();
        });

        for (int index = 0; index < providerContext.providerDefinition().options().size() && index < OPTION_SLOTS.size(); index++) {
            final RCoreBossBarService.ProviderOption option = providerContext.providerDefinition().options().get(index);
            final int slot = OPTION_SLOTS.get(index);
            render.slot(slot).renderWith(() -> this.createOptionItem(player, providerContext, option)).onClick(clickContext -> {
                final String currentValue = providerContext.preferenceSnapshot().options().getOrDefault(option.key(), option.defaultValue());
                final String nextValue = this.resolveNextChoice(option, currentValue);
                providerContext.bossBarService().setOption(player.getUniqueId(), providerContext.providerDefinition().key(), option.key(), nextValue);
                clickContext.update();
            });
        }
    }

    private @NotNull ItemStack createToggleItem(
        final @NotNull Player player,
        final @NotNull ProviderContext providerContext
    ) {
        final boolean enabled = providerContext.preferenceSnapshot().enabled();
        final Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        final String nameKey = enabled ? "toggle.enabled_name" : "toggle.disabled_name";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n(nameKey, player).build().component())
            .setLore(this.i18n("toggle.lore", player)
                .withPlaceholder("provider_name", providerContext.providerNamePlaceholder())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createOptionItem(
        final @NotNull Player player,
        final @NotNull ProviderContext providerContext,
        final @NotNull RCoreBossBarService.ProviderOption option
    ) {
        final String currentValue = providerContext.preferenceSnapshot().options().getOrDefault(option.key(), option.defaultValue());
        final String currentLabelKey = option.choices().stream()
            .filter(choice -> choice.value().equals(currentValue))
            .findFirst()
            .map(RCoreBossBarService.ProviderOptionChoice::labelTranslationKey)
            .orElse(option.choices().getFirst().labelTranslationKey());

        final ArrayList<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(this.translate(option.descriptionTranslationKey(), player));
        lore.add(this.i18n("option.current", player)
            .withPlaceholder("current_value", this.translatePlaceholder(currentLabelKey, player))
            .build()
            .component());
        lore.add(this.i18n("option.available", player)
            .withPlaceholder("available_values", this.joinChoiceLabels(option, player))
            .build()
            .component());
        lore.add(this.i18n("option.click", player).build().component());

        return UnifiedBuilderFactory.item(Material.REPEATER)
            .setName(this.translate(option.nameTranslationKey(), player))
            .setLore(lore)
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

    private @NotNull String resolveNextChoice(
        final @NotNull RCoreBossBarService.ProviderOption option,
        final @NotNull String currentValue
    ) {
        final List<RCoreBossBarService.ProviderOptionChoice> choices = option.choices();
        int currentIndex = 0;
        for (int index = 0; index < choices.size(); index++) {
            if (choices.get(index).value().equalsIgnoreCase(currentValue)) {
                currentIndex = index;
                break;
            }
        }
        return choices.get((currentIndex + 1) % choices.size()).value();
    }

    private @NotNull String joinChoiceLabels(
        final @NotNull RCoreBossBarService.ProviderOption option,
        final @NotNull Player player
    ) {
        return option.choices().stream()
            .map(choice -> this.translatePlaceholder(choice.labelTranslationKey(), player))
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
    }

    private @Nullable ProviderContext resolveProviderContext(
        final @NotNull Player player,
        final @Nullable RCoreImpl pluginRuntime,
        final @Nullable String currentProviderKey
    ) {
        if (pluginRuntime == null || pluginRuntime.getBossBarService() == null || currentProviderKey == null) {
            return null;
        }

        final RCoreBossBarService bossBarService = pluginRuntime.getBossBarService();
        final RCoreBossBarService.ProviderDefinition providerDefinition = bossBarService.findProvider(currentProviderKey).orElse(null);
        if (providerDefinition == null) {
            return null;
        }

        final String providerNamePlaceholder = this.translatePlaceholder(providerDefinition.nameTranslationKey(), player);
        final RCoreBossBarService.PreferenceSnapshot preferenceSnapshot = bossBarService.resolvePreferences(
            player.getUniqueId(),
            providerDefinition.key()
        );
        return new ProviderContext(bossBarService, providerDefinition, preferenceSnapshot, providerNamePlaceholder);
    }

    private @NotNull net.kyori.adventure.text.Component translate(
        final @NotNull String translationKey,
        final @NotNull Player player
    ) {
        return new I18n.Builder(translationKey, player).build().component();
    }

    private @NotNull String translatePlaceholder(
        final @NotNull String translationKey,
        final @NotNull Player player
    ) {
        return new I18n.Builder(translationKey, player)
            .build()
            .getI18nVersionWrapper()
            .asPlaceholder();
    }

    private record ProviderContext(
        @NotNull RCoreBossBarService bossBarService,
        @NotNull RCoreBossBarService.ProviderDefinition providerDefinition,
        @NotNull RCoreBossBarService.PreferenceSnapshot preferenceSnapshot,
        @NotNull String providerNamePlaceholder
    ) {}
}
