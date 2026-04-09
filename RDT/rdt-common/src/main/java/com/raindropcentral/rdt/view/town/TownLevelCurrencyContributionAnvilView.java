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
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.service.ContributionResult;
import com.raindropcentral.rdt.service.ContributionStatus;
import com.raindropcentral.rdt.service.LevelProgressSnapshot;
import com.raindropcentral.rdt.service.TownLevelRequirementSnapshot;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Anvil input view for partial currency turn-ins on level requirements.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownLevelCurrencyContributionAnvilView extends AbstractAnvilView {

    /**
     * Creates the currency contribution input view.
     */
    public TownLevelCurrencyContributionAnvilView() {
        super(TownLevelRequirementsView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "town_level_currency_input_ui";
    }

    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    @Override
    protected String getInitialInputText(final @NotNull OpenContext context) {
        return "";
    }

    @Override
    protected @Nullable Object processInput(final @NotNull String input, final @NotNull Context context) {
        final Double requestedAmount = this.parseAmount(input);
        if (requestedAmount == null) {
            return new ContributionResult(ContributionStatus.INVALID_ENTRY, 0.0D, false, false);
        }

        final RDT plugin = TownLevelViewSupport.plugin(context);
        final RTown town = TownLevelViewSupport.resolveTown(context);
        if (plugin == null || plugin.getTownRuntimeService() == null || town == null) {
            return new ContributionResult(ContributionStatus.INVALID_TARGET, 0.0D, false, false);
        }

        final String entryKey = this.resolveEntryKey(context);
        if (entryKey == null) {
            return new ContributionResult(ContributionStatus.INVALID_ENTRY, 0.0D, false, false);
        }

        return switch (TownLevelViewSupport.scope(context)) {
            case NEXUS -> plugin.getTownRuntimeService().contributeNexusCurrency(
                context.getPlayer(),
                town,
                entryKey,
                requestedAmount
            );
            case SECURITY, BANK, FARM, OUTPOST, MEDIC, ARMORY -> {
                final RTownChunk townChunk = TownLevelViewSupport.resolveChunk(context);
                yield townChunk == null
                    ? new ContributionResult(ContributionStatus.INVALID_TARGET, 0.0D, false, false)
                    : plugin.getTownRuntimeService().contributeChunkCurrency(
                        context.getPlayer(),
                        townChunk,
                        entryKey,
                        requestedAmount
                    );
            }
        };
    }

    @Override
    protected boolean isValidInput(final @NotNull String input, final @NotNull Context context) {
        final Double requestedAmount = this.parseAmount(input);
        return requestedAmount != null && requestedAmount > 0.0D;
    }

    @Override
    protected @NotNull Map<String, Object> prepareResultData(
        final @Nullable Object result,
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final ContributionResult contributionResult = result instanceof ContributionResult resolvedResult
            ? resolvedResult
            : new ContributionResult(ContributionStatus.INVALID_TARGET, 0.0D, false, false);
        final Map<String, Object> resultData = new LinkedHashMap<>();
        final Map<String, Object> copiedData = TownLevelViewSupport.copyInitialData(context);
        if (copiedData != null) {
            resultData.putAll(TownLevelViewSupport.stripTransientData(copiedData));
        }
        resultData.put(TownLevelViewSupport.CONTRIBUTION_STATUS_KEY, this.resolveStatusKey(contributionResult.status()));
        resultData.put(TownLevelViewSupport.CONTRIBUTION_AMOUNT_KEY, contributionResult.contributedAmount());
        resultData.put(TownLevelViewSupport.CONTRIBUTION_COMPLETED_KEY, contributionResult.requirementCompleted());
        resultData.put(TownLevelViewSupport.LEVEL_READY_KEY, contributionResult.levelReady());
        return resultData;
    }

    private @Nullable TownLevelRequirementSnapshot resolveRequirement(final @NotNull Context context) {
        final String entryKey = this.resolveEntryKey(context);
        final LevelProgressSnapshot snapshot = TownLevelViewSupport.resolveSnapshot(context);
        return snapshot == null || entryKey == null ? null : snapshot.findRequirement(entryKey);
    }

    private @Nullable String resolveEntryKey(final @NotNull Context context) {
        final Map<String, Object> data = TownLevelViewSupport.copyInitialData(context);
        return data != null && data.get(TownLevelViewSupport.ENTRY_KEY) instanceof String entryKey
            ? entryKey
            : null;
    }

    private @Nullable Double parseAmount(final @NotNull String input) {
        try {
            return Double.parseDouble(input.trim());
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private @NotNull String resolveStatusKey(final @NotNull ContributionStatus status) {
        return switch (status) {
            case SUCCESS -> "contribution_saved";
            case NO_PERMISSION -> "no_permission";
            case NOT_ENOUGH_RESOURCES -> "not_enough_resources";
            case ALREADY_COMPLETE -> "requirement_complete";
            case MAX_LEVEL -> "max_level";
            case INVALID_TARGET, INVALID_ENTRY, FAILED -> "invalid_target";
        };
    }
}
