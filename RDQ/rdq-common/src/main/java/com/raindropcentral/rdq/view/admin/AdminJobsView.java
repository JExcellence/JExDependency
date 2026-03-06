package com.raindropcentral.rdq.view.admin;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

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

    private static final int[] ENTRY_COLUMNS = {2, 3};

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
            render.slot(2, ENTRY_COLUMNS[index]).withItem(this.createPluginItem(player, entries.get(index)));
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

        return UnifiedBuilderFactory.item(detected ? entry.iconType() : Material.BARRIER)
            .setName(this.i18n(nameKey, player)
                .withPlaceholder("plugin_name", entry.displayName())
                .build()
                .component())
            .setLore(this.i18n("plugin_entry.lore", player)
                .withPlaceholders(Map.of(
                    "plugin_name", entry.displayName(),
                    "integration_id", entry.integrationId(),
                    "detection_status", status
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
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
