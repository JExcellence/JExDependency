/**
 * Provides the implementation for the <b>rq</b> player command and its subcommands in RaindropQuests.
 * <p>
 * This package contains the core classes, enums, and configuration for handling the <code>/rq</code> command,
 * which serves as the main entry point for players to access various RaindropQuests subsystems such as
 * bounties, quests, ranks, and administrative controls.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.rdq.command.player.rq.PRQ} – Main command handler for <code>/rq</code>, dispatching subcommands and opening relevant GUIs.</li>
 *   <li>{@link com.raindropcentral.rdq.command.player.rq.PRQSection} – Command section configuration for the <code>/rq</code> command, integrating with the command framework.</li>
 *   <li>{@link com.raindropcentral.rdq.command.player.rq.EPRQAction} – Enum listing all available subcommands (e.g., ADMIN, BOUNTY, MAIN, QUESTS, RANKS, HELP).</li>
 *   <li>{@link com.raindropcentral.rdq.command.player.rq.ERQPermission} – Enum defining permission nodes for each subcommand, used for access control.</li>
 * </ul>
 *
 * <h2>Functionality</h2>
 * <ul>
 *   <li>Handles command execution and tab completion for <code>/rq</code> and its subcommands.</li>
 *   <li>Performs permission checks before executing subcommands or opening GUIs.</li>
 *   <li>Integrates with the InventoryFramework-based GUI system to open views such as admin, bounty, quest, and rank overviews.</li>
 *   <li>Supports internationalized messages for user feedback and help.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>
 * Players can use <code>/rq</code> followed by a subcommand (e.g., <code>/rq bounty</code>) to access different features.
 * Permissions are enforced for each subcommand, and helpful messages are shown if access is denied or if the command is misused.
 * </p>
 *
 * @author ItsRainingHP
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
package com.raindropcentral.rdq.command.player.rq;