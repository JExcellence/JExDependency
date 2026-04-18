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

/**
 * Machine command implementation for the RDQ plugin.
 *
 * <p>This package contains the command handler and supporting classes for managing
 * the Machine Fabrication System through the {@code /rq machine} command hierarchy.
 *
 * <h2>Command Structure:</h2>
 * <pre>
 * /rq machine
 *     ├── give {@literal <player> <machine_type>}  - Give a machine item
 *     ├── list [player]                - List machines
 *     ├── remove {@literal <machine_id>}           - Remove a machine
 *     ├── reload                       - Reload configurations
 *     ├── info {@literal <machine_id>}             - Display machine info
 *     ├── teleport {@literal <machine_id>}         - Teleport to machine
 *     └── help                         - Display help
 * </pre>
 *
 * <h2>Key Classes:</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.rdq.command.player.rq.machine.EMachineAction} - Available subcommands</li>
 *   <li>{@link com.raindropcentral.rdq.command.player.rq.machine.EMachinePermission} - Permission nodes</li>
 *   <li>{@link com.raindropcentral.rdq.command.player.rq.machine.MachineCommandSection} - Configuration section</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
package com.raindropcentral.rdq.command.player.rq.machine;
