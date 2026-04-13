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
import com.raindropcentral.core.config.RCoreMainMenuConfig;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Root module hub for the {@code /rc main} command.
 */
public final class RCoreMainOverviewView extends BaseView {

    private static final int SUMMARY_SLOT = 4;
    private static final List<Integer> MODULE_SLOTS = List.of(11, 12, 13, 14, 15);

    private final State<RCoreImpl> plugin = initialState("plugin");

    /**
     * Creates the root module hub view.
     */
    public RCoreMainOverviewView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "rc_main_ui";
    }

    @Override
    protected int getSize() {
        return 3;
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    public void renderNavigationButtons(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        // Root hub; no back button.
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RCoreImpl pluginRuntime = this.plugin.get(render);
        if (pluginRuntime == null) {
            render.slot(SUMMARY_SLOT).renderWith(() -> this.createSummaryItem(player, 0, 0));
            return;
        }

        final Server server = pluginRuntime.getPlugin().getServer();
        final RCoreMainMenuConfig config = pluginRuntime.getMainMenuConfig();
        final List<RCoreMainModule> modules = RCoreMainModule.orderedModules(config);
        final long installedCount = modules.stream().filter(module -> module.isAvailable(server)).count();

        render.slot(SUMMARY_SLOT).renderWith(() -> this.createSummaryItem(player, installedCount, modules.size()));

        for (int index = 0; index < MODULE_SLOTS.size() && index < modules.size(); index++) {
            final RCoreMainModule module = modules.get(index);
            final int slot = MODULE_SLOTS.get(index);
            final boolean available = module.isAvailable(server);
            render.slot(slot)
                .renderWith(() -> this.createModuleItem(player, config, module, available))
                .onClick(clickContext -> this.handleModuleClick(clickContext, module));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleModuleClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RCoreMainModule module
    ) {
        clickContext.setCancelled(true);

        final RCoreImpl pluginRuntime = this.plugin.get(clickContext);
        if (pluginRuntime == null) {
            return;
        }

        if (!module.isAvailable(pluginRuntime.getPlugin().getServer())) {
            return;
        }

        try {
            if (module.openForPlayer(clickContext.getPlayer())) {
                return;
            }
        } catch (final RuntimeException exception) {
            pluginRuntime.getPlugin().getLogger().log(
                Level.WARNING,
                "Failed to open module hub target for " + module.moduleId(),
                exception
            );
        }

        new I18n.Builder("rcmain.error.open_failed", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholder("module_name", this.translatePlaceholder(module.nameTranslationKey(), clickContext.getPlayer()))
            .build()
            .sendMessage();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final long installedCount,
        final int moduleCount
    ) {
        return UnifiedBuilderFactory.item(Material.COMPASS)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "installed_count", installedCount,
                    "module_count", moduleCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createModuleItem(
        final @NotNull Player player,
        final @NotNull RCoreMainMenuConfig config,
        final @NotNull RCoreMainModule module,
        final boolean available
    ) {
        final Material material = available ? config.getButtonMaterial(module.moduleId()) : Material.BARRIER;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n(module.nameTranslationKey(), player).build().component())
            .setLore(List.of(
                this.i18n(module.descriptionTranslationKey(), player).build().component(),
                this.i18n(available ? "status.installed" : "status.missing", player).build().component(),
                this.i18n(available ? "action.open" : "action.unavailable", player).build().component()
            ))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull String translatePlaceholder(
        final @NotNull String translationKey,
        final @NotNull Player player
    ) {
        return this.i18n(translationKey, player)
            .build()
            .getI18nVersionWrapper()
            .asPlaceholder();
    }
}
