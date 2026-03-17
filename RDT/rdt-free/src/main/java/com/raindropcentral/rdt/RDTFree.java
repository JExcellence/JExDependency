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

package com.raindropcentral.rdt;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for RDT Free Edition.
 *
 * <p>This Bukkit entry point performs dependency remapping and delegates runtime bootstrap work to
 * {@link RDTFreeImpl} so the shared RDT core can stay edition-agnostic.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RDTFree extends JavaPlugin {

    private RDTFreeImpl impl;

    /**
     * Loads the dependency remapper and initializes the free-edition delegate.
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RDTFree.class);
            this.impl = new RDTFreeImpl(this);
            this.impl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "Failed to load RDT Free", exception);
            this.impl = null;
        }
    }

    /**
     * Enables the plugin delegate or disables the plugin when bootstrap failed during load.
     */
    @Override
    public void onEnable() {
        if (this.impl != null) {
            this.impl.onEnable();
            return;
        }

        this.getLogger().severe("Cannot enable RDT Free because the delegate failed to load.");
        this.getServer().getPluginManager().disablePlugin(this);
    }

    /**
     * Shuts down the free delegate when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (this.impl != null) {
            this.impl.onDisable();
        }
    }
}
