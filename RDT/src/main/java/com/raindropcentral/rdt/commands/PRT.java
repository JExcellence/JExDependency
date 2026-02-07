package com.raindropcentral.rdt.commands;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.factory.CommandFactory;
import de.jexcellence.evaluable.section.ACommandSection;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Primary player command entry point for Raindrop Towns (alias: {@code /prt}).
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Authorization check via {@link EPRTPermission}</li>
 *   <li>Dispatching sub-commands to {@link CommandFactory} based on {@link EPRTAction}</li>
 *   <li>Providing lightweight tab-completion for the first argument (action)</li>
 * </ul>
 *
 * Behavior-only wrapper: business logic is delegated to {@link CommandFactory} methods.
 */
@Command
@SuppressWarnings("unused")
public class PRT extends PlayerCommand {

    /** Owning plugin instance. */
    private final RDT plugin;

    /** Command operations delegate. */
    private final CommandFactory commandFactory;

    /**
     * Construct the player command handler for {@code /prt}.
     *
     * @param commandSection concrete command section definition
     * @param plugin         main plugin instance
     */
    public PRT (ACommandSection commandSection, RDT plugin){
        super(commandSection);
        this.plugin = plugin;
        this.commandFactory = new CommandFactory(plugin);
    }

    /**
     * Handle player command execution. Verifies permission and routes to a concrete
     * action in {@link CommandFactory}. Defaults to {@link EPRTAction#INFO} when the
     * action argument is missing or invalid.
     *
     * @param player invoking player (non-null)
     * @param alias  command alias used
     * @param args   raw command arguments
     */
    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String alias, @NonNull @NotNull String[] args) {
        if (this.hasNoPermission(player, EPRTPermission.CREATE)){
            return;
        }
        EPRTAction action = enumParameterOrElse(args,0, EPRTAction.class,EPRTAction.INFO);
        switch (action) {
            case CREATE -> commandFactory.create(player, alias, args);
            case DELETE -> commandFactory.delete(player, alias, args);
            case INVITE -> commandFactory.invite(player, alias, args);
            case JOIN -> commandFactory.join(player, alias, args);
            case ACCEPT -> commandFactory.accept(player, alias, args);
            case CLAIM -> commandFactory.claim(player, alias, args);
            case UNCLAIM -> commandFactory.unclaim(player, alias, args);
            case DEBUG -> commandFactory.debug(player, alias, args);
            case DEPOSIT -> {commandFactory.deposit(player, alias, args);}
            case WITHDRAW -> {commandFactory.withdraw(player, alias, args);}
            default -> commandFactory.info(player, alias, args);
        }
    }

    /**
     * Provide tab completion for the first argument with available {@link EPRTAction} values.
     * Requires {@link EPRTPermission#COMMAND}.
     *
     * @param player invoking player (non-null)
     * @param alias  command alias used
     * @param args   current input arguments
     * @return matching suggestions or an empty list when not permitted or out of scope
     */
    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String alias, @NonNull @NotNull String[] args) {
        if (
                this.hasNoPermission(
                        player,
                        EPRTPermission.COMMAND
                )
        ){
            return List.of();
        }
        if (args.length == 1){
            List<String> suggestions = new ArrayList<>(Arrays.stream(EPRTAction.values()).map(Enum::name).toList());
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());

        }
        return List.of();
    }
}