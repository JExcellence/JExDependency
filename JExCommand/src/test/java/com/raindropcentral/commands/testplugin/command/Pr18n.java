package com.raindropcentral.commands.testplugin.command;

import com.raindropcentral.commands.testplugin.TestPluginBase;
import org.bukkit.command.CommandSender;

public class Pr18n extends BaseTestCommand {

    public Pr18n(Pr18nSection section, TestPluginBase plugin) {
        super(section);
    }

    @Override
    protected void onInvocation(CommandSender sender, String alias, String[] args) {
    }
}
