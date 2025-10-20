package com.raindropcentral.tests.misconfigured;

import org.bukkit.event.Listener;

/**
 * Listener without a compatible constructor so the factory logs a warning.
 */
public class MalformedConstructorListener implements Listener {

    public MalformedConstructorListener(String ignored) {
    }
}
