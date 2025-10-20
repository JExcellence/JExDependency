package com.raindropcentral.commands.testplugin.command;

import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.commands.testplugin.TestContexts;
import org.bukkit.command.CommandSender;

import java.util.concurrent.atomic.AtomicInteger;

@Command
public class ContextAwareCommand extends BaseTestCommand {

    private static final AtomicInteger INSTANCES = new AtomicInteger();
    private static final AtomicInteger CONSTRUCTOR_WITH_CONTEXT = new AtomicInteger();

    public ContextAwareCommand(ContextAwareCommandSection section, TestContexts.ConcreteContext context) {
        super(section);
        INSTANCES.incrementAndGet();
        if (context != null) {
            CONSTRUCTOR_WITH_CONTEXT.incrementAndGet();
        }
    }

    @Override
    protected void onInvocation(CommandSender sender, String alias, String[] args) {
    }

    public static void reset() {
        INSTANCES.set(0);
        CONSTRUCTOR_WITH_CONTEXT.set(0);
    }

    public static int instances() {
        return INSTANCES.get();
    }
}
