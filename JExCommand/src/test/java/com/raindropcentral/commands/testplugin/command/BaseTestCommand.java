package com.raindropcentral.commands.testplugin.command;

import com.raindropcentral.commands.BukkitCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class BaseTestCommand extends BukkitCommand {

    protected BaseTestCommand(TestCommandSection section) {
        super(section);
    }

    @Override
    protected List<String> onTabCompletion(CommandSender sender, String alias, String[] args) {
        return List.of();
    }
}
