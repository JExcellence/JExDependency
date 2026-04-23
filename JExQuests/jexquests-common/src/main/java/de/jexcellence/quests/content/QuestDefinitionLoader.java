package de.jexcellence.quests.content;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.database.entity.Quest;
import de.jexcellence.quests.database.entity.QuestDifficulty;
import de.jexcellence.quests.database.entity.QuestTask;
import de.jexcellence.quests.database.repository.QuestRepository;
import de.jexcellence.quests.database.repository.QuestTaskRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Loads quest definitions from
 * {@code plugins/JExQuests/quests/definitions/&lt;category&gt;/*.yml} into
 * the {@code jexquests_quest} / {@code jexquests_quest_task} tables.
 * Upsert semantics: lookup by identifier, update if present, otherwise
 * create.
 */
public final class QuestDefinitionLoader {

    private static final String DEFINITIONS_DIR = "quests/definitions";

    private final JavaPlugin plugin;
    private final QuestRepository quests;
    private final QuestTaskRepository tasks;
    private final JExLogger logger;

    public QuestDefinitionLoader(
            @NotNull JavaPlugin plugin,
            @NotNull QuestRepository quests,
            @NotNull QuestTaskRepository tasks,
            @NotNull JExLogger logger
    ) {
        this.plugin = plugin;
        this.quests = quests;
        this.tasks = tasks;
        this.logger = logger;
    }

    /** Runs a full scan + upsert. Returns the number of quest definitions applied. */
    public int load() {
        final File root = new File(this.plugin.getDataFolder(), DEFINITIONS_DIR);
        if (!root.exists()) {
            this.logger.info("No quest definitions directory at {} — skipping load", root.getPath());
            return 0;
        }
        int applied = 0;
        for (final Path path : ContentLoaderSupport.yamlFiles(root)) {
            try {
                if (loadOne(path)) applied++;
            } catch (final RuntimeException ex) {
                this.logger.error("quest definition failed at {}: {}", path, ex.getMessage());
            }
        }
        this.logger.info("Quest definitions loaded: {}", applied);
        return applied;
    }

    private boolean loadOne(@NotNull Path path) {
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(path.toFile());

        final String identifier = cfg.getString("identifier");
        final String category = cfg.getString("category", deriveCategory(path));
        final String displayName = cfg.getString("displayName", cfg.getString("display_name", identifier));
        if (identifier == null || identifier.isBlank()) {
            this.logger.warn("quest {} missing identifier — skipped", path);
            return false;
        }

        final QuestDifficulty difficulty = parseDifficulty(cfg.getString("difficulty"));
        final Optional<Quest> existing = this.quests.findByIdentifier(identifier);
        final Quest quest = existing.orElseGet(() -> new Quest(identifier, category, displayName, difficulty));
        quest.setCategory(category);
        quest.setDisplayName(displayName);
        quest.setDifficulty(difficulty);
        quest.setRepeatable(cfg.getBoolean("repeatable", false));
        quest.setMaxCompletions(cfg.getInt("maxCompletions", 0));
        quest.setCooldownSeconds(cfg.getLong("cooldownSeconds", 0L));
        quest.setTimeLimitSeconds(cfg.getLong("timeLimitSeconds", 0L));
        quest.setEnabled(cfg.getBoolean("enabled", true));
        quest.setIconData(ContentLoaderSupport.sectionToJson(cfg.getConfigurationSection("icon")));
        quest.setRequirementData(ContentLoaderSupport.sectionToJson(cfg.getConfigurationSection("requirement")));
        quest.setRewardData(ContentLoaderSupport.sectionToJson(cfg.getConfigurationSection("reward")));
        quest.getPrerequisites().clear();
        quest.getPrerequisites().addAll(cfg.getStringList("prerequisites"));
        quest.getDependents().clear();
        quest.getDependents().addAll(cfg.getStringList("dependents"));

        final Quest persisted = existing.isPresent() ? this.quests.update(quest) : this.quests.create(quest);
        applyTasks(persisted, cfg);
        return true;
    }

    private void applyTasks(@NotNull Quest quest, @NotNull YamlConfiguration cfg) {
        final List<?> rawTasks = cfg.getList("tasks");
        if (rawTasks == null || rawTasks.isEmpty()) return;

        int orderFallback = 0;
        for (final Object raw : rawTasks) {
            if (!(raw instanceof java.util.Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            final java.util.Map<String, Object> map = (java.util.Map<String, Object>) rawMap;

            final String taskId = stringOrNull(map.get("identifier"));
            if (taskId == null || taskId.isBlank()) continue;

            final String displayName = stringOr(map.get("displayName"),
                    stringOr(map.get("display_name"), taskId));
            final int orderIndex = intOr(map.get("orderIndex"), orderFallback++);
            final QuestDifficulty difficulty = parseDifficulty(stringOrNull(map.get("difficulty")));

            // Upsert by (quest, taskIdentifier). QuestTask lacks a
            // uniqueness constraint (legacy), so without this check
            // every reload would create a duplicate row and the
            // progression listener would trip over a stale copy.
            final java.util.Optional<QuestTask> existing =
                    this.tasks.findByQuestAndTaskIdentifier(quest, taskId);
            final QuestTask task = existing.orElseGet(() ->
                    new QuestTask(quest, taskId, displayName, orderIndex));
            task.setDisplayName(displayName);
            task.setOrderIndex(orderIndex);
            task.setDifficulty(difficulty);
            task.setSequential(boolOr(map.get("sequential"), false));
            // Serialise nested maps directly — the Bukkit YamlConfiguration
            // round-trip used to flatten them into opaque Map values so
            // getConfigurationSection returned null and the objective /
            // requirement / reward JSON was always null in the DB. That's
            // why the progression listener logged "no objective data or
            // failed to decode" for every task.
            task.setIconData(ContentLoaderSupport.mapToJson(map.get("icon")));
            task.setRequirementData(ContentLoaderSupport.mapToJson(map.get("requirement")));
            task.setRewardData(ContentLoaderSupport.mapToJson(map.get("reward")));
            task.setObjectiveData(ContentLoaderSupport.mapToJson(map.get("objective")));

            if (existing.isPresent()) this.tasks.update(task);
            else this.tasks.create(task);
        }
    }

    private static @org.jetbrains.annotations.Nullable String stringOrNull(@org.jetbrains.annotations.Nullable Object raw) {
        return raw != null ? raw.toString() : null;
    }

    private static @NotNull String stringOr(@org.jetbrains.annotations.Nullable Object raw, @NotNull String fallback) {
        return raw != null ? raw.toString() : fallback;
    }

    private static int intOr(@org.jetbrains.annotations.Nullable Object raw, int fallback) {
        if (raw instanceof Number n) return n.intValue();
        if (raw != null) {
            try { return Integer.parseInt(raw.toString()); } catch (final NumberFormatException ignored) { /* fall through */ }
        }
        return fallback;
    }

    private static boolean boolOr(@org.jetbrains.annotations.Nullable Object raw, boolean fallback) {
        if (raw instanceof Boolean b) return b;
        if (raw != null) return Boolean.parseBoolean(raw.toString());
        return fallback;
    }

    private static @NotNull String deriveCategory(@NotNull Path path) {
        final Path parent = path.getParent();
        return parent != null ? parent.getFileName().toString() : "misc";
    }

    private static @NotNull QuestDifficulty parseDifficulty(String raw) {
        if (raw == null) return QuestDifficulty.MEDIUM;
        try {
            return QuestDifficulty.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return QuestDifficulty.MEDIUM;
        }
    }
}
