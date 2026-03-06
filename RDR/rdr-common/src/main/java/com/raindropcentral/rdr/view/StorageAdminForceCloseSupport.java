package com.raindropcentral.rdr.view;

import java.util.Map;

import com.raindropcentral.rdr.RDR;
import me.devnatan.inventoryframework.ViewFrame;
import me.devnatan.inventoryframework.Viewer;
import me.devnatan.inventoryframework.context.IFRenderContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Helpers for force-closing RDR views from storage admin tooling.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
final class StorageAdminForceCloseSupport {

    private StorageAdminForceCloseSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    static int closeAllRdrViews(
        final @NotNull RDR plugin
    ) {
        final ViewFrame viewFrame = plugin.getViewFrame();
        if (viewFrame == null) {
            return 0;
        }

        int closedViews = 0;
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            final Viewer viewer = viewFrame.getViewer(onlinePlayer);
            if (viewer == null || !isRdrView(viewer.getCurrentContext())) {
                continue;
            }
            viewer.close();
            closedViews++;
        }
        return closedViews;
    }

    static int closeStorageViews(
        final @NotNull RDR plugin,
        final long storageId
    ) {
        final ViewFrame viewFrame = plugin.getViewFrame();
        if (viewFrame == null) {
            return 0;
        }

        int closedViews = 0;
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            final Viewer viewer = viewFrame.getViewer(onlinePlayer);
            if (viewer == null || !isViewerOnStorage(viewer.getCurrentContext(), storageId)) {
                continue;
            }
            viewer.close();
            closedViews++;
        }
        return closedViews;
    }

    private static boolean isViewerOnStorage(
        final IFRenderContext context,
        final long storageId
    ) {
        if (!isRdrView(context)) {
            return false;
        }

        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return false;
        }

        final Object storageIdValue = data.get("storage_id");
        return storageIdValue instanceof Number number && number.longValue() == storageId;
    }

    private static boolean isRdrView(
        final IFRenderContext context
    ) {
        if (context == null || context.getRoot() == null) {
            return false;
        }

        return context.getRoot().getClass().getPackageName()
            .startsWith("com.raindropcentral.rdr.view");
    }
}
