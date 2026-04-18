package de.jexcellence.jexplatform.reward.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when a referenced reward type is not registered.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RewardNotFoundException extends RewardException {

    /** Creates an exception for the given unregistered type. */
    public RewardNotFoundException(@NotNull String typeName) {
        super("Reward type not found: " + typeName, typeName, null);
    }
}
