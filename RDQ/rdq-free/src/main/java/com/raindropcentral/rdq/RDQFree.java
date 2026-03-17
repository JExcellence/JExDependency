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

package com.raindropcentral.rdq;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for RDQ Free Edition.
 *
 * <p>This class serves as the entry point for the Bukkit plugin system and delegates all
 * functionality to {@link RDQFreeImpl}. The delegate handles the staged enable pipeline:
 * asynchronous platform and executor preparation (stage 1), component and view wiring (stage 2),
 * and repository hydration (stage 3) that provides database-backed services for commands,
 * views, and cross-plugin integrations.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class RDQFree extends JavaPlugin {

    private RDQFreeImpl impl;

    /**
     * Executes onLoad.
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RDQFree.class);
            impl = new RDQFreeImpl(this);
            impl.onLoad();
        } catch (final Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to load RDQ", exception);
            impl = null;
        }
    }

    /**
     * Executes onEnable.
     */
    @Override
    public void onEnable() {
        if (impl != null) {
            impl.onEnable();
        } else {
            getLogger().log(Level.SEVERE, "Cannot enable - RDQ failed to load");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Shuts down the free delegate when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (impl != null) {
            impl.onDisable();
        }
    }

    /**
     * Gets impl.
     */
    public RDQFreeImpl getImpl() {
        return impl;
    }
}
