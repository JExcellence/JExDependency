package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.type.EPerkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of PerkRegistry.
 *
 * @author qodo
 * @version 1.0.0
 * @since TBD
 */
public class DefaultPerkRegistry extends PerkRegistry {

    private final RDQ rdq;
    private final Map<String, PerkRuntime> perkRuntimes = new ConcurrentHashMap<>();

    public DefaultPerkRegistry(@NotNull RDQ rdq) {
        super(rdq);
        this.rdq = rdq;
    }

    @Override
    @Nullable
    public PerkRuntime getPerkRuntime(@NotNull String id) {
        return perkRuntimes.get(id);
    }

    @Override
    @NotNull
    public List<PerkRuntime> getAllPerkRuntimes() {
        return List.copyOf(perkRuntimes.values());
    }

    @Override
    @NotNull
    public List<PerkRuntime> getPerkRuntimesByType(@NotNull EPerkType type) {
        return perkRuntimes.values().stream()
                .filter(runtime -> runtime.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public void registerPerkRuntime(@NotNull PerkRuntime runtime) {
        perkRuntimes.put(runtime.getId(), runtime);
    }

    @Override
    public void unregisterPerkRuntime(@NotNull String id) {
        perkRuntimes.remove(id);
    }

    @Override
    @NotNull
    public PerkRuntime buildPerkRuntime(@NotNull RPerk perk, @NotNull PerkSection config) {
        // TODO: Implement builder logic based on config
        // For now, return a placeholder
        return new PlaceholderPerkRuntime(perk.getIdentifier(), perk.getPerkType());
    }

    @Override
    public void reloadAllPerkRuntimes() {
        perkRuntimes.clear();
        // TODO: Load from database and build runtimes
    }

    // Placeholder implementation
    private static class PlaceholderPerkRuntime implements PerkRuntime {
        private final String id;
        private final EPerkType type;

        public PlaceholderPerkRuntime(String id, EPerkType type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public @NotNull String getId() {
            return id;
        }

        @Override
        public @NotNull EPerkType getType() {
            return type;
        }

        @Override
        public boolean canActivate(@NotNull org.bukkit.entity.Player player) {
            return false;
        }

        @Override
        public boolean activate(@NotNull org.bukkit.entity.Player player) {
            return false;
        }

        @Override
        public boolean deactivate(@NotNull org.bukkit.entity.Player player) {
            return false;
        }

        @Override
        public void trigger(@NotNull org.bukkit.entity.Player player) {
            // No-op
        }

        @Override
        public boolean isOnCooldown(@NotNull org.bukkit.entity.Player player) {
            return false;
        }

        @Override
        public long getRemainingCooldown(@NotNull org.bukkit.entity.Player player) {
            return 0;
        }

        @Override
        public void setCooldown(@NotNull org.bukkit.entity.Player player, long seconds) {
            // No-op
        }

        @Override
        public boolean isActive(@NotNull org.bukkit.entity.Player player) {
            return false;
        }
    }
}