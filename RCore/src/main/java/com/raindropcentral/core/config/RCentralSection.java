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

package com.raindropcentral.core.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration section for RaindropCentral backend connection settings.
 */
@CSAlways
public class RCentralSection extends AConfigSection {

    private String backendUrl;
    private Boolean developmentMode;
    private Boolean autoDetect;

    /**
     * Executes RCentralSection.
     */
    public RCentralSection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Gets the backend URL. Returns null if not explicitly set or empty.
     */
    @Nullable
    public String getBackendUrl() {
        return backendUrl != null && !backendUrl.isEmpty() ? backendUrl : null;
    }

    /**
     * Checks if development mode is explicitly enabled.
     */
    public boolean isDevelopmentMode() {
        return developmentMode != null && developmentMode;
    }

    /**
     * Checks if auto-detection should be used.
     * Defaults to true if not specified.
     */
    public boolean isAutoDetect() {
        return autoDetect == null || autoDetect;
    }
}
