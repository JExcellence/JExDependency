package com.raindropcentral.rplatform.reward.validation;

import com.raindropcentral.rplatform.requirement.validation.ValidationResult;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the RewardValidator API type.
 */
@FunctionalInterface
public interface RewardValidator<T extends AbstractReward> {

    /**
     * Executes validate.
     */
    @NotNull ValidationResult validate(@NotNull T reward);
}
