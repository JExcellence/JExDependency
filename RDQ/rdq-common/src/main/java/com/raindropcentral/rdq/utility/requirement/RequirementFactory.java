package com.raindropcentral.rdq.utility.requirement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.requirement.*;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import com.raindropcentral.rdq.database.json.requirement.RequirementParser;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.configmapper.sections.AConfigSection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RequirementFactory {

    private static final Logger LOGGER = CentralLogger.getLogger(RequirementFactory.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RDQ rdq;

    public RequirementFactory(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    public @NotNull List<RRankUpgradeRequirement> parseRequirements(final @NotNull RRank rank,
                                                                    final @NotNull Map<String, ? extends BaseRequirementSection> requirements) {
        final List<RRankUpgradeRequirement> result = new ArrayList<>();
        if (requirements.isEmpty()) {
            return result;
        }
        final AtomicInteger order = new AtomicInteger(0);
        requirements.forEach((key, base) -> {
            if (base == null) {
                LOGGER.log(Level.WARNING, "Null requirement section for key: {0}", key);
                return;
            }
            final AConfigSection specific = getSpecificRequirementSection(base);
            if (specific == null) {
                LOGGER.log(Level.WARNING, "No valid requirement section for key: {0} in rank: {1}", new Object[]{key, rank.getIdentifier()});
                return;
            }
            try {
                if (isInvalidRequirement(specific)) {
                    return;
                }
                final AbstractRequirement abstractRequirement = createRequirementFromSection(specific);
                RRequirement rRequirement = new RRequirement(abstractRequirement, base.getIcon());
                final RRankUpgradeRequirement upgradeRequirement = new RRankUpgradeRequirement(rank, rRequirement, rRequirement.getShowcase());
                final Integer configuredOrder = base.getDisplayOrder();
                upgradeRequirement.setDisplayOrder(configuredOrder != null && configuredOrder > 0 ? configuredOrder : order.getAndIncrement());
                result.add(upgradeRequirement);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to parse requirement '" + key + "' for rank '" + rank.getIdentifier() + "'", e);
            }
        });
        return result;
    }

    public @NotNull CompletableFuture<List<RRankUpgradeRequirement>> parseRequirementsAsync(final @NotNull RRank rank,
                                                                                            final @NotNull Map<String, ? extends BaseRequirementSection> requirements) {
        return parseRequirementsAsync(rank, requirements, ForkJoinPool.commonPool());
    }

    public @NotNull CompletableFuture<List<RRankUpgradeRequirement>> parseRequirementsAsync(final @NotNull RRank rank,
                                                                                            final @NotNull Map<String, ? extends BaseRequirementSection> requirements,
                                                                                            final @NotNull Executor executor) {
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(requirements, "requirements");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.supplyAsync(() -> parseRequirements(rank, requirements), executor);
    }

    public @NotNull CompletableFuture<List<RRankUpgradeRequirement>> persistRequirementEntitiesAsync(final @NotNull List<RRankUpgradeRequirement> upgrades) {
        return persistRequirementEntitiesAsync(upgrades, this.rdq.getExecutor());
    }

    public @NotNull CompletableFuture<List<RRankUpgradeRequirement>> persistRequirementEntitiesAsync(final @NotNull List<RRankUpgradeRequirement> upgrades,
                                                                                                     final @NotNull Executor executor) {
        Objects.requireNonNull(upgrades, "upgrades");
        Objects.requireNonNull(executor, "executor");
        if (upgrades.isEmpty()) {
            return CompletableFuture.completedFuture(upgrades);
        }
        final List<CompletableFuture<RRankUpgradeRequirement>> futures = new ArrayList<>(upgrades.size());
        for (RRankUpgradeRequirement upgrade : upgrades) {
            futures.add(persistSingleRequirementAsync(upgrade, executor));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    final List<RRankUpgradeRequirement> out = new ArrayList<>(futures.size());
                    for (CompletableFuture<RRankUpgradeRequirement> f : futures) {
                        out.add(f.join());
                    }
                    return out;
                });
    }

    private CompletableFuture<RRankUpgradeRequirement> persistSingleRequirementAsync(final @NotNull RRankUpgradeRequirement upgrade,
                                                                                     final @NotNull Executor executor) {
        final RRequirement req = upgrade.getRequirement();
        if (req.getId() != null) {
            return CompletableFuture.completedFuture(upgrade);
        }

        return rdq.getRequirementRepository().createAsync(req).thenApply(saved -> {
            upgrade.setRequirement(saved);
            return upgrade;
        });
    }

    public @NotNull AbstractRequirement createRequirementFromSection(final @NotNull AConfigSection section) throws IOException {
        final String json = convertSectionToJson(section);
        return RequirementParser.parse(json);
    }

    private @NotNull String convertSectionToJson(final @NotNull AConfigSection section) throws IOException {
        final Map<String, Object> jsonMap = new HashMap<>();
        final String type = determineRequirementType(section);
        jsonMap.put("type", type);
        switch (section) {
            case ItemRequirementSection s -> addItemProperties(jsonMap, s);
            case CurrencyRequirementSection s -> addCurrencyProperties(jsonMap, s);
            case ExperienceLevelRequirementSection s -> addExperienceProperties(jsonMap, s);
            case PlaytimeRequirementSection s -> addPlaytimeProperties(jsonMap, s);
            case PermissionRequirementSection s -> addPermissionProperties(jsonMap, s);
            case LocationRequirementSection s -> addLocationProperties(jsonMap, s);
            case CompositeRequirementSection s -> addCompositeProperties(jsonMap, s);
            case ChoiceRequirementSection s -> addChoiceProperties(jsonMap, s);
            case AchievementRequirementSection s -> addAchievementProperties(jsonMap, s);
            case SkillRequirementSection s -> addSkillProperties(jsonMap, s);
            case JobRequirementSection s -> addJobProperties(jsonMap, s);
            case TimeBasedRequirementSection s -> addTimeBasedProperties(jsonMap, s);
            default -> LOGGER.log(Level.WARNING, "Unknown requirement section type: {0}", section.getClass().getSimpleName());
        }
        return OBJECT_MAPPER.writeValueAsString(jsonMap);
    }

    private @Nullable AConfigSection getSpecificRequirementSection(final @NotNull BaseRequirementSection base) {
        final String type = base.getType();
        if (type == null || type.equals("not_defined")) {
            if (base.getItemRequirement() != null && base.getItemRequirement().getRequiredItemsList() != null && !base.getItemRequirement().getRequiredItemsList().isEmpty()) {
                return base.getItemRequirement();
            }
            if (base.getCurrencyRequirement() != null && base.getCurrencyRequirement().getRequiredCurrencies() != null && !base.getCurrencyRequirement().getRequiredCurrencies().isEmpty()) {
                return base.getCurrencyRequirement();
            }
            if (base.getExperienceRequirement() != null && base.getExperienceRequirement().getRequiredLevel() > 0) {
                return base.getExperienceRequirement();
            }
            if (base.getPlaytimeRequirement() != null && (base.getPlaytimeRequirement().getRequiredPlaytimeSeconds() > 0 || (base.getPlaytimeRequirement().getTime() != null && base.getPlaytimeRequirement().getTime() > 0))) {
                return base.getPlaytimeRequirement();
            }
            if (base.getPermissionRequirement() != null && base.getPermissionRequirement().getRequiredPermissions() != null && !base.getPermissionRequirement().getRequiredPermissions().isEmpty()) {
                return base.getPermissionRequirement();
            }
            if (base.getLocationRequirement() != null) {
                return base.getLocationRequirement();
            }
            if (base.getCompositeRequirement() != null && base.getCompositeRequirement().getCompositeRequirements() != null && !base.getCompositeRequirement().getCompositeRequirements().isEmpty()) {
                return base.getCompositeRequirement();
            }
            if (base.getChoiceRequirement() != null && base.getChoiceRequirement().getChoices() != null && !base.getChoiceRequirement().getChoices().isEmpty()) {
                return base.getChoiceRequirement();
            }
            if (base.getAchievementRequirement() != null && base.getAchievementRequirement().getRequiredAchievements() != null && !base.getAchievementRequirement().getRequiredAchievements().isEmpty()) {
                return base.getAchievementRequirement();
            }
            if (base.getSkillRequirement() != null && base.getSkillRequirement().getRequiredSkills() != null && !base.getSkillRequirement().getRequiredSkills().isEmpty()) {
                return base.getSkillRequirement();
            }
            if (base.getJobRequirement() != null && base.getJobRequirement().getRequiredJobs() != null && !base.getJobRequirement().getRequiredJobs().isEmpty()) {
                return base.getJobRequirement();
            }
            if (base.getTimeBasedRequirement() != null && (base.getTimeBasedRequirement().getTimeConstraintSeconds() > 0
                    || (base.getTimeBasedRequirement().getStartTime() != null && base.getTimeBasedRequirement().getEndTime() != null)
                    || (base.getTimeBasedRequirement().getActiveDays() != null && !base.getTimeBasedRequirement().getActiveDays().isEmpty())
                    || (base.getTimeBasedRequirement().getActiveDates() != null && !base.getTimeBasedRequirement().getActiveDates().isEmpty()))) {
                return base.getTimeBasedRequirement();
            }
            return null;
        } else {
            return switch (type.toUpperCase(Locale.ROOT)) {
                case "ITEM" -> base.getItemRequirement();
                case "CURRENCY" -> base.getCurrencyRequirement();
                case "EXPERIENCE_LEVEL" -> base.getExperienceRequirement();
                case "PLAYTIME" -> base.getPlaytimeRequirement();
                case "TIME_BASED" -> base.getTimeBasedRequirement();
                case "PERMISSION" -> base.getPermissionRequirement();
                case "LOCATION" -> base.getLocationRequirement();
                case "COMPOSITE" -> base.getCompositeRequirement();
                case "CHOICE" -> base.getChoiceRequirement();
                case "ACHIEVEMENT", "CUSTOM" -> base.getAchievementRequirement();
                case "SKILLS" -> base.getSkillRequirement();
                case "JOBS" -> base.getJobRequirement();
                default -> null;
            };
        }
    }

    private boolean isInvalidRequirement(final @NotNull AConfigSection section) {
        return !switch (section) {
            case ItemRequirementSection s -> s.getRequiredItemsList() != null && !s.getRequiredItemsList().isEmpty();
            case CurrencyRequirementSection s -> s.getRequiredCurrencies() != null && !s.getRequiredCurrencies().isEmpty();
            case ExperienceLevelRequirementSection s -> s.getRequiredLevel() > 0;
            case PlaytimeRequirementSection s -> s.getRequiredPlaytimeSeconds() > 0 || (s.getTime() != null && s.getTime() > 0);
            case PermissionRequirementSection s -> s.getRequiredPermissions() != null && !s.getRequiredPermissions().isEmpty();
            case LocationRequirementSection s -> (s.getRequiredWorld() != null && !s.getRequiredWorld().trim().isEmpty())
                    || (s.getRequiredRegion() != null && !s.getRequiredRegion().trim().isEmpty())
                    || (s.getRequiredCoordinates() != null && !s.getRequiredCoordinates().isEmpty())
                    || s.getRequiredDistance() > 0;
            case CompositeRequirementSection s -> s.getCompositeRequirements() != null && !s.getCompositeRequirements().isEmpty();
            case ChoiceRequirementSection s -> s.getChoices() != null && !s.getChoices().isEmpty();
            case AchievementRequirementSection s -> s.getRequiredAchievements() != null && !s.getRequiredAchievements().isEmpty();
            case SkillRequirementSection s -> s.getRequiredSkills() != null && !s.getRequiredSkills().isEmpty();
            case JobRequirementSection s -> s.getRequiredJobs() != null && !s.getRequiredJobs().isEmpty();
            case TimeBasedRequirementSection s -> s.getTimeConstraintSeconds() > 0
                    || (s.getStartTime() != null && s.getEndTime() != null)
                    || (s.getActiveDays() != null && !s.getActiveDays().isEmpty())
                    || (s.getActiveDates() != null && !s.getActiveDates().isEmpty());
            default -> {
                LOGGER.log(Level.WARNING, "Unknown requirement section type for validation: {0}", section.getClass().getSimpleName());
                yield false;
            }
        };
    }

    private String determineRequirementType(final @NotNull AConfigSection section) {
        return switch (section) {
            case ItemRequirementSection ignored -> "ITEM";
            case CurrencyRequirementSection ignored -> "CURRENCY";
            case ExperienceLevelRequirementSection ignored -> "EXPERIENCE_LEVEL";
            case PlaytimeRequirementSection ignored -> "PLAYTIME";
            case PermissionRequirementSection ignored -> "PERMISSION";
            case LocationRequirementSection ignored -> "LOCATION";
            case CompositeRequirementSection ignored -> "COMPOSITE";
            case ChoiceRequirementSection ignored -> "CHOICE";
            case AchievementRequirementSection ignored -> "CUSTOM";
            case SkillRequirementSection ignored -> "SKILLS";
            case JobRequirementSection ignored -> "JOBS";
            case TimeBasedRequirementSection ignored -> "TIME_BASED";
            default -> "UNKNOWN";
        };
    }

    private void addItemProperties(final @NotNull Map<String, Object> json, final @NotNull ItemRequirementSection s) {
        json.put("consumeOnComplete", s.getConsumeOnComplete());
        json.put("exactMatch", false);
        json.put("description", "Item requirement");
        if (s.getRequiredItemsList() != null && !s.getRequiredItemsList().isEmpty()) {
            final List<Map<String, Object>> items = new ArrayList<>();
            for (ItemStack stack : s.getRequiredItemsList()) {
                final Map<String, Object> m = new HashMap<>();
                m.put("type", stack.getType().name());
                m.put("amount", stack.getAmount());
                if (stack.hasItemMeta()) {
                    final Map<String, Object> meta = new HashMap<>();
                    if (stack.getItemMeta().hasDisplayName()) {
                        meta.put("displayName", stack.getItemMeta().getDisplayName());
                    }
                    if (stack.getItemMeta().hasLore()) {
                        meta.put("lore", stack.getItemMeta().getLore());
                    }
                    if (!meta.isEmpty()) {
                        m.put("meta", meta);
                    }
                }
                items.add(m);
            }
            json.put("requiredItems", items);
            json.put("itemBuilders", new ArrayList<>());
        }
    }

    private void addCurrencyProperties(final @NotNull Map<String, Object> json, final @NotNull CurrencyRequirementSection s) {
        json.put("consumeOnComplete", s.getConsumeOnComplete());
        String plugin = s.getCurrencyPlugin();
        if (plugin == null || plugin.trim().isEmpty()) {
            plugin = "vault";
        }
        json.put("currencyPlugin", plugin);
        final Map<String, Double> required = s.getRequiredCurrencies();
        json.put("requiredCurrencies", required != null && !required.isEmpty() ? required : Map.of("default", 100.0));
    }

    private void addExperienceProperties(final @NotNull Map<String, Object> json, final @NotNull ExperienceLevelRequirementSection s) {
        json.put("consumeOnComplete", s.getConsumeOnComplete());
        json.put("requiredLevel", Math.max(1, s.getRequiredLevel()));
        String type = s.getExperienceType();
        if (type == null || type.trim().isEmpty()) {
            type = "LEVEL";
        }
        json.put("experienceType", type);
        if (s.getDescription() != null && !s.getDescription().trim().isEmpty()) {
            json.put("description", s.getDescription());
        }
    }

    private void addPlaytimeProperties(final @NotNull Map<String, Object> json, final @NotNull PlaytimeRequirementSection s) {
        long seconds = s.getRequiredPlaytimeSeconds();
        if (seconds <= 0) {
            seconds = 3600;
        }
        json.put("requiredPlaytimeSeconds", seconds);
        Boolean useTotal = s.getUseTotalPlaytime();
        if (useTotal == null) {
            useTotal = !s.hasWorldSpecificConfiguration();
        }
        json.put("useTotalPlaytime", useTotal);
        final Map<String, Long> world = s.getWorldPlaytimeRequirements();
        json.put("worldPlaytimeRequirements", world != null && !world.isEmpty() ? world : new HashMap<String, Long>());
        if (s.getDescription() != null && !s.getDescription().trim().isEmpty()) {
            json.put("description", s.getDescription());
        } else {
            json.put("description", useTotal ? describeTotal(seconds) : "Meet world-specific playtime requirements");
        }
    }

    private String describeTotal(final long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        if (h > 0) {
            return "Play for a total of " + h + " hour" + (h > 1 ? "s" : "") + (m > 0 ? " and " + m + " minute" + (m > 1 ? "s" : "") : "");
        }
        if (m > 0) {
            return "Play for a total of " + m + " minute" + (m > 1 ? "s" : "");
        }
        return "Play for a total of " + seconds + " second" + (seconds > 1 ? "s" : "");
    }

    private void addPermissionProperties(final @NotNull Map<String, Object> json, final @NotNull PermissionRequirementSection s) {
        json.put("requireAll", s.getRequireAll());
        json.put("checkNegation", s.getCheckNegation());
        final List<String> required = s.getRequiredPermissions();
        json.put("requiredPermissions", required != null && !required.isEmpty() ? required : List.of("default.permission"));
    }

    private void addLocationProperties(final @NotNull Map<String, Object> json, final @NotNull LocationRequirementSection s) {
        json.put("exactLocation", s.getExactLocation());
        if (s.getRequiredWorld() != null && !s.getRequiredWorld().trim().isEmpty()) json.put("requiredWorld", s.getRequiredWorld());
        if (s.getRequiredRegion() != null && !s.getRequiredRegion().trim().isEmpty()) json.put("requiredRegion", s.getRequiredRegion());
        if (s.getRequiredCoordinates() != null && !s.getRequiredCoordinates().isEmpty()) json.put("requiredCoordinates", s.getRequiredCoordinates());
        if (s.getRequiredDistance() > 0) json.put("requiredDistance", s.getRequiredDistance());
        if ((!json.containsKey("requiredWorld")) && (!json.containsKey("requiredRegion")) && (!json.containsKey("requiredCoordinates")) && (!json.containsKey("requiredDistance"))) {
            json.put("requiredWorld", "world");
        }
    }

    private void addCompositeProperties(final @NotNull Map<String, Object> json, final @NotNull CompositeRequirementSection s) throws IOException {
        json.put("operator", s.getOperator());
        json.put("minimumRequired", Math.max(0, s.getMinimumRequired()));
        json.put("maximumRequired", Math.max(0, s.getMaximumRequired()));
        json.put("allowPartialProgress", s.getAllowPartialProgress());
        if (s.getDescription() != null && !s.getDescription().trim().isEmpty()) {
            json.put("description", s.getDescription());
        }
        final List<BaseRequirementSection> sub = s.getCompositeRequirements();
        if (sub != null && !sub.isEmpty()) {
            final List<Map<String, Object>> reqs = new ArrayList<>();
            for (BaseRequirementSection r : sub) {
                final String js = convertSectionToJson(r);
                @SuppressWarnings("unchecked")
                final Map<String, Object> map = OBJECT_MAPPER.readValue(js, Map.class);
                reqs.add(map);
            }
            json.put("requirements", reqs);
        } else {
            json.put("requirements", new ArrayList<>());
        }
    }

    private void addChoiceProperties(final @NotNull Map<String, Object> json, final @NotNull ChoiceRequirementSection s) throws IOException {
        json.put("minimumRequired", Math.max(1, s.getMinimumRequired()));
        json.put("maximumRequired", Math.max(0, s.getMaximumRequired()));
        json.put("allowPartialProgress", s.getAllowPartialProgress());
        json.put("mutuallyExclusive", s.getMutuallyExclusive());
        json.put("allowChoiceChange", s.getAllowChoiceChange());
        if (s.getDescription() != null && !s.getDescription().trim().isEmpty()) {
            json.put("description", s.getDescription());
        }
        final List<BaseRequirementSection> choices = s.getChoices();
        if (choices != null && !choices.isEmpty()) {
            final List<Map<String, Object>> list = new ArrayList<>();
            for (BaseRequirementSection c : choices) {
                final String js = convertSectionToJson(c);
                @SuppressWarnings("unchecked")
                final Map<String, Object> map = OBJECT_MAPPER.readValue(js, Map.class);
                list.add(map);
            }
            json.put("choices", list);
        } else {
            json.put("choices", new ArrayList<>());
        }
    }

    private void addAchievementProperties(final @NotNull Map<String, Object> json, final @NotNull AchievementRequirementSection s) {
        json.put("requireAll", s.getRequireAll());
        String plugin = s.getAchievementPlugin();
        if (plugin == null || plugin.trim().isEmpty()) plugin = "advancedachievements";
        json.put("achievementPlugin", plugin);
        final List<String> required = s.getRequiredAchievements();
        json.put("requiredAchievements", required != null && !required.isEmpty() ? required : List.of("default_achievement"));
    }

    private void addSkillProperties(final @NotNull Map<String, Object> json, final @NotNull SkillRequirementSection s) {
        json.put("consumeOnComplete", s.getConsumeOnComplete());
        String plugin = s.getSkillPlugin();
        if (plugin != null) {
            plugin = plugin.toUpperCase(Locale.ROOT);
            switch (plugin) {
                case "MCMMO" -> plugin = "MCMMO";
                case "ECO_SKILLS", "ECOSKILLS" -> plugin = "ECO_SKILLS";
                case "AURA_SKILLS", "AURASKILLS" -> plugin = "AURA_SKILLS";
                default -> plugin = "AUTO";
            }
        } else {
            plugin = "AUTO";
        }
        json.put("skillPlugin", plugin);
        final Map<String, Integer> required = s.getRequiredSkills();
        if (required != null && !required.isEmpty()) {
            json.put("requiredSkills", required);
        } else {
            final Map<String, Integer> def = new HashMap<>();
            def.put("mining", 10);
            json.put("requiredSkills", def);
        }
    }

    private void addJobProperties(final @NotNull Map<String, Object> json, final @NotNull JobRequirementSection s) {
        json.put("consumeOnComplete", s.getConsumeOnComplete());
        json.put("requireAll", s.getRequireAll());
        String plugin = s.getJobPlugin();
        if (plugin != null) {
            plugin = plugin.toUpperCase(Locale.ROOT);
            switch (plugin) {
                case "JOBS", "JOBS_REBORN", "JOBSREBORN" -> plugin = "JOBS_REBORN";
                case "ECO_JOBS", "ECOJOBS" -> plugin = "ECO_JOBS";
                default -> plugin = "AUTO";
            }
        } else {
            plugin = "AUTO";
        }
        json.put("jobPlugin", plugin);
        final Map<String, Integer> required = s.getRequiredJobs();
        if (required != null && !required.isEmpty()) {
            json.put("requiredJobs", required);
        } else {
            final Map<String, Integer> def = new HashMap<>();
            def.put("miner", 10);
            json.put("requiredJobs", def);
        }
    }

    private void addTimeBasedProperties(final @NotNull Map<String, Object> json, final @NotNull TimeBasedRequirementSection s) {
        final long timeConstraint = Math.max(1, s.getTimeConstraintSeconds());
        json.put("timeConstraintSeconds", timeConstraint);
        json.put("cooldownSeconds", Math.max(0, s.getCooldownSeconds()));
        String tz = s.getTimeZone();
        if (tz == null || tz.trim().isEmpty()) tz = "UTC";
        json.put("timeZone", tz);
        json.put("recurring", s.getRecurring());
        json.put("useRealTime", s.getUseRealTime());
        if (s.getStartTime() != null) json.put("startTime", s.getStartTime());
        if (s.getEndTime() != null) json.put("endTime", s.getEndTime());
        if (s.getActiveDays() != null && !s.getActiveDays().isEmpty()) json.put("activeDays", s.getActiveDays());
        if (s.getActiveDates() != null && !s.getActiveDates().isEmpty()) json.put("activeDates", s.getActiveDates());
    }

    public static @NotNull Material determineShowcaseMaterial(final @NotNull BaseRequirementSection base) {
        try {
            return Material.valueOf(base.getIcon().getMaterial());
        } catch (Exception ignored) {
            final String type = base.getType();
            if (type != null) {
                return switch (type.toUpperCase(Locale.ROOT)) {
                    case "ITEM" -> Material.CHEST;
                    case "CURRENCY" -> Material.GOLD_INGOT;
                    case "EXPERIENCE_LEVEL" -> Material.EXPERIENCE_BOTTLE;
                    case "PERMISSION" -> Material.NAME_TAG;
                    case "LOCATION" -> Material.COMPASS;
                    case "CUSTOM" -> Material.COMMAND_BLOCK;
                    case "COMPOSITE" -> Material.REDSTONE;
                    case "CHOICE" -> Material.CROSSBOW;
                    case "TIME_BASED" -> Material.CLOCK;
                    case "JOBS" -> Material.IRON_PICKAXE;
                    case "SKILLS" -> Material.ENCHANTED_BOOK;
                    case "ACHIEVEMENT" -> Material.DIAMOND;
                    case "PLAYTIME" -> Material.CLOCK;
                    case "PREVIOUS_LEVEL" -> Material.LADDER;
                    default -> Material.PAPER;
                };
            }
            return Material.PAPER;
        }
    }
}