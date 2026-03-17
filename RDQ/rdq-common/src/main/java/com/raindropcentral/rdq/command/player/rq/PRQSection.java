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

package com.raindropcentral.rdq.command.player.rq;

import com.raindropcentral.commands.permission.PermissionParentProvider;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the PRQSection API type.
 */
public class PRQSection extends ACommandSection implements PermissionParentProvider {

    private static final String COMMAND_NAME = "prq";

    @CSAlways
    private Map<String, Object> permissionParents;

    /**
     * Executes PRQSection.
     */
    public PRQSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }

    /**
     * Gets permissionParents.
     */
    @Override
    public @NotNull Map<String, List<String>> getPermissionParents() {
        if (this.permissionParents == null) {
            return Map.of();
        }

        final Map<String, List<String>> normalizedParents = new java.util.HashMap<>();

        for (Map.Entry<String, Object> entry : this.permissionParents.entrySet()) {
            if (entry.getValue() instanceof List<?> listValue) {
                final List<String> normalizedChildren = new ArrayList<>();

                for (Object childValue : listValue) {
                    if (childValue instanceof String stringChild) {
                        normalizedChildren.add(stringChild);
                    }
                }

                normalizedParents.put(
                    entry.getKey(),
                    normalizedChildren
                );
                continue;
            }

            if (entry.getValue() instanceof String stringValue) {
                normalizedParents.put(
                    entry.getKey(),
                    Collections.singletonList(stringValue)
                );
            }
        }

        return normalizedParents;
    }
}
