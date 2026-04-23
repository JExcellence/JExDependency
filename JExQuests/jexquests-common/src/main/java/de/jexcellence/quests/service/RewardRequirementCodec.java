package de.jexcellence.quests.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jexcellence.core.api.requirement.Requirement;
import de.jexcellence.core.api.reward.Reward;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Jackson-backed serdes helper for the JExCore shared reward and
 * requirement hierarchies. Stateless and thread-safe; the embedded
 * {@link ObjectMapper} is reused across calls.
 *
 * <p>Entities store {@code requirementData} / {@code rewardData} as
 * JSON strings; services use this helper to round-trip them into
 * concrete {@link Reward} / {@link Requirement} graphs.
 */
public final class RewardRequirementCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Reward>> REWARD_LIST = new TypeReference<>() {};
    private static final TypeReference<List<Requirement>> REQUIREMENT_LIST = new TypeReference<>() {};

    private RewardRequirementCodec() {
    }

    /**
     * Decodes a reward from JSON.
     */
    public static @Nullable Reward decodeReward(@Nullable String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, Reward.class);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("invalid reward JSON: " + ex.getOriginalMessage(), ex);
        }
    }

    /**
     * Decodes a list of rewards from JSON.
     */
    public static @NotNull List<Reward> decodeRewardList(@Nullable String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, REWARD_LIST);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("invalid reward-list JSON: " + ex.getOriginalMessage(), ex);
        }
    }

    /**
     * Encodes a reward as JSON.
     */
    public static @NotNull String encodeReward(@NotNull Reward reward) {
        try {
            return MAPPER.writeValueAsString(reward);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("reward encode failed: " + ex.getOriginalMessage(), ex);
        }
    }

    /**
     * Decodes a requirement from JSON.
     */
    public static @Nullable Requirement decodeRequirement(@Nullable String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, Requirement.class);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("invalid requirement JSON: " + ex.getOriginalMessage(), ex);
        }
    }

    /**
     * Decodes a list of requirements from JSON.
     */
    public static @NotNull List<Requirement> decodeRequirementList(@Nullable String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, REQUIREMENT_LIST);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("invalid requirement-list JSON: " + ex.getOriginalMessage(), ex);
        }
    }

    /**
     * Encodes a requirement as JSON.
     */
    public static @NotNull String encodeRequirement(@NotNull Requirement requirement) {
        try {
            return MAPPER.writeValueAsString(requirement);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("requirement encode failed: " + ex.getOriginalMessage(), ex);
        }
    }
}
