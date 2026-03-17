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

package com.raindropcentral.rds;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.raindropcentral.rds.service.FreeShopService;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Delegate implementation for the RDS free edition.
 *
 * <p>This class owns the edition-specific bootstrap wiring and instantiates the shared
 * {@link RDS} runtime with the free-edition shop service.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RDSFreeImpl extends AbstractPluginDelegate<RDSFree> {

    private static final Logger LOGGER = Logger.getLogger(RDSFreeImpl.class.getName());
    private static final String EDITION = "Free";

    private @Nullable RDS rds;

    /**
     * Creates a new free-edition delegate.
     *
     * @param plugin owning Bukkit plugin
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public RDSFreeImpl(final @NotNull RDSFree plugin) {
        super(plugin);
    }

    /**
     * Creates the shared RDS runtime for the free edition.
     */
    @Override
    public void onLoad() {
        try {
            this.rds = new RDS(this.getPlugin(), EDITION, new FreeShopService());
            this.rds.onLoad();
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load RDS (" + EDITION + ")", exception);
            throw new RuntimeException(exception);
        }
    }

    /**
     * Enables the shared runtime or disables the Bukkit plugin when initialization failed.
     */
    @Override
    public void onEnable() {
        if (this.rds == null) {
            LOGGER.severe("Cannot enable RDS (" + EDITION + ") because initialization failed during onLoad.");
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
            return;
        }

        this.rds.onEnable();
    }

    /**
     * Shuts down the shared runtime.
     */
    @Override
    public void onDisable() {
        try {
            if (this.rds != null) {
                this.rds.onDisable();
            }
            LOGGER.info("RDS (" + EDITION + ") Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during RDS (" + EDITION + ") shutdown", exception);
        }
    }
}
