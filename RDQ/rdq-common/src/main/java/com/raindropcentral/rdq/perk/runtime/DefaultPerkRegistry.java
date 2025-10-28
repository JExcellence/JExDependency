package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSettingsSection;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Registry for managing PerkRuntime instances.
 *
 * @author qodo
 * @version 1.0.1
 * @since TBD
 */
public class DefaultPerkRegistry extends PerkRegistry {

    private static final int DEFAULT_PAGE_SIZE = 256;

    private final RDQ rdq;
    private final Map<String, PerkRuntime> perkRuntimes = new ConcurrentHashMap<>();

    public DefaultPerkRegistry(@NotNull RDQ rdq, @NotNull PerkTypeRegistry typeRegistry) {
        super(typeRegistry);
        this.rdq = rdq;
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
        perkRuntimes.remove(id);
    }

    @NotNull
    public PerkRuntime buildPerkRuntime(@NotNull RPerk perk, @NotNull PerkSection config) {
        final String identifier = perk.getIdentifier();
        unregister(identifier);
        final PerkConfig perkConfig = adaptPerkConfig(perk, config);
        register(perkConfig);
        final var loadedPerk = Objects.requireNonNull(get(identifier), "Perk registration failed for " + identifier);
        final PerkRuntime runtime = new SectionBackedPerkRuntime(perk, loadedPerk, config);
        registerPerkRuntime(runtime);
        return runtime;
    }

    public void reloadAllPerkRuntimes() {
        perkRuntimes.clear();
        onReload();
        final var repository = rdq.getPerkRepository();
        int page = 0;
        List<RPerk> fetched;
        do {
            fetched = repository.findAll(page, DEFAULT_PAGE_SIZE);
            for (RPerk perk : fetched) {
                if (!perk.isEnabled()) {
                    continue;
                }
                buildPerkRuntime(perk, perk.getPerkSection());
            }
            page++;
        } while (!fetched.isEmpty() && fetched.size() == DEFAULT_PAGE_SIZE);
    }

    private @NotNull PerkConfig adaptPerkConfig(@NotNull RPerk perk, @NotNull PerkSection section) {
        final PerkSettingsSection settings = section.getPerkSettings();
        final Map<String, Object> metadata = new java.util.HashMap<>(settings.getMetadata());
        metadata.putIfAbsent("id", perk.getIdentifier());
        metadata.putIfAbsent("displayName", perk.getDisplayNameKey());
        metadata.putIfAbsent("description", perk.getDescriptionKey());
        metadata.put("perkType", perk.getPerkType().name());

        final String categoryId = Optional.ofNullable(metadata.get("category"))
                .map(Object::toString)
                .orElse(EPerkCategory.UTILITY.getIdentifier());
        final EPerkCategory category = Optional.ofNullable(EPerkCategory.fromIdentifier(categoryId))
                .orElse(EPerkCategory.UTILITY);

        final boolean enabled = perk.isEnabled() && Boolean.TRUE.equals(settings.getEnabled());
        final long defaultCooldown = Optional.ofNullable(section.getPermissionCooldowns().getDefaultCooldownSeconds())
                .orElse(0L);
        final Long durationSeconds = Optional.ofNullable(section.getPermissionDurations().getDefaultDurationSeconds())
                .filter(value -> value > 0)
                .orElse(null);

        final Map<String, Long> permissionCooldowns = new ConcurrentHashMap<>(section.getPermissionCooldowns().getPermissionCooldowns());
        final Map<String, Integer> permissionAmplifiers = new ConcurrentHashMap<>(section.getPermissionAmplifiers().getPermissionAmplifiers());

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
                Map.copyOf(permissionCooldowns),
                Map.copyOf(permissionAmplifiers)
        );
    }

    private static final class SectionBackedPerkRuntime implements PerkRuntime {

        private final RPerk perk;
        private final LoadedPerk loadedPerk;
        private final boolean globallyEnabled;
        private final Integer maxConcurrentUsers;
        private final String requiredPermission;
        private final long defaultCooldownSeconds;
        private final long defaultDurationSeconds;
        private final Map<String, Long> permissionCooldowns;
        private final Map<String, Long> permissionDurations;
        private final Map<String, Integer> permissionAmplifiers;
        private final int defaultAmplifier;
        private final ConcurrentMap<java.util.UUID, Boolean> activeStates = new ConcurrentHashMap<>();
        private final ConcurrentMap<java.util.UUID, Long> activeUntil = new ConcurrentHashMap<>();
        private final ConcurrentMap<java.util.UUID, Long> cooldowns = new ConcurrentHashMap<>();

        private SectionBackedPerkRuntime(@NotNull RPerk perk, @NotNull LoadedPerk loadedPerk, @NotNull PerkSection section) {
            this.perk = perk;
            this.loadedPerk = loadedPerk;
            this.globallyEnabled = perk.isEnabled();
            this.maxConcurrentUsers = Optional.ofNullable(perk.getMaxConcurrentUsers())
                    .orElse(section.getPerkSettings().getMaxConcurrentUsers());
            this.requiredPermission = Optional.ofNullable(perk.getRequiredPermission())
                    .orElseGet(() -> Optional.ofNullable(section.getPerkSettings().getMetadata().get("requiredPermission"))
                            .map(Object::toString)
                            .orElse(null));
            this.defaultCooldownSeconds = Optional.ofNullable(section.getPermissionCooldowns().getDefaultCooldownSeconds())
                    .orElse(0L);
            this.defaultDurationSeconds = Optional.ofNullable(section.getPermissionDurations().getDefaultDurationSeconds())
                    .orElse(0L);
            this.permissionCooldowns = new ConcurrentHashMap<>(section.getPermissionCooldowns().getPermissionCooldowns());
            this.permissionDurations = new ConcurrentHashMap<>(section.getPermissionDurations().getPermissionDurationsSeconds());
            this.permissionAmplifiers = new ConcurrentHashMap<>(section.getPermissionAmplifiers().getPermissionAmplifiers());
            this.defaultAmplifier = Optional.ofNullable(section.getPermissionAmplifiers().getDefaultAmplifier()).orElse(0);
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
        public boolean canActivate(@NotNull org.bukkit.entity.Player player) {
            if (!globallyEnabled) {
                return false;
            }
            if (requiredPermission != null && !requiredPermission.isBlank() && !player.hasPermission(requiredPermission)) {
                return false;
            }
            if (isOnCooldown(player)) {
                return false;
            }
            if (maxConcurrentUsers == null) {
                return true;
            }
            final long activeCount = activeStates.values().stream().filter(Boolean::booleanValue).count();
            return activeCount < maxConcurrentUsers;
        }

        @Override
        public boolean activate(@NotNull org.bukkit.entity.Player player) {
            if (!canActivate(player)) {
                return false;
            }
            final boolean activated = loadedPerk.type().activate(player, loadedPerk);
            if (!activated) {
                return false;
            }
            activeStates.put(player.getUniqueId(), true);
            final long duration = resolveDurationSeconds(player);
            if (duration > 0) {
                activeUntil.put(player.getUniqueId(), System.currentTimeMillis() + (duration * 1000));
            } else {
                activeUntil.remove(player.getUniqueId());
            }
            final long cooldown = resolveCooldownSeconds(player);
            if (cooldown > 0 && getType().hasCooldown()) {
                setCooldown(player, cooldown);
            }
            return true;
        }

        @Override
        public boolean deactivate(@NotNull org.bukkit.entity.Player player) {
            final boolean success = loadedPerk.type().deactivate(player, loadedPerk);
            if (success) {
                activeStates.put(player.getUniqueId(), false);
                activeUntil.remove(player.getUniqueId());
            }
            return success;
        }

        @Override
        public void trigger(@NotNull org.bukkit.entity.Player player) {
            if (isOnCooldown(player)) {
                return;
            }
            loadedPerk.type().trigger(player, loadedPerk);
            final long duration = resolveDurationSeconds(player);
            if (duration > 0) {
                activeStates.put(player.getUniqueId(), true);
                activeUntil.put(player.getUniqueId(), System.currentTimeMillis() + (duration * 1000));
            }
            final long cooldown = resolveCooldownSeconds(player);
            if (cooldown > 0) {
                setCooldown(player, cooldown);
            }
        }

        @Override
        public boolean isOnCooldown(@NotNull org.bukkit.entity.Player player) {
            final Long expiry = cooldowns.get(player.getUniqueId());
            if (expiry == null) {
                return false;
            }
            if (expiry <= System.currentTimeMillis()) {
                cooldowns.remove(player.getUniqueId());
                return false;
            }
            return true;
        }

        @Override
        public long getRemainingCooldown(@NotNull org.bukkit.entity.Player player) {
            final Long expiry = cooldowns.get(player.getUniqueId());
            if (expiry == null) {
                return 0L;
            }
            final long remaining = expiry - System.currentTimeMillis();
            if (remaining <= 0) {
                cooldowns.remove(player.getUniqueId());
                return 0L;
            }
            return remaining / 1000;
        }

        @Override
        public void setCooldown(@NotNull org.bukkit.entity.Player player, long seconds) {
            final long expiry = System.currentTimeMillis() + (seconds * 1000);
            cooldowns.put(player.getUniqueId(), expiry);
        }

        @Override
        public boolean isActive(@NotNull org.bukkit.entity.Player player) {
            final Boolean active = activeStates.get(player.getUniqueId());
            if (Boolean.FALSE.equals(active)) {
                return false;
            }
            final Long expiry = activeUntil.get(player.getUniqueId());
            if (expiry != null && expiry <= System.currentTimeMillis()) {
                loadedPerk.type().deactivate(player, loadedPerk);
                activeStates.put(player.getUniqueId(), false);
                activeUntil.remove(player.getUniqueId());
                return false;
            }
            return active != null && active;
        }

        private long resolveCooldownSeconds(@NotNull org.bukkit.entity.Player player) {
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
            return result;
        }

        private long resolveDurationSeconds(@NotNull org.bukkit.entity.Player player) {
            long result = defaultDurationSeconds;
            for (Map.Entry<String, Long> entry : permissionDurations.entrySet()) {
                if (player.hasPermission(entry.getKey())) {
                    result = Math.max(result, entry.getValue());
                }
            }
            return result;
        }

        @SuppressWarnings("unused")
        private int resolveAmplifier(@NotNull org.bukkit.entity.Player player) {
            int result = defaultAmplifier;
            for (Map.Entry<String, Integer> entry : permissionAmplifiers.entrySet()) {
                if (player.hasPermission(entry.getKey())) {
                    result = Math.max(result, entry.getValue());
                }
            }
            return result;
        }
    }
}