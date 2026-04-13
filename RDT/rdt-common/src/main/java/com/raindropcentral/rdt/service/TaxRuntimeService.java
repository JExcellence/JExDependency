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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.configs.TaxConfigSection;
import com.raindropcentral.rdt.database.entity.RNation;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
import de.jexcellence.gpeee.GPEEE;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.gpeee.parser.expression.AExpression;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.raindropcentral.rplatform.scheduler.CancellableTaskHandle.noop;

/**
 * Schedules and processes recurring town and nation taxes.
 *
 * <p>The tax runtime owns scheduled collection, debt reminders, grace-period enforcement, and join
 * reminders for players whose town or nation currently owes taxes.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TaxRuntimeService {

    private static final Logger LOGGER = Logger.getLogger(TaxRuntimeService.class.getName());
    private static final double EPSILON = 1.0E-6D;

    private final RDT plugin;
    private final GPEEE evaluator;
    private final AtomicBoolean runInProgress;

    private volatile com.raindropcentral.rplatform.scheduler.CancellableTaskHandle collectionTask;
    private volatile com.raindropcentral.rplatform.scheduler.CancellableTaskHandle debtTask;
    private volatile boolean running;
    private volatile @Nullable Instant nextScheduledRunAt;
    private volatile @Nullable Instant lastCollectionAt;

    /**
     * Creates the tax runtime service.
     *
     * @param plugin active RDT runtime
     */
    public TaxRuntimeService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.evaluator = new GPEEE(Logger.getLogger(TaxRuntimeService.class.getName() + ".GPEEE"));
        this.runInProgress = new AtomicBoolean(false);
        this.collectionTask = noop();
        this.debtTask = noop();
    }

    /**
     * Starts recurring collection and debt-monitor tasks.
     */
    public void start() {
        if (this.running || this.plugin.getScheduler() == null) {
            return;
        }

        final TaxConfigSection config = this.plugin.getTaxConfig();
        final long initialDelayTicks = calculateInitialDelayTicks(config.getSchedule(), Instant.now());
        final long collectionPeriodTicks = Math.max(1L, config.getSchedule().durationTicks());
        final long debtPeriodTicks = Math.max(20L, config.getDebt().warningIntervalTicks());
        this.nextScheduledRunAt = calculateNextRun(config.getSchedule(), Instant.now());
        this.collectionTask = this.plugin.getScheduler().runRepeating(this::runScheduledCollection, initialDelayTicks, collectionPeriodTicks);
        this.debtTask = this.plugin.getScheduler().runRepeating(this::processDebtTick, debtPeriodTicks, debtPeriodTicks);
        this.running = true;
        LOGGER.info(
            "Started RDT tax runtime with collection period "
                + collectionPeriodTicks
                + " ticks and debt monitor period "
                + debtPeriodTicks
                + " ticks."
        );
    }

    /**
     * Stops recurring collection and debt-monitor tasks.
     */
    public void shutdown() {
        this.running = false;
        this.collectionTask.cancel();
        this.debtTask.cancel();
        this.collectionTask = noop();
        this.debtTask = noop();
    }

    /**
     * Returns whether the runtime scheduler is active.
     *
     * @return {@code true} when recurring tasks are active
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Returns the next scheduled automatic tax-run time.
     *
     * @return next scheduled automatic run, or {@code null} when unavailable
     */
    public @Nullable Instant getNextScheduledRunAt() {
        return this.nextScheduledRunAt;
    }

    /**
     * Returns the timestamp of the most recent completed tax run.
     *
     * @return last completed tax run, or {@code null} when none has completed
     */
    public @Nullable Instant getLastCollectionAt() {
        return this.lastCollectionAt;
    }

    /**
     * Returns one immutable status snapshot for admin commands.
     *
     * @return tax status snapshot
     */
    public @NotNull TaxStatusSnapshot getStatusSnapshot() {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return new TaxStatusSnapshot(this.running, this.lastCollectionAt, this.nextScheduledRunAt, 0, 0, 0, 0);
        }

        final List<RTown> towns = runtimeService.getTowns();
        final List<RNation> nations = runtimeService.getActiveNations();
        final int townsInDebt = (int) towns.stream().filter(RTown::hasTaxDebt).count();
        final int nationsInDebt = (int) nations.stream().filter(RNation::hasTaxDebt).count();
        return new TaxStatusSnapshot(
            this.running,
            this.lastCollectionAt,
            this.nextScheduledRunAt,
            towns.size(),
            nations.size(),
            townsInDebt,
            nationsInDebt
        );
    }

    /**
     * Runs tax collection immediately without changing the repeating schedule cadence.
     *
     * @return {@code true} when the manual run completed
     */
    public boolean collectTaxesNow() {
        return this.runCollection(this.plugin.getTaxConfig(), true);
    }

    /**
     * Sends join reminders when the player's town or nation currently owes tax debt.
     *
     * @param player joining player
     */
    public void handlePlayerJoin(final @NotNull Player player) {
        final TaxConfigSection config = this.plugin.getTaxConfig();
        if (!config.getDebt().joinReminderEnabled()) {
            return;
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return;
        }

        final RTown town = runtimeService.getTownFor(player.getUniqueId());
        if (town == null) {
            return;
        }
        if (town.hasTaxDebt()) {
            this.sendTownDebtMessage(player, town, "tax_runtime.join_reminder.town");
        }

        final RNation nation = runtimeService.getNationForTown(town);
        if (nation != null && nation.hasTaxDebt()) {
            this.sendNationDebtMessage(player, nation, "tax_runtime.join_reminder.nation");
        }
    }

    /**
     * Immutable admin-facing status snapshot for the tax runtime.
     *
     * @param running whether the recurring runtime is active
     * @param lastCollectionAt most recent completed tax run
     * @param nextScheduledRunAt next scheduled automatic run
     * @param activeTownCount active towns currently tracked
     * @param activeNationCount active nations currently tracked
     * @param townsInDebt towns that currently owe tax debt
     * @param nationsInDebt nations that currently owe tax debt
     */
    public record TaxStatusSnapshot(
        boolean running,
        @Nullable Instant lastCollectionAt,
        @Nullable Instant nextScheduledRunAt,
        int activeTownCount,
        int activeNationCount,
        int townsInDebt,
        int nationsInDebt
    ) {
    }

    private void runScheduledCollection() {
        final TaxConfigSection config = this.plugin.getTaxConfig();
        this.nextScheduledRunAt = calculateNextRun(config.getSchedule(), Instant.now());
        this.runCollection(config, true);
    }

    private void processDebtTick() {
        final TaxConfigSection config = this.plugin.getTaxConfig();
        if (!this.runInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
            final ServerBankService serverBankService = this.plugin.getServerBankService();
            if (runtimeService == null || serverBankService == null || this.plugin.getScheduler() == null) {
                return;
            }

            final AExpression expression = this.parseExpression(config);
            final ItemStack[] serverStorage = serverBankService.expandInventory(
                serverBankService.getSharedStorage(),
                serverBankService.getSharedStorageSize()
            );
            boolean serverStorageChanged = false;

            for (final RTown town : runtimeService.getTowns().stream().filter(RTown::hasTaxDebt).toList()) {
                serverStorageChanged |= this.collectTownTaxes(town, config, expression, false, serverStorage, false);
                final RTown liveTown = runtimeService.getTownIncludingInactive(town.getTownUUID());
                if (liveTown == null || !liveTown.getActive() || !liveTown.hasTaxDebt()) {
                    continue;
                }

                final long remainingGraceMillis = this.getRemainingGraceMillis(liveTown.getTaxDebtStartedAt(), config.getDebt().gracePeriodTicks());
                if (remainingGraceMillis <= 0L) {
                    serverStorageChanged |= this.collectTownTaxes(liveTown, config, expression, false, serverStorage, false);
                    final RTown finalTown = runtimeService.getTownIncludingInactive(liveTown.getTownUUID());
                    if (finalTown != null && finalTown.getActive() && finalTown.hasTaxDebt() && runtimeService.fallTownForTaxDebt(finalTown)) {
                        if (config.getDebt().broadcastTownFall()) {
                            this.broadcastTownFall(finalTown);
                        }
                    }
                    continue;
                }

                this.maybeWarnTownDebt(liveTown, config, false);
            }

            for (final RNation nation : runtimeService.getActiveNations().stream().filter(RNation::hasTaxDebt).toList()) {
                serverStorageChanged |= this.collectNationTaxes(nation, config, expression, false, serverStorage, false);
                final RNation liveNation = runtimeService.getNation(nation.getNationUuid());
                if (liveNation == null || !liveNation.isActive() || !liveNation.hasTaxDebt()) {
                    continue;
                }

                final long remainingGraceMillis = this.getRemainingGraceMillis(liveNation.getTaxDebtStartedAt(), config.getDebt().gracePeriodTicks());
                if (remainingGraceMillis <= 0L) {
                    serverStorageChanged |= this.collectNationTaxes(liveNation, config, expression, false, serverStorage, false);
                    final RNation finalNation = runtimeService.getNation(liveNation.getNationUuid());
                    if (finalNation != null && finalNation.isActive() && finalNation.hasTaxDebt() && runtimeService.disbandNationForTaxDebt(finalNation)) {
                        if (config.getDebt().broadcastNationDisband()) {
                            this.broadcastNationDisband(finalNation);
                        }
                    }
                    continue;
                }

                this.maybeWarnNationDebt(liveNation, config, false);
            }

            if (serverStorageChanged) {
                serverBankService.saveSharedStorage(serverStorage);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed while processing RDT tax debt checks.", exception);
        } finally {
            this.runInProgress.set(false);
        }
    }

    private boolean runCollection(final @NotNull TaxConfigSection config, final boolean includeCurrentTaxes) {
        if (!this.runInProgress.compareAndSet(false, true)) {
            return false;
        }

        try {
            final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
            final ServerBankService serverBankService = this.plugin.getServerBankService();
            if (runtimeService == null || serverBankService == null) {
                return false;
            }

            final AExpression expression = this.parseExpression(config);
            final ItemStack[] serverStorage = serverBankService.expandInventory(
                serverBankService.getSharedStorage(),
                serverBankService.getSharedStorageSize()
            );
            boolean serverStorageChanged = false;

            for (final RTown town : runtimeService.getTowns()) {
                serverStorageChanged |= this.collectTownTaxes(town, config, expression, includeCurrentTaxes, serverStorage, includeCurrentTaxes);
            }
            for (final RNation nation : runtimeService.getActiveNations()) {
                serverStorageChanged |= this.collectNationTaxes(nation, config, expression, includeCurrentTaxes, serverStorage, includeCurrentTaxes);
            }

            if (serverStorageChanged) {
                serverBankService.saveSharedStorage(serverStorage);
            }
            this.lastCollectionAt = Instant.now();
            return true;
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed while collecting scheduled RDT taxes.", exception);
            return false;
        } finally {
            this.runInProgress.set(false);
        }
    }

    private boolean collectTownTaxes(
        final @NotNull RTown town,
        final @NotNull TaxConfigSection config,
        final @NotNull AExpression expression,
        final boolean includeCurrentTaxes,
        final ItemStack @NotNull [] serverStorage,
        final boolean forceWarning
    ) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final ServerBankService serverBankService = this.plugin.getServerBankService();
        final TownBankService townBankService = this.plugin.getTownBankService();
        if (runtimeService == null || serverBankService == null || townBankService == null || this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTown liveTown = runtimeService.getTownIncludingInactive(town.getTownUUID());
        if (liveTown == null || !liveTown.getActive()) {
            return false;
        }

        final boolean hadDebtBefore = liveTown.hasTaxDebt();
        final Instant now = Instant.now();
        final RNation nation = runtimeService.getNationForTown(liveTown);
        final TownVariableContext variables = this.createTownVariableContext(liveTown, nation);
        final double taxRatePercent = this.evaluateTaxRate(config, expression, variables);

        boolean changed = false;
        for (final String currencyId : config.getCurrency().currencyIds()) {
            final double currentBalance = liveTown.getBankAmount(currencyId);
            final double newlyDue = includeCurrentTaxes ? Math.max(0.0D, currentBalance * (taxRatePercent / 100.0D)) : 0.0D;
            final double totalDue = liveTown.getCurrencyTaxDebt(currencyId) + newlyDue;
            if (totalDue <= EPSILON) {
                liveTown.setCurrencyTaxDebt(currencyId, 0.0D);
                continue;
            }

            final double collected = Math.min(currentBalance, totalDue);
            if (collected > EPSILON) {
                liveTown.withdrawBank(currencyId, collected);
                serverBankService.addCurrency(currencyId, collected);
                changed = true;
            }
            liveTown.setCurrencyTaxDebt(currencyId, totalDue - collected);
        }

        final Map<Material, Integer> itemDebt = this.toItemCountMap(liveTown.getItemTaxDebt());
        if (includeCurrentTaxes) {
            this.mergeCounts(itemDebt, this.calculateTownItemTaxes(liveTown, config));
        }
        final ItemStack[] townStorage = townBankService.expandInventory(liveTown.getSharedBankStorage(), townBankService.getSharedStorageSize());
        final int movedItems = this.collectItemDebt(itemDebt, townStorage, serverStorage);
        if (movedItems > 0) {
            changed = true;
        }
        liveTown.setSharedBankStorage(townBankService.snapshotInventory(townStorage, townBankService.getSharedStorageSize()));
        this.replaceItemDebt(liveTown, itemDebt);
        changed |= this.updateTownDebtState(liveTown, hadDebtBefore, config, forceWarning, now);
        if (changed) {
            this.plugin.getTownRepository().update(liveTown);
        }
        return movedItems > 0;
    }

    private boolean collectNationTaxes(
        final @NotNull RNation nation,
        final @NotNull TaxConfigSection config,
        final @NotNull AExpression expression,
        final boolean includeCurrentTaxes,
        final ItemStack @NotNull [] serverStorage,
        final boolean forceWarning
    ) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final ServerBankService serverBankService = this.plugin.getServerBankService();
        final NationBankService nationBankService = this.plugin.getNationBankService();
        if (runtimeService == null || serverBankService == null || nationBankService == null || this.plugin.getNationRepository() == null) {
            return false;
        }

        final RNation liveNation = runtimeService.getNation(nation.getNationUuid());
        if (liveNation == null || !liveNation.isActive()) {
            return false;
        }

        final boolean hadDebtBefore = liveNation.hasTaxDebt();
        final Instant now = Instant.now();
        final NationVariableContext variables = this.createNationVariableContext(liveNation);
        final double taxRatePercent = this.evaluateTaxRate(config, expression, variables);

        boolean changed = false;
        for (final String currencyId : config.getCurrency().currencyIds()) {
            final double currentBalance = liveNation.getBankAmount(currencyId);
            final double newlyDue = includeCurrentTaxes ? Math.max(0.0D, currentBalance * (taxRatePercent / 100.0D)) : 0.0D;
            final double totalDue = liveNation.getCurrencyTaxDebt(currencyId) + newlyDue;
            if (totalDue <= EPSILON) {
                liveNation.setCurrencyTaxDebt(currencyId, 0.0D);
                continue;
            }

            final double collected = Math.min(currentBalance, totalDue);
            if (collected > EPSILON) {
                liveNation.withdrawBank(currencyId, collected);
                serverBankService.addCurrency(currencyId, collected);
                changed = true;
            }
            liveNation.setCurrencyTaxDebt(currencyId, totalDue - collected);
        }

        final Map<Material, Integer> itemDebt = this.toItemCountMap(liveNation.getItemTaxDebt());
        if (includeCurrentTaxes) {
            this.mergeCounts(itemDebt, this.calculateNationItemTaxes(liveNation, config));
        }
        final ItemStack[] nationStorage = nationBankService.expandInventory(liveNation.getSharedBankStorage(), nationBankService.getSharedStorageSize());
        final int movedItems = this.collectItemDebt(itemDebt, nationStorage, serverStorage);
        if (movedItems > 0) {
            changed = true;
        }
        liveNation.setSharedBankStorage(nationBankService.snapshotInventory(nationStorage, nationBankService.getSharedStorageSize()));
        this.replaceItemDebt(liveNation, itemDebt);
        changed |= this.updateNationDebtState(liveNation, hadDebtBefore, config, forceWarning, now);
        if (changed) {
            this.plugin.getNationRepository().update(liveNation);
        }
        return movedItems > 0;
    }

    private boolean updateTownDebtState(
        final @NotNull RTown town,
        final boolean hadDebtBefore,
        final @NotNull TaxConfigSection config,
        final boolean forceWarning,
        final @NotNull Instant now
    ) {
        if (!town.hasTaxDebt()) {
            if (hadDebtBefore) {
                town.clearTaxDebt();
                this.notifyTownDebtCleared(town);
                return true;
            }
            town.clearTaxDebt();
            return false;
        }

        boolean changed = false;
        if (town.getTaxDebtStartedAt() <= 0L) {
            town.setTaxDebtStartedAt(now.toEpochMilli());
            changed = true;
        }
        if (forceWarning) {
            this.maybeWarnTownDebt(town, config, true);
            return true;
        }
        return changed;
    }

    private boolean updateNationDebtState(
        final @NotNull RNation nation,
        final boolean hadDebtBefore,
        final @NotNull TaxConfigSection config,
        final boolean forceWarning,
        final @NotNull Instant now
    ) {
        if (!nation.hasTaxDebt()) {
            if (hadDebtBefore) {
                nation.clearTaxDebt();
                this.notifyNationDebtCleared(nation);
                return true;
            }
            nation.clearTaxDebt();
            return false;
        }

        boolean changed = false;
        if (nation.getTaxDebtStartedAt() <= 0L) {
            nation.setTaxDebtStartedAt(now.toEpochMilli());
            changed = true;
        }
        if (forceWarning) {
            this.maybeWarnNationDebt(nation, config, true);
            return true;
        }
        return changed;
    }

    private void maybeWarnTownDebt(
        final @NotNull RTown town,
        final @NotNull TaxConfigSection config,
        final boolean force
    ) {
        if (!town.hasTaxDebt()) {
            return;
        }

        final long now = System.currentTimeMillis();
        if (!force && now - town.getTaxDebtLastWarningAt() < (config.getDebt().warningIntervalTicks() * 50L)) {
            return;
        }
        town.setTaxDebtLastWarningAt(now);
        if (this.plugin.getTownRepository() != null) {
            this.plugin.getTownRepository().update(town);
        }
        this.notifyTownMembers(town, "tax_runtime.warning.town");
    }

    private void maybeWarnNationDebt(
        final @NotNull RNation nation,
        final @NotNull TaxConfigSection config,
        final boolean force
    ) {
        if (!nation.hasTaxDebt()) {
            return;
        }

        final long now = System.currentTimeMillis();
        if (!force && now - nation.getTaxDebtLastWarningAt() < (config.getDebt().warningIntervalTicks() * 50L)) {
            return;
        }
        nation.setTaxDebtLastWarningAt(now);
        if (this.plugin.getNationRepository() != null) {
            this.plugin.getNationRepository().update(nation);
        }
        this.notifyNationMembers(nation, "tax_runtime.warning.nation");
    }

    private void notifyTownDebtCleared(final @NotNull RTown town) {
        this.notifyTownMembers(town, "tax_runtime.cleared.town");
    }

    private void notifyNationDebtCleared(final @NotNull RNation nation) {
        this.notifyNationMembers(nation, "tax_runtime.cleared.nation");
    }

    private void broadcastTownFall(final @NotNull RTown town) {
        this.broadcastToServer("tax_runtime.broadcasts.town_fall", Map.of(
            "town_name", this.resolveTownDisplayName(town),
            "currency_debt", this.formatCurrencySummary(town.getCurrencyTaxDebt()),
            "item_debt", this.formatItemSummary(town.getItemTaxDebt())
        ));
    }

    private void broadcastNationDisband(final @NotNull RNation nation) {
        this.broadcastToServer("tax_runtime.broadcasts.nation_disband", Map.of(
            "nation_name", nation.getNationName(),
            "currency_debt", this.formatCurrencySummary(nation.getCurrencyTaxDebt()),
            "item_debt", this.formatItemSummary(nation.getItemTaxDebt())
        ));
    }

    private void notifyTownMembers(final @NotNull RTown town, final @NotNull String translationKey) {
        final Server server = this.plugin.getServer();
        for (final var member : town.getMembers()) {
            final Player onlinePlayer = server.getPlayer(member.getIdentifier());
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                continue;
            }
            this.sendTownDebtMessage(onlinePlayer, town, translationKey);
        }
    }

    private void notifyNationMembers(final @NotNull RNation nation, final @NotNull String translationKey) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final Server server = this.plugin.getServer();
        if (runtimeService == null) {
            return;
        }

        for (final RTown memberTown : runtimeService.getNationMemberTowns(nation)) {
            for (final var member : memberTown.getMembers()) {
                final Player onlinePlayer = server.getPlayer(member.getIdentifier());
                if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                    continue;
                }
                this.sendNationDebtMessage(onlinePlayer, nation, translationKey);
            }
        }
    }

    private void sendTownDebtMessage(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull String translationKey
    ) {
        new I18n.Builder(translationKey, player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "town_name", this.resolveTownDisplayName(town),
                "currency_debt", this.formatCurrencySummary(town.getCurrencyTaxDebt()),
                "item_debt", this.formatItemSummary(town.getItemTaxDebt()),
                "grace_remaining", this.formatDurationMillis(this.getRemainingGraceMillis(
                    town.getTaxDebtStartedAt(),
                    this.plugin.getTaxConfig().getDebt().gracePeriodTicks()
                ))
            ))
            .build()
            .sendMessage();
    }

    private void sendNationDebtMessage(
        final @NotNull Player player,
        final @NotNull RNation nation,
        final @NotNull String translationKey
    ) {
        new I18n.Builder(translationKey, player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "nation_name", nation.getNationName(),
                "currency_debt", this.formatCurrencySummary(nation.getCurrencyTaxDebt()),
                "item_debt", this.formatItemSummary(nation.getItemTaxDebt()),
                "grace_remaining", this.formatDurationMillis(this.getRemainingGraceMillis(
                    nation.getTaxDebtStartedAt(),
                    this.plugin.getTaxConfig().getDebt().gracePeriodTicks()
                ))
            ))
            .build()
            .sendMessage();
    }

    private void broadcastToServer(final @NotNull String translationKey, final @NotNull Map<String, Object> placeholders) {
        final Server server = this.plugin.getServer();
        for (final Player onlinePlayer : server.getOnlinePlayers()) {
            new I18n.Builder(translationKey, onlinePlayer)
                .includePrefix()
                .withPlaceholders(placeholders)
                .build()
                .sendMessage();
        }
    }

    private @NotNull AExpression parseExpression(final @NotNull TaxConfigSection config) {
        try {
            return this.evaluator.parseString(config.getCurrency().rateExpression());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Falling back to base RDT tax expression after parse failure.", exception);
            try {
                return this.evaluator.parseString("base_tax_percent");
            } catch (final Exception fallbackException) {
                throw new IllegalStateException("Failed to parse fallback RDT tax expression.", fallbackException);
            }
        }
    }

    private double evaluateTaxRate(
        final @NotNull TaxConfigSection config,
        final @NotNull AExpression expression,
        final @NotNull VariableContext context
    ) {
        try {
            final EvaluationEnvironmentBuilder builder = new EvaluationEnvironmentBuilder()
                .withStaticVariable("town_level", context.townLevel())
                .withStaticVariable("town_plots_count", context.townPlotsCount())
                .withStaticVariable("chunk_level_total", context.chunkLevelTotal())
                .withStaticVariable("town_population", context.townPopulation())
                .withStaticVariable("nation_population", context.nationPopulation())
                .withStaticVariable("nation_level", context.nationLevel())
                .withStaticVariable("base_tax_percent", config.getCurrency().baseTaxPercent())
                .withStaticVariable("archetype_modifier", context.archetypeModifier());
            for (final ChunkType chunkType : ChunkType.values()) {
                builder.withStaticVariable(
                    "chunk_type_" + this.normalizeChunkTypeKey(chunkType),
                    context.chunkTypeCounts().getOrDefault(chunkType, 0)
                );
            }
            final Object evaluated = this.evaluator.evaluateExpression(expression, builder.build());
            return config.getCurrency().clampRate(toFiniteDouble(evaluated, config.getCurrency().baseTaxPercent()));
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to evaluate RDT tax-rate expression; using base tax percent.", exception);
            return config.getCurrency().clampRate(config.getCurrency().baseTaxPercent());
        }
    }

    private @NotNull TownVariableContext createTownVariableContext(final @NotNull RTown town, final @Nullable RNation nation) {
        final Map<ChunkType, Integer> chunkCounts = this.countChunks(town.getChunks());
        return new TownVariableContext(
            Math.max(1, town.getTownLevel()),
            town.getChunks().size(),
            town.getChunks().stream().mapToInt(RTownChunk::getChunkLevel).sum(),
            town.getMembers().size(),
            nation == null ? 0 : this.countNationPopulation(nation),
            nation == null ? 0 : nation.getNationLevel(),
            this.plugin.getTaxConfig().getArchetypeModifier(town.getArchetype()),
            chunkCounts
        );
    }

    private @NotNull NationVariableContext createNationVariableContext(final @NotNull RNation nation) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final List<RTown> memberTowns = runtimeService == null ? List.of() : runtimeService.getNationMemberTowns(nation);
        final RTown capitalTown = runtimeService == null ? null : runtimeService.getTown(nation.getCapitalTownUuid());
        final Map<ChunkType, Integer> chunkCounts = new EnumMap<>(ChunkType.class);
        int aggregatePlots = 0;
        int aggregateChunkLevels = 0;
        int nationPopulation = 0;
        for (final RTown memberTown : memberTowns) {
            aggregatePlots += memberTown.getChunks().size();
            aggregateChunkLevels += memberTown.getChunks().stream().mapToInt(RTownChunk::getChunkLevel).sum();
            nationPopulation += memberTown.getMembers().size();
            this.mergeChunkCounts(chunkCounts, this.countChunks(memberTown.getChunks()));
        }
        return new NationVariableContext(
            capitalTown == null ? 0 : capitalTown.getTownLevel(),
            aggregatePlots,
            aggregateChunkLevels,
            capitalTown == null ? 0 : capitalTown.getMembers().size(),
            nationPopulation,
            nation.getNationLevel(),
            capitalTown == null ? 0.0D : this.plugin.getTaxConfig().getArchetypeModifier(capitalTown.getArchetype()),
            chunkCounts
        );
    }

    private @NotNull Map<Material, Integer> calculateTownItemTaxes(
        final @NotNull RTown town,
        final @NotNull TaxConfigSection config
    ) {
        return this.expandItemTaxes(this.countChunks(town.getChunks()), config.getTownItemTaxes());
    }

    private @NotNull Map<Material, Integer> calculateNationItemTaxes(
        final @NotNull RNation nation,
        final @NotNull TaxConfigSection config
    ) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final Map<ChunkType, Integer> chunkCounts = new EnumMap<>(ChunkType.class);
        if (runtimeService != null) {
            for (final RTown memberTown : runtimeService.getNationMemberTowns(nation)) {
                this.mergeChunkCounts(chunkCounts, this.countChunks(memberTown.getChunks()));
            }
        }
        return this.expandItemTaxes(chunkCounts, config.getNationItemTaxes());
    }

    private @NotNull Map<ChunkType, Integer> countChunks(final @NotNull List<RTownChunk> chunks) {
        final Map<ChunkType, Integer> counts = new EnumMap<>(ChunkType.class);
        for (final RTownChunk chunk : chunks) {
            if (chunk.getChunkType() == null) {
                continue;
            }
            counts.merge(chunk.getChunkType(), 1, Integer::sum);
        }
        return counts;
    }

    private void mergeChunkCounts(
        final @NotNull Map<ChunkType, Integer> target,
        final @NotNull Map<ChunkType, Integer> source
    ) {
        for (final Map.Entry<ChunkType, Integer> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    private @NotNull Map<Material, Integer> expandItemTaxes(
        final @NotNull Map<ChunkType, Integer> chunkCounts,
        final @NotNull Map<ChunkType, Map<String, Integer>> configuredTaxes
    ) {
        final Map<Material, Integer> expanded = new LinkedHashMap<>();
        for (final Map.Entry<ChunkType, Map<String, Integer>> entry : configuredTaxes.entrySet()) {
            final int ownedChunkCount = Math.max(0, chunkCounts.getOrDefault(entry.getKey(), 0));
            if (ownedChunkCount <= 0 || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            for (final Map.Entry<String, Integer> itemEntry : entry.getValue().entrySet()) {
                final Material material = Material.matchMaterial(itemEntry.getKey() == null ? "" : itemEntry.getKey().trim().toUpperCase(Locale.ROOT));
                final int amount = itemEntry.getValue() == null ? 0 : itemEntry.getValue();
                if (material == null || material.isAir() || amount <= 0) {
                    continue;
                }
                expanded.merge(material, amount * ownedChunkCount, Integer::sum);
            }
        }
        return expanded;
    }

    private @NotNull Map<Material, Integer> toItemCountMap(final @NotNull Map<String, ItemStack> storedItems) {
        final Map<Material, Integer> counts = new LinkedHashMap<>();
        for (final ItemStack itemStack : storedItems.values()) {
            if (itemStack == null || itemStack.isEmpty() || itemStack.getType().isAir()) {
                continue;
            }
            counts.merge(itemStack.getType(), itemStack.getAmount(), Integer::sum);
        }
        return counts;
    }

    private int collectItemDebt(
        final @NotNull Map<Material, Integer> outstandingDebt,
        final ItemStack @NotNull [] sourceStorage,
        final ItemStack @NotNull [] serverStorage
    ) {
        int movedTotal = 0;
        final List<Map.Entry<Material, Integer>> debtEntries = new ArrayList<>(outstandingDebt.entrySet());
        debtEntries.sort(Comparator.comparing(entry -> entry.getKey().name()));
        for (final Map.Entry<Material, Integer> entry : debtEntries) {
            final Material material = entry.getKey();
            final int requested = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            if (material == null || requested <= 0) {
                outstandingDebt.remove(material);
                continue;
            }

            final int moved = this.moveMaterialBetweenStorages(sourceStorage, serverStorage, material, requested);
            movedTotal += moved;
            final int remaining = requested - moved;
            if (remaining <= 0) {
                outstandingDebt.remove(material);
            } else {
                outstandingDebt.put(material, remaining);
            }
        }
        return movedTotal;
    }

    private int moveMaterialBetweenStorages(
        final ItemStack @NotNull [] sourceStorage,
        final ItemStack @NotNull [] targetStorage,
        final @NotNull Material material,
        final int requestedAmount
    ) {
        int moved = 0;
        for (int slot = 0; slot < sourceStorage.length && moved < requestedAmount; slot++) {
            final ItemStack sourceStack = sourceStorage[slot];
            if (sourceStack == null || sourceStack.isEmpty() || sourceStack.getType() != material) {
                continue;
            }

            final int transferable = Math.min(sourceStack.getAmount(), requestedAmount - moved);
            final int actuallyMoved = this.moveItemAmount(sourceStorage, slot, targetStorage, transferable);
            moved += actuallyMoved;
            if (actuallyMoved < transferable) {
                break;
            }
        }
        return moved;
    }

    private int moveItemAmount(
        final ItemStack @NotNull [] sourceStorage,
        final int sourceSlot,
        final ItemStack @NotNull [] targetStorage,
        final int requestedAmount
    ) {
        final ItemStack sourceStack = sourceStorage[sourceSlot];
        if (sourceStack == null || sourceStack.isEmpty() || requestedAmount <= 0) {
            return 0;
        }

        int remainingToMove = requestedAmount;
        for (int targetSlot = 0; targetSlot < targetStorage.length && remainingToMove > 0; targetSlot++) {
            final ItemStack targetStack = targetStorage[targetSlot];
            if (targetStack == null || targetStack.isEmpty() || !targetStack.isSimilar(sourceStack)) {
                continue;
            }
            final int freeSpace = targetStack.getMaxStackSize() - targetStack.getAmount();
            if (freeSpace <= 0) {
                continue;
            }
            final int moved = Math.min(freeSpace, remainingToMove);
            targetStack.setAmount(targetStack.getAmount() + moved);
            remainingToMove -= moved;
        }
        for (int targetSlot = 0; targetSlot < targetStorage.length && remainingToMove > 0; targetSlot++) {
            final ItemStack targetStack = targetStorage[targetSlot];
            if (targetStack != null && !targetStack.isEmpty()) {
                continue;
            }
            final int moved = Math.min(sourceStack.getMaxStackSize(), remainingToMove);
            final ItemStack createdStack = sourceStack.clone();
            createdStack.setAmount(moved);
            targetStorage[targetSlot] = createdStack;
            remainingToMove -= moved;
        }

        final int actuallyMoved = requestedAmount - remainingToMove;
        if (actuallyMoved <= 0) {
            return 0;
        }

        final int remainingSource = sourceStack.getAmount() - actuallyMoved;
        if (remainingSource <= 0) {
            sourceStorage[sourceSlot] = null;
        } else {
            sourceStack.setAmount(remainingSource);
            sourceStorage[sourceSlot] = sourceStack;
        }
        return actuallyMoved;
    }

    private void replaceItemDebt(final @NotNull RTown town, final @NotNull Map<Material, Integer> outstandingDebt) {
        for (final String key : new ArrayList<>(town.getItemTaxDebt().keySet())) {
            town.setItemTaxDebt(key, null);
        }
        for (final Map.Entry<Material, Integer> entry : outstandingDebt.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            town.setItemTaxDebt(entry.getKey().name().toLowerCase(Locale.ROOT), new ItemStack(entry.getKey(), entry.getValue()));
        }
    }

    private void replaceItemDebt(final @NotNull RNation nation, final @NotNull Map<Material, Integer> outstandingDebt) {
        for (final String key : new ArrayList<>(nation.getItemTaxDebt().keySet())) {
            nation.setItemTaxDebt(key, null);
        }
        for (final Map.Entry<Material, Integer> entry : outstandingDebt.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            nation.setItemTaxDebt(entry.getKey().name().toLowerCase(Locale.ROOT), new ItemStack(entry.getKey(), entry.getValue()));
        }
    }

    private void mergeCounts(final @NotNull Map<Material, Integer> target, final @NotNull Map<Material, Integer> additions) {
        for (final Map.Entry<Material, Integer> entry : additions.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    private int countNationPopulation(final @NotNull RNation nation) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return 0;
        }

        int population = 0;
        for (final RTown memberTown : runtimeService.getNationMemberTowns(nation)) {
            population += memberTown.getMembers().size();
        }
        return population;
    }

    private @NotNull String resolveTownDisplayName(final @NotNull RTown town) {
        if (town.getArchivedTownName() != null && !town.getArchivedTownName().isBlank()) {
            return town.getArchivedTownName();
        }
        return town.getTownName();
    }

    private @NotNull String normalizeChunkTypeKey(final @NotNull ChunkType chunkType) {
        return chunkType.name().toLowerCase(Locale.ROOT);
    }

    private @NotNull String formatCurrencySummary(final @NotNull Map<String, Double> debtEntries) {
        if (debtEntries.isEmpty()) {
            return "None";
        }

        final TownBankService townBankService = this.plugin.getTownBankService();
        final List<String> parts = new ArrayList<>();
        debtEntries.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > EPSILON)
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                final String currencyName = townBankService == null
                    ? entry.getKey()
                    : townBankService.resolveCurrencyDisplayName(entry.getKey());
                parts.add(String.format(Locale.US, "%.2f %s", entry.getValue(), currencyName));
            });
        return parts.isEmpty() ? "None" : String.join(", ", parts);
    }

    private @NotNull String formatItemSummary(final @NotNull Map<String, ItemStack> debtEntries) {
        if (debtEntries.isEmpty()) {
            return "None";
        }

        final List<String> parts = new ArrayList<>();
        for (final ItemStack itemStack : debtEntries.values()) {
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            parts.add(itemStack.getAmount() + "x " + itemStack.getType().name().toLowerCase(Locale.ROOT));
        }
        return parts.isEmpty() ? "None" : String.join(", ", parts);
    }

    private long getRemainingGraceMillis(final long debtStartedAt, final long gracePeriodTicks) {
        if (debtStartedAt <= 0L) {
            return Math.max(0L, gracePeriodTicks * 50L);
        }
        final long graceMillis = Math.max(1L, gracePeriodTicks) * 50L;
        return Math.max(0L, graceMillis - (System.currentTimeMillis() - debtStartedAt));
    }

    private static long calculateInitialDelayTicks(
        final @NotNull TaxConfigSection.TaxSchedule schedule,
        final @NotNull Instant now
    ) {
        final Instant nextRun = calculateNextRun(schedule, now);
        final Duration delay = Duration.between(now, nextRun);
        return Math.max(1L, (long) Math.ceil(Math.max(1D, delay.toMillis()) / 50.0D));
    }

    private static @NotNull Instant calculateNextRun(
        final @NotNull TaxConfigSection.TaxSchedule schedule,
        final @NotNull Instant now
    ) {
        final ZonedDateTime zonedNow = now.atZone(schedule.timeZoneId());
        final ZonedDateTime anchor = zonedNow.toLocalDate()
            .atTime(schedule.startTime())
            .atZone(schedule.timeZoneId());
        if (anchor.isAfter(zonedNow)) {
            return anchor.toInstant();
        }

        final long periodMillis = Math.max(1L, schedule.durationTicks()) * 50L;
        final long elapsedMillis = Duration.between(anchor.toInstant(), now).toMillis();
        final long periodsElapsed = (elapsedMillis / periodMillis) + 1L;
        return anchor.toInstant().plusMillis(periodMillis * periodsElapsed);
    }

    private static double toFiniteDouble(final @Nullable Object rawValue, final double fallback) {
        if (rawValue instanceof Number number) {
            final double value = number.doubleValue();
            return Double.isFinite(value) ? value : fallback;
        }
        if (rawValue != null) {
            try {
                final double parsed = Double.parseDouble(rawValue.toString().trim());
                return Double.isFinite(parsed) ? parsed : fallback;
            } catch (final NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private @NotNull String formatDurationMillis(final long durationMillis) {
        final long clampedMillis = Math.max(0L, durationMillis);
        final long totalSeconds = clampedMillis / 1000L;
        final long days = totalSeconds / 86400L;
        final long hours = (totalSeconds % 86400L) / 3600L;
        final long minutes = (totalSeconds % 3600L) / 60L;
        if (days > 0L) {
            return days + "d " + hours + "h";
        }
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        return Math.max(0L, minutes) + "m";
    }

    private sealed interface VariableContext permits TownVariableContext, NationVariableContext {
        int townLevel();

        int townPlotsCount();

        int chunkLevelTotal();

        int townPopulation();

        int nationPopulation();

        int nationLevel();

        double archetypeModifier();

        @NotNull Map<ChunkType, Integer> chunkTypeCounts();
    }

    private record TownVariableContext(
        int townLevel,
        int townPlotsCount,
        int chunkLevelTotal,
        int townPopulation,
        int nationPopulation,
        int nationLevel,
        double archetypeModifier,
        @NotNull Map<ChunkType, Integer> chunkTypeCounts
    ) implements VariableContext {
    }

    private record NationVariableContext(
        int townLevel,
        int townPlotsCount,
        int chunkLevelTotal,
        int townPopulation,
        int nationPopulation,
        int nationLevel,
        double archetypeModifier,
        @NotNull Map<ChunkType, Integer> chunkTypeCounts
    ) implements VariableContext {
    }
}
