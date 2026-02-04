package com.raindropcentral.rplatform.reward.config;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@FunctionalInterface
public interface RewardSectionAdapter<T> {

    @Nullable
    AbstractReward convert(@NotNull T section, @Nullable Map<String, Object> context);
}
