package com.raindropcentral.commands.testplugin.command;

import com.raindropcentral.commands.testplugin.TestContexts;
import com.raindropcentral.commands.utility.Command;
import org.bukkit.command.CommandSender;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Command
public class SuperclassFallbackCommand extends BaseTestCommand {

    private static final AtomicInteger INSTANCES = new AtomicInteger();
    private static final AtomicReference<Object> LAST_DEPENDENCY = new AtomicReference<>();

    public SuperclassFallbackCommand(SuperclassFallbackCommandSection section, TestContexts.BaseContext context) {
        super(section);
        INSTANCES.incrementAndGet();
        LAST_DEPENDENCY.set(context);
    }

    @Override
    protected void onInvocation(CommandSender sender, String alias, String[] args) {
    }

    public static void reset() {
        INSTANCES.set(0);
        LAST_DEPENDENCY.set(null);
    }

    public static int instances() {
        return INSTANCES.get();
    }

    public static Object lastDependency() {
        return LAST_DEPENDENCY.get();
    }
}
