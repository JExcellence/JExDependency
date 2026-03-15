package com.raindropcentral.rdq.config.requirement;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter that converts BaseRequirementSection to AbstractRequirement.
 *
 * <p>This adapter bridges the RDQ config system with the RPlatform requirement system,
 * allowing BaseRequirementSection instances to be converted to AbstractRequirement.
 */
public class BaseRequirementSectionAdapter implements RequirementSectionAdapter<BaseRequirementSection> {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private static final BaseRequirementSectionAdapter INSTANCE = new BaseRequirementSectionAdapter();

    private BaseRequirementSectionAdapter() {}

    /**
     * Gets instance.
     */
    @NotNull
    public static BaseRequirementSectionAdapter getInstance() {
        return INSTANCE;
    }

    /**
     * Executes convert.
     */
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
                case "JOBS" -> convertJobsRequirement(section);
                case "SKILLS" -> convertSkillsRequirement(section);
                case "PLUGIN" -> convertPluginRequirement(section);
                case "TIME_BASED" -> convertTimeBasedRequirement(section);
                default -> {
                    LOGGER.warning("Unknown requirement type: " + type);
                    yield null;
                }
            };
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to convert requirement of type: " + type, e);
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
                .build();
    }

    private AbstractRequirement convertCurrencyRequirement(BaseRequirementSection section) {
        CurrencyRequirementSection currencySection = section.getCurrencyRequirement();
        
        if (currencySection == null) {
            throw new IllegalArgumentException("Currency requirement section is null");
        }
        
        Map<String, Double> currencies = currencySection.getRequiredCurrencies();

        if (currencies == null || currencies.isEmpty()) {
            throw new IllegalArgumentException("No currencies specified for CURRENCY requirement");
        }

        // Use the first currency from the map (new API only supports single currency)
        Map.Entry<String, Double> firstCurrency = currencies.entrySet().iterator().next();
        
        return RequirementBuilder.currency()
                .currency(firstCurrency.getKey())
                .amount(firstCurrency.getValue())
                .consumable(currencySection.getConsumeOnComplete())
                .build();
    }

    private AbstractRequirement convertExperienceRequirement(BaseRequirementSection section) {
        ExperienceLevelRequirementSection expSection = section.getExperienceRequirement();

        String typeStr = expSection.getExperienceType();
        ExperienceLevelRequirement.ExperienceType type;
        try {
            type = ExperienceLevelRequirement.ExperienceType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ExperienceLevelRequirement.ExperienceType.LEVEL;
        }

        return RequirementBuilder.experience()
                .level(expSection.getRequiredLevel())
                .type(type)
                .consumeOnComplete(expSection.getConsumeOnComplete())
                .description(expSection.getDescription())
                .build();
    }

    private AbstractRequirement convertPermissionRequirement(BaseRequirementSection section) {
        PermissionRequirementSection permSection = section.getPermissionRequirement();
        List<String> permissions = permSection.getRequiredPermissions();

        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("No permissions specified for PERMISSION requirement");
        }

        // Map requireAll to PermissionMode
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

        Double distance = locSection.getRequiredDistance();
        if (distance != null && distance > 0) {
            builder.distance(distance);
        }

        return builder.build();
    }

    private AbstractRequirement convertPlaytimeRequirement(BaseRequirementSection section) {
        PlaytimeRequirementSection playSection = section.getPlaytimeRequirement();
        var builder = RequirementBuilder.playtime();

        long seconds = playSection.getRequiredPlaytimeSeconds();
        if (seconds > 0) {
            builder.seconds(seconds);
        }

        Map<String, Long> worldReqs = playSection.getWorldPlaytimeRequirements();
        if (worldReqs != null) {
            worldReqs.forEach(builder::worldPlaytime);
        }

        return builder.build();
    }

    private AbstractRequirement convertCompositeRequirement(BaseRequirementSection section) {
        CompositeRequirementSection compSection = section.getCompositeRequirement();
        List<BaseRequirementSection> subSections = compSection.getCompositeRequirements();

        if (subSections == null || subSections.isEmpty()) {
            throw new IllegalArgumentException("No sub-requirements for COMPOSITE requirement");
        }

        var builder = RequirementBuilder.composite();

        for (BaseRequirementSection subSection : subSections) {
            AbstractRequirement subReq = convert(subSection, null);
            if (subReq != null) {
                builder.add(subReq);
            }
        }

        String operator = compSection.getOperator();
        if (operator != null) {
            switch (operator.toUpperCase()) {
                case "OR" -> builder.or();
                case "MINIMUM" -> builder.minimum(compSection.getMinimumRequired());
                default -> builder.and();
            }
        }

        builder.allowPartialProgress(compSection.getAllowPartialProgress());

        return builder.build();
    }

    private AbstractRequirement convertChoiceRequirement(BaseRequirementSection section) {
        ChoiceRequirementSection choiceSection = section.getChoiceRequirement();
        List<BaseRequirementSection> choices = choiceSection.getChoices();

        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("No choices for CHOICE requirement");
        }

        var builder = RequirementBuilder.choice();

        for (BaseRequirementSection choiceSubSection : choices) {
            AbstractRequirement choiceReq = convert(choiceSubSection, null);
            if (choiceReq != null) {
                builder.add(choiceReq);
            }
        }

        builder.minimumRequired(choiceSection.getMinimumRequired());
        builder.mutuallyExclusive(choiceSection.getMutuallyExclusive());
        builder.allowChoiceChange(choiceSection.getAllowChoiceChange());

        return builder.build();
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

    private AbstractRequirement convertPluginRequirement(BaseRequirementSection section) {
        SkillRequirementSection skillSection = section.getSkillRequirement();
        if (skillSection != null && skillSection.getRequiredSkills() != null
            && !skillSection.getRequiredSkills().isEmpty()) {
            return convertSkillsRequirement(section);
        }

        JobRequirementSection jobSection = section.getJobRequirement();
        if (jobSection != null && jobSection.getRequiredJobs() != null
            && !jobSection.getRequiredJobs().isEmpty()) {
            return convertJobsRequirement(section);
        }

        throw new IllegalArgumentException(
            "PLUGIN requirement in RDQ requires either skillRequirement or jobRequirement data"
        );
    }

    private AbstractRequirement convertTimeBasedRequirement(BaseRequirementSection section) {
        TimeBasedRequirementSection timeSection = section.getTimeBasedRequirement();

        // TIME_BASED requirements in RDQ are simpler - they just have time constraints
        // For now, create a timed requirement with a placeholder delegate if needed
        // The actual delegate would need to be configured in the YAML
        
        long timeConstraint = timeSection.getTimeConstraintSeconds();
        if (timeConstraint <= 0) {
            throw new IllegalArgumentException("No time constraint for TIME_BASED requirement");
        }

        // Create a simple permission requirement as delegate (placeholder)
        // In practice, the YAML should define a proper delegate
        AbstractRequirement delegate = RequirementBuilder.permission()
                .permission("rdq.timed.placeholder")
                .build();

        return RequirementBuilder.timed()
                .delegate(delegate)
                .seconds(timeConstraint)
                .autoStart(true)
                .build();
    }
}
