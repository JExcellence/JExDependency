package com.raindropcentral.rplatform.reward.validation;

import com.raindropcentral.rplatform.requirement.validation.ValidationResult;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface RewardValidator<T extends AbstractReward> {

    @NotNull ValidationResult validate(@NotNull T reward);
}
