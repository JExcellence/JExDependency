package com.raindropcentral.commands.testplugin.command;

import de.jexcellence.evaluable.EnumInfo;
import de.jexcellence.evaluable.error.ErrorContext;
import de.jexcellence.evaluable.section.ACommandSection;
import net.kyori.adventure.text.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class TestCommandSection extends ACommandSection {

    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;

    protected TestCommandSection(String configKey) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("commands/" + configKey + ".yml")) {
            Objects.requireNonNull(stream, "Missing YAML for " + configKey);
            Map<String, Object> yaml = new org.yaml.snakeyaml.Yaml().load(stream);
            this.name = Objects.toString(yaml.getOrDefault("name", configKey));
            this.description = Objects.toString(yaml.getOrDefault("description", configKey + " description"));
            this.usage = Objects.toString(yaml.getOrDefault("usage", "/" + configKey));
            Object aliasesValue = yaml.get("aliases");
            if (aliasesValue instanceof Collection<?> collection) {
                List<String> parsed = new ArrayList<>();
                for (Object alias : collection) {
                    parsed.add(String.valueOf(alias));
                }
                this.aliases = List.copyOf(parsed);
            } else {
                this.aliases = List.of();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load YAML for " + configKey, exception);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getUsage() {
        return this.usage;
    }

    @Override
    public List<String> getAliases() {
        return this.aliases;
    }

    private Component message(String key) {
        return Component.text(this.name + " " + key);
    }

    @Override
    public Component getInternalErrorMessage(ErrorContext context) {
        return message("internal-error");
    }

    @Override
    public Component getMalformedDoubleMessage(ErrorContext context) {
        return message("malformed-double");
    }

    @Override
    public Component getMalformedFloatMessage(ErrorContext context) {
        return message("malformed-float");
    }

    @Override
    public Component getMalformedLongMessage(ErrorContext context) {
        return message("malformed-long");
    }

    @Override
    public Component getMalformedIntegerMessage(ErrorContext context) {
        return message("malformed-integer");
    }

    @Override
    public Component getMalformedUuidMessage(ErrorContext context) {
        return message("malformed-uuid");
    }

    @Override
    public Component getMalformedEnumMessage(ErrorContext context, EnumInfo enumInfo) {
        return message("malformed-enum");
    }

    @Override
    public Component getMissingArgumentMessage(ErrorContext context) {
        return message("missing-argument");
    }

    @Override
    public Component getNotAPlayerMessage(ErrorContext context) {
        return message("not-a-player");
    }

    @Override
    public Component getNotAConsoleMessage(ErrorContext context) {
        return message("not-a-console");
    }

    @Override
    public Component getPlayerUnknownMessage(ErrorContext context) {
        return message("player-unknown");
    }

    @Override
    public Component getPlayerNotOnlineMessage(ErrorContext context) {
        return message("player-not-online");
    }
}
