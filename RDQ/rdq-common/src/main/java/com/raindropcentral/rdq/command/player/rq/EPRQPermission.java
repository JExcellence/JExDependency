/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Represents the EPRQPermission API type.
 */
@Getter
public enum EPRQPermission implements IPermissionNode {

    COMMAND(
        "command",
            "raindropquests.command"
    ),
    ADMIN(
        "commandAdmin",
        "raindropquests.command.admin"
    ),
    BOUNTY(
        "commandBounty",
        "raindropquests.command.bounty"
    ),
    MAIN(
        "commandMain",
        "raindropquests.command.main"
    ),
    QUESTS(
        "commandQuests",
        "raindropquests.command.quests"
    ),
    RANKS(
        "commandRanks",
        "raindropquests.command.ranks"
    ),
    SCOREBOARD(
        "commandScoreboard",
        "raindropquests.command.scoreboard"
    ),
    PERKS(
        "commandPerks",
        "raindropquests.command.perks"
    )
    ;

    private final String internalName;
    private final String fallbackNode;

    EPRQPermission(String internalName, String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}
