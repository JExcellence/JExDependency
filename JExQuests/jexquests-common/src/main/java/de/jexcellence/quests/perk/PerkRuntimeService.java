package de.jexcellence.quests.perk;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PerkKind;
import de.jexcellence.quests.service.PerkService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Runtime bridge between a perk's stored {@code behaviourData} and the
 * live server — applies potion effects, grants flight, and exposes
 * predicates for damage / death / XP listeners.
 *
 * <p>Per-player owned-perk lists are cached ({@link #ownedCache})
 * and rebuilt on join + every 30 seconds; the in-tick scheduler
 * iterates that cache and re-applies potion effects before they
 * expire. {@link #refreshAsync(UUID)} is the escape hatch for when
 * an action (toggle / unlock / admin grant) changes the cached view
 * and callers need immediate consistency.
 *
 * <p>ACTIVE-kind perks don't sit in the periodic loop — they fire
 * once through {@link PerkRuntimeService} (wired from the service
 * layer's {@code PerkActivatedEvent}) which pushes a timed effect
 * and, when {@code multiplyXp > 0}, a window into
 * {@link #xpBoostEndsAt}.
 */
public final class PerkRuntimeService {

    private static final long CACHE_REFRESH_TICKS = 600L;   // 30 s
    private static final long EFFECT_TICK_INTERVAL = 100L;  // 5 s

    private final JExQuests quests;
    private final JExLogger logger;

    /** UUID → every owned-and-enabled perk resolved to its parsed behaviour. */
    private final Map<UUID, List<RuntimePerk>> ownedCache = new ConcurrentHashMap<>();

    /** UUID → epoch-millis when an active XP boost expires. */
    private final Map<UUID, Long> xpBoostEndsAt = new ConcurrentHashMap<>();
    private final Map<UUID, Double> xpBoostMultiplier = new ConcurrentHashMap<>();

    private BukkitTask refreshTask;
    private BukkitTask applyTask;

    public PerkRuntimeService(@NotNull JExQuests quests) {
        this.quests = quests;
        this.logger = quests.logger();
    }

    public void start() {
        this.refreshTask = Bukkit.getScheduler().runTaskTimer(
                this.quests.getPlugin(), this::refreshAllOnline, 40L, CACHE_REFRESH_TICKS);
        this.applyTask = Bukkit.getScheduler().runTaskTimer(
                this.quests.getPlugin(), this::applyAllTick, 60L, EFFECT_TICK_INTERVAL);
    }

    public void stop() {
        if (this.refreshTask != null) this.refreshTask.cancel();
        if (this.applyTask != null) this.applyTask.cancel();
    }

    /** Rebuild the cached view for one player. Call after toggle / unlock. */
    public void refreshAsync(@NotNull UUID playerUuid) {
        this.quests.perkService().ownedWithDefinitionsAsync(playerUuid).thenAccept(owned -> {
            final List<RuntimePerk> resolved = owned.stream()
                    .filter(op -> shouldApply(op))
                    .map(RuntimePerk::of)
                    .collect(Collectors.toList());
            this.ownedCache.put(playerUuid, Collections.unmodifiableList(resolved));
        }).exceptionally(ex -> {
            this.logger.warn("perk runtime refresh failed for {}: {}", playerUuid, ex.getMessage());
            return null;
        });
    }

    public void forget(@NotNull UUID playerUuid) {
        this.ownedCache.remove(playerUuid);
        this.xpBoostEndsAt.remove(playerUuid);
        this.xpBoostMultiplier.remove(playerUuid);
    }

    /** Returns {@code true} when the player owns a perk that nullifies {@code damageCause}. */
    public boolean cancelsDamage(@NotNull UUID playerUuid, @NotNull String damageCause) {
        final List<RuntimePerk> owned = this.ownedCache.get(playerUuid);
        if (owned == null) return false;
        for (final RuntimePerk rp : owned) {
            if (damageCause.equalsIgnoreCase(rp.behaviour().cancelDamageCause())) return true;
        }
        return false;
    }

    /** Returns {@code true} when the player has an active KEEP_INVENTORY toggle. */
    public boolean keepsInventory(@NotNull UUID playerUuid) {
        final List<RuntimePerk> owned = this.ownedCache.get(playerUuid);
        if (owned == null) return false;
        for (final RuntimePerk rp : owned) {
            if (rp.behaviour().isKeepInventory()) return true;
        }
        return false;
    }

    /** Current XP multiplier — 1.0 when no active boost. */
    public double xpMultiplier(@NotNull UUID playerUuid) {
        final Long endsAt = this.xpBoostEndsAt.get(playerUuid);
        if (endsAt == null || endsAt < System.currentTimeMillis()) {
            this.xpBoostEndsAt.remove(playerUuid);
            this.xpBoostMultiplier.remove(playerUuid);
            return 1.0;
        }
        return this.xpBoostMultiplier.getOrDefault(playerUuid, 1.0);
    }

    /** Called from {@code PerkActivatedEvent} — fires ACTIVE-kind effects. */
    public void onActivate(@NotNull UUID playerUuid, @NotNull String perkIdentifier) {
        this.quests.perkService().perks().findByIdentifierAsync(perkIdentifier).thenAccept(opt -> {
            if (opt.isEmpty()) return;
            final var perk = opt.get();
            final PerkBehaviour b = PerkBehaviourCodec.decode(perk.getBehaviourData());
            Bukkit.getScheduler().runTask(this.quests.getPlugin(), () -> {
                final Player player = Bukkit.getPlayer(playerUuid);
                if (player == null || !player.isOnline()) return;
                if (b.hasPotionEffect()) applyPotion(player, b);
                if (b.multiplyXp() > 0.0 && b.durationTicks() > 0) {
                    final long endsAt = System.currentTimeMillis()
                            + (b.durationTicks() * 50L); // ticks → ms
                    this.xpBoostEndsAt.put(playerUuid, endsAt);
                    this.xpBoostMultiplier.put(playerUuid, b.multiplyXp());
                }
            });
        }).exceptionally(ex -> {
            this.logger.warn("perk activation runtime failed for {}/{}: {}",
                    playerUuid, perkIdentifier, ex.getMessage());
            return null;
        });
    }

    private void refreshAllOnline() {
        for (final Player player : Bukkit.getOnlinePlayers()) refreshAsync(player.getUniqueId());
    }

    private void applyAllTick() {
        for (final Player player : Bukkit.getOnlinePlayers()) applyTickFor(player);
    }

    private void applyTickFor(@NotNull Player player) {
        final List<RuntimePerk> owned = this.ownedCache.get(player.getUniqueId());
        if (owned == null || owned.isEmpty()) return;
        boolean wantsFlight = false;
        for (final RuntimePerk rp : owned) {
            final PerkBehaviour b = rp.behaviour();
            if (b.hasPotionEffect()) applyPotion(player, b);
            if (b.isFlight()) wantsFlight = true;
        }
        // Creative players are always flying; only touch in survival / adventure.
        if (wantsFlight && !player.getAllowFlight()) player.setAllowFlight(true);
    }

    private static void applyPotion(@NotNull Player player, @NotNull PerkBehaviour b) {
        final PotionEffectType type = PotionEffectType.getByName(b.effect());
        if (type == null) return;
        player.addPotionEffect(new PotionEffect(
                type, Math.max(1, b.durationTicks()), Math.max(0, b.amplifier()),
                b.ambient(), b.particles(), b.particles()), true);
    }

    /**
     * Predicate: does this owned perk participate in the runtime loop
     * at all? PASSIVE always; TOGGLE only when enabled; ACTIVE never
     * (they're one-shots driven by {@link #onActivate(UUID, String)}).
     */
    private static boolean shouldApply(@NotNull PerkService.OwnedPerk op) {
        final PerkKind kind = op.definition().getKind();
        if (kind == PerkKind.PASSIVE) return true;
        if (kind == PerkKind.TOGGLE) return op.ownership().isEnabled();
        return false;
    }

    /** Cached pair of identifier + parsed behaviour. */
    private record RuntimePerk(@NotNull String identifier, @NotNull PerkBehaviour behaviour) {
        static @NotNull RuntimePerk of(@NotNull PerkService.OwnedPerk op) {
            return new RuntimePerk(
                    op.definition().getIdentifier(),
                    PerkBehaviourCodec.decode(op.definition().getBehaviourData()));
        }
    }
}
