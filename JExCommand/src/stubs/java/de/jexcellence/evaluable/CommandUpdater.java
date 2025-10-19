package de.jexcellence.evaluable;

/**
 * Lightweight test double for the production CommandUpdater type. The real
 * implementation lives in a proprietary module and is therefore replaced with
 * a simple shell that can be mocked during unit tests.
 */
public class CommandUpdater {

    private final Object plugin;

    public CommandUpdater(Object plugin) {
        this.plugin = plugin;
    }

    public Object getPlugin() {
        return this.plugin;
    }

    public void tryRegisterCommand(Object command) {
        // No-op in tests; behaviour verified via Mockito mocks
    }

    public void trySyncCommands() {
        // No-op in tests; behaviour verified via Mockito mocks
    }
}
