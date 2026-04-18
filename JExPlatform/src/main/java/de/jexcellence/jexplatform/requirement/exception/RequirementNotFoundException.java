package de.jexcellence.jexplatform.requirement.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when a referenced requirement type is not registered.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RequirementNotFoundException extends RequirementException {

    /**
     * Creates an exception for the given unregistered type.
     *
     * @param typeName the type that was not found
     */
    public RequirementNotFoundException(@NotNull String typeName) {
        super("Requirement type not found: " + typeName, typeName, null);
    }
}
