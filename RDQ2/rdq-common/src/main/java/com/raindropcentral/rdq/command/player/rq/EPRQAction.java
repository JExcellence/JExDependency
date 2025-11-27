package com.raindropcentral.rdq.command.player.rq;

/**
 * Enumerates the discrete actions exposed by the {@code /prq} command.
 * <p>
 * Each value routes the root {@link PRQ} command to a specific handler method
 * that opens the corresponding view or workflow when the caller satisfies the
 * relevant {@link ERQPermission}. Documenting the available actions here keeps
 * the command tree discoverable for future additions and ensures tab
 * completions stay aligned with the runtime behavior.
 * </p>
 *
 * @see PRQ
 * @see ERQPermission
 * @see PRQ#onPlayerInvocation(org.bukkit.entity.Player, String, String[])
 * @see PRQ#onPlayerTabCompletion(org.bukkit.entity.Player, String, String[])
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum EPRQAction {

    /**
     * Grants access to administrative tooling and diagnostics for privileged
     * staff members when {@link ERQPermission#ADMIN} is satisfied.
     */
    ADMIN,

    /**
     * Opens {@link com.raindropcentral.rdq.view.bounty.BountyMainView} for
     * players with {@link ERQPermission#BOUNTY}, exposing bounty browsing and
     * management features.
     */
    BOUNTY,

    /**
     * Directs players to the primary overview guarded by
     * {@link ERQPermission#MAIN}, providing high-level progression details.
     */
    MAIN,

    /**
     * Navigates to quest-related interfaces when the caller holds
     * {@link ERQPermission#QUESTS}.
     */
    QUESTS,

    /**
     * Routes to rank configuration surfaces contingent on
     * {@link ERQPermission#RANKS}.
     */
    RANKS,

    /**
     * Opens perk selection or management screens for players with
     * {@link ERQPermission#PERKS}.
     */
    PERKS,

    /**
     * Displays contextual help and acts as the default action when no specific
     * sub-command is supplied by the player.
     */
    HELP
}
