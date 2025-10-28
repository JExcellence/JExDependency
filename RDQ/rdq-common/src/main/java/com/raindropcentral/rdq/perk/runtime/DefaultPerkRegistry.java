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
 * @version 1.0.2
 * @since TBD
 */
public class DefaultPerkRegistry extends PerkRegistry {

    private static final Logger LOGGER = CentralLogger.getLogger(DefaultPerkRegistry.class.getName());
    private static final int DEFAULT_PAGE_SIZE = 256;

    private final RDQ rdq;
    private final CooldownService cooldownService;
    private final PerkRuntimeStateService runtimeStateService;
    private final Map<String, PerkRuntime> perkRuntimes = new ConcurrentHashMap<>();

    public DefaultPerkRegistry(
            @NotNull RDQ rdq,
            @NotNull PerkTypeRegistry typeRegistry,
            @NotNull CooldownService cooldownService,
            @NotNull PerkRuntimeStateService runtimeStateService
    ) {
        super(typeRegistry);
        this.rdq = rdq;
        this.cooldownService = cooldownService;
        this.runtimeStateService = runtimeStateService;
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
        final PerkRuntime runtime = new SectionBackedPerkRuntime(perk, loadedPerk, config, cooldownService, runtimeStateService);
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
                @NotNull PerkRuntimeStateService runtimeStateService
        ) {
            this.perk = perk;
            this.loadedPerk = loadedPerk;
            this.cooldownService = cooldownService;
            this.runtimeState = runtimeStateService.stateFor(loadedPerk.getId());
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
            final long durationSeconds = resolveDurationSeconds(player);
            final long expiryMillis = durationSeconds > 0 ? System.currentTimeMillis() + (durationSeconds * 1000) : 0L;
            if (!runtimeState.markActive(player.getUniqueId(), expiryMillis, maxConcurrentUsers)) {
                return false;
            }
            boolean activated = false;
            try {
                activated = loadedPerk.type().activate(player, loadedPerk);
                if (!activated) {
                    runtimeState.markInactive(player.getUniqueId());
                    return false;
                }
                applyCooldownIfNecessary(player);
                return true;
            } catch (Exception exception) {
                runtimeState.markInactive(player.getUniqueId());
                LOGGER.log(Level.WARNING, "Failed to activate perk {0} for player {1}", new Object[]{getId(), player.getUniqueId()});
                LOGGER.log(Level.FINER, "Activation failure", exception);
                return false;
            }
        }

        @Override
        public boolean deactivate(@NotNull Player player) {
            try {
                final boolean success = loadedPerk.type().deactivate(player, loadedPerk);
                if (success) {
                    runtimeState.markInactive(player.getUniqueId());
                }
                return success;
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to deactivate perk {0} for player {1}", new Object[]{getId(), player.getUniqueId()});
                LOGGER.log(Level.FINER, "Deactivation failure", exception);
                return false;
            }
        }

        @Override
        public void trigger(@NotNull Player player) {
            if (isOnCooldown(player)) {
                return;
            }
            final long durationSeconds = resolveDurationSeconds(player);
            final boolean trackDuration = durationSeconds > 0;
            final long expiryMillis = trackDuration ? System.currentTimeMillis() + (durationSeconds * 1000) : 0L;
            if (trackDuration && !runtimeState.markActive(player.getUniqueId(), expiryMillis, maxConcurrentUsers)) {
                return;
            }
            try {
                loadedPerk.type().trigger(player, loadedPerk);
                applyCooldownIfNecessary(player);
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to trigger perk {0} for player {1}", new Object[]{getId(), player.getUniqueId()});
                LOGGER.log(Level.FINER, "Trigger failure", exception);
                if (trackDuration) {
                    runtimeState.markInactive(player.getUniqueId());
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

        private void applyCooldownIfNecessary(@NotNull Player player) {
            if (!getType().hasCooldown()) {
                return;
            }
            final long cooldown = resolveCooldownSeconds(player);
            if (cooldown > 0) {
                setCooldown(player, cooldown);
            }
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
            try {
                loadedPerk.type().deactivate(player, loadedPerk);
            } catch (Exception exception) {
                LOGGER.log(Level.FINE, "Failed to deactivate expired perk {0} for player {1}", new Object[]{getId(), player.getUniqueId()});
                LOGGER.log(Level.FINER, "Expiry deactivation failure", exception);
            } finally {
                runtimeState.markInactive(player.getUniqueId());
                cooldownService.clearCooldown(player, getId());
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
                }
            }
            runtimeState.clearAll();
        }
    }
}
