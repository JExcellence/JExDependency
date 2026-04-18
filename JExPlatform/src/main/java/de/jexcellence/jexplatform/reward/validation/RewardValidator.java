package de.jexcellence.jexplatform.reward.validation;

import de.jexcellence.jexplatform.requirement.validation.ValidationResult;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for validating reward configurations.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@FunctionalInterface
public interface RewardValidator {

    /**
     * Validates the given reward.
     *
     * @param reward the reward to validate
     * @return the validation result
     */
    @NotNull ValidationResult validate(@NotNull AbstractReward reward);
}
