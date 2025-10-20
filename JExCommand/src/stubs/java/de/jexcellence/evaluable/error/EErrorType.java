package de.jexcellence.evaluable.error;

/**
 * Enumeration mirroring the values referenced by the command framework when
 * mapping errors back to localized messages.
 */
public enum EErrorType {
    MALFORMED_DOUBLE,
    MALFORMED_FLOAT,
    MALFORMED_LONG,
    MALFORMED_INTEGER,
    MALFORMED_UUID,
    MALFORMED_ENUM,
    MISSING_ARGUMENT,
    NOT_A_PLAYER,
    NOT_A_CONSOLE,
    PLAYER_UNKNOWN,
    PLAYER_NOT_ONLINE
}
