package com.raindropcentral.commands.testplugin.listener;

import com.raindropcentral.commands.testplugin.TestContexts;
import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicInteger;

public class ContextAwareListener implements Listener {

    public static final AtomicInteger INSTANCES = new AtomicInteger();

    public ContextAwareListener(TestContexts.ConcreteContext context) {
        INSTANCES.incrementAndGet();
    }
}
