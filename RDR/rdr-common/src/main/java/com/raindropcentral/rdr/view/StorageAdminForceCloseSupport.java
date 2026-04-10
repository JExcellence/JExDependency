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

import com.raindropcentral.rdr.RDR;
import me.devnatan.inventoryframework.ViewFrame;
import me.devnatan.inventoryframework.Viewer;
import me.devnatan.inventoryframework.context.IFRenderContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Helpers for force-closing RDR views from storage admin tooling.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class StorageAdminForceCloseSupport {

    private StorageAdminForceCloseSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static int closeAllRdrViews(
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

    public static int closeStorageViews(
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
