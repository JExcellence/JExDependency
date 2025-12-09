/*
package com.raindropcentral.rdq2.perk.runtime;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.config.perk.PerkSection;
import com.raindropcentral.rdq2.config.perk.PerkSettingsSection;
import com.raindropcentral.rdq2.database.entity.perk.EventTriggeredPerk;
import com.raindropcentral.rdq2.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq2.database.entity.perk.RPerk;
import com.raindropcentral.rdq2.database.entity.perk.YamlLoadedPerk;
import com.raindropcentral.rdq2.database.entity.perk.event.*;
import com.raindropcentral.rdq2.database.entity.perk.potion.*;
import com.raindropcentral.rdq2.perk.config.PerkConfig;
import com.raindropcentral.rdq2.perk.event.PerkEventBus;
import com.raindropcentral.rdq2.type.EPerkCategory;
import com.raindropcentral.rdq2.type.EPerkType;
import com.raindropcentral.rplatform.config.DurationSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


public class DefaultPerkRegistry extends PerkRegistry {

    private static final Logger LOGGER = CentralLogger.getLogger(DefaultPerkRegistry.class.getName());
    private static final int DEFAULT_PAGE_SIZE = 256;
    private static final long ERROR_THROTTLE_MILLIS = 5_000L;

    private static final Map<String, Class<? extends RPerk>> KNOWN_PERK_CLASSES = Map.ofEntries(
            Map.entry("speed", Speed.class),
            Map.entry("resistance", Resistance.class),
            Map.entry("glow", Glow.class),
            Map.entry("haste", Haste.class),
            Map.entry("fire_resistance", FireResistance.class),
            Map.entry("night_vision", NightVision.class),
            Map.entry("jump_boost", JumpBoost.class),
            Map.entry("saturation", Saturation.class),
            Map.entry("strength", Strength.class),
            Map.entry("double_experience", DoubleExperiencePerk.class),
            Map.entry("damage_reduction", DamageReductionPerk.class),
            Map.entry("death_protection", DeathProtectionPerk.class),
            Map.entry("prevent_death", DeathProtectionPerk.class),
            Map.entry("keep_experience", KeepExperiencePerk.class),
            Map.entry("keep_inventory", KeepInventoryPerk.class)
    );

    private final RDQ rdq;
    private final CooldownService cooldownService;
    private final PerkRuntimeStateService runtimeStateService;
    private final PerkAuditService auditService;
    private final PerkEventBus eventBus;
    private final Map<String, PerkRuntime> perkRuntimes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> logWindows = new ConcurrentHashMap<>();

    public DefaultPerkRegistry(@NotNull RDQ rdq, @NotNull PerkTypeRegistry typeRegistry, @NotNull CooldownService cooldownService, @NotNull PerkRuntimeStateService runtimeStateService, @NotNull PerkAuditService auditService, @NotNull PerkEventBus eventBus) {
        super(typeRegistry);
        this.rdq = rdq;
        this.cooldownService = cooldownService;
        this.runtimeStateService = runtimeStateService;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    @Nullable
    public PerkRuntime getPerkRuntime(@NotNull String id) {
        return perkRuntimes.get(id);
    }

    @NotNull
    public List<PerkRuntime> getAllPerkRuntimes() {
        return List.copyOf(perkRuntimes.values());
    }

    public @NotNull List<PerkRuntime> getPerkRuntimesByType(@NotNull EPerkType type) {
        return perkRuntimes.values().stream()
                .filter(runtime -> runtime.getType() == type)
                .toList();
    }

    public void registerPerkRuntime(@NotNull PerkRuntime runtime) {
        perkRuntimes.put(runtime.getId(), runtime);
    }

    public void unregisterPerkRuntime(@NotNull String id) {
        var runtime = perkRuntimes.remove(id);
        if (runtime instanceof SectionBackedPerkRuntime sectionRuntime) {
            sectionRuntime.cleanup();
        }
        runtimeStateService.clearPerk(id);
    }

    public @NotNull PerkRuntime buildPerkRuntime(@NotNull RPerk perk, @NotNull PerkSection config) {
        var identifier = perk.getIdentifier();
        unregister(identifier);
        var perkConfig = adaptPerkConfig(perk, config);
        register(perkConfig);
        var loadedPerk = Objects.requireNonNull(get(identifier), "Perk registration failed for " + identifier);
        var runtime = new SectionBackedPerkRuntime(perk, loadedPerk, config, cooldownService, runtimeStateService, auditService, this, eventBus);
        registerPerkRuntime(runtime);
        return runtime;
    }

    public void reloadFrom(@NotNull Map<String, RPerk> perks) {
        perkRuntimes.values().forEach(runtime -> {
            if (runtime instanceof SectionBackedPerkRuntime section) {
                section.cleanup();
            }
        });
        perkRuntimes.clear();
        runtimeStateService.clearAll();
        onReload();

        var totalLoaded = 0;
        for (var perk : perks.values()) {
            if (perk == null || !perk.isEnabled()) continue;
            
            try {
                var section = perk.getPerkSection();
                buildPerkRuntime(perk, section);
                totalLoaded++;
            } catch (Exception runtimeException) {
                LOGGER.log(Level.SEVERE, "Failed to build runtime for perk {0}", new Object[]{perk.getIdentifier()});
                LOGGER.log(Level.FINER, "Runtime build failure", runtimeException);
            }
        }
        LOGGER.log(Level.INFO, "Loaded {0} perk runtimes", totalLoaded);
    }

    private @NotNull EPerkType parsePerkType(@NotNull String perkTypeStr) {
        var normalized = perkTypeStr.trim().toUpperCase(Locale.ROOT);
        try {
            return EPerkType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Unknown perk type {0}, defaulting to TOGGLEABLE_PASSIVE", normalized);
            LOGGER.log(Level.FINER, "Perk type parsing failure", ex);
            return EPerkType.TOGGLEABLE_PASSIVE;
        }
    }

    private @NotNull RPerk instantiateConfiguredPerk(@NotNull String perkId, @NotNull PerkSection perkSection, @NotNull EPerkType fallbackType, @NotNull Map<String, Object> metadata) {
        var perkClass = resolvePerkClass(perkId, metadata);
        if (perkClass == null) {
            return new YamlLoadedPerk(perkId, perkSection, fallbackType);
        }

        try {
            RPerk perk;
            if (EventTriggeredPerk.class.isAssignableFrom(perkClass)) {
                var constructor = perkClass.getDeclaredConstructor(String.class, PerkSection.class, RDQ.class);
                constructor.setAccessible(true);
                perk = constructor.newInstance(perkId, perkSection, rdq);
            } else if (PotionEffectPerk.class.isAssignableFrom(perkClass)) {
                var constructor = perkClass.getDeclaredConstructor(PerkSection.class);
                constructor.setAccessible(true);
                perk = constructor.newInstance(perkSection);
            } else {
                var constructor = perkClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                perk = constructor.newInstance();
                perk.setIdentifier(perkId);
                perk.setPerkSection(perkSection);
                perk.setPerkType(fallbackType);
            }
            return perk;
        } catch (ReflectiveOperationException reflectiveException) {
            LOGGER.log(Level.WARNING, "Failed to instantiate perk class {0} for id {1}. Falling back to YAML-backed perk.", new Object[]{perkClass.getName(), perkId});
            LOGGER.log(Level.FINER, "Perk instantiation failure", reflectiveException);
            return new YamlLoadedPerk(perkId, perkSection, fallbackType);
        }
    }

    private @Nullable Class<? extends RPerk> resolvePerkClass(@NotNull String perkId, @NotNull Map<String, Object> metadata) {
        var candidates = new LinkedHashSet<String>();
        addCandidate(candidates, perkId);
        addCandidate(candidates, metadata.get("id"));
        addCandidate(candidates, metadata.get("identifier"));
        addCandidate(candidates, metadata.get("effectType"));

        return candidates.stream()
                .map(KNOWN_PERK_CLASSES::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void addCandidate(@NotNull Set<String> target, @Nullable Object rawCandidate) {
        var normalised = normaliseIdentifier(rawCandidate);
        if (normalised != null) {
            target.add(normalised);
        }
    }

    private @Nullable String normaliseIdentifier(@Nullable Object rawCandidate) {
        if (rawCandidate == null) return null;
        
        var value = rawCandidate.toString().trim();
        if (value.isEmpty()) return null;
        
        return value.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private void configurePerkFromSettings(
            @NotNull RPerk perk,
            @NotNull PerkSection perkSection,
            @NotNull PerkSettingsSection settings,
            @NotNull Map<String, Object> metadata
    ) {
        perk.setPerkSection(perkSection);
        perk.setEnabled(Boolean.TRUE.equals(settings.getEnabled()));
        perk.setPriority(settings.getPriority());
        perk.setMaxConcurrentUsers(settings.getMaxConcurrentUsers());

        final String displayNameKey = settings.getDisplayNameKey();
        if (displayNameKey != null && !"not_defined".equalsIgnoreCase(displayNameKey)) {
            perk.setDisplayNameKey(displayNameKey);
            metadata.putIfAbsent("displayNameKey", displayNameKey);
        }

        final String descriptionKey = settings.getDescriptionKey();
        if (descriptionKey != null && !"not_defined".equalsIgnoreCase(descriptionKey)) {
            perk.setDescriptionKey(descriptionKey);
            metadata.putIfAbsent("descriptionKey", descriptionKey);
        }

        final String categoryInput = Optional.ofNullable(metadata.get("category"))
                .map(Object::toString)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(null);

        EPerkCategory category = EPerkCategory.UTILITY;
        if (categoryInput != null) {
            try {
                category = EPerkCategory.valueOf(categoryInput.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid perk category ''{0}'' for perk ''{1}'', defaulting to UTILITY.", new Object[]{categoryInput, perk.getIdentifier()});
            }
        }
        metadata.put("category", category.getIdentifier());

        final Object requiredPermission = metadata.get("requiredPermission");
        if (requiredPermission instanceof String permission && !permission.isBlank()) {
            perk.setRequiredPermission(permission);
        }

        metadata.put("perkType", perk.getPerkType().name());

        if (perk instanceof PotionEffectPerk potionEffectPerk) {
            Optional.ofNullable(metadata.get("effectType"))
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty()).ifPresent(effectType -> potionEffectPerk.setPotionEffectTypeName(effectType.toUpperCase(Locale.ROOT)));
            metadata.put("effectType", potionEffectPerk.getPotionEffectTypeName());
        }
    }

    private @NotNull PerkConfig adaptPerkConfig(@NotNull RPerk perk, @NotNull PerkSection section) {
        final PerkSettingsSection settings = section.getPerkSettings();
        final Map<String, Object> metadata = new HashMap<>(settings.getMetadata());
        metadata.putIfAbsent("id", perk.getIdentifier());
        metadata.putIfAbsent("displayName", perk.getDisplayNameKey());
        metadata.putIfAbsent("description", perk.getDescriptionKey());
        metadata.put("perkType", perk.getPerkType().name());
        final Map<String, Object> sanitizedMetadata = new LinkedHashMap<>(sanitizeMetadata(metadata));

        final String categoryId = Optional.ofNullable(sanitizedMetadata.get("category"))
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

        final String iconMaterial = sanitizeMaterial(settings.getIcon().getMaterial());
        final int priority = perk.getPriority() == 0 ? settings.getPriority() : perk.getPriority();

        sanitizedMetadata.putIfAbsent("category", category.getIdentifier());
        sanitizedMetadata.putIfAbsent("defaultCooldownSeconds", defaultCooldown);

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
                Map.copyOf(sanitizedMetadata),
                List.of(),
                List.of(),
                permissionCooldowns,
                permissionAmplifiers
        );
    }

    void logThrottled(
            @NotNull String key,
            @NotNull Level level,
            @NotNull String message,
            @NotNull Object[] parameters,
            @Nullable Throwable cause
    ) {
        final long now = System.currentTimeMillis();
        final AtomicLong marker = logWindows.computeIfAbsent(key, ignored -> new AtomicLong(0L));
        long previous = marker.get();
        if (previous != 0L && (now - previous) < ERROR_THROTTLE_MILLIS) {
            return;
        }
        if (!marker.compareAndSet(previous, now)) {
            return;
        }
        final LogRecord record = new LogRecord(level, message);
        record.setLoggerName(LOGGER.getName());
        record.setParameters(parameters);
        record.setThrown(cause);
        LOGGER.log(record);
    }

    @NotNull
    Map<String, Object> sanitizeMetadata(@NotNull Map<String, Object> metadata) {
        return PerkMetadataSanitizer.sanitizeMetadata(metadata);
    }

    @NotNull
    Map<String, Long> sanitizeCooldownMap(@NotNull Map<String, Long> source) {
        return PerkMetadataSanitizer.sanitizeCooldownMap(source);
    }

    @NotNull
    Map<String, Long> sanitizeDurationMap(@NotNull Map<String, Long> source) {
        return PerkMetadataSanitizer.sanitizeDurationMap(source);
    }

    @NotNull
    Map<String, Integer> sanitizeAmplifierMap(@NotNull Map<String, Integer> source) {
        return PerkMetadataSanitizer.sanitizeAmplifierMap(source);
    }

    @NotNull
    String sanitizeMaterial(@Nullable String material) {
        return PerkMetadataSanitizer.sanitizeMaterial(material);
    }

    @NotNull
    Set<String> determineSupportedEvents(@NotNull Map<String, Object> metadata) {
        return PerkMetadataSanitizer.determineSupportedEvents(metadata);
    }

    @NotNull
    String normaliseEventKey(@Nullable String raw) {
        return PerkMetadataSanitizer.normaliseEventKey(raw);
    }

    private static final class SectionBackedPerkRuntime implements PerkRuntime {

        private static final long FAILURE_WINDOW_MILLIS = 5_000L;
        private static final int FAILURE_THRESHOLD = 3;
        private static final long SUSPEND_DURATION_MILLIS = 15_000L;

        private final RPerk perk;
        private final LoadedPerk loadedPerk;
        private final CooldownService cooldownService;
        private final PerkRuntimeStateService.PerkRuntimeState runtimeState;
        private final PerkAuditService auditService;
        private final DefaultPerkRegistry registry;
        private final boolean globallyEnabled;
        private final Integer maxConcurrentUsers;
        private final String requiredPermission;
        private final long defaultCooldownSeconds;
        private final long defaultDurationSeconds;
        private final Map<String, Long> permissionCooldowns;
        private final Map<String, DurationSection> permissionDurations;
        private final Map<String, Integer> permissionAmplifiers;
        private final int defaultAmplifier;
        private final Set<String> supportedEvents;
        private final PerkCircuitBreaker circuitBreaker;
        private final PerkEventBus eventBus;

        private SectionBackedPerkRuntime(
                @NotNull RPerk perk,
                @NotNull LoadedPerk loadedPerk,
                @NotNull PerkSection section,
                @NotNull CooldownService cooldownService,
                @NotNull PerkRuntimeStateService runtimeStateService,
                @NotNull PerkAuditService auditService,
                @NotNull DefaultPerkRegistry registry,
                @NotNull PerkEventBus eventBus
        ) {
            this.perk = perk;
            this.loadedPerk = loadedPerk;
            this.cooldownService = cooldownService;
            this.runtimeState = runtimeStateService.stateFor(loadedPerk.getId());
            this.auditService = auditService;
            this.registry = registry;
            this.eventBus = eventBus;
            this.globallyEnabled = perk.isEnabled();
            this.maxConcurrentUsers = Optional.ofNullable(perk.getMaxConcurrentUsers())
                    .filter(value -> value > 0)
                    .orElseGet(() -> Optional.ofNullable(section.getPerkSettings().getMaxConcurrentUsers())
                            .filter(value -> value > 0)
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
            this.permissionCooldowns = registry.sanitizeCooldownMap(section.getPermissionCooldowns().getPermissionCooldowns());
            this.permissionDurations = section.getPermissionDurations().getPermissionDurations();
            this.permissionAmplifiers = registry.sanitizeAmplifierMap(section.getPermissionAmplifiers().getPermissionAmplifiers());
            this.defaultAmplifier = Math.max(0, Optional.ofNullable(section.getPermissionAmplifiers().getDefaultAmplifier()).orElse(0));
            this.supportedEvents = registry.determineSupportedEvents(registry.sanitizeMetadata(section.getPerkSettings().getMetadata()));
            this.circuitBreaker = new PerkCircuitBreaker(
                    FAILURE_WINDOW_MILLIS,
                    FAILURE_THRESHOLD,
                    SUSPEND_DURATION_MILLIS
            );
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
            final UUID playerId = player.getUniqueId();
            if (!globallyEnabled) {
                return false;
            }
            if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
                return false;
            }
            if (isOnCooldown(player)) {
                return false;
            }
            if (runtimeState.isActive(playerId, () -> handleExpiry(player))) {
                return true;
            }
            if (isSuspended(playerId)) {
                return false;
            }
            if (maxConcurrentUsers == null) {
                return true;
            }
            return runtimeState.activeCount() < maxConcurrentUsers;
        }

        @Override
        public boolean activate(@NotNull Player player) {
            final UUID playerId = player.getUniqueId();
            if (isSuspended(playerId)) {
                final long remaining = emitSuspensionNotice("activation-suspended", playerId);
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("remainingSeconds", remaining);
                auditService.recordActivation(getId(), playerId, false, "suspended", context, null);
                return false;
            }
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
                clearFailureState(playerId);
                eventBus.fireActivated(player, getId());
                return true;
            } catch (Exception exception) {
                runtimeState.markInactive(playerId);
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("durationSeconds", durationSeconds);
                auditService.recordActivation(getId(), playerId, false, "exception", context, exception);
                registerFailure(playerId, "activation");
                registry.logThrottled(
                        "activation-exception",
                        Level.WARNING,
                        "Failed to activate perk {0} for player fingerprint {1}",
                        new Object[]{getId(), auditService.fingerprint(playerId)},
                        exception
                );
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
                    clearFailureState(playerId);
                    eventBus.fireDeactivated(player, getId());
                } else {
                    auditService.recordDeactivation(getId(), playerId, false, "runtime-rejected", null);
                }
                return success;
            } catch (Exception exception) {
                auditService.recordDeactivation(getId(), playerId, false, "exception", exception);
                registerFailure(playerId, "deactivation");
                registry.logThrottled(
                        "deactivation-exception",
                        Level.WARNING,
                        "Failed to deactivate perk {0} for player fingerprint {1}",
                        new Object[]{getId(), auditService.fingerprint(playerId)},
                        exception
                );
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
            if (isSuspended(playerId)) {
                final long remaining = emitSuspensionNotice("trigger-suspended", playerId);
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("remainingSeconds", remaining);
                auditService.recordTrigger(getId(), playerId, source, false, "suspended", context, null);
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
                clearFailureState(playerId);
                eventBus.fireTriggered(player, getId());
            } catch (Exception exception) {
                final Map<String, Object> context = new LinkedHashMap<>();
                context.put("durationSeconds", durationSeconds);
                auditService.recordTrigger(getId(), playerId, source, false, "exception", context, exception);
                registerFailure(playerId, "trigger");
                registry.logThrottled(
                        "trigger-exception",
                        Level.WARNING,
                        "Failed to trigger perk {0} for player fingerprint {1}",
                        new Object[]{getId(), auditService.fingerprint(playerId)},
                        exception
                );
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
            final long duration = Math.max(0L, seconds);
            if (duration <= 0L) {
                cooldownService.clearCooldown(player, getId());
                eventBus.fireCooldownEnd(player, getId());
                return;
            }
            cooldownService.setCooldown(player, getId(), duration);
            eventBus.fireCooldownStart(player, getId(), duration);
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
            final String normalised = registry.normaliseEventKey(event.getClass().getSimpleName());
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
            for (Map.Entry<String, DurationSection> entry : permissionDurations.entrySet()) {
                if (player.hasPermission(entry.getKey())) {
                    result = Math.max(result, entry.getValue().getSeconds());
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
                clearFailureState(playerId);
                eventBus.fireDeactivated(player, getId());
            } catch (Exception exception) {
                auditService.recordDeactivation(getId(), playerId, false, "expiry-exception", exception);
                registerFailure(playerId, "expiry");
                registry.logThrottled(
                        "expiry-exception",
                        Level.FINE,
                        "Failed to deactivate expired perk {0} for player fingerprint {1}",
                        new Object[]{getId(), auditService.fingerprint(playerId)},
                        exception
                );
                runtimeState.markInactive(playerId);
            }
        }

        private boolean isSuspended(@NotNull UUID playerId) {
            return circuitBreaker.isSuspended(playerId);
        }

        private long emitSuspensionNotice(@NotNull String channel, @NotNull UUID playerId) {
            final long remainingSeconds = getRemainingSuspensionSeconds(playerId);
            final String fingerprint = auditService.fingerprint(playerId);
            registry.logThrottled(
                    channel + '-' + fingerprint,
                    Level.FINE,
                    "Perk {0} suspended for player fingerprint {1} ({2}s remaining)",
                    new Object[]{getId(), fingerprint, remainingSeconds},
                    null
            );
            return remainingSeconds;
        }

        private long getRemainingSuspensionSeconds(@NotNull UUID playerId) {
            return circuitBreaker.getRemainingSuspensionSeconds(playerId);
        }

        private void registerFailure(@NotNull UUID playerId, @NotNull String channel) {
            final long suspendSeconds = circuitBreaker.registerFailure(playerId);
            if (suspendSeconds <= 0L) {
                return;
            }
            final Map<String, Object> context = new LinkedHashMap<>();
            context.put("channel", channel);
            context.put("suspendSeconds", suspendSeconds);
            auditService.recordSuppression(getId(), playerId, "auto-suspend", context);
            emitSuspensionNotice(channel + "-suspend", playerId);
        }

        private void clearFailureState(@NotNull UUID playerId) {
            circuitBreaker.clear(playerId);
        }

        void cleanup() {
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
                    circuitBreaker.clear(playerId);
                }
            }
            runtimeState.clearAll();
            circuitBreaker.clearAll();
        }
    }

}
*/
