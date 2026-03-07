package com.raindropcentral.rdq.view.admin;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Displays job-plugin detection status for RDQ admin integrations.
 *
 * <p>This view reports support state for configured job integrations and helps administrators
 * verify whether required plugins are currently available on the server.</p>
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
public class AdminJobsView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

    private static final int[] ENTRY_COLUMNS = {2, 3};

    private final State<RDQ> rdq = initialState("plugin");

    /**
     * Creates the job-plugin detection view.
     */
    public AdminJobsView() {
        super(PluginIntegrationManagementView.class);
    }

    /**
     * Returns the translation key used by this view.
     *
     * @return i18n key root
     */
    @Override
    protected @NotNull String getKey() {
        return "admin_jobs_ui";
    }

    /**
     * Returns the inventory size for this view.
     *
     * @return inventory row count
     */
    @Override
    protected int getSize() {
        return 3;
    }

    /**
     * Renders job-plugin detection entries.
     *
     * @param render render context for this view
     * @param player player viewing the interface
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final List<AdminPluginIntegrationSupport.PluginDetectionEntry> entries =
            AdminPluginIntegrationSupport.detectJobPlugins(
                AdminPluginIntegrationSupport.resolveInstalledPlugins()
            );
        final int supportedCount = entries.size();
        final int detectedCount = AdminPluginIntegrationSupport.countDetectedEntries(entries);

        render.slot(1, 2).withItem(this.createSummaryItem(player, supportedCount, detectedCount));
        if (entries.isEmpty()) {
            render.slot(2, 2).withItem(this.createEmptyItem(player));
            return;
        }

        for (int index = 0; index < entries.size() && index < ENTRY_COLUMNS.length; index++) {
            final AdminPluginIntegrationSupport.PluginDetectionEntry entry = entries.get(index);
            render.slot(2, ENTRY_COLUMNS[index])
                .withItem(this.createPluginItem(player, entry))
                .onClick(clickContext -> this.handlePluginInjection(clickContext, entry));
        }
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

    private @NotNull ItemStack createPluginItem(
        final @NotNull Player player,
        final @NotNull AdminPluginIntegrationSupport.PluginDetectionEntry entry
    ) {
        final boolean detected = entry.detected();
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

        final List<Component> lore = new ArrayList<>(this.i18n("plugin_entry.lore", player)
            .withPlaceholders(Map.of(
                "plugin_name", entry.displayName(),
                "integration_id", entry.integrationId(),
                "detection_status", status
            ))
            .build()
            .children());
        if (detected) {
            lore.add(Component.empty());
            lore.add(this.i18n("plugin_entry.detected.click_action", player).build().component());
        }

        return UnifiedBuilderFactory.item(detected ? entry.iconType() : Material.BARRIER)
            .setName(this.i18n(nameKey, player)
                .withPlaceholder("plugin_name", entry.displayName())
                .build()
                .component())
            .setLore(lore)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void handlePluginInjection(
        final @NotNull SlotClickContext clickContext,
        final @NotNull AdminPluginIntegrationSupport.PluginDetectionEntry entry
    ) {
        final Player player = clickContext.getPlayer();

        if (!entry.detected()) {
            this.i18n("messages.plugin_missing", player)
                .withPlaceholder("plugin_name", entry.displayName())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final RDQ plugin = this.rdq.get(clickContext);
        if (plugin == null) {
            this.i18n("messages.inject_failed", player)
                .withPlaceholder("plugin_name", entry.displayName())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        try {
            final AdminIntegrationRequirementInjector.InjectionResult result =
                AdminIntegrationRequirementInjector.injectJobRequirements(plugin, entry.integrationId());

            if (result.hasChanges()) {
                this.i18n("messages.inject_success", player)
                    .withPlaceholders(Map.of(
                        "plugin_name", entry.displayName(),
                        "perk_count", result.perkRequirementsAdded(),
                        "rank_count", result.rankRequirementsAdded()
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();
            } else {
                this.i18n("messages.inject_no_changes", player)
                    .withPlaceholder("plugin_name", entry.displayName())
                    .includePrefix()
                    .build()
                    .sendMessage();
            }

            clickContext.openForPlayer(
                AdminJobsView.class,
                Map.of("plugin", plugin)
            );
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to inject job requirements for: " + entry.integrationId(), exception);
            this.i18n("messages.inject_failed", player)
                .withPlaceholder("plugin_name", entry.displayName())
                .includePrefix()
                .build()
                .sendMessage();
        }
    }

    private @NotNull ItemStack createEmptyItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
