package de.jexcellence.quests.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Shared helpers for YAML-driven content loaders. Stateless —
 * {@link #MAPPER} is reused across loaders to serialise nested
 * {@code requirement:} / {@code reward:} sections into JSON blobs
 * that match JExCore's sealed-interface polymorphic deserialisation.
 */
public final class ContentLoaderSupport {

    /** Shared ObjectMapper. Matches JExCore's {@code RewardRequirementCodec}. */
    static final ObjectMapper MAPPER = new ObjectMapper();

    private ContentLoaderSupport() {
    }

    /**
     * Converts a Bukkit {@link ConfigurationSection} (deep YAML map)
     * into a JSON string preserving the section's type discriminators.
     * Returns {@code null} when the section itself is {@code null}.
     */
    public static @Nullable String sectionToJson(@Nullable ConfigurationSection section) {
        if (section == null) return null;
        try {
            return MAPPER.writeValueAsString(section.getValues(true));
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialise config section: " + ex.getOriginalMessage(), ex);
        }
    }

    /**
     * Serialise a raw Map (typically a nested value pulled out of a
     * {@code List<Map>} via {@link org.bukkit.configuration.file.YamlConfiguration#getList})
     * directly to JSON. Necessary because SnakeYAML returns nested
     * {@code LinkedHashMap}s for block scalars inside list entries —
     * feeding those through Bukkit's {@code YamlConfiguration.set} /
     * {@code getConfigurationSection} round-trip collapses them into
     * opaque Map values that aren't navigable as sections, so
     * {@link #sectionToJson} returns {@code null}. This helper takes
     * the Map directly.
     *
     * <p>Returns {@code null} when {@code raw} is null, not a map, or
     * empty so callers can keep the same "absent → null JSON" contract
     * they had with {@link #sectionToJson}.
     */
    public static @Nullable String mapToJson(@Nullable Object raw) {
        if (!(raw instanceof java.util.Map<?, ?> map) || map.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(map);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialise map: " + ex.getOriginalMessage(), ex);
        }
    }

    /** Walks a directory tree (recursively) for all {@code *.yml} files. */
    public static @NotNull List<Path> yamlFiles(@NotNull File root) {
        if (!root.exists() || !root.isDirectory()) return List.of();
        try (final Stream<Path> stream = Files.walk(root.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml"))
                    .sorted()
                    .toList();
        } catch (final java.io.IOException ex) {
            throw new IllegalStateException("failed to walk " + root + ": " + ex.getMessage(), ex);
        }
    }
}
