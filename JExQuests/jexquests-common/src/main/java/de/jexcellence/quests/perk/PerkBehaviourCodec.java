package de.jexcellence.quests.perk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decodes a perk's free-form {@code behaviourData} JSON into a
 * strongly-typed {@link PerkBehaviour}. Unlike the sealed Reward /
 * Requirement hierarchies, behaviour fields are fully optional — the
 * codec tolerates missing keys and returns {@link PerkBehaviour#EMPTY}
 * for blank / unparseable input so a malformed YAML can't brick the
 * runtime listener.
 */
public final class PerkBehaviourCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PerkBehaviourCodec() {
    }

    public static @NotNull PerkBehaviour decode(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return PerkBehaviour.EMPTY;
        try {
            final JsonNode n = MAPPER.readTree(raw);
            return new PerkBehaviour(
                    textOrNull(n, "effect"),
                    intOr(n, "amplifier", 0),
                    intOr(n, "durationTicks", 0),
                    intOr(n, "refreshEveryTicks", 0),
                    boolOr(n, "ambient", false),
                    boolOr(n, "particles", false),
                    textOrNull(n, "specialType"),
                    textOrNull(n, "cancelDamageCause"),
                    doubleOr(n, "multiplyXp", 0.0)
            );
        } catch (final RuntimeException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            return PerkBehaviour.EMPTY;
        }
    }

    private static @Nullable String textOrNull(@NotNull JsonNode n, @NotNull String key) {
        final JsonNode v = n.get(key);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static int intOr(@NotNull JsonNode n, @NotNull String key, int fallback) {
        final JsonNode v = n.get(key);
        return (v == null || v.isNull()) ? fallback : v.asInt(fallback);
    }

    private static double doubleOr(@NotNull JsonNode n, @NotNull String key, double fallback) {
        final JsonNode v = n.get(key);
        return (v == null || v.isNull()) ? fallback : v.asDouble(fallback);
    }

    private static boolean boolOr(@NotNull JsonNode n, @NotNull String key, boolean fallback) {
        final JsonNode v = n.get(key);
        return (v == null || v.isNull()) ? fallback : v.asBoolean(fallback);
    }
}
