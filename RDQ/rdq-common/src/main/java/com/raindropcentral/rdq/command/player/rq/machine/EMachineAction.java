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

package com.raindropcentral.rdq.command.player.rq.machine;

/**
 * Enumeration of available machine command actions.
 *
 * <p>Defines the subcommands available under the {@code /rq machine} command hierarchy.
 * Each action corresponds to a specific machine management operation that can be performed
 * by players or administrators.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public enum EMachineAction {
    /**
     * Gives a machine item to a player.
     * Usage: /rq machine give {@literal <player> <machine_type>}
     */
    GIVE,
    
    /**
     * Lists all machines owned by a player.
     * Usage: /rq machine list [player]
     */
    LIST,
    
    /**
     * Removes a machine by ID.
     * Usage: /rq machine remove {@literal <machine_id>}
     */
    REMOVE,
    
    /**
     * Reloads machine configurations.
     * Usage: /rq machine reload
     */
    RELOAD,
    
    /**
     * Displays information about a machine.
     * Usage: /rq machine info {@literal <machine_id>}
     */
    INFO,
    
    /**
     * Teleports to a machine location.
     * Usage: /rq machine teleport {@literal <machine_id>}
     */
    TELEPORT,
    
    /**
     * Displays help information.
     * Usage: /rq machine help
     */
    HELP
}
