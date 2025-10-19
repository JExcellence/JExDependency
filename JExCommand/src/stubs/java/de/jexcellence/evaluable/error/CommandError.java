package de.jexcellence.evaluable.error;

/**
 * Simplified exception capturing only the state referenced by the production
 * command framework.
 */
public class CommandError extends RuntimeException {

    public final EErrorType errorType;
    public final Integer argumentIndex;
    public final Object parameter;

    public CommandError(int argumentIndex, EErrorType errorType) {
        this(argumentIndex, errorType, null, null);
    }

    public CommandError(EErrorType errorType, Object parameter) {
        this(null, errorType, parameter, null);
    }

    public CommandError(int argumentIndex, EErrorType errorType, Object parameter) {
        this(argumentIndex, errorType, parameter, null);
    }

    public CommandError(Integer argumentIndex, EErrorType errorType, Object parameter, String message) {
        super(message);
        this.errorType = errorType;
        this.argumentIndex = argumentIndex;
        this.parameter = parameter;
    }
}
