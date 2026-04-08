/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.reward.validation;

import com.raindropcentral.rplatform.requirement.validation.ValidationResult;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.impl.ChoiceReward;
import com.raindropcentral.rplatform.reward.impl.CommandReward;
import com.raindropcentral.rplatform.reward.impl.CompositeReward;
import com.raindropcentral.rplatform.reward.impl.CurrencyReward;
import com.raindropcentral.rplatform.reward.impl.ExperienceReward;
import com.raindropcentral.rplatform.reward.impl.ItemReward;
import com.raindropcentral.rplatform.reward.impl.PermissionReward;
import com.raindropcentral.rplatform.reward.impl.VanishingChestReward;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the RewardValidators API type.
 */
public final class RewardValidators {

    private static final Map<Class<? extends AbstractReward>, RewardValidator<?>> VALIDATORS = new HashMap<>();

    static {
        registerValidator(ItemReward.class, reward -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            
            if (reward.getItem() == null || reward.getItem().getType().isAir()) {
                builder.addError("Item reward must have a valid item");
            }
            
            return builder.build();
        });

        registerValidator(CurrencyReward.class, reward -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            
            if (reward.getCurrencyId() == null || reward.getCurrencyId().isEmpty()) {
                builder.addError("Currency ID cannot be empty");
            }
            
            if (reward.getAmount() <= 0) {
                builder.addError("Currency amount must be positive");
            }
            
            return builder.build();
        });

        registerValidator(ExperienceReward.class, reward -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            
            if (reward.getAmount() <= 0) {
                builder.addError("Experience amount must be positive");
            }
            
            return builder.build();
        });

        registerValidator(CommandReward.class, reward -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            
            if (reward.getCommand() == null || reward.getCommand().trim().isEmpty()) {
                builder.addError("Command cannot be empty");
            }
            
            if (reward.getDelayTicks() < 0) {
                builder.addError("Delay ticks cannot be negative");
            }
            
            return builder.build();
        });

        registerValidator(CompositeReward.class, reward -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            
            if (reward.getRewards().isEmpty()) {
                builder.addError("Composite reward must have at least one reward");
            }
            
            for (AbstractReward subReward : reward.getRewards()) {
                ValidationResult subResult = validate(subReward);
                if (!subResult.valid()) {
                    builder.addError("Invalid sub-reward: " + subResult.getMessage());
                }
            }
            
            return builder.build();
        });

        registerValidator(ChoiceReward.class, reward -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            
            if (reward.getChoices().isEmpty()) {
                builder.addError("Choice reward must have at least one choice");
            }
            
            if (reward.getMinimumRequired() < 1) {
                builder.addError("Minimum required must be at least 1");
            }
            
            if (reward.getMinimumRequired() > reward.getChoices().size()) {
                builder.addError("Minimum required cannot exceed number of choices");
            }
            
            if (reward.getMaximumRequired() != null && 
                reward.getMaximumRequired() < reward.getMinimumRequired()) {
                builder.addError("Maximum required cannot be less than minimum required");
            }
            
            for (AbstractReward choice : reward.getChoices()) {
                ValidationResult choiceResult = validate(choice);
                if (!choiceResult.valid()) {
                    builder.addError("Invalid choice: " + choiceResult.getMessage());
                }
            }
            
            return builder.build();
        });

        registerValidator(PermissionReward.class, reward -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            
            if (reward.getPermissions().isEmpty()) {
                builder.addError("Permission reward must have at least one permission");
            }
            
            if (reward.isTemporary() && reward.getDurationSeconds() != null && 
                reward.getDurationSeconds() <= 0) {
                builder.addError("Duration must be positive for temporary permissions");
            }
            
            return builder.build();
        });

        registerValidator(VanishingChestReward.class, reward -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            
            if (reward.getItems().isEmpty()) {
                builder.addError("Vanishing chest must have at least one item");
            }
            
            if (reward.getDurationTicks() < 20) {
                builder.addError("Duration must be at least 20 ticks (1 second)");
            }
            
            for (org.bukkit.inventory.ItemStack item : reward.getItems()) {
                if (item == null || item.getType().isAir()) {
                    builder.addError("Vanishing chest cannot contain null or air items");
                    break;
                }
            }
            
            return builder.build();
        });
    }

    private RewardValidators() {}

    /**
     * Executes registerValidator.
     */
    public static <T extends AbstractReward> void registerValidator(
        @NotNull Class<T> rewardClass,
        @NotNull RewardValidator<T> validator
    ) {
        VALIDATORS.put(rewardClass, validator);
    }

    /**
     * Executes validate.
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractReward> ValidationResult validate(@NotNull T reward) {
        RewardValidator<T> validator = (RewardValidator<T>) VALIDATORS.get(reward.getClass());
        
        if (validator != null) {
            return validator.validate(reward);
        }
        
        return validateDefault(reward);
    }

    /**
     * Executes validateOrThrow.
     */
    public static <T extends AbstractReward> void validateOrThrow(@NotNull T reward) {
        ValidationResult result = validate(reward);
        if (!result.valid()) {
            throw new IllegalArgumentException("Reward validation failed: " + result.getMessage());
        }
    }

    private static ValidationResult validateDefault(@NotNull AbstractReward reward) {
        try {
            reward.validate();
            return ValidationResult.success();
        } catch (Exception e) {
            return ValidationResult.error(e.getMessage());
        }
    }
}
