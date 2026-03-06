package com.raindropcentral.rds.view.shop;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.service.config.ShopRequirementConfigSupport;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;

/**
 * Renders the admin job-plugin requirement integration view.
 *
 * <p>This view lists supported job plugins, shows whether each plugin is currently detected, and
 * allows admins to insert a default PLUGIN requirement into all configured shop purchase tiers.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopJobsView extends APaginatedView<Map<String, Object>> {

    private static final Logger LOGGER = Logger.getLogger(ShopJobsView.class.getName());
    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";
    private static final String DEFAULT_JOB_ID = "miner";
    private static final double DEFAULT_REQUIRED_LEVEL = 10.0D;
    private static final double DEFAULT_LEVEL_INCREMENT = 5.0D;
    private static final int DEFAULT_TIER_COUNT = 10;

    private static final List<SupportedJobPluginDefinition> SUPPORTED_JOB_PLUGINS = List.of(
            new SupportedJobPluginDefinition(
                    "ecojobs",
                    "EcoJobs",
                    List.of("EcoJobs"),
                    List.of(
                            "com.willfp.ecojobs.api.EcoJobsAPI",
                            "com.willfp.ecojobs.api.EcoJobsApi"
                    ),
                    DEFAULT_JOB_ID,
                    DEFAULT_REQUIRED_LEVEL,
                    Material.DIAMOND_PICKAXE
            ),
            new SupportedJobPluginDefinition(
                    "jobsreborn",
                    "Jobs",
                    List.of("Jobs", "JobsReborn"),
                    List.of("com.gamingmesh.jobs.Jobs"),
                    DEFAULT_JOB_ID,
                    DEFAULT_REQUIRED_LEVEL,
                    Material.GOLDEN_PICKAXE
            )
    );

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the admin job-plugin integration view.
     */
    public ShopJobsView() {
        super(PluginIntegrationManagementView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_admin_jobs_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "XXXSXXXXX",
                " OOOOOOO ",
                " OOOOOOO ",
                "XXXXXXXXX",
                "   <p>   "
        };
    }

    /**
     * Resolves paginated job-plugin entries.
     *
     * @param context active menu context
     * @return async list of supported job plugins
     */
    @Override
    protected @NotNull java.util.concurrent.CompletableFuture<List<Map<String, Object>>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        return java.util.concurrent.CompletableFuture.completedFuture(this.detectJobPlugins());
    }

    /**
     * Renders a single job-plugin entry.
     *
     * @param context menu context
     * @param builder item component builder
     * @param index rendered index
     * @param entry entry payload
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull Map<String, Object> entry
    ) {
        final Player player = context.getPlayer();
        final boolean detected = this.readBoolean(entry, "detected");
        final String displayName = this.readString(entry, "displayName");
        final String integrationId = this.readString(entry, "integrationId");
        final String defaultJobId = this.readString(entry, "defaultJobId");
        final double requiredLevel = this.readDouble(entry, "requiredLevel");
        final String levelRange = this.readString(entry, "levelRange");
        final Material iconType = this.readMaterial(entry, "iconType");

        final String status = this.i18n(
                        detected ? "plugin_entry.status.detected" : "plugin_entry.status.missing",
                        player
                )
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
        final String nameKey = detected
                ? "plugin_entry.detected.name"
                : "plugin_entry.missing.name";

        builder.withItem(
                        UnifiedBuilderFactory.item(detected ? iconType : Material.BARRIER)
                                .setName(this.i18n(nameKey, player)
                                        .withPlaceholder("plugin_name", displayName)
                                        .build()
                                        .component())
                                .setLore(this.i18n("plugin_entry.lore", player)
                                        .withPlaceholders(Map.of(
                                                "plugin_name", displayName,
                                                "integration_id", integrationId,
                                                "detection_status", status,
                                                "default_job", defaultJobId,
                                                "required_level", requiredLevel,
                                                "level_range", levelRange
                                        ))
                                        .build()
                                        .children())
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build()
                )
                .onClick(clickContext -> this.handlePluginClick(clickContext, entry));
    }

    /**
     * Renders static controls for the paginated job-plugin view.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        if (!this.hasAdminAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        final RDS plugin = this.rds.get(render);
        if (!plugin.canChangeConfigs()) {
            render.slot(22).renderWith(() -> this.createConfigLockedItem(player));
            return;
        }

        final List<Map<String, Object>> detectedPlugins = this.detectJobPlugins();
        final int supportedCount = detectedPlugins.size();
        final int detectedCount = this.countDetectedEntries(detectedPlugins);

        final Pagination pagination = this.getPagination(render);
        final int entryCount = pagination.source() == null ? 0 : pagination.source().size();

        render.layoutSlot('X', this.createDecorationItem(player));
        render.layoutSlot('S', this.createSummaryItem(player, supportedCount, detectedCount));

        if (pagination.source() != null && entryCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context for the current inventory interaction
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private @NotNull List<Map<String, Object>> detectJobPlugins() {
        final List<Map<String, Object>> plugins = new ArrayList<>();
        for (final SupportedJobPluginDefinition definition : SUPPORTED_JOB_PLUGINS) {
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("integrationId", definition.integrationId());
            entry.put("displayName", definition.displayName());
            entry.put("detected", isJobPluginDetected(definition));
            entry.put("defaultJobId", definition.defaultJobId());
            entry.put("requiredLevel", definition.requiredLevel());
            entry.put(
                    "levelRange",
                    formatLevelRange(definition.requiredLevel(), DEFAULT_LEVEL_INCREMENT, DEFAULT_TIER_COUNT)
            );
            entry.put("iconType", definition.iconType());
            plugins.add(entry);
        }

        plugins.sort(Comparator.comparing(
                entry -> this.readString(entry, "displayName"),
                String.CASE_INSENSITIVE_ORDER
        ));
        return plugins;
    }

    private void handlePluginClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull Map<String, Object> entry
    ) {
        clickContext.setCancelled(true);

        if (!this.hasAdminAccess(clickContext.getPlayer())) {
            this.i18n("feedback.locked_message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDS plugin = this.rds.get(clickContext);
        if (!plugin.canChangeConfigs()) {
            this.i18n("feedback.config_locked_message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final String integrationId = this.readString(entry, "integrationId");
        final String displayName = this.readString(entry, "displayName");
        final String defaultJobId = this.readString(entry, "defaultJobId");
        final double requiredLevel = this.readDouble(entry, "requiredLevel");
        final String levelRange = this.readString(entry, "levelRange");

        final File configFile = this.getConfigFile(plugin);
        try {
            final int updatedTiers = ShopRequirementConfigSupport.applyDefaultJobRequirement(
                    configFile,
                    integrationId,
                    defaultJobId,
                    requiredLevel,
                    DEFAULT_LEVEL_INCREMENT
            );
            this.i18n("feedback.applied", clickContext.getPlayer())
                    .withPlaceholders(Map.of(
                            "plugin_name", displayName,
                            "integration_id", integrationId,
                            "default_job", defaultJobId,
                            "required_level", requiredLevel,
                            "level_range", levelRange,
                            "updated_tiers", updatedTiers
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.i18n("feedback.restart_required", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
        } catch (final IllegalArgumentException | IllegalStateException exception) {
            LOGGER.log(Level.WARNING, "Failed to apply default job requirements.", exception);
            this.i18n("feedback.apply_failed", clickContext.getPlayer())
                    .withPlaceholder("plugin_name", displayName)
                    .includePrefix()
                    .build()
                    .sendMessage();
        }
    }

    private @NotNull File getConfigFile(
            final @NotNull RDS plugin
    ) {
        return new File(new File(plugin.getDataFolder(), "config"), "config.yml");
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final int supportedCount,
            final int detectedCount
    ) {
        return UnifiedBuilderFactory.item(Material.DIAMOND_PICKAXE)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "supported_count", supportedCount,
                                "detected_count", detectedCount
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createDecorationItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(this.i18n("decoration.border.name", player).build().component())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEmptyItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.empty.name", player).build().component())
                .setLore(this.i18n("feedback.empty.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.locked.name", player).build().component())
                .setLore(this.i18n("feedback.locked.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createConfigLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.config_locked.name", player).build().component())
                .setLore(this.i18n("feedback.config_locked.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private int countDetectedEntries(
            final @NotNull List<Map<String, Object>> entries
    ) {
        int detectedCount = 0;
        for (final Map<String, Object> entry : entries) {
            if (this.readBoolean(entry, "detected")) {
                detectedCount++;
            }
        }
        return detectedCount;
    }

    private boolean hasAdminAccess(
            final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }

    private static boolean isJobPluginDetected(
            final @NotNull SupportedJobPluginDefinition definition
    ) {
        if (isPluginDetected(definition.detectedPluginNames())) {
            return true;
        }

        for (final String className : definition.classProbeNames()) {
            if (isClassPresent(className)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPluginDetected(
            final @NotNull List<String> pluginNames
    ) {
        final Plugin[] installedPlugins = Bukkit.getPluginManager().getPlugins();
        for (final Plugin installedPlugin : installedPlugins) {
            final String installedName = normalizePluginKey(installedPlugin.getName());
            if (installedName.isEmpty() || !installedPlugin.isEnabled()) {
                continue;
            }

            for (final String pluginName : pluginNames) {
                if (installedName.equals(normalizePluginKey(pluginName))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @NotNull String normalizePluginKey(
            final @NotNull String pluginName
    ) {
        final String normalized = pluginName
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
        if (normalized.isEmpty()) {
            return "";
        }

        for (final String jobsAlias : List.of("jobs", "jobsreborn")) {
            if (normalized.equals(jobsAlias)) {
                return "jobsreborn";
            }
        }

        for (final String ecoJobsAlias : List.of("ecojobs", "ecojobsplugin")) {
            if (normalized.equals(ecoJobsAlias)) {
                return "ecojobs";
            }
        }

        return normalized;
    }

    private static boolean isClassPresent(
            final @NotNull String className
    ) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static @NotNull String formatLevelRange(
            final double baseLevel,
            final double levelIncrement,
            final int tierCount
    ) {
        final int normalizedTierCount = Math.max(1, tierCount);
        final double maxLevel = baseLevel + (Math.max(0, normalizedTierCount - 1) * levelIncrement);
        return formatLevel(baseLevel) + " -> " + formatLevel(maxLevel);
    }

    private static @NotNull String formatLevel(
            final double value
    ) {
        if (Math.rint(value) == value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private @NotNull String readString(
            final @NotNull Map<String, Object> source,
            final @NotNull String key
    ) {
        final Object value = source.get(key);
        return value == null ? "" : value.toString();
    }

    private boolean readBoolean(
            final @NotNull Map<String, Object> source,
            final @NotNull String key
    ) {
        final Object value = source.get(key);
        return value instanceof Boolean bool && bool;
    }

    private double readDouble(
            final @NotNull Map<String, Object> source,
            final @NotNull String key
    ) {
        final Object value = source.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return DEFAULT_REQUIRED_LEVEL;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (final NumberFormatException ignored) {
            return DEFAULT_REQUIRED_LEVEL;
        }
    }

    private @NotNull Material readMaterial(
            final @NotNull Map<String, Object> source,
            final @NotNull String key
    ) {
        final Object value = source.get(key);
        if (value instanceof Material material) {
            return material;
        }
        if (value == null) {
            return Material.IRON_PICKAXE;
        }
        try {
            return Material.valueOf(value.toString().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return Material.IRON_PICKAXE;
        }
    }

    private record SupportedJobPluginDefinition(
            @NotNull String integrationId,
            @NotNull String displayName,
            @NotNull List<String> detectedPluginNames,
            @NotNull List<String> classProbeNames,
            @NotNull String defaultJobId,
            double requiredLevel,
            @NotNull Material iconType
    ) {
    }
}
