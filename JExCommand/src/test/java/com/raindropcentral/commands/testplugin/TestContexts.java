package com.raindropcentral.commands.testplugin;

/**
 * Context objects forwarded to commands and listeners during tests.
 */
public final class TestContexts {

    private TestContexts() {
    }

    public static class BaseContext {
    }

    public static class ConcreteContext extends BaseContext {
    }
}
