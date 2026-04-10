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

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.service.ContributionResult;
import com.raindropcentral.rdt.service.ContributionStatus;
import com.raindropcentral.rdt.service.NationCreationProgressSnapshot;
import com.raindropcentral.rdt.service.RequirementKind;
import com.raindropcentral.rdt.service.TownLevelRequirementSnapshot;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Requirement view for the dedicated nation-creation flow.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownNationRequirementsView extends BaseView {

    private static final List<Integer> REQUIREMENT_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    );

    /**
     * Creates the nation requirement view.
     */
    public TownNationRequirementsView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "town_nation_requirements_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "         ",
            "         ",
            "         ",
            "   w h   ",
            "r        "
        };
    }

    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        final Map<String, Object> targetData = TownNationViewSupport.copyInitialData(target);
        final Map<String, Object> data = targetData != null
            ? targetData
            : TownNationViewSupport.copyInitialData(origin);
        if (data != null && data.get(TownNationViewSupport.CONTRIBUTION_STATUS_KEY) instanceof String statusKey) {
            this.sendContributionMessage(target.getPlayer(), statusKey, data);
        }
        target.update();
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final NationCreationProgressSnapshot snapshot = TownNationViewSupport.resolveCreationSnapshot(render);
        if (snapshot == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player, snapshot));
        render.layoutSlot('w', this.createRewardsShortcut(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationRewardsView.class,
                this.createNavigationData(clickContext)
            ));
        render.layoutSlot('h', this.createHubShortcut(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationView.class,
                this.createNavigationData(clickContext)
            ));
        render.layoutSlot('r', this.createReturnItem(player))
            .onClick(SlotClickContext::back);

        for (int index = 0; index < REQUIREMENT_SLOTS.size(); index++) {
            final int slot = REQUIREMENT_SLOTS.get(index);
            if (index >= snapshot.requirements().size()) {
                render.slot(slot).renderWith(() -> this.createEmptyItem(player));
                continue;
            }

            final TownLevelRequirementSnapshot requirement = snapshot.requirements().get(index);
            render.slot(slot).renderWith(() -> this.createRequirementItem(player, requirement))
                .onClick(clickContext -> this.handleRequirementClick(clickContext, snapshot, requirement));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleRequirementClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull NationCreationProgressSnapshot snapshot,
        final @NotNull TownLevelRequirementSnapshot requirement
    ) {
        if (!requirement.contributable()) {
            return;
        }
        if (requirement.kind() == RequirementKind.CURRENCY) {
            clickContext.openForPlayer(
                TownNationCurrencyContributionAnvilView.class,
                TownNationViewSupport.mergeInitialData(
                    clickContext,
                    Map.of(TownNationViewSupport.ENTRY_KEY, requirement.entryKey())
                )
            );
            return;
        }
        if (requirement.kind() != RequirementKind.ITEM) {
            return;
        }

        final RDT plugin = TownNationViewSupport.plugin(clickContext);
        if (plugin == null || plugin.getTownRuntimeService() == null) {
            this.sendSharedMessage(clickContext.getPlayer(), "invalid_target", Map.of("contributed_amount", 0));
            return;
        }

        final ContributionResult result = plugin.getTownRuntimeService().contributeNationCreationItem(
            clickContext.getPlayer(),
            requirement.entryKey()
        );
        this.sendContributionMessage(clickContext.getPlayer(), result);
        clickContext.openForPlayer(TownNationRequirementsView.class, this.createNavigationData(clickContext));
    }

    private void sendContributionMessage(
        final @NotNull Player player,
        final @NotNull String statusKey,
        final @NotNull Map<String, Object> data
    ) {
        final Map<String, Object> placeholders = new LinkedHashMap<>();
        placeholders.put("contributed_amount", data.getOrDefault(TownNationViewSupport.CONTRIBUTION_AMOUNT_KEY, 0));
        this.sendSharedMessage(player, statusKey, placeholders);
        if (Boolean.TRUE.equals(data.get(TownNationViewSupport.CONTRIBUTION_COMPLETED_KEY))) {
            this.sendSharedMessage(player, "requirement_complete", placeholders);
        }
        if (Boolean.TRUE.equals(data.get(TownNationViewSupport.READY_TO_CREATE_KEY))) {
            this.sendSharedMessage(player, "ready_to_create", placeholders);
        }
    }

    private void sendContributionMessage(
        final @NotNull Player player,
        final @NotNull ContributionResult result
    ) {
        final Map<String, Object> placeholders = Map.of("contributed_amount", result.contributedAmount());
        switch (result.status()) {
            case SUCCESS -> this.sendSharedMessage(player, "contribution_saved", placeholders);
            case NOT_ENOUGH_RESOURCES -> this.sendSharedMessage(player, "not_enough_resources", placeholders);
            case ALREADY_COMPLETE -> this.sendSharedMessage(player, "requirement_complete", placeholders);
            case NO_PERMISSION, INVALID_TARGET, INVALID_ENTRY, FAILED, MAX_LEVEL -> {
                this.sendSharedMessage(player, "invalid_target", placeholders);
            }
        }
        if (result.requirementCompleted() && result.status() == ContributionStatus.SUCCESS) {
            this.sendSharedMessage(player, "requirement_complete", placeholders);
        }
        if (result.levelReady() && result.status() == ContributionStatus.SUCCESS) {
            this.sendSharedMessage(player, "ready_to_create", placeholders);
        }
    }

    private @NotNull Map<String, Object> createNavigationData(final @NotNull Context context) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final Map<String, Object> copiedData = TownNationViewSupport.copyInitialData(context);
        if (copiedData != null) {
            data.putAll(TownNationViewSupport.stripTransientData(copiedData));
        }
        return data;
    }

    private void sendSharedMessage(
        final @NotNull Player player,
        final @NotNull String key,
        final @NotNull Map<String, Object> placeholders
    ) {
        new I18n.Builder("town_nation_shared.messages." + key, player)
            .includePrefix()
            .withPlaceholders(placeholders)
            .build()
            .sendMessage();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull NationCreationProgressSnapshot snapshot
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "requirement_count", snapshot.requirements().size(),
                    "progress_percent", Math.round(snapshot.progress() * 100.0D)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRequirementItem(
        final @NotNull Player player,
        final @NotNull TownLevelRequirementSnapshot requirement
    ) {
        final ItemStack displayItem = requirement.displayItem() != null
            ? requirement.displayItem()
            : new ItemStack(requirement.kind() == RequirementKind.CURRENCY ? Material.GOLD_INGOT : Material.BOOK);
        final Material material = requirement.completed()
            ? Material.LIME_STAINED_GLASS_PANE
            : requirement.contributable()
                ? displayItem.getType()
                : Material.PAPER;
        final ItemStack baseItem = requirement.contributable()
            ? displayItem
            : new ItemStack(material);
        return UnifiedBuilderFactory.item(baseItem)
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("requirement_name", requirement.title())
                .build()
                .component())
            .setLore(this.i18n(
                    requirement.completed()
                        ? "entry.completed.lore"
                        : requirement.contributable()
                            ? "entry.contributable.lore"
                            : "entry.view_only.lore",
                    player
                )
                .withPlaceholders(Map.of(
                    "requirement_name", requirement.title(),
                    "description", requirement.description(),
                    "current_amount", Math.round(requirement.currentAmount() * 100.0D) / 100.0D,
                    "required_amount", Math.round(requirement.requiredAmount() * 100.0D) / 100.0D,
                    "available_amount", Math.round(requirement.availableAmount() * 100.0D) / 100.0D,
                    "progress_percent", Math.round(requirement.progress() * 100.0D)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRewardsShortcut(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("rewards.name", player).build().component())
            .setLore(this.i18n("rewards.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createHubShortcut(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BEACON)
            .setName(this.i18n("hub.name", player).build().component())
            .setLore(this.i18n("hub.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
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
}
