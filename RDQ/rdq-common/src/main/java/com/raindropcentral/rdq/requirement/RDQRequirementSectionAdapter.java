package com.raindropcentral.rdq.requirement;

import com.raindropcentral.rdq.config.requirement.*;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.config.RequirementBuilder;
import com.raindropcentral.rplatform.requirement.config.RequirementSectionAdapter;
import com.raindropcentral.rplatform.requirement.impl.ExperienceLevelRequirement;
import com.raindropcentral.rplatform.requirement.impl.PermissionRequirement;
import com.raindropcentral.rplatform.requirement.impl.PluginRequirement;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Adapter that converts RDQ's BaseRequirementSection config sections to AbstractRequirement instances.
 * <p>
 * This adapter bridges RDQ's YAML configuration format with RPlatform's requirement system,
 * allowing seamless conversion from config files to runtime requirement objects.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RDQRequirementSectionAdapter implements RequirementSectionAdapter<BaseRequirementSection> {

    private static final Logger LOGGER = CentralLogger.getLogger(RDQRequirementSectionAdapter.class);

    @Override
    @Nullable
    public AbstractRequirement convert(@NotNull BaseRequirementSection section, @Nullable Map<String, Object> context) {
        String type = section.getType().toUpperCase();
        
        try {
            return switch (type) {
                case "ITEM" -> convertItemRequirement(section);
                case "CURRENCY" -> convertCurrencyRequirement(section);
                case "EXPERIENCE_LEVEL" -> convertExperienceRequirement(section);
                case "PERMISSION" -> convertPermissionRequirement(section);
                case "LOCATION" -> convertLocationRequirement(section);
                case "PLAYTIME" -> convertPlaytimeRequirement(section);
                case "COMPOSITE" -> convertCompositeRequirement(section);
                case "CHOICE" -> convertChoiceRequirement(section);
                case "SKILLS" -> convertSkillsRequirement(section);
                case "JOBS" -> convertJobsRequirement(section);
                default -> {
                    LOGGER.warning("Unknown requirement type: " + type);
                    yield null;
                }
            };
        } catch (Exception e) {
            LOGGER.severe("Failed to convert requirement section of type " + type + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private AbstractRequirement convertItemRequirement(BaseRequirementSection section) {
        ItemRequirementSection itemSection = section.getItemRequirement();
        List<ItemStack> items = itemSection.getRequiredItemsList();
        
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No items specified for ITEM requirement");
        }

        return RequirementBuilder.item()
                .items(items)
                .consumeOnComplete(itemSection.getConsumeOnComplete())
                .exactMatch(true) // Default to exact match
                .build();
    }

    private AbstractRequirement convertCurrencyRequirement(BaseRequirementSection section) {
        CurrencyRequirementSection currencySection = section.getCurrencyRequirement();
        Map<String, Double> currencies = currencySection.getRequiredCurrencies();
        
        if (currencies.isEmpty()) {
            throw new IllegalArgumentException("No currencies specified for CURRENCY requirement");
        }

        AbstractRequirement requirement = RequirementBuilder.currency()
                .currencies(currencies)
                .plugin(currencySection.getCurrencyPlugin())
                .timeout(5000L) // Default timeout
                .build();
        
        // Set consumeOnComplete flag
        requirement.setConsumeOnComplete(currencySection.getConsumeOnComplete());
        
        return requirement;
    }

    private AbstractRequirement convertExperienceRequirement(BaseRequirementSection section) {
        ExperienceLevelRequirementSection expSection = section.getExperienceRequirement();
        int level = expSection.getRequiredLevel();
        
        String typeStr = expSection.getExperienceType();
        ExperienceLevelRequirement.ExperienceType type;
        try {
            type = ExperienceLevelRequirement.ExperienceType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ExperienceLevelRequirement.ExperienceType.LEVEL;
        }

        var builder = RequirementBuilder.experience()
                .level(level)
                .type(type)
                .consumeOnComplete(expSection.getConsumeOnComplete());
        
        if (expSection.getDescription() != null) {
            builder.description(expSection.getDescription());
        }
        
        return builder.build();
    }

    private AbstractRequirement convertPermissionRequirement(BaseRequirementSection section) {
        PermissionRequirementSection permSection = section.getPermissionRequirement();
        List<String> permissions = permSection.getRequiredPermissions();
        
        if (permissions.isEmpty()) {
            throw new IllegalArgumentException("No permissions specified for PERMISSION requirement");
        }

        // Convert requireAll boolean to PermissionMode
        PermissionRequirement.PermissionMode mode = permSection.getRequireAll() 
            ? PermissionRequirement.PermissionMode.ALL 
            : PermissionRequirement.PermissionMode.ANY;

        return RequirementBuilder.permission()
                .permissions(permissions)
                .mode(mode)
                .negated(permSection.getCheckNegation())
                .build();
    }

    private AbstractRequirement convertLocationRequirement(BaseRequirementSection section) {
        LocationRequirementSection locSection = section.getLocationRequirement();
        var builder = RequirementBuilder.location();

        String world = locSection.getRequiredWorld();
        if (world != null && !world.isEmpty()) {
            builder.world(world);
        }

        String region = locSection.getRequiredRegion();
        if (region != null && !region.isEmpty()) {
            builder.region(region);
        }

        Map<String, Double> coords = locSection.getRequiredCoordinates();
        if (coords != null && coords.containsKey("x") && coords.containsKey("y") && coords.containsKey("z")) {
            builder.coordinates(coords.get("x"), coords.get("y"), coords.get("z"));
        }

        double distance = locSection.getRequiredDistance();
        if (distance > 0) {
            builder.distance(distance);
        }

        return builder.build();
    }

    private AbstractRequirement convertPlaytimeRequirement(BaseRequirementSection section) {
        PlaytimeRequirementSection playtimeSection = section.getPlaytimeRequirement();
        var builder = RequirementBuilder.playtime();

        long seconds = playtimeSection.getRequiredPlaytimeSeconds();
        if (seconds > 0) {
            builder.seconds(seconds);
        }

        Map<String, Long> worldReqs = playtimeSection.getWorldPlaytimeRequirements();
        if (worldReqs != null && !worldReqs.isEmpty()) {
            worldReqs.forEach(builder::worldPlaytime);
        }

        if (playtimeSection.getDescription() != null) {
            builder.description(playtimeSection.getDescription());
        }

        return builder.build();
    }

    private AbstractRequirement convertCompositeRequirement(BaseRequirementSection section) {
        CompositeRequirementSection compositeSection = section.getCompositeRequirement();
        List<BaseRequirementSection> subSections = compositeSection.getCompositeRequirements();
        
        if (subSections.isEmpty()) {
            throw new IllegalArgumentException("No sub-requirements specified for COMPOSITE requirement");
        }

        var builder = RequirementBuilder.composite();

        // Recursively convert sub-requirements
        for (BaseRequirementSection subSection : subSections) {
            AbstractRequirement subReq = convert(subSection, null);
            if (subReq != null) {
                builder.add(subReq);
            }
        }

        String opStr = compositeSection.getOperator();
        switch (opStr.toUpperCase()) {
            case "OR" -> builder.or();
            case "MINIMUM" -> builder.minimum(compositeSection.getMinimumRequired());
            default -> builder.and();
        }

        builder.allowPartialProgress(compositeSection.getAllowPartialProgress());
        
        if (compositeSection.getDescription() != null) {
            builder.description(compositeSection.getDescription());
        }

        return builder.build();
    }

    private AbstractRequirement convertChoiceRequirement(BaseRequirementSection section) {
        ChoiceRequirementSection choiceSection = section.getChoiceRequirement();
        List<BaseRequirementSection> choiceSections = choiceSection.getChoices();
        
        if (choiceSections.isEmpty()) {
            throw new IllegalArgumentException("No choices specified for CHOICE requirement");
        }

        var builder = RequirementBuilder.choice();

        // Recursively convert choice requirements
        for (BaseRequirementSection choiceSubSection : choiceSections) {
            AbstractRequirement choiceReq = convert(choiceSubSection, null);
            if (choiceReq != null) {
                builder.add(choiceReq);
            }
        }

        builder.minimumRequired(choiceSection.getMinimumRequired());
        builder.mutuallyExclusive(choiceSection.getMutuallyExclusive());
        builder.allowChoiceChange(choiceSection.getAllowChoiceChange());
        
        if (choiceSection.getDescription() != null && !choiceSection.getDescription().isEmpty()) {
            builder.description(choiceSection.getDescription());
        }

        return builder.build();
    }

    private AbstractRequirement convertSkillsRequirement(BaseRequirementSection section) {
        SkillRequirementSection skillSection = section.getSkillRequirement();
        String pluginStr = skillSection.getSkillPlugin();
        
        Map<String, Integer> skills = skillSection.getRequiredSkills();
        Map<String, Double> values = new HashMap<>();
        if (skills != null) {
            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                values.put(entry.getKey(), entry.getValue().doubleValue());
            }
        }
        
        return new PluginRequirement(
            pluginStr != null ? pluginStr.toLowerCase() : "auto",
            "SKILLS",
            values,
            false,
            null
        );
    }

    private AbstractRequirement convertJobsRequirement(BaseRequirementSection section) {
        JobRequirementSection jobSection = section.getJobRequirement();
        String pluginStr = jobSection.getJobPlugin();
        
        Map<String, Integer> jobs = jobSection.getRequiredJobs();
        Map<String, Double> values = new HashMap<>();
        if (jobs != null) {
            for (Map.Entry<String, Integer> entry : jobs.entrySet()) {
                values.put(entry.getKey(), entry.getValue().doubleValue());
            }
        }
        
        return new PluginRequirement(
            pluginStr != null ? pluginStr.toLowerCase() : "auto",
            "JOBS",
            values,
            false,
            null
        );
    }
}