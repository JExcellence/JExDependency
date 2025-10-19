package com.raindropcentral.tests.misconfigured;

import com.raindropcentral.commands.testplugin.command.BaseTestCommand;
import com.raindropcentral.commands.utility.Command;
import org.bukkit.command.CommandSender;

@Command
public class MissingConstructorCommand extends BaseTestCommand {

    public MissingConstructorCommand(MissingConstructorCommandSection section, String unsupported) {
        super(section);
    }

    @Override
    protected void onInvocation(CommandSender sender, String alias, String[] args) {
    }
}
