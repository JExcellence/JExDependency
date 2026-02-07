package com.raindropcentral.rdq.reward;

import com.raindropcentral.rdq.config.utility.RewardSection;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.config.RewardSectionAdapter;
import com.raindropcentral.rplatform.reward.impl.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class RDQRewardSectionAdapter implements RewardSectionAdapter<RewardSection> {

    private static final Logger LOGGER = Logger.getLogger(RDQRewardSectionAdapter.class.getName());

    @Override
    public @Nullable AbstractReward convert(@NotNull RewardSection section, @Nullable Map<String, Object> context) {
        String type = section.getType();
        if (type == null) {
            LOGGER.warning("Reward type not specified");
            return null;
        }

        try {
            return switch (type.toUpperCase()) {
                case "ITEM" -> convertItemReward(section);
                case "CURRENCY" -> convertCurrencyReward(section);
                case "EXPERIENCE" -> convertExperienceReward(section);
                case "COMMAND" -> convertCommandReward(section);
                case "COMPOSITE" -> convertCompositeReward(section);
                case "CHOICE" -> convertChoiceReward(section);
                case "PERMISSION" -> convertPermissionReward(section);
                default -> {
                    LOGGER.warning("Unknown reward type: " + type);
                    yield null;
                }
            };
        } catch (Exception e) {
            LOGGER.warning("Failed to convert reward: " + e.getMessage());
            return null;
        }
    }

    private ItemReward convertItemReward(RewardSection section) {
        if (section.getItem() == null) {
            throw new IllegalArgumentException("Item reward requires 'item' field");
        }
        
        // Convert Map to ItemStack
        Map<String, Object> itemMap = section.getItem();
        org.bukkit.inventory.ItemStack itemStack = convertMapToItemStack(itemMap);
        
        return new ItemReward(itemStack);
    }
    
    private org.bukkit.inventory.ItemStack convertMapToItemStack(Map<String, Object> itemMap) {
        // Get material
        Object materialObj = itemMap.get("material");
        if (materialObj == null) {
            throw new IllegalArgumentException("Item requires 'material' field");
        }
        
        String materialName = materialObj.toString();
        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid material: " + materialName);
        }
        
        // Get amount (default to 1)
        int amount = 1;
        Object amountObj = itemMap.get("amount");
        if (amountObj != null) {
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).intValue();
            } else {
                amount = Integer.parseInt(amountObj.toString());
            }
        }
        
        // Create ItemStack
        return new org.bukkit.inventory.ItemStack(material, amount);
    }

    private CurrencyReward convertCurrencyReward(RewardSection section) {
        String currencyId = section.getCurrencyId() != null ? section.getCurrencyId() : "vault";
        double amount = section.getAmount() != null ? section.getAmount() : 0.0;
        return new CurrencyReward(currencyId, amount);
    }

    private ExperienceReward convertExperienceReward(RewardSection section) {
        int amount = section.getExperienceAmount() != null ? section.getExperienceAmount() : 0;
        String typeStr = section.getExperienceType() != null ? section.getExperienceType() : "POINTS";
        ExperienceReward.ExperienceType type = ExperienceReward.ExperienceType.valueOf(typeStr.toUpperCase());
        return new ExperienceReward(amount, type);
    }

    private CommandReward convertCommandReward(RewardSection section) {
        if (section.getCommand() == null) {
            throw new IllegalArgumentException("Command reward requires 'command' field");
        }
        boolean executeAsPlayer = section.getExecuteAsPlayer() != null && section.getExecuteAsPlayer();
        long delayTicks = section.getDelayTicks() != null ? section.getDelayTicks() : 0L;
        return new CommandReward(section.getCommand(), executeAsPlayer, delayTicks);
    }

    private CompositeReward convertCompositeReward(RewardSection section) {
        if (section.getRewards() == null || section.getRewards().isEmpty()) {
            throw new IllegalArgumentException("Composite reward requires 'rewards' list");
        }

        List<AbstractReward> rewards = new ArrayList<>();
        for (var rewardSection : section.getRewards()) {
            AbstractReward reward = convert(rewardSection, null);
            if (reward != null) {
                rewards.add(reward);
            }
        }

        boolean continueOnError = section.getContinueOnError() != null && section.getContinueOnError();
        return new CompositeReward(rewards, continueOnError);
    }

    private ChoiceReward convertChoiceReward(RewardSection section) {
        if (section.getChoices() == null || section.getChoices().isEmpty()) {
            throw new IllegalArgumentException("Choice reward requires 'choices' list");
        }

        List<AbstractReward> choices = new ArrayList<>();
        for (var choiceSection : section.getChoices()) {
            AbstractReward choice = convert(choiceSection, null);
            if (choice != null) {
                choices.add(choice);
            }
        }

        int minimumRequired = section.getMinimumRequired() != null ? section.getMinimumRequired() : 1;
        Integer maximumRequired = section.getMaximumRequired();
        boolean allowMultipleSelections = section.getAllowMultipleSelections() != null && 
                                         section.getAllowMultipleSelections();

        return new ChoiceReward(choices, minimumRequired, maximumRequired, allowMultipleSelections);
    }

    private PermissionReward convertPermissionReward(RewardSection section) {
        if (section.getPermissions() == null || section.getPermissions().isEmpty()) {
            throw new IllegalArgumentException("Permission reward requires 'permissions' field");
        }

        Long durationSeconds = section.getDurationSeconds();
        boolean temporary = section.getTemporary() != null && section.getTemporary();

        return new PermissionReward(section.getPermissions(), durationSeconds, temporary);
    }
}
