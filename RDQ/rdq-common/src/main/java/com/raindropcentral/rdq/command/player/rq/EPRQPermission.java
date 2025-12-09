package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

@Getter
public enum EPRQPermission implements IPermissionNode {

    COMMAND("command", "raindropquests.command"),
    ADMIN("commandAdmin", "raindropquests.command.admin"),
    BOUNTY("commandBounty", "raindropquests.command.bounty"),
    MAIN("commandMain", "raindropquests.command.main"),
    QUESTS("commandQuests", "raindropquests.command.quests"),
    RANKS("commandRanks", "raindropquests.command.ranks"),
    PERKS("commandPerks", "raindropquests.command.perks")
    ;

    private final String internalName;
    private final String fallbackNode;

    EPRQPermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
