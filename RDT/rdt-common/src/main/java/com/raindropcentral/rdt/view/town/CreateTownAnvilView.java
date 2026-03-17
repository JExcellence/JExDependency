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

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.items.Nexus;
import com.raindropcentral.rplatform.view.AbstractAnvilView;

/**
 * Anvil input view used to collect and validate a new town name before issuing a Nexus block.
 *
 * <p>When a valid name is submitted, the player receives a {@link Nexus} item and the anvil closes.
 * Invalid submissions are rejected, the click is cancelled, and a warning message is sent.</p>
 *
 * @author RaindropCentral
 * @since 1.0.0
 * @version 1.0.0
 */
public class CreateTownAnvilView extends AbstractAnvilView {

    private static final Pattern TOWN_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 _-]+$");
    private static final int MINIMUM_NAME_LENGTH = 3;
    private static final int MAXIMUM_NAME_LENGTH = 24;

    private final State<RDT> rdt = initialState("plugin");

    /**
     * Creates the town creation anvil view.
     */
    public CreateTownAnvilView() {
        super();
    }

    /**
     * Returns the translation key namespace for this view.
     *
     * @return create-town anvil translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "create_town_anvil_ui";
    }

    /**
     * Uses the root title key under this view namespace.
     *
     * @return title suffix key
     */
    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    /**
     * Prevents players from moving anvil display items while interacting with the view.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Provides a starter text for the town name input.
     *
     * @param context open context
     * @return initial anvil input
     */
    @Override
    protected @NotNull String getInitialInputText(final @NotNull OpenContext context) {
        return "TownName";
    }

    /**
     * Validates the proposed town name and player eligibility.
     *
     * @param input user-entered town name
     * @param context interaction context
     * @return {@code true} when the input can be accepted
     */
    @Override
    protected boolean isValidInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        return this.validateTownName(input, context) == ValidationResult.VALID;
    }

    /**
     * Creates and gives the Nexus block for a valid town name, then closes the inventory.
     *
     * @param input validated town name
     * @param context interaction context
     * @return created nexus item
     */
    @Override
    protected @Nullable Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final RDT plugin = this.resolvePlugin(context);
        final Player player = context.getPlayer();

        if (plugin == null) {
            this.i18n("error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return null;
        }

        final String townName = input.trim();
        final ItemStack nexusBlock = Nexus.getNexusItem(
                plugin,
                UUID.randomUUID(),
                townName,
                player.getUniqueId()
        );
        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(nexusBlock);

        if (leftovers.isEmpty()) {
            this.i18n("message.nexus_given", player)
                    .includePrefix()
                    .withPlaceholder("town_name", townName)
                    .build()
                    .sendMessage();
        } else {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            this.i18n("message.nexus_dropped", player)
                    .includePrefix()
                    .withPlaceholder("town_name", townName)
                    .build()
                    .sendMessage();
        }

        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> player.closeInventory());
        return nexusBlock;
    }

    /**
     * Sends a specific validation warning and ensures the invalid submit click is cancelled.
     *
     * @param input invalid user input
     * @param context interaction context
     */
    @Override
    protected void onValidationFailed(
            final @Nullable String input,
            final @NotNull Context context
    ) {
        if (context instanceof final SlotClickContext clickContext) {
            clickContext.setCancelled(true);
        }

        final String normalizedInput = input == null ? "" : input;
        final ValidationResult validationResult = this.validateTownName(normalizedInput, context);

        this.i18n(this.toValidationMessageKey(validationResult), context.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "input", normalizedInput,
                        "min_length", MINIMUM_NAME_LENGTH,
                        "max_length", MAXIMUM_NAME_LENGTH
                ))
                .build()
                .sendMessage();
    }

    /**
     * Preserves plugin state in case the framework navigates back to the previous view.
     *
     * @param processingResult processing result
     * @param input submitted input
     * @param context interaction context
     * @return result payload
     */
    @Override
    protected @NotNull Map<String, Object> prepareResultData(
            final @Nullable Object processingResult,
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final Map<String, Object> resultData = super.prepareResultData(processingResult, input, context);
        final RDT plugin = this.resolvePlugin(context);
        if (plugin != null) {
            resultData.put("plugin", plugin);
        }
        return resultData;
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @NotNull ValidationResult validateTownName(
            final @NotNull String rawInput,
            final @NotNull Context context
    ) {
        final String townName = rawInput.trim();

        if (townName.isEmpty()) {
            return ValidationResult.EMPTY;
        }

        if (townName.length() < MINIMUM_NAME_LENGTH || townName.length() > MAXIMUM_NAME_LENGTH) {
            return ValidationResult.INVALID_LENGTH;
        }

        if (!TOWN_NAME_PATTERN.matcher(townName).matches()) {
            return ValidationResult.INVALID_CHARACTERS;
        }

        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return ValidationResult.SYSTEM_UNAVAILABLE;
        }

        final RRTown townRepository = plugin.getTownRepository();
        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (townRepository == null || playerRepository == null) {
            return ValidationResult.SYSTEM_UNAVAILABLE;
        }

        final RDTPlayer existingPlayer = playerRepository.findByPlayer(context.getPlayer().getUniqueId());
        if (existingPlayer != null && existingPlayer.getTownUUID() != null) {
            return ValidationResult.ALREADY_IN_TOWN;
        }

        final RTown existingTown = townRepository.findByTName(townName);
        if (existingTown != null) {
            return ValidationResult.NAME_TAKEN;
        }

        return ValidationResult.VALID;
    }

    private @NotNull String toValidationMessageKey(final @NotNull ValidationResult result) {
        return switch (result) {
            case EMPTY -> "error.invalid_name.empty";
            case INVALID_LENGTH -> "error.invalid_name.length";
            case INVALID_CHARACTERS -> "error.invalid_name.pattern";
            case NAME_TAKEN -> "error.invalid_name.exists";
            case ALREADY_IN_TOWN -> "error.invalid_name.already_in_town";
            case SYSTEM_UNAVAILABLE -> "error.system_unavailable";
            case VALID -> "error.invalid_name.generic";
        };
    }

    private enum ValidationResult {
        VALID,
        EMPTY,
        INVALID_LENGTH,
        INVALID_CHARACTERS,
        NAME_TAKEN,
        ALREADY_IN_TOWN,
        SYSTEM_UNAVAILABLE
    }
}
