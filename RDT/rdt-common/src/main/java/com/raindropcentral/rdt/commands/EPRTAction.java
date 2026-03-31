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

package com.raindropcentral.rdt.commands;

/**
 * Sub-commands supported by the primary player command ({@code /prt}).
 *
 * <p>Each enum value maps to a corresponding handler method in
 * {@link PRT}.
 */
public enum EPRTAction {
    /** Create a new town at the player's location. */
    CREATE,
    /** Delete the town owned by the invoking mayor. */
    DELETE,
    /** Show an information summary for the player's current town. */
    INFO,
    /** Invite another player to your town. */
    INVITE,
    /** Request to join a town by name. */
    JOIN,
    /** Accept a pending invitation or (as mayor) accept join requests. */
    ACCEPT,
    /** Claim the current chunk for your town (mayor only). */
    CLAIM,
    //unclaim a chunk
    UNCLAIM,
    /** Print raw debug output for your town. */
    DEBUG,
    //deposit money to the town bank
    DEPOSIT,
    //withdraw money from the town bank
    WITHDRAW,
    //Main menu GUI
    MAIN,
    /** Opens the town overview when the player is part of a town. */
    TOWN,
    /** Starts the delayed safe-teleport flow to the player's town nexus. */
    SPAWN,
    /** Displays basic command help and usage examples. */
    HELP
}
