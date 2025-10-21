package com.raindropcentral.commands.testplugin.command;

import com.raindropcentral.commands.testplugin.TestPluginBase;
import com.raindropcentral.commands.utility.Command;
import org.bukkit.command.CommandSender;

import java.util.concurrent.atomic.AtomicInteger;

@Command
public class Pr18nCommand extends BaseTestCommand {

    private static final AtomicInteger INSTANCES = new AtomicInteger();

    public Pr18nCommand(Pr18nCommandSection section, TestPluginBase plugin) {
        super(section);
        INSTANCES.incrementAndGet();
    }

    @Override
    protected void onInvocation(CommandSender sender, String alias, String[] args) {
    }

    public static void reset() {
        INSTANCES.set(0);
    }

    public static int instances() {
        return INSTANCES.get();
    }
}
