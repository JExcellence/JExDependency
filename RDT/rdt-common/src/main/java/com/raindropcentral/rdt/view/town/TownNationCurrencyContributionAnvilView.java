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
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Anvil input view for partial currency turn-ins during nation creation.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownNationCurrencyContributionAnvilView extends AbstractAnvilView {

    /**
     * Creates the nation currency contribution input view.
     */
    public TownNationCurrencyContributionAnvilView() {
        super(TownNationRequirementsView.class);
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

        final RDT plugin = TownNationViewSupport.plugin(context);
        if (plugin == null || plugin.getTownRuntimeService() == null) {
            return new ContributionResult(ContributionStatus.INVALID_TARGET, 0.0D, false, false);
        }

        final String entryKey = this.resolveEntryKey(context);
        if (entryKey == null) {
            return new ContributionResult(ContributionStatus.INVALID_ENTRY, 0.0D, false, false);
        }

        return plugin.getTownRuntimeService().contributeNationCreationCurrency(
            context.getPlayer(),
            entryKey,
            requestedAmount
        );
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
        final Map<String, Object> copiedData = TownNationViewSupport.copyInitialData(context);
        if (copiedData != null) {
            resultData.putAll(TownNationViewSupport.stripTransientData(copiedData));
        }
        resultData.put(TownNationViewSupport.CONTRIBUTION_STATUS_KEY, this.resolveStatusKey(contributionResult.status()));
        resultData.put(TownNationViewSupport.CONTRIBUTION_AMOUNT_KEY, contributionResult.contributedAmount());
        resultData.put(TownNationViewSupport.CONTRIBUTION_COMPLETED_KEY, contributionResult.requirementCompleted());
        resultData.put(TownNationViewSupport.READY_TO_CREATE_KEY, contributionResult.levelReady());
        return resultData;
    }

    private @Nullable String resolveEntryKey(final @NotNull Context context) {
        final Map<String, Object> data = TownNationViewSupport.copyInitialData(context);
        return data != null && data.get(TownNationViewSupport.ENTRY_KEY) instanceof String entryKey
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
            case NOT_ENOUGH_RESOURCES -> "not_enough_resources";
            case ALREADY_COMPLETE -> "requirement_complete";
            case NO_PERMISSION, MAX_LEVEL, INVALID_TARGET, INVALID_ENTRY, FAILED -> "invalid_target";
        };
    }
}
