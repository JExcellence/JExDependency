package de.jexcellence.quests.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jexcellence.quests.api.QuestObjective;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Jackson bridge for {@link QuestObjective} JSON blobs stored on
 * {@link de.jexcellence.quests.database.entity.QuestTask}. Stateless
 * and thread-safe.
 */
public final class QuestObjectiveCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private QuestObjectiveCodec() {
    }

    public static @Nullable QuestObjective decode(@Nullable String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, QuestObjective.class);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("invalid objective JSON: " + ex.getOriginalMessage(), ex);
        }
    }

    public static @NotNull String encode(@NotNull QuestObjective objective) {
        try {
            return MAPPER.writeValueAsString(objective);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("objective encode failed: " + ex.getOriginalMessage(), ex);
        }
    }
}
