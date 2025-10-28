package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSettingsSection;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Registry for managing {@link PerkRuntime} instances.
 *
 * @author JExcellence
 * @version 1.0.3
 * @since TBD
 */
public class DefaultPerkRegistry extends PerkRegistry {

    private static final Logger LOGGER = CentralLogger.getLogger(DefaultPerkRegistry.class.getName());
    private static final int DEFAULT_PAGE_SIZE = 256;

    private final RDQ rdq;
    private final CooldownService cooldownService;
    private final PerkRuntimeStateService runtimeStateService;
    private final PerkAuditService auditService;
    private final Map<String, PerkRuntime> perkRuntimes = new ConcurrentHashMap<>();

    public DefaultPerkRegistry(
            @NotNull RDQ rdq,
            @NotNull PerkTypeRegistry typeRegistry,
            @NotNull CooldownService cooldownService,
            @NotNull PerkRuntimeStateService runtimeStateService,
            @NotNull PerkAuditService auditService
    ) {
        super(typeRegistry);
        this.rdq = rdq;
        this.cooldownService = cooldownService;
        this.runtimeStateService = runtimeStateService;
        this.auditService = auditService;
    }

    @Nullable
    public PerkRuntime getPerkRuntime(@NotNull String id) {
        return perkRuntimes.get(id);
    }

    @NotNull
    public List<PerkRuntime> getAllPerkRuntimes() {
        return List.copyOf(perkRuntimes.values());
    }

    @NotNull
    public List<PerkRuntime> getPerkRuntimesByType(@NotNull EPerkType type) {
        return perkRuntimes.values().stream()
                .filter(runtime -> runtime.getType() == type)
                .collect(Collectors.toList());
    }

    public void registerPerkRuntime(@NotNull PerkRuntime runtime) {
        perkRuntimes.put(runtime.getId(), runtime);
    }

    public void unregisterPerkRuntime(@NotNull String id) {
        final PerkRuntime runtime = perkRuntimes.remove(id);
        if (runtime instanceof SectionBackedPerkRuntime sectionRuntime) {
            sectionRuntime.cleanup();
        }
        runtimeStateService.clearPerk(id);
    }

    @NotNull
    public PerkRuntime buildPerkRuntime(@NotNull RPerk perk, @NotNull PerkSection config) {
        final String identifier = perk.getIdentifier();
        unregister(identifier);
        final PerkConfig perkConfig = adaptPerkConfig(perk, config);
        register(perkConfig);
        final var loadedPerk = Objects.requireNonNull(get(identifier), "Perk registration failed for " + identifier);
        final PerkRuntime runtime = new SectionBackedPerkRuntime(perk, loadedPerk, config, cooldownService, runtimeStateService, auditService);
        registerPerkRuntime(runtime);
        return runtime;
    }

    public void reloadAllPerkRuntimes() {
        perkRuntimes.values().forEach(runtime -> {
            if (runtime instanceof SectionBackedPerkRuntime section) {
                section.cleanup();
            }
        });
        perkRuntimes.clear();
        runtimeStateService.clearAll();
        onReload();
        try {
            final var repository = rdq.getPerkRepository();
            int page = 0;
            List<RPerk> fetched;
            do {
                fetched = repository.findAll(page, DEFAULT_PAGE_SIZE);
                for (RPerk perk : fetched) {
                    if (!perk.isEnabled()) {
                        continue;
                    }
                    try {
                        buildPerkRuntime(perk, perk.getPerkSection());
                    } catch (Exception runtimeException) {
                        LOGGER.log(Level.SEVERE, "Failed to build runtime for perk {0}", new Object[]{perk.getIdentifier()});
                        LOGGER.log(Level.FINER, "Runtime build failure", runtimeException);
                    }
                }
                page++;
            } while (!fetched.isEmpty() && fetched.size() == DEFAULT_PAGE_SIZE);
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to reload perk runtimes", exception);
        }
    }

    private @NotNull PerkConfig adaptPerkConfig(@NotNull RPerk perk, @NotNull PerkSection section) {
        final PerkSettingsSection settings = section.getPerkSettings();
        final Map<String, Object> metadata = new HashMap<>(settings.getMetadata());
        metadata.putIfAbsent("id", perk.getIdentifier());
        metadata.putIfAbsent("displayName", perk.getDisplayNameKey());
        metadata.putIfAbsent("description", perk.getDescriptionKey());
        metadata.put("perkType", perk.getPerkType().name());

        final String categoryId = Optional.ofNullable(metadata.get("category"))
                .map(Object::toString)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(EPerkCategory.UTILITY.getIdentifier());
        final EPerkCategory category = Optional.ofNullable(EPerkCategory.fromIdentifier(categoryId))
                .orElse(EPerkCategory.UTILITY);

        final boolean enabled = perk.isEnabled() && Boolean.TRUE.equals(settings.getEnabled());
        final long defaultCooldown = Math.max(0L, Optional.ofNullable(section.getPermissionCooldowns().getDefaultCooldownSeconds())
                .orElse(0L));
        final Long durationSeconds = Optional.ofNullable(section.getPermissionDurations().getDefaultDurationSeconds())
                .filter(value -> value > 0)
                .orElse(null);

        final Map<String, Long> permissionCooldowns = sanitizeCooldownMap(section.getPermissionCooldowns().getPermissionCooldowns());
        final Map<String, Integer> permissionAmplifiers = sanitizeAmplifierMap(section.getPermissionAmplifiers().getPermissionAmplifiers());

        final String iconMaterial = settings.getIcon().getMaterial();
        final int priority = perk.getPriority() == 0 ? settings.getPriority() : perk.getPriority();

        metadata.putIfAbsent("category", category.getIdentifier());
        metadata.putIfAbsent("defaultCooldownSeconds", defaultCooldown);

        return new PerkConfig(
                perk.getIdentifier(),
                perk.getDisplayNameKey(),
                perk.getDescriptionKey(),
                perk.getPerkType(),
                category,
                iconMaterial,
                priority,
                enabled,
                defaultCooldown,
                durationSeconds,
                Map.copyOf(metadata),
                List.of(),
                List.of(),
                permissionCooldowns,
                permissionAmplifiers
        );
    }

    private Map<String, Long> sanitizeCooldownMap(Map<String, Long> source) {
        return source.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() >= 0)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue(), Math::min));
    }

    private Map<String, Integer> sanitizeAmplifierMap(Map<String, Integer> source) {
        return source.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Math.max(0, entry.getValue()), Math::max));
    }

    private static final class SectionBackedPerkRuntime implements PerkRuntime {

        private final RPerk perk;
        private final LoadedPerk loadedPerk;
        private final CooldownService cooldownService;
        private final PerkRuntimeStateService.PerkRuntimeState runtimeState;
        private final PerkAuditService auditService;
        private final boolean globallyEnabled;
        private final Integer maxConcurrentUsers;
        private final String requiredPermission;
        private final long defaultCooldownSeconds;
        private final long defaultDurationSeconds;
        private final Map<String, Long> permissionCooldowns;
        private final Map<String, Long> permissionDurations;
        private final Map<String, Integer> permissionAmplifiers;
        private final int defaultAmplifier;
        private final Set<String> supportedEvents;

        private SectionBackedPerkRuntime(
                @NotNull RPerk perk,
                @NotNull LoadedPerk loadedPerk,
                @NotNull PerkSection section,
                @NotNull CooldownService cooldownService,
                @NotNull PerkRuntimeStateService runtimeStateService,
                @NotNull PerkAuditService auditService
        ) {
            this.perk = perk;
            this.loadedPerk = loadedPerk;
            this.cooldownService = cooldownService;
            this.runtimeState = runtimeStateService.stateFor(loadedPerk.getId());
            this.auditService = auditService;
            this.globallyEnabled = perk.isEnabled();
            this.maxConcurrentUsers = Optional.ofNullable(perk.getMaxConcurrentUsers())
                    .filter(value -> value != null && value > 0)
                    .orElseGet(() -> Optional.ofNullable(section.getPerkSettings().getMaxConcurrentUsers())
                            .filter(value -> value != null && value > 0)
                            .orElse(null));
            this.requiredPermission = Optional.ofNullable(perk.getRequiredPermission())
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .orElseGet(() -> Optional.ofNullable(section.getPerkSettings().getMetadata().get("requiredPermission"))
                            .map(Object::toString)
                            .map(String::trim)
                            .filter(value -> !value.isEmpty())
                            .orElse(null));
            this.defaultCooldownSeconds = Math.max(0L, Optional.ofNullable(section.getPermissionCooldowns().getDefaultCooldownSeconds()).orElse(0L));
            this.defaultDurationSeconds = Math.max(0L, Optional.ofNullable(section.getPermissionDurations().getDefaultDurationSeconds()).orElse(0L));
            this.permissionCooldowns = Map.copyOf(section.getPermissionCooldowns().getPermissionCooldowns().entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() >= 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Math::min)));
            this.permissionDurations = Map.copyOf(section.getPermissionDurations().getPermissionDurationsSeconds().entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() >= 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Math::max)));
            this.permissionAmplifiers = Map.copyOf(section.getPermissionAmplifiers().getPermissionAmplifiers().entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> Math.max(0, entry.getValue()), Math::max)));
            this.defaultAmplifier = Optional.ofNullable(section.getPermissionAmplifiers().getDefaultAmplifier()).orElse(0);
            this.supportedEvents = determineSupportedEvents(section.getPerkSettings().getMetadata());
        }

        @Override
        public @NotNull String getId() {
            return loadedPerk.getId();
        }

        @Override
        public @NotNull EPerkType getType() {
            return loadedPerk.config().perkType();
        }

        @Override
        public boolean canActivate(@NotNull Player player) {
            if (!globallyEnabled) {
                return false;
            }
            if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
                return false;
            }
            if (isOnCooldown(player)) {
                return false;
            }
            if (maxConcurrentUsers == null) {
                return true;
            }
            if (runtimeState.isActive(player.getUniqueId(), () -> handleExpiry(player))) {
                return true;
            }
            return runtimeState.activeCount() < maxConcurrentUsers;
        }

        @Override
        public boolean activate(@NotNull Player player) {
            final UUID playerId = player.getUniqueId();
            final long durationSeconds = resolveDurationSeconds(player);
            final long expiryMillis = durationSeconds > 0 ? System.currentTimeMillis() + (durationSeconds * 1000L) : 0L;
            if (!runtimeState.markActive(playerId, expiryMillis, maxConcurrentUsers)) {
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("durationSeconds", durationSeconds);
                if (maxConcurrentUsers != null) {
                    context.put("maxConcurrentUsers", maxConcurrentUsers);
                }
                auditService.recordActivation(getId(), playerId, false, "concurrency-limit", context, null);
                return false;
            }
            boolean activated = false;
            try {
                activated = loadedPerk.type().activate(player, loadedPerk);
                if (!activated) {
                    runtimeState.markInactive(playerId);
                    final Map<String, Object> context = new LinkedHashMap<>();
                    context.put("durationSeconds", durationSeconds);
                    auditService.recordActivation(getId(), playerId, false, "runtime-rejected", context, null);
                    return false;
                }
                final long cooldownSeconds = applyCooldownIfNecessary(player);
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("durationSeconds", durationSeconds);
                context.put("cooldownSeconds", cooldownSeconds);
                auditService.recordActivation(getId(), playerId, true, "activated", context, null);
                return true;
            } catch (Exception exception) {
                runtimeState.markInactive(playerId);
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("durationSeconds", durationSeconds);
                auditService.recordActivation(getId(), playerId, false, "exception", context, exception);
                LOGGER.log(Level.WARNING, "Failed to activate perk {0} for player fingerprint {1}", new Object[]{getId(), auditService.fingerprint(playerId)});
                LOGGER.log(Level.FINER, "Activation failure", exception);
                return false;
            }
        }

        @Override
        public boolean deactivate(@NotNull Player player) {
            final UUID playerId = player.getUniqueId();
            try {
                final boolean success = loadedPerk.type().deactivate(player, loadedPerk);
                if (success) {
                    runtimeState.markInactive(playerId);
                    auditService.recordDeactivation(getId(), playerId, true, "deactivated", null);
                } else {
                    auditService.recordDeactivation(getId(), playerId, false, "runtime-rejected", null);
                }
                return success;
            } catch (Exception exception) {
                auditService.recordDeactivation(getId(), playerId, false, "exception", exception);
                LOGGER.log(Level.WARNING, "Failed to deactivate perk {0} for player fingerprint {1}", new Object[]{getId(), auditService.fingerprint(playerId)});
                LOGGER.log(Level.FINER, "Deactivation failure", exception);
                return false;
            }
        }

        @Override
        public void trigger(@NotNull Player player) {
            trigger(player, "runtime");
        }

        @Override
        public void trigger(@NotNull Player player, @NotNull String source) {
            final UUID playerId = player.getUniqueId();
            if (isOnCooldown(player)) {
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("remainingCooldownSeconds", getRemainingCooldown(player));
                auditService.recordTrigger(getId(), playerId, source, false, "cooldown-active", context, null);
                return;
            }
            final long durationSeconds = resolveDurationSeconds(player);
            final boolean trackDuration = durationSeconds > 0;
            final long expiryMillis = trackDuration ? System.currentTimeMillis() + (durationSeconds * 1000L) : 0L;
            if (trackDuration && !runtimeState.markActive(playerId, expiryMillis, maxConcurrentUsers)) {
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("durationSeconds", durationSeconds);
                if (maxConcurrentUsers != null) {
                    context.put("maxConcurrentUsers", maxConcurrentUsers);
                }
                auditService.recordTrigger(getId(), playerId, source, false, "concurrency-limit", context, null);
                return;
            }
            try {
                loadedPerk.type().trigger(player, loadedPerk);
                final long cooldownSeconds = applyCooldownIfNecessary(player);
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("durationSeconds", durationSeconds);
                context.put("cooldownSeconds", cooldownSeconds);
                auditService.recordTrigger(getId(), playerId, source, true, "triggered", context, null);
            } catch (Exception exception) {
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("durationSeconds", durationSeconds);
                auditService.recordTrigger(getId(), playerId, source, false, "exception", context, exception);
                LOGGER.log(Level.WARNING, "Failed to trigger perk {0} for player fingerprint {1}", new Object[]{getId(), auditService.fingerprint(playerId)});
                LOGGER.log(Level.FINER, "Trigger failure", exception);
                if (trackDuration) {
                    runtimeState.markInactive(playerId);
                }
            }
        }

        @Override
        public boolean isOnCooldown(@NotNull Player player) {
            return cooldownService.isOnCooldown(player, getId());
        }

        @Override
        public long getRemainingCooldown(@NotNull Player player) {
            return cooldownService.getRemainingCooldown(player, getId());
        }

        @Override
        public void setCooldown(@NotNull Player player, long seconds) {
            cooldownService.setCooldown(player, getId(), Math.max(0L, seconds));
        }

        @Override
        public boolean isActive(@NotNull Player player) {
            return runtimeState.isActive(player.getUniqueId(), () -> handleExpiry(player));
        }

        @Override
        public boolean supports(@NotNull Event event) {
            if (supportedEvents.isEmpty()) {
                return true;
            }
            final String normalised = normaliseEventKey(event.getClass().getSimpleName());
            return supportedEvents.contains(normalised);
        }

        private long applyCooldownIfNecessary(@NotNull Player player) {
            if (!getType().hasCooldown()) {
                return 0L;
            }
            final long cooldown = resolveCooldownSeconds(player);
            if (cooldown > 0) {
                setCooldown(player, cooldown);
            }
            return cooldown;
        }

        private long resolveCooldownSeconds(@NotNull Player player) {
            long result = defaultCooldownSeconds;
            for (Map.Entry<String, Long> entry : permissionCooldowns.entrySet()) {
                if (!player.hasPermission(entry.getKey())) {
                    continue;
                }
                final long candidate = entry.getValue();
                if (candidate == 0) {
                    return 0;
                }
                if (result == 0 || candidate < result) {
                    result = candidate;
                }
            }
            return Math.max(0L, result);
        }

        private long resolveDurationSeconds(@NotNull Player player) {
            long result = defaultDurationSeconds;
            for (Map.Entry<String, Long> entry : permissionDurations.entrySet()) {
                if (player.hasPermission(entry.getKey())) {
                    result = Math.max(result, entry.getValue());
                }
            }
            return Math.max(0L, result);
        }

        @SuppressWarnings("unused")
        private int resolveAmplifier(@NotNull Player player) {
            int result = defaultAmplifier;
            for (Map.Entry<String, Integer> entry : permissionAmplifiers.entrySet()) {
                if (player.hasPermission(entry.getKey())) {
                    result = Math.max(result, entry.getValue());
                }
            }
            return result;
        }

        private void handleExpiry(@NotNull Player player) {
            final UUID playerId = player.getUniqueId();
            try {
                loadedPerk.type().deactivate(player, loadedPerk);
                auditService.recordExpiry(getId(), playerId, "expired");
            } catch (Exception exception) {
                auditService.recordDeactivation(getId(), playerId, false, "expiry-exception", exception);
                LOGGER.log(Level.FINE, "Failed to deactivate expired perk {0} for player fingerprint {1}", new Object[]{getId(), auditService.fingerprint(playerId)});
                LOGGER.log(Level.FINER, "Expiry deactivation failure", exception);
            } finally {
                runtimeState.markInactive(playerId);
            }
        }

        private Set<String> determineSupportedEvents(Map<String, Object> metadata) {
            final Set<String> events = new HashSet<>();
            Optional.ofNullable(metadata.get("triggerEvent"))
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(SectionBackedPerkRuntime::normaliseEventKey)
                    .filter(value -> !value.isEmpty())
                    .ifPresent(events::add);
            Optional.ofNullable(metadata.get("triggerEvents"))
                    .filter(Collection.class::isInstance)
                    .map(Collection.class::cast)
                    .ifPresent(collection -> collection.forEach(item -> {
                        final String normalised = normaliseEventKey(String.valueOf(item));
                        if (!normalised.isEmpty()) {
                            events.add(normalised);
                        }
                    }));
            return Set.copyOf(events);
        }

        private static String normaliseEventKey(String raw) {
            final String trimmed = Optional.ofNullable(raw)
                    .map(String::trim)
                    .orElse("");
            if (trimmed.isEmpty()) {
                return "";
            }
            String upper = trimmed.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
            if (upper.endsWith("EVENT")) {
                upper = upper.substring(0, upper.length() - 5);
            }
            return upper;
        }

        private void cleanup() {
            final Set<UUID> toCleanup = new HashSet<>();
            runtimeState.forEachActive(toCleanup::add);
            for (UUID playerId : toCleanup) {
                final Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    handleExpiry(player);
                } else {
                    runtimeState.markInactive(playerId);
                    cooldownService.clearCooldown(playerId, getId());
                    auditService.recordCleanup(getId(), playerId, "offline-cleanup");
                }
            }
            runtimeState.clearAll();
        }
    }
}
