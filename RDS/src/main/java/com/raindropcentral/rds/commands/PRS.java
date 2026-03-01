package com.raindropcentral.rds.commands;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.items.ShopBlock;
import de.jexcellence.evaluable.section.ACommandSection;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Command
@SuppressWarnings("unused")
public class PRS extends PlayerCommand {

    private final RDS rds;

    public PRS(ACommandSection commandSection, RDS rds){
        super(commandSection);
        this.rds = rds;
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String alias, @NonNull @NotNull String[] args) {
        EPRSAction action = enumParameterOrElse(args,0, EPRSAction.class, EPRSAction.INFO);
        switch (action) {
            case MAIN -> {
                this.rds.getLogger().info("Main Command");
            }
            case DEV -> {
                player.getInventory().addItem(ShopBlock.getShopBlock(this.rds, player));
            }
            default -> {
                this.rds.getLogger().info("Info Command");
            }
        }
    }

    /**
     * Provide tab completion for the first argument with available {@link EPRSAction} values.
     * Requires {@link EPRSPermission#COMMAND}.
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
                        EPRSPermission.COMMAND
                )
        ){
            return List.of();
        }
        if (args.length == 1){
            List<String> suggestions = new ArrayList<>(Arrays.stream(EPRSAction.values()).map(Enum::name).toList());
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());

        }
        return List.of();
    }
}
