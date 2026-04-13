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

package com.raindropcentral.rda.command.player.ra;

import com.raindropcentral.commands.permission.PermissionParentProvider;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapped command section for the {@code /ra} player command.
 *
 * @author Codex
 * @since 1.0.0
 * @version 1.0.0
 */
public final class PRASection extends ACommandSection implements PermissionParentProvider {

    private static final String COMMAND_NAME = "pra";

    @CSAlways
    private Map<String, Object> permissionParents;

    /**
     * Creates the command section mapping for {@code /ra}.
     *
     * @param environmentBuilder evaluable environment builder
     */
    public PRASection(final @NotNull EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }

    /**
     * Returns the configured permission parent graph normalized to string lists.
     *
     * @return normalized permission parents
     */
    @Override
    public @NotNull Map<String, List<String>> getPermissionParents() {
        if (this.permissionParents == null) {
            return Map.of();
        }

        final Map<String, List<String>> normalizedParents = new HashMap<>();
        for (final Map.Entry<String, Object> entry : this.permissionParents.entrySet()) {
            if (entry.getValue() instanceof List<?> listValue) {
                final List<String> normalizedChildren = new ArrayList<>();
                for (final Object childValue : listValue) {
                    if (childValue instanceof String stringChild) {
                        normalizedChildren.add(stringChild);
                    }
                }
                normalizedParents.put(entry.getKey(), normalizedChildren);
                continue;
            }

            if (entry.getValue() instanceof String stringValue) {
                normalizedParents.put(entry.getKey(), Collections.singletonList(stringValue));
            }
        }

        return normalizedParents;
    }
}
