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

public class PRQSection extends ACommandSection implements PermissionParentProvider {

    private static final String COMMAND_NAME = "prq";

    @CSAlways
    private Map<String, Object> permissionParents;

    public PRQSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }

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
