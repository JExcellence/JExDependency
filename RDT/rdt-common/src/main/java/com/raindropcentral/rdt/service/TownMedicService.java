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
import com.raindropcentral.rdt.configs.MedicConfigSection;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rplatform.scheduler.CancellableTaskHandle;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Maintains runtime-only Medic chunk buffs, timed recovery state, and refill cooldowns.
 *
 * <p>The service is intentionally ephemeral: all timers, active buffs, and cooldowns live only in
 * memory and are cleared when the player disconnects or the plugin shuts down.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownMedicService {

    private static final long TICKS_PER_SECOND = 20L;
    private static final long MILLIS_PER_TICK = 50L;
    private static final double DEFAULT_VANILLA_MAX_HEALTH = 20.0D;
    private static final int VANILLA_MAX_FOOD_LEVEL = 20;

    private final RDT plugin;
    private final NamespacedKey fortifiedRecoveryModifierKey;
    private final Map<UUID, PlayerMedicState> playerStates = new LinkedHashMap<>();

    private long currentTick;
    private @Nullable CancellableTaskHandle pulseTask;

    /**
     * Creates the Medic chunk runtime service.
     *
     * @param plugin active plugin runtime
     */
    public TownMedicService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.fortifiedRecoveryModifierKey = new NamespacedKey(plugin.getPlugin(), "town_medic_fortified_recovery");
    }

    /**
     * Starts the repeating medic pulse task when a scheduler is available.
     */
    public void start() {
        if (this.plugin.getScheduler() == null || this.pulseTask != null) {
            return;
        }
        this.pulseTask = this.plugin.getScheduler().runRepeating(this::tick, 1L, 1L);
        final var server = this.plugin.getPlugin().getServer();
        if (server == null) {
            return;
        }
        for (final Player player : server.getOnlinePlayers()) {
            this.handlePlayerJoin(player);
        }
    }

    /**
     * Stops medic pulses and clears every active runtime effect.
     */
    public void shutdown() {
        if (this.pulseTask != null) {
            this.pulseTask.cancel();
            this.pulseTask = null;
        }

        for (final UUID playerUuid : new ArrayList<>(this.playerStates.keySet())) {
            this.clearPlayerState(playerUuid);
        }
        this.currentTick = 0L;
    }

    /**
     * Refreshes one player's medic state after a join event.
     *
     * @param player joining player
     */
    public void handlePlayerJoin(final @NotNull Player player) {
        this.synchronizePlayerState(player, player.getLocation(), true);
    }

    /**
     * Refreshes one player's medic state after a cross-chunk movement event.
     *
     * @param player moving player
     * @param from previous location, or {@code null} when unavailable
     * @param to destination location, or {@code null} when unavailable
     */
    public void handlePlayerMove(
        final @NotNull Player player,
        final @Nullable Location from,
        final @Nullable Location to
    ) {
        if (to == null || ChunkKey.from(from) != null && ChunkKey.from(from).equals(ChunkKey.from(to))) {
            return;
        }
        this.synchronizePlayerState(player, to, true);
    }

    /**
     * Refreshes one player's medic state after a teleport event.
     *
     * @param player teleported player
     * @param to destination location, or {@code null} when unavailable
     */
    public void handlePlayerTeleport(final @NotNull Player player, final @Nullable Location to) {
        this.synchronizePlayerState(player, to, true);
    }

    /**
     * Refreshes one player's medic state after a respawn event.
     *
     * @param player respawned player
     * @param respawnLocation respawn destination, or {@code null} when unavailable
     */
    public void handlePlayerRespawn(final @NotNull Player player, final @Nullable Location respawnLocation) {
        this.synchronizePlayerState(player, respawnLocation, true);
    }

    /**
     * Clears every medic runtime effect owned by one disconnecting player.
     *
     * @param player disconnecting player
     */
    public void handlePlayerQuit(final @NotNull Player player) {
        this.clearPlayerState(player.getUniqueId());
    }

    /**
     * Returns viewer-specific Medic chunk status for the chunk view.
     *
     * @param player viewer to inspect
     * @param townChunk chunk being rendered
     * @return viewer-specific medic status for that chunk
     */
    public @NotNull MedicChunkStatus getChunkStatus(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(townChunk, "townChunk");

        if (townChunk.getChunkType() != ChunkType.MEDIC) {
            return new MedicChunkStatus(false, false, false, 0L, 0L);
        }

        final boolean viewerEligible = this.isPlayerInTown(player, townChunk.getTown().getTownUUID());
        final boolean viewerInsideChunk = ChunkKey.matches(player.getLocation(), townChunk);
        final PlayerMedicState state = this.playerStates.get(player.getUniqueId());
        final long fortifiedRemainingMillis = state != null
            && Objects.equals(state.fortifiedRecoveryTownUuid, townChunk.getTown().getTownUUID())
            && state.fortifiedRecoveryExpiryTick > this.currentTick
            ? (state.fortifiedRecoveryExpiryTick - this.currentTick) * MILLIS_PER_TICK
            : 0L;
        final long emergencyCooldownRemainingMillis = state != null
            && viewerEligible
            && viewerInsideChunk
            && state.nextEmergencyRefillTick > this.currentTick
            ? (state.nextEmergencyRefillTick - this.currentTick) * MILLIS_PER_TICK
            : 0L;
        return new MedicChunkStatus(
            true,
            viewerEligible,
            viewerInsideChunk,
            Math.max(0L, fortifiedRemainingMillis),
            Math.max(0L, emergencyCooldownRemainingMillis)
        );
    }

    private void tick() {
        this.currentTick++;
        if (this.playerStates.isEmpty()) {
            return;
        }

        for (final PlayerMedicState state : new ArrayList<>(this.playerStates.values())) {
            this.tickPlayer(state);
        }
    }

    private void tickPlayer(final @NotNull PlayerMedicState state) {
        final Player player = state.player;
        if (!player.isOnline()) {
            this.clearPlayerState(player.getUniqueId());
            return;
        }

        final RTownChunk currentMedicChunk = this.resolveCurrentMedicChunk(state);
        state.currentMedicChunk = currentMedicChunk;
        if (currentMedicChunk != null) {
            this.refreshFortifiedRecovery(state, currentMedicChunk);
            this.applyStandingEffects(state, currentMedicChunk);
            this.applyEmergencyRefillIfReady(state, currentMedicChunk, false);
        }

        this.tickFortifiedRecovery(state);
        if (!state.shouldRetain(this.currentTick)) {
            this.playerStates.remove(player.getUniqueId());
        }
    }

    private void synchronizePlayerState(
        final @NotNull Player player,
        final @Nullable Location location,
        final boolean processArrival
    ) {
        Objects.requireNonNull(player, "player");
        final PlayerMedicState state = this.playerStates.computeIfAbsent(
            player.getUniqueId(),
            ignored -> new PlayerMedicState(player)
        );
        final ChunkKey previousMedicKey = ChunkKey.from(state.currentMedicChunk);
        state.currentMedicChunk = this.resolveQualifiedMedicChunk(player, location);
        final ChunkKey currentMedicKey = ChunkKey.from(state.currentMedicChunk);

        if (!processArrival || state.currentMedicChunk == null || Objects.equals(previousMedicKey, currentMedicKey)) {
            return;
        }

        this.applyArrivalEffects(state, state.currentMedicChunk);
    }

    private @Nullable RTownChunk resolveCurrentMedicChunk(final @NotNull PlayerMedicState state) {
        if (state.currentMedicChunk == null) {
            return null;
        }
        if (!ChunkKey.matches(state.player.getLocation(), state.currentMedicChunk)) {
            return null;
        }
        return this.isPlayerEligibleForChunk(state.player, state.currentMedicChunk) ? state.currentMedicChunk : null;
    }

    private @Nullable RTownChunk resolveQualifiedMedicChunk(
        final @NotNull Player player,
        final @Nullable Location location
    ) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null || location == null || location.getWorld() == null) {
            return null;
        }

        final RTownChunk townChunk = runtimeService.getChunkAt(location);
        if (townChunk == null || townChunk.getChunkType() != ChunkType.MEDIC) {
            return null;
        }
        return this.isPlayerEligibleForChunk(player, townChunk) ? townChunk : null;
    }

    private void applyArrivalEffects(final @NotNull PlayerMedicState state, final @NotNull RTownChunk townChunk) {
        this.refreshFortifiedRecovery(state, townChunk);
        this.applyFoodRegen(state, townChunk, true);
        this.applyHealthRegen(state, townChunk, true);
        this.applyCleanse(state, townChunk, true);
        this.applyEmergencyRefillIfReady(state, townChunk, true);
    }

    private void applyStandingEffects(final @NotNull PlayerMedicState state, final @NotNull RTownChunk townChunk) {
        this.applyFoodRegen(state, townChunk, false);
        this.applyHealthRegen(state, townChunk, false);
        this.applyCleanse(state, townChunk, false);
    }

    private void applyFoodRegen(
        final @NotNull PlayerMedicState state,
        final @NotNull RTownChunk townChunk,
        final boolean immediate
    ) {
        final MedicConfigSection.FoodRegenSettings settings = this.plugin.getMedicConfig().getFoodRegen();
        if (!settings.enabled() || !settings.isUnlocked(townChunk.getChunkLevel())) {
            return;
        }
        if (!immediate && this.currentTick < state.nextFoodPulseTick) {
            return;
        }
        if (!this.isPlayerEligibleForChunk(state.player, townChunk)) {
            state.currentMedicChunk = null;
            return;
        }

        final Player player = state.player;
        final int targetFood = Math.min(VANILLA_MAX_FOOD_LEVEL, player.getFoodLevel() + settings.foodPointsPerPulse());
        player.setFoodLevel(targetFood);
        final float targetSaturation = (float) Math.min(targetFood, player.getSaturation() + settings.saturationPerPulse());
        player.setSaturation(targetSaturation);
        state.nextFoodPulseTick = this.currentTick + settings.intervalTicks();
    }

    private void applyHealthRegen(
        final @NotNull PlayerMedicState state,
        final @NotNull RTownChunk townChunk,
        final boolean immediate
    ) {
        final MedicConfigSection.HealthRegenSettings settings = this.plugin.getMedicConfig().getHealthRegen();
        if (!settings.enabled() || !settings.isUnlocked(townChunk.getChunkLevel())) {
            return;
        }
        if (!immediate && this.currentTick < state.nextHealthPulseTick) {
            return;
        }
        if (!this.isPlayerEligibleForChunk(state.player, townChunk)) {
            state.currentMedicChunk = null;
            return;
        }

        final Player player = state.player;
        final double maxHealth = this.resolveCurrentMaxHealth(player);
        player.setHealth(Math.min(maxHealth, player.getHealth() + settings.healthPointsPerPulse()));
        state.nextHealthPulseTick = this.currentTick + settings.intervalTicks();
    }

    private void applyCleanse(
        final @NotNull PlayerMedicState state,
        final @NotNull RTownChunk townChunk,
        final boolean immediate
    ) {
        final MedicConfigSection.CleanseSettings settings = this.plugin.getMedicConfig().getCleanse();
        if (!settings.enabled() || !settings.isUnlocked(townChunk.getChunkLevel())) {
            return;
        }
        if (!immediate && this.currentTick < state.nextCleansePulseTick) {
            return;
        }
        if (!this.isPlayerEligibleForChunk(state.player, townChunk)) {
            state.currentMedicChunk = null;
            return;
        }

        for (final String harmfulEffectKey : settings.harmfulEffects()) {
            final PotionEffectType harmfulEffect = this.resolvePotionEffectType(harmfulEffectKey);
            if (harmfulEffect != null) {
                state.player.removePotionEffect(harmfulEffect);
            }
        }
        state.nextCleansePulseTick = this.currentTick + settings.intervalTicks();
    }

    private void refreshFortifiedRecovery(final @NotNull PlayerMedicState state, final @NotNull RTownChunk townChunk) {
        final MedicConfigSection.FortifiedRecoverySettings settings = this.plugin.getMedicConfig().getFortifiedRecovery();
        if (!settings.enabled() || !settings.isUnlocked(townChunk.getChunkLevel())) {
            return;
        }

        final boolean newTownBuff = !Objects.equals(state.fortifiedRecoveryTownUuid, townChunk.getTown().getTownUUID());
        if (newTownBuff || state.fortifiedRecoveryExpiryTick <= this.currentTick || settings.refreshWhileInside()) {
            state.fortifiedRecoveryTownUuid = townChunk.getTown().getTownUUID();
            state.fortifiedRecoveryExpiryTick = this.currentTick + (settings.durationSeconds() * TICKS_PER_SECOND);
        }

        this.ensureFortifiedRecoveryModifier(state.player, settings.targetMaxHealth());
        if (newTownBuff || state.nextFortifiedUpkeepTick <= this.currentTick) {
            this.applyFortifiedUpkeep(state.player, settings);
            state.nextFortifiedUpkeepTick = this.currentTick + settings.upkeepIntervalTicks();
        }
    }

    private void tickFortifiedRecovery(final @NotNull PlayerMedicState state) {
        if (state.fortifiedRecoveryTownUuid == null) {
            this.clearFortifiedRecoveryModifier(state.player);
            return;
        }

        final MedicConfigSection.FortifiedRecoverySettings settings = this.plugin.getMedicConfig().getFortifiedRecovery();
        if (!settings.enabled()
            || state.fortifiedRecoveryExpiryTick <= this.currentTick
            || !this.isPlayerInTown(state.player, state.fortifiedRecoveryTownUuid)) {
            this.clearFortifiedRecovery(state);
            return;
        }

        this.ensureFortifiedRecoveryModifier(state.player, settings.targetMaxHealth());
        if (state.nextFortifiedUpkeepTick <= this.currentTick) {
            this.applyFortifiedUpkeep(state.player, settings);
            state.nextFortifiedUpkeepTick = this.currentTick + settings.upkeepIntervalTicks();
        }
    }

    private void applyFortifiedUpkeep(
        final @NotNull Player player,
        final @NotNull MedicConfigSection.FortifiedRecoverySettings settings
    ) {
        player.setFoodLevel(settings.targetFoodLevel());
        player.setSaturation((float) Math.min(settings.targetFoodLevel(), settings.targetSaturation()));
    }

    private void applyEmergencyRefillIfReady(
        final @NotNull PlayerMedicState state,
        final @NotNull RTownChunk townChunk,
        final boolean entering
    ) {
        final MedicConfigSection.EmergencyRefillSettings settings = this.plugin.getMedicConfig().getEmergencyRefill();
        if (!settings.enabled() || !settings.isUnlocked(townChunk.getChunkLevel())) {
            return;
        }

        final long cooldownTicks = Math.max(0L, settings.cooldownSeconds() * TICKS_PER_SECOND);
        if (entering && !settings.triggerOnEntry() && state.nextEmergencyRefillTick <= this.currentTick) {
            state.nextEmergencyRefillTick = this.currentTick + cooldownTicks;
            return;
        }
        if (state.nextEmergencyRefillTick > this.currentTick) {
            return;
        }
        if (!this.isPlayerEligibleForChunk(state.player, townChunk)) {
            state.currentMedicChunk = null;
            return;
        }

        final Player player = state.player;
        final double targetHealth = switch (settings.targetHealthMode()) {
            case CURRENT_MAX -> this.resolveCurrentMaxHealth(player);
            case VANILLA_MAX -> this.resolveVanillaMaxHealth(player);
        };
        player.setHealth(Math.min(this.resolveCurrentMaxHealth(player), targetHealth));
        player.setFoodLevel(settings.targetFoodLevel());
        player.setSaturation((float) Math.min(settings.targetFoodLevel(), settings.targetSaturation()));
        state.nextEmergencyRefillTick = this.currentTick + cooldownTicks;
    }

    private void clearPlayerState(final @NotNull UUID playerUuid) {
        final PlayerMedicState state = this.playerStates.remove(Objects.requireNonNull(playerUuid, "playerUuid"));
        if (state == null) {
            return;
        }
        this.clearFortifiedRecovery(state);
    }

    private void clearFortifiedRecovery(final @NotNull PlayerMedicState state) {
        state.fortifiedRecoveryTownUuid = null;
        state.fortifiedRecoveryExpiryTick = 0L;
        state.nextFortifiedUpkeepTick = 0L;
        this.clearFortifiedRecoveryModifier(state.player);
    }

    private @Nullable PotionEffectType resolvePotionEffectType(final @NotNull String effectKey) {
        try {
            return Registry.EFFECT.get(NamespacedKey.minecraft(effectKey));
        } catch (final ExceptionInInitializerError | IllegalStateException | NoClassDefFoundError ignored) {
            return null;
        }
    }

    private void ensureFortifiedRecoveryModifier(final @NotNull Player player, final double targetMaxHealth) {
        final AttributeInstance attribute = this.resolveMaxHealthAttribute(player);
        if (attribute == null) {
            if (this.resolveCurrentMaxHealth(player) < targetMaxHealth) {
                this.setLegacyMaxHealth(player, targetMaxHealth);
            }
            return;
        }

        attribute.removeModifier(this.fortifiedRecoveryModifierKey);
        final double currentMaxHealth = attribute.getValue();
        final double modifierAmount = Math.max(0.0D, targetMaxHealth - currentMaxHealth);
        if (modifierAmount > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(
                this.fortifiedRecoveryModifierKey,
                modifierAmount,
                AttributeModifier.Operation.ADD_NUMBER
            ));
        }

        final double effectiveMaxHealth = attribute.getValue();
        if (player.getHealth() > effectiveMaxHealth) {
            player.setHealth(effectiveMaxHealth);
        }
    }

    private void clearFortifiedRecoveryModifier(final @NotNull Player player) {
        final AttributeInstance attribute = this.resolveMaxHealthAttribute(player);
        if (attribute == null) {
            this.setLegacyMaxHealth(player, this.resolveVanillaMaxHealth(player));
            return;
        }

        attribute.removeModifier(this.fortifiedRecoveryModifierKey);
        final double effectiveMaxHealth = attribute.getValue();
        if (player.getHealth() > effectiveMaxHealth) {
            player.setHealth(effectiveMaxHealth);
        }
    }

    private double resolveCurrentMaxHealth(final @NotNull Player player) {
        final AttributeInstance attribute = this.resolveMaxHealthAttribute(player);
        return attribute == null ? this.getLegacyMaxHealth(player) : attribute.getValue();
    }

    private double resolveVanillaMaxHealth(final @NotNull Player player) {
        final AttributeInstance attribute = this.resolveMaxHealthAttribute(player);
        return attribute == null ? DEFAULT_VANILLA_MAX_HEALTH : attribute.getDefaultValue();
    }

    private @Nullable AttributeInstance resolveMaxHealthAttribute(final @NotNull Player player) {
        try {
            final Object maxHealthAttribute = Class.forName("org.bukkit.attribute.Attribute")
                .getField("MAX_HEALTH")
                .get(null);
            return player.getAttribute((org.bukkit.attribute.Attribute) maxHealthAttribute);
        } catch (final ReflectiveOperationException | ExceptionInInitializerError | NoClassDefFoundError ignored) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private double getLegacyMaxHealth(final @NotNull Player player) {
        try {
            return player.getMaxHealth();
        } catch (final UnsupportedOperationException ignored) {
            return DEFAULT_VANILLA_MAX_HEALTH;
        }
    }

    @SuppressWarnings("deprecation")
    private void setLegacyMaxHealth(final @NotNull Player player, final double maxHealth) {
        try {
            player.setMaxHealth(maxHealth);
            if (player.getHealth() > maxHealth) {
                player.setHealth(maxHealth);
            }
        } catch (final UnsupportedOperationException ignored) {
        }
    }

    private boolean isPlayerEligibleForChunk(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.MEDIC
            && this.isPlayerInTown(player, townChunk.getTown().getTownUUID());
    }

    private boolean isPlayerInTown(final @NotNull Player player, final @NotNull UUID townUuid) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return false;
        }
        final var playerData = runtimeService.getPlayerData(player.getUniqueId());
        return playerData != null && Objects.equals(playerData.getTownUUID(), townUuid);
    }

    /**
     * Viewer-specific Medic chunk state exposed to chunk views.
     *
     * @param available whether the rendered chunk is a Medic chunk
     * @param viewerEligible whether the viewer belongs to the chunk-owning town
     * @param viewerInsideChunk whether the viewer is currently standing inside this Medic chunk
     * @param fortifiedRecoveryRemainingMillis remaining fortified-recovery duration in milliseconds
     * @param emergencyRefillCooldownRemainingMillis remaining emergency-refill cooldown in milliseconds
     */
    public record MedicChunkStatus(
        boolean available,
        boolean viewerEligible,
        boolean viewerInsideChunk,
        long fortifiedRecoveryRemainingMillis,
        long emergencyRefillCooldownRemainingMillis
    ) {

        /**
         * Returns whether fortified recovery is currently active for the viewer.
         *
         * @return {@code true} when fortified recovery is active
         */
        public boolean fortifiedRecoveryActive() {
            return this.fortifiedRecoveryRemainingMillis > 0L;
        }

        /**
         * Returns whether the viewer's emergency refill is still cooling down.
         *
         * @return {@code true} when emergency refill is on cooldown
         */
        public boolean emergencyRefillOnCooldown() {
            return this.emergencyRefillCooldownRemainingMillis > 0L;
        }
    }

    private static final class PlayerMedicState {

        private final Player player;

        private @Nullable RTownChunk currentMedicChunk;
        private @Nullable UUID fortifiedRecoveryTownUuid;
        private long nextFoodPulseTick;
        private long nextHealthPulseTick;
        private long nextCleansePulseTick;
        private long nextFortifiedUpkeepTick;
        private long fortifiedRecoveryExpiryTick;
        private long nextEmergencyRefillTick;

        private PlayerMedicState(final @NotNull Player player) {
            this.player = Objects.requireNonNull(player, "player");
        }

        private boolean shouldRetain(final long currentTick) {
            return this.currentMedicChunk != null
                || this.fortifiedRecoveryExpiryTick > currentTick
                || this.nextEmergencyRefillTick > currentTick;
        }
    }

    private record ChunkKey(@NotNull String worldName, int chunkX, int chunkZ) {

        private static @Nullable ChunkKey from(final @Nullable Location location) {
            if (location == null || location.getWorld() == null) {
                return null;
            }
            return new ChunkKey(
                location.getWorld().getName(),
                TownRuntimeService.toChunkCoordinate(location.getBlockX()),
                TownRuntimeService.toChunkCoordinate(location.getBlockZ())
            );
        }

        private static @Nullable ChunkKey from(final @Nullable RTownChunk townChunk) {
            if (townChunk == null) {
                return null;
            }
            return new ChunkKey(townChunk.getWorldName(), townChunk.getX(), townChunk.getZ());
        }

        private static boolean matches(final @Nullable Location location, final @NotNull RTownChunk townChunk) {
            return Objects.equals(from(location), from(townChunk));
        }
    }
}
