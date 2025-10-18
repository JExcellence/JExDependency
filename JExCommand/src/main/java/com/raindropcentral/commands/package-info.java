/**
 * <h2>Choosing a base command</h2>
 * <table border="1">
 *     <caption>Command base class overview</caption>
 *     <thead>
 *     <tr>
 *         <th>Base class</th>
 *         <th>Primary responsibilities</th>
 *         <th>Thread / sender expectations</th>
 *         <th>Lifecycle &amp; helpers</th>
 *     </tr>
 *     </thead>
 *     <tbody>
 *     <tr>
 *         <td>{@link com.raindropcentral.commands.BukkitCommand}</td>
 *         <td>
 *             <ul>
 *                 <li>Extends Bukkit's {@link org.bukkit.command.Command} to centralise registration logic driven by {@link com.raindropcentral.commands.CommandFactory}.</li>
 *                 <li>Maps {@link de.jexcellence.evaluable.error.CommandError} instances to user-facing messages sourced from an {@link de.jexcellence.evaluable.section.ACommandSection}.</li>
 *                 <li>Provides typed argument parsers and shared error handling for all command flavours.</li>
 *             </ul>
 *         </td>
 *         <td>
 *             <ul>
 *                 <li>Invoked on the main server thread by Bukkit.</li>
 *                 <li>Safe for any {@link org.bukkit.command.CommandSender}, leaving context filtering to subclasses.</li>
 *             </ul>
 *         </td>
 *         <td>
 *             <ul>
 *                 <li>Override {@link com.raindropcentral.commands.BukkitCommand#onInvocation(CommandSender, String, String[])} and {@link com.raindropcentral.commands.BukkitCommand#onTabCompletion(CommandSender, String, String[])} for behaviour.</li>
 *                 <li>Use {@link com.raindropcentral.commands.BukkitCommand#executeAndHandleCommandErrors(java.util.function.Supplier, Object, CommandSender, String, String[])} to wrap execution with consistent messaging.</li>
 *             </ul>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@link com.raindropcentral.commands.PlayerCommand}</td>
 *         <td>
 *             <ul>
 *                 <li>Restricts execution to in-game players and exposes {@link com.raindropcentral.commands.PlayerCommand#onPlayerInvocation(org.bukkit.entity.Player, String, String[])}.</li>
 *                 <li>Offers {@link com.raindropcentral.commands.PlayerCommand#hasNoPermission(org.bukkit.entity.Player, de.jexcellence.evaluable.section.IPermissionNode)} backed by {@link de.jexcellence.evaluable.section.PermissionsSection} to standardise permission failures.</li>
 *             </ul>
 *         </td>
 *         <td>
 *             <ul>
 *                 <li>Main-thread only; throws {@link de.jexcellence.evaluable.error.EErrorType#NOT_A_PLAYER} when invoked from console or automation.</li>
 *                 <li>Tab completion runs only for player senders via {@link com.raindropcentral.commands.PlayerCommand#onPlayerTabCompletion(org.bukkit.entity.Player, String, String[])}.</li>
 *             </ul>
 *         </td>
 *         <td>
 *             <ul>
 *                 <li>Implement {@link com.raindropcentral.commands.PlayerCommand#onPlayerInvocation(org.bukkit.entity.Player, String, String[])} for command logic.</li>
 *                 <li>Override {@link com.raindropcentral.commands.PlayerCommand#onPlayerTabCompletion(org.bukkit.entity.Player, String, String[])} when suggestions depend on player context.</li>
 *             </ul>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@link com.raindropcentral.commands.ServerCommand}</td>
 *         <td>
 *             <ul>
 *                 <li>Enforces console-only execution and forwards control to {@link com.raindropcentral.commands.ServerCommand#onPlayerInvocation(org.bukkit.command.ConsoleCommandSender, String, String[])}.</li>
 *                 <li>Complements the player pipeline for automation and administrative tasks initiated outside the game client.</li>
 *             </ul>
 *         </td>
 *         <td>
 *             <ul>
 *                 <li>Console-safe; throws {@link de.jexcellence.evaluable.error.EErrorType#NOT_A_CONSOLE} when triggered by players.</li>
 *                 <li>Tab completion is deliberately disabled (returns {@link java.util.List#of()}), preventing player-facing assumptions.</li>
 *             </ul>
 *         </td>
 *         <td>
 *             <ul>
 *                 <li>Supply console logic via {@link com.raindropcentral.commands.ServerCommand#onPlayerInvocation(org.bukkit.command.ConsoleCommandSender, String, String[])}.</li>
 *                 <li>Inherit shared error reporting and argument helpers from {@link com.raindropcentral.commands.BukkitCommand} without additional hooks.</li>
 *             </ul>
 *         </td>
 *     </tr>
 *     </tbody>
 * </table>
 *
 * <p><strong>Shared infrastructure:</strong> Commands are discovered through {@link com.raindropcentral.commands.utility.Command} annotations and registered by the {@link com.raindropcentral.commands.CommandFactory}, which also loads their {@code *Section} configuration classes via {@link de.jexcellence.evaluable.ConfigManager} and synchronises Bukkit registration through {@link de.jexcellence.evaluable.CommandUpdater}.</p>
 */
package com.raindropcentral.commands;

import org.bukkit.command.CommandSender;
