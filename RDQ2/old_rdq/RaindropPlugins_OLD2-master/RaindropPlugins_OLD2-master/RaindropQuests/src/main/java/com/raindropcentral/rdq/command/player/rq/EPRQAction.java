package com.raindropcentral.rdq.command.player.rq;

/**
 * Represents the subcommands available for the main <code>rq</code> command.
 * Each enum constant corresponds to a specific subcommand that can be executed by the player.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public enum EPRQAction {
    /**
     * The admin subcommand, used for administrative actions and management.
     */
    ADMIN,
    /**
     * The bounty subcommand, related to bounty quests or tasks.
     */
    BOUNTY,
    /**
     * The main subcommand, typically the default or primary action.
     */
    MAIN,
    /**
     * The quests subcommand, used to interact with available quests.
     */
    QUESTS,
    /**
     * The ranks subcommand, for viewing or managing player ranks.
     */
    RANKS,
    /**
     * The perks subcommand, for viewing and managing player perks.
     */
    PERKS,
    /**
     * The help subcommand, displays help information for the rq command.
     */
    HELP
}