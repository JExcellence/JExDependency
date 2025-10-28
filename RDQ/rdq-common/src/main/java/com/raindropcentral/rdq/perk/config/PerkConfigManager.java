package com.raindropcentral.rdq.perk.config;

import com.raindropcentral.rdq.type.EPerkCategory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PerkConfigManager {

    private final PerkConfigLoader loader;
    private final Cache<String, PerkConfig> cache;
    private final Map<String, PerkConfig> configsByCategory;
    private final List<Runnable> reloadListeners;

    public PerkConfigManager() {
        this.loader = new PerkConfigLoader();
        this.cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
        this.configsByCategory = new ConcurrentHashMap<>();
        this.reloadListeners = new ArrayList<>();
    }

    public @NotNull CompletableFuture<PerkConfig> loadAsync(@NotNull Path filePath) {
        return loader.loadAsync(filePath)
            .thenApply(config -> {
                PerkConfigValidator.ValidationResult validation = PerkConfigValidator.validate(config);
                if (!validation.valid()) {
                    throw new IllegalArgumentException("Invalid perk config: " + validation.errors());
                }
                cache.put(config.id(), config);
                return config;
            });
    }

    public @NotNull PerkConfig load(@NotNull Path filePath) throws IOException {
        PerkConfig config = loader.load(filePath);
        PerkConfigValidator.ValidationResult validation = PerkConfigValidator.validate(config);
        if (!validation.valid()) {
            throw new IllegalArgumentException("Invalid perk config: " + validation.errors());
        }
        cache.put(config.id(), config);
        return config;
    }

    public @NotNull PerkConfig loadFromResource(@NotNull String resourcePath) throws IOException {
        PerkConfig config = loader.loadFromResource(resourcePath);
        PerkConfigValidator.ValidationResult validation = PerkConfigValidator.validate(config);
        if (!validation.valid()) {
            throw new IllegalArgumentException("Invalid perk config: " + validation.errors());
        }
        cache.put(config.id(), config);
        return config;
    }

    public @Nullable PerkConfig getConfig(@NotNull String perkId) {
        return cache.getIfPresent(perkId);
    }

    public @NotNull List<PerkConfig> getConfigsByCategory(@NotNull EPerkCategory category) {
        return cache.asMap().values().stream()
            .filter(config -> config.category() == category)
            .collect(Collectors.toList());
    }

    public @NotNull List<PerkConfig> getAllConfigs() {
        return new ArrayList<>(cache.asMap().values());
    }

    public void invalidateCache(@NotNull String perkId) {
        cache.invalidate(perkId);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public void registerReloadListener(@NotNull Runnable listener) {
        reloadListeners.add(listener);
    }

    public void onReload() {
        invalidateAll();
        for (Runnable listener : reloadListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public @NotNull CompletableFuture<Void> loadAllFromDirectory(@NotNull Path directory) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!Files.exists(directory)) {
                    return;
                }
                Files.list(directory)
                    .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                    .forEach(path -> {
                        try {
                            load(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
