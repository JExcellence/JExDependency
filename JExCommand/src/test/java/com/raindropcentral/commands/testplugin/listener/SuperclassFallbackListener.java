package com.raindropcentral.commands.testplugin.listener;

import com.raindropcentral.commands.testplugin.TestPluginBase;
import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicInteger;

public class SuperclassFallbackListener implements Listener {

    public static final AtomicInteger INSTANCES = new AtomicInteger();

    public SuperclassFallbackListener(TestPluginBase plugin) {
        INSTANCES.incrementAndGet();
    }
}
