package com.raindropcentral.tests.misconfigured;

import com.raindropcentral.commands.testplugin.command.TestCommandSection;

public class MissingConstructorCommandSection extends TestCommandSection {
    public MissingConstructorCommandSection() {
        super("contextawarecommand");
    }
}
