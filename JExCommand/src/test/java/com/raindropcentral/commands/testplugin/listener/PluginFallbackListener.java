package com.raindropcentral.commands.testplugin.listener;

import com.raindropcentral.commands.testplugin.TestPlugin;
import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicInteger;

public class PluginFallbackListener implements Listener {

    public static final AtomicInteger INSTANCES = new AtomicInteger();

    public PluginFallbackListener(TestPlugin plugin) {
        INSTANCES.incrementAndGet();
    }
}
