package com.raindropcentral.rdq.command.player.rq;

/**
 * Enumerates the discrete actions exposed by the {@code /prq} command.
 * <p>
 * Each value corresponds to a branch within {@link PRQ} and maps to
 * permission-gated views or workflows such as bounty browsing or perk
 * management.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum EPRQAction {

    ADMIN,
    BOUNTY,
    MAIN,
    QUESTS,
    RANKS,
    PERKS,
    HELP
}