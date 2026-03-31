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

package com.raindropcentral.rdq.config.permissions;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration section for managing permissions in the admin view.
 *
 * <p>This section handles the default group and its associated permissions,
 * providing methods to retrieve permissions in a structured format.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class PermissionsSection extends AConfigSection {
    
    /**
     * The name of the default group.
     * If not set, defaults to "default".
     */
    private String defaultGroup;
    
    /**
     * The permissions associated with the default group.
     * The map's keys are permission names, and values are either a String or a List of Strings.
     */
    private Map<String, Object> defaultGroupPermissions;
    
    /**
     * Constructs a new PermissionsSection with the specified evaluation environment.
     *
     * @param baseEnvironment the evaluation environment builder to use for this section
     */
    public PermissionsSection(
        final EvaluationEnvironmentBuilder baseEnvironment
    ) {
        
        super(baseEnvironment);
    }
    
    /**
     * Retrieves the permissions for the default group in a normalized format.
 *
 * <p>The returned map contains permission names as keys and lists of permission values as values.
     * If a permission value is a single string, it is wrapped in a singleton list.
     * If a permission value is a list, only string elements are included.
     *
     * @return a map of permission names to lists of permission values, or an empty map if no permissions are set
     */
    public Map<String, List<String>> getPermissions() {
        
        Map<String, List<String>> result = new HashMap<>();
        
        if (
            defaultGroupPermissions == null
        )
            return Map.of();
        
        for (
            Map.Entry<String, Object> entry : defaultGroupPermissions.entrySet()
        ) {
            if (entry.getValue() instanceof List<?> list) {
                List<String> stringList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof String) {
                        stringList.add((String) item);
                    }
                }
                result.put(
                    entry.getKey(),
                    stringList
                );
            } else if (entry.getValue() instanceof String) {
                result.put(
                    entry.getKey(),
                    Collections.singletonList((String) entry.getValue())
                );
            }
        }
        return result;
    }
    
    /**
     * Returns the name of the default group.
     * If the default group is not set, returns "default".
     *
     * @return the default group name, or "default" if not set
     */
    public String getDefaultGroup() {
        
        return defaultGroup == null ?
               "default" :
               defaultGroup;
    }
    
}
