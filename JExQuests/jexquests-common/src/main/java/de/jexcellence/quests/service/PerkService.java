package de.jexcellence.quests.service;

import de.jexcellence.core.api.requirement.Requirement;
import de.jexcellence.core.api.requirement.RequirementContext;
import de.jexcellence.core.api.requirement.RequirementEvaluator;
import de.jexcellence.core.api.requirement.RequirementResult;
import de.jexcellence.core.api.reward.Reward;
import de.jexcellence.core.api.reward.RewardContext;
import de.jexcellence.core.api.reward.RewardExecutor;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.api.PerkSnapshot;
import de.jexcellence.quests.api.event.PerkActivatedEvent;
import de.jexcellence.quests.database.entity.Perk;
import de.jexcellence.quests.database.entity.PerkKind;
import de.jexcellence.quests.database.entity.PlayerPerk;
import de.jexcellence.quests.database.repository.PerkRepository;
import de.jexcellence.quests.database.repository.PlayerPerkRepository;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Perk lifecycle: unlock (gate by requirement), toggle on/off
 * (TOGGLE kind), activate with cooldown (ACTIVE kind), list owned.
 */
public class PerkService {

    private static final String SOURCE = "JExQuests";

    private final PerkRepository perks;
    private final PlayerPerkRepository playerPerks;
    private final JExLogger logger;

    public PerkService(
            @NotNull PerkRepository perks,
            @NotNull PlayerPerkRepository playerPerks,
            @NotNull JExLogger logger
    ) {
        this.perks = perks;
        this.playerPerks = playerPerks;
        this.logger = logger;
    }

    /** Unlock a perk for the player after gate check. */
    public @NotNull CompletableFuture<UnlockResult> unlockAsync(@NotNull UUID playerUuid, @NotNull String perkIdentifier) {
        return this.perks.findByIdentifierAsync(perkIdentifier).thenCompose(optPerk -> {
            if (optPerk.isEmpty()) return CompletableFuture.completedFuture(UnlockResult.NOT_FOUND);
            final Perk perk = optPerk.get();
            if (!perk.isEnabled()) return CompletableFuture.completedFuture(UnlockResult.DISABLED);

            return this.playerPerks.findAsync(playerUuid, perkIdentifier).thenCompose(existing -> {
                if (existing.isPresent()) return CompletableFuture.completedFuture(UnlockResult.ALREADY_OWNED);
                final Requirement requirement = RewardRequirementCodec.decodeRequirement(perk.getRequirementData());
                return gate(requirement, playerUuid).thenApply(passed -> {
                    if (!passed) return UnlockResult.REQUIREMENTS_NOT_MET;
                    this.playerPerks.create(new PlayerPerk(playerUuid, perkIdentifier));
                    return UnlockResult.UNLOCKED;
                });
            });
        }).exceptionally(ex -> {
            this.logger.error("unlock failed: {}", ex.getMessage());
            return UnlockResult.ERROR;
        });
    }

    /** Toggle on/off — only valid for {@link PerkKind#TOGGLE}. */
    public @NotNull CompletableFuture<ToggleResult> toggleAsync(@NotNull UUID playerUuid, @NotNull String perkIdentifier) {
        return this.perks.findByIdentifierAsync(perkIdentifier).thenCombine(
                this.playerPerks.findAsync(playerUuid, perkIdentifier),
                (optPerk, optOwned) -> {
                    if (optPerk.isEmpty()) return ToggleResult.NOT_FOUND;
                    if (optOwned.isEmpty()) return ToggleResult.NOT_OWNED;
                    if (optPerk.get().getKind() != PerkKind.TOGGLE) return ToggleResult.NOT_TOGGLEABLE;
                    final PlayerPerk row = optOwned.get();
                    row.setEnabled(!row.isEnabled());
                    this.playerPerks.update(row);
                    return row.isEnabled() ? ToggleResult.ENABLED : ToggleResult.DISABLED;
                }
        ).exceptionally(ex -> {
            this.logger.error("toggle failed: {}", ex.getMessage());
            return ToggleResult.ERROR;
        });
    }

    /** Activate a one-shot perk. Honours {@link Perk#getCooldownSeconds()}. */
    public @NotNull CompletableFuture<ActivationResult> activateAsync(@NotNull UUID playerUuid, @NotNull String perkIdentifier) {
        return this.perks.findByIdentifierAsync(perkIdentifier).thenCombine(
                this.playerPerks.findAsync(playerUuid, perkIdentifier),
                (optPerk, optOwned) -> {
                    if (optPerk.isEmpty()) return ActivationResult.notFound();
                    if (optOwned.isEmpty()) return ActivationResult.notOwned();
                    final Perk perk = optPerk.get();
                    if (perk.getKind() != PerkKind.ACTIVE) return ActivationResult.notActivatable();

                    final PlayerPerk row = optOwned.get();
                    final LocalDateTime last = row.getLastActivatedAt();
                    if (last != null && perk.getCooldownSeconds() > 0) {
                        final long cooldownRemaining = perk.getCooldownSeconds()
                                - java.time.Duration.between(last, LocalDateTime.now()).toSeconds();
                        if (cooldownRemaining > 0) return ActivationResult.onCooldown(cooldownRemaining);
                    }

                    row.setLastActivatedAt(LocalDateTime.now());
                    row.setActivationCount(row.getActivationCount() + 1);
                    this.playerPerks.update(row);

                    final Reward reward = RewardRequirementCodec.decodeReward(perk.getRewardData());
                    if (reward != null) {
                        final RewardExecutor executor = RewardExecutor.get();
                        if (executor != null) {
                            executor.grantSync(reward, new RewardContext(playerUuid, SOURCE, "perk-activate"));
                        }
                    }

                    de.jexcellence.quests.util.EventDispatch.fire(new PerkActivatedEvent(new PerkSnapshot(
                            playerUuid,
                            perk.getIdentifier(),
                            perk.getKind().name(),
                            row.isEnabled(),
                            0L,
                            row.getActivationCount()
                    )));
                    return ActivationResult.activated();
                }
        ).exceptionally(ex -> {
            this.logger.error("activate failed: {}", ex.getMessage());
            return ActivationResult.error(ex.getMessage());
        });
    }

    /**
     * Finds all perks owned by the player.
     */
    public @NotNull CompletableFuture<List<PlayerPerk>> ownedAsync(@NotNull UUID playerUuid) {
        return this.playerPerks.findByPlayerAsync(playerUuid).exceptionally(ex -> {
            this.logger.error("owned perks failed: {}", ex.getMessage());
            return List.of();
        });
    }

    /**
     * Returns every owned perk paired with its up-to-date
     * {@link Perk} definition, so views can render display name,
     * kind, cooldown, description without doing the join themselves.
     * Perks whose definition has been removed from YAML are filtered
     * out (stale ownership rows remain in the DB but aren't rendered).
     */
    public @NotNull CompletableFuture<List<OwnedPerk>> ownedWithDefinitionsAsync(@NotNull UUID playerUuid) {
        return this.playerPerks.findByPlayerAsync(playerUuid).thenCompose(owned -> {
            if (owned.isEmpty()) return CompletableFuture.completedFuture(List.<OwnedPerk>of());
            final CompletableFuture<OwnedPerk>[] futures = owned.stream()
                    .map(row -> this.perks.findByIdentifierAsync(row.getPerkIdentifier())
                            .thenApply(opt -> opt.map(definition -> new OwnedPerk(row, definition)).orElse(null)))
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures).thenApply(v -> {
                final List<OwnedPerk> out = new java.util.ArrayList<>(futures.length);
                for (final CompletableFuture<OwnedPerk> f : futures) {
                    final OwnedPerk resolved = f.getNow(null);
                    if (resolved != null) out.add(resolved);
                }
                return out;
            });
        }).exceptionally(ex -> {
            this.logger.error("owned perks + definitions failed: {}", ex.getMessage());
            return List.of();
        });
    }

    /** Raw access to the perk repository — used by views for bulk listing. */
    public @NotNull de.jexcellence.quests.database.repository.PerkRepository perks() {
        return this.perks;
    }

    /** Raw access to the player-perk repository — used by migration tooling. */
    public @NotNull PlayerPerkRepository playerPerks() {
        return this.playerPerks;
    }

    /**
     * View-friendly bundle pairing the player's perk row with the
     * currently loaded {@link Perk} definition. Exposes
     * {@link #cooldownRemainingSeconds()} so paginated overviews can
     * render countdowns without recomputing duration math.
     */
    public record OwnedPerk(@NotNull PlayerPerk ownership, @NotNull Perk definition) {

        /**
         * Seconds left until an ACTIVE-kind perk can be fired again.
         * Returns {@code 0} for non-ACTIVE perks, never-activated
         * perks, or when the cooldown has elapsed.
         */
        public long cooldownRemainingSeconds() {
            if (this.definition.getKind() != PerkKind.ACTIVE) return 0L;
            final long cooldown = this.definition.getCooldownSeconds();
            if (cooldown <= 0L) return 0L;
            final LocalDateTime last = this.ownership.getLastActivatedAt();
            if (last == null) return 0L;
            final long elapsed = java.time.Duration.between(last, LocalDateTime.now()).toSeconds();
            final long remaining = cooldown - elapsed;
            return Math.max(0L, remaining);
        }

        /**
         * Returns the identifier of the perk.
         */
        public @NotNull String identifier() {
            return this.ownership.getPerkIdentifier();
        }
    }

    private @NotNull CompletableFuture<Boolean> gate(Requirement requirement, @NotNull UUID playerUuid) {
        if (requirement == null) return CompletableFuture.completedFuture(true);
        final RequirementEvaluator evaluator = RequirementEvaluator.get();
        if (evaluator == null) return CompletableFuture.completedFuture(true);
        return evaluator.evaluate(requirement, new RequirementContext(playerUuid, SOURCE, "perk-unlock"))
                .thenApply(RequirementResult::isMet);
    }

    /**
     * Result of unlocking a perk.
     */
    public enum UnlockResult { UNLOCKED, ALREADY_OWNED, REQUIREMENTS_NOT_MET, NOT_FOUND, DISABLED, ERROR }

    /**
     * Result of toggling a perk.
     */
    public enum ToggleResult { ENABLED, DISABLED, NOT_OWNED, NOT_TOGGLEABLE, NOT_FOUND, ERROR }

    /**
     * Result of activating a perk.
     */
    public record ActivationResult(@NotNull Status status, long cooldownRemaining, String error) {
        public enum Status { ACTIVATED, ON_COOLDOWN, NOT_OWNED, NOT_ACTIVATABLE, NOT_FOUND, ERROR }
        public static @NotNull ActivationResult activated() { return new ActivationResult(Status.ACTIVATED, 0L, null); }
        public static @NotNull ActivationResult onCooldown(long remaining) { return new ActivationResult(Status.ON_COOLDOWN, remaining, null); }
        public static @NotNull ActivationResult notOwned() { return new ActivationResult(Status.NOT_OWNED, 0L, null); }
        public static @NotNull ActivationResult notActivatable() { return new ActivationResult(Status.NOT_ACTIVATABLE, 0L, null); }
        public static @NotNull ActivationResult notFound() { return new ActivationResult(Status.NOT_FOUND, 0L, null); }
        public static @NotNull ActivationResult error(String msg) { return new ActivationResult(Status.ERROR, 0L, msg); }
    }
}
