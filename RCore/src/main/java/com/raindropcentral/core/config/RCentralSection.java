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
