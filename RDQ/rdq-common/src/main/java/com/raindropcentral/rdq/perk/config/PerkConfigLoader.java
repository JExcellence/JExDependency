package com.raindropcentral.rdq.perk.config;

import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PerkConfigLoader {

    private final Yaml yaml;

    public PerkConfigLoader() {
        this.yaml = new Yaml();
    }

    public @NotNull CompletableFuture<PerkConfig> loadAsync(@NotNull Path filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return load(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load perk config from " + filePath, e);
            }
        });
    }

    public @NotNull PerkConfig load(@NotNull Path filePath) throws IOException {
        try (InputStream input = Files.newInputStream(filePath)) {
            Map<String, Object> data = yaml.load(input);
            return parseConfig(data, filePath.getFileName().toString());
        }
    }

    public @NotNull PerkConfig loadFromResource(@NotNull String resourcePath) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Map<String, Object> data = yaml.load(input);
            return parseConfig(data, resourcePath);
        }
    }

    @SuppressWarnings("unchecked")
    private @NotNull PerkConfig parseConfig(@NotNull Map<String, Object> data, @NotNull String source) {
        Map<String, Object> perkSettings = (Map<String, Object>) data.getOrDefault("perkSettings", new HashMap<>());
        Map<String, Object> metadata = (Map<String, Object>) perkSettings.getOrDefault("metadata", new HashMap<>());

        String id = extractId(source, metadata);
        String displayName = (String) metadata.getOrDefault("displayName", id);
        String description = (String) metadata.get("description");
        String perkTypeStr = (String) metadata.getOrDefault("perkType", "TOGGLEABLE_PASSIVE");
        String categoryStr = (String) metadata.getOrDefault("category", "UTILITY");
        Map<String, Object> icon = (Map<String, Object>) perkSettings.getOrDefault("icon", new HashMap<>());
        String iconMaterial = "PAPER";
        if (icon instanceof Map<String, Object>) {
            iconMaterial = icon.getOrDefault("material", "PAPER").toString();
        }
        int priority = ((Number) perkSettings.getOrDefault("priority", 100)).intValue();
        boolean enabled = (boolean) perkSettings.getOrDefault("isEnabled", true);

        EPerkType perkType = EPerkType.valueOf(perkTypeStr);
        EPerkCategory category = EPerkCategory.fromIdentifier(categoryStr);
        if (category == null) {
            category = EPerkCategory.UTILITY;
        }

        Long cooldownSeconds = null;
        Long durationSeconds = null;

        Map<String, Long> permissionCooldowns = extractPermissionCooldowns(data);
        Map<String, Integer> permissionAmplifiers = extractPermissionAmplifiers(data);
        List<PerkRequirementConfig> requirements = extractRequirements(data);
        List<PerkRewardConfig> rewards = extractRewards(data);

        return new PerkConfig(
            id, displayName, description, perkType, category, iconMaterial,
            priority, enabled, cooldownSeconds, durationSeconds, metadata,
            requirements, rewards, permissionCooldowns, permissionAmplifiers
        );
    }

    @SuppressWarnings("unchecked")
    private @NotNull Map<String, Long> extractPermissionCooldowns(@NotNull Map<String, Object> data) {
        Map<String, Long> result = new HashMap<>();
        Map<String, Object> cooldowns = (Map<String, Object>) data.get("permissionCooldowns");
        if (cooldowns != null) {
            Map<String, Object> perms = (Map<String, Object>) cooldowns.get("permissionCooldowns");
            if (perms != null) {
                for (Map.Entry<String, Object> entry : perms.entrySet()) {
                    result.put(entry.getKey(), ((Number) entry.getValue()).longValue());
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private @NotNull Map<String, Integer> extractPermissionAmplifiers(@NotNull Map<String, Object> data) {
        Map<String, Integer> result = new HashMap<>();
        Map<String, Object> amplifiers = (Map<String, Object>) data.get("permissionAmplifiers");
        if (amplifiers != null) {
            Map<String, Object> perms = (Map<String, Object>) amplifiers.get("permissionAmplifiers");
            if (perms != null) {
                for (Map.Entry<String, Object> entry : perms.entrySet()) {
                    result.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private @NotNull List<PerkRequirementConfig> extractRequirements(@NotNull Map<String, Object> data) {
        List<PerkRequirementConfig> result = new ArrayList<>();
        Map<String, Object> requirements = (Map<String, Object>) data.get("requirements");
        if (requirements != null) {
            for (Map.Entry<String, Object> entry : requirements.entrySet()) {
                Map<String, Object> req = (Map<String, Object>) entry.getValue();
                String type = (String) req.get("type");
                if ("LEVEL".equals(type)) {
                    int level = ((Number) req.get("level")).intValue();
                    result.add(new PerkRequirementConfig.LevelRequirementConfig("LEVEL", level));
                } else if ("PERMISSION".equals(type)) {
                    String permission = (String) req.get("permission");
                    result.add(new PerkRequirementConfig.PermissionRequirementConfig("PERMISSION", permission));
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private @NotNull List<PerkRewardConfig> extractRewards(@NotNull Map<String, Object> data) {
        List<PerkRewardConfig> result = new ArrayList<>();
        Map<String, Object> rewards = (Map<String, Object>) data.get("rewards");
        if (rewards != null) {
            for (Map.Entry<String, Object> entry : rewards.entrySet()) {
                Map<String, Object> reward = (Map<String, Object>) entry.getValue();
                String type = (String) reward.get("type");
                if ("MESSAGE".equals(type)) {
                    String message = (String) reward.get("message");
                    result.add(new PerkRewardConfig.MessageRewardConfig("MESSAGE", message));
                } else if ("COMMAND".equals(type)) {
                    String command = (String) reward.get("command");
                    result.add(new PerkRewardConfig.CommandRewardConfig("COMMAND", command));
                }
            }
        }
        return result;
    }

    private @NotNull String extractId(@NotNull String source, @NotNull Map<String, Object> metadata) {
        Object idObj = metadata.get("id");
        if (idObj instanceof String) {
            return (String) idObj;
        }
        return source.replace(".yml", "").replace(".yaml", "");
    }
}
