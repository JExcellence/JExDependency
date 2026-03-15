package com.raindropcentral.rplatform.requirement.config;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Adapter interface for converting config sections to AbstractRequirement instances.
 *
 * <p>Plugins can implement this interface to provide custom adapters for their
 * requirement config section types.
 *
 * @param <T> the config section type this adapter handles
 */
@FunctionalInterface
public interface RequirementSectionAdapter<T> {

    /**
     * Converts a config section to an AbstractRequirement.
     *
     * @param section the config section to convert
     * @param context optional context data for conversion
     * @return the created requirement, or null if conversion fails
     */
    @Nullable
    AbstractRequirement convert(@NotNull T section, @Nullable Map<String, Object> context);
}
