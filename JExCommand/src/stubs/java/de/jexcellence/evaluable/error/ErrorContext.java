package de.jexcellence.evaluable.error;

/**
 * Data carrier used by the command framework when resolving localized error
 * messages. The implementation mirrors the shape used in production while
 * keeping behaviour intentionally minimal for the unit tests.
 */
public class ErrorContext {

    private final Object sender;
    private final String alias;
    private final String[] args;
    private final Integer argumentIndex;

    public ErrorContext(Object sender, String alias, String[] args, Integer argumentIndex) {
        this.sender = sender;
        this.alias = alias;
        this.args = args;
        this.argumentIndex = argumentIndex;
    }

    public Object getSender() {
        return sender;
    }

    public String getAlias() {
        return alias;
    }

    public String[] getArgs() {
        return args;
    }

    public Integer getArgumentIndex() {
        return argumentIndex;
    }
}
