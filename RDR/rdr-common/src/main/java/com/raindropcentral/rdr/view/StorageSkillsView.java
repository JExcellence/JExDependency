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

package com.raindropcentral.rdr.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
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

/**
 * Renders the admin skill-plugin requirement integration view for storage purchases.
 *
 * <p>This view lists supported skill plugins, shows whether each plugin is currently detected, and
 * allows admins to insert default PLUGIN requirements into all ten default storage purchase tiers.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageSkillsView extends APaginatedView<Map<String, Object>> {

    private static final Logger LOGGER = Logger.getLogger(StorageSkillsView.class.getName());
    private static final String ADMIN_COMMAND_PERMISSION = "raindroprdr.command.admin";
    private static final String DEFAULT_SKILL_ID = "mining";
    private static final double DEFAULT_REQUIRED_LEVEL = 10.0D;
    private static final double DEFAULT_LEVEL_INCREMENT = 5.0D;
    private static final int DEFAULT_TIER_COUNT = 10;

    private static final List<SupportedSkillPluginDefinition> SUPPORTED_SKILL_PLUGINS = List.of(
        new SupportedSkillPluginDefinition(
            "ecoskills",
            "EcoSkills",
            List.of("EcoSkills"),
            List.of(
                "com.willfp.ecoskills.api.EcoSkillsAPI",
                "com.willfp.ecoskills.api.EcoSkillsApi"
            ),
            DEFAULT_SKILL_ID,
            DEFAULT_REQUIRED_LEVEL,
            Material.EMERALD
        ),
        new SupportedSkillPluginDefinition(
            "auraskills",
            "AuraSkills",
            List.of("AuraSkills"),
            List.of("dev.aurelium.auraskills.api.AuraSkillsApi"),
            DEFAULT_SKILL_ID,
            DEFAULT_REQUIRED_LEVEL,
            Material.AMETHYST_SHARD
        ),
        new SupportedSkillPluginDefinition(
            "mcmmo",
            "mcMMO",
            List.of("mcMMO", "McMMO"),
            List.of("com.gmail.nossr50.api.ExperienceAPI"),
            DEFAULT_SKILL_ID,
            DEFAULT_REQUIRED_LEVEL,
            Material.IRON_PICKAXE
        )
    );

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the admin skill-plugin integration view.
     */
    public StorageSkillsView() {
        super(PluginIntegrationManagementView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_skills_ui";
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
     * Resolves paginated skill-plugin entries.
     *
     * @param context active menu context
     * @return async list of supported skill plugins
     */
    @Override
    protected @NotNull CompletableFuture<List<Map<String, Object>>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        return CompletableFuture.completedFuture(this.detectSkillPlugins());
    }

    /**
     * Renders a single skill-plugin entry.
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
        final String defaultSkillId = this.readString(entry, "defaultSkillId");
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
                            "default_skill", defaultSkillId,
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
     * Renders static controls for the paginated skill-plugin view.
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

        final RDR plugin = this.rdr.get(render);
        if (!plugin.canChangeStorageSettings()) {
            render.slot(22).renderWith(() -> this.createConfigLockedItem(player));
            return;
        }

        final List<Map<String, Object>> detectedPlugins = this.detectSkillPlugins();
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

    private @NotNull List<Map<String, Object>> detectSkillPlugins() {
        final List<Map<String, Object>> plugins = new ArrayList<>();
        for (final SupportedSkillPluginDefinition definition : SUPPORTED_SKILL_PLUGINS) {
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("integrationId", definition.integrationId());
            entry.put("displayName", definition.displayName());
            entry.put("detected", isSkillPluginDetected(definition));
            entry.put("defaultSkillId", definition.defaultSkillId());
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

        final RDR plugin = this.rdr.get(clickContext);
        if (!plugin.canChangeStorageSettings()) {
            this.i18n("feedback.config_locked_message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final String integrationId = this.readString(entry, "integrationId");
        final String displayName = this.readString(entry, "displayName");
        final String defaultSkillId = this.readString(entry, "defaultSkillId");
        final double requiredLevel = this.readDouble(entry, "requiredLevel");
        final String levelRange = this.readString(entry, "levelRange");

        final File configFile = this.getConfigFile(plugin);
        try {
            final int updatedTiers = StorageRequirementConfigSupport.applyDefaultSkillRequirement(
                configFile,
                integrationId,
                defaultSkillId,
                requiredLevel,
                DEFAULT_LEVEL_INCREMENT
            );
            this.i18n("feedback.applied", clickContext.getPlayer())
                .withPlaceholders(Map.of(
                    "plugin_name", displayName,
                    "integration_id", integrationId,
                    "default_skill", defaultSkillId,
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
            LOGGER.log(Level.WARNING, "Failed to apply default skill requirements.", exception);
            this.i18n("feedback.apply_failed", clickContext.getPlayer())
                .withPlaceholder("plugin_name", displayName)
                .includePrefix()
                .build()
                .sendMessage();
        }
    }

    private @NotNull File getConfigFile(
        final @NotNull RDR plugin
    ) {
        return new File(new File(plugin.getPlugin().getDataFolder(), "config"), "config.yml");
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int supportedCount,
        final int detectedCount
    ) {
        return UnifiedBuilderFactory.item(Material.ENCHANTED_BOOK)
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

    private static boolean isSkillPluginDetected(
        final @NotNull SupportedSkillPluginDefinition definition
    ) {
        if (isPluginDetected(definition.detectedPluginNames())) {
            return true;
        }

        for (final String className : definition.classProbeNames()) {
            if (isClassPresent(className)) {
                return true;
            }
        }

        return "auraskills".equals(definition.integrationId()) && isAuraSkillsApiLoaded();
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

        for (final String pluginName : pluginNames) {
            if ("auraskills".equals(normalizePluginKey(pluginName)) && isAuraSkillsApiLoaded()) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAuraSkillsApiLoaded() {
        try {
            final Class<?> apiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
            final Object api = apiClass.getMethod("get").invoke(null);
            return api != null;
        } catch (final Exception | LinkageError ignored) {
            return false;
        }
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

        for (final String auraAlias : List.of("auraskills", "aureliumskills")) {
            if (normalized.equals(auraAlias)) {
                return "auraskills";
            }
        }
        return normalized;
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
            return Material.ENCHANTED_BOOK;
        }
        try {
            return Material.valueOf(value.toString().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return Material.ENCHANTED_BOOK;
        }
    }

    private record SupportedSkillPluginDefinition(
        @NotNull String integrationId,
        @NotNull String displayName,
        @NotNull List<String> detectedPluginNames,
        @NotNull List<String> classProbeNames,
        @NotNull String defaultSkillId,
        double requiredLevel,
        @NotNull Material iconType
    ) {
    }
}
