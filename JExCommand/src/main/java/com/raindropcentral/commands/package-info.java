/**
 * <h2>Command discovery overview</h2>
 * <p>
 *     {@link com.raindropcentral.commands.CommandFactory CommandFactory} scans the plugin classpath for
 *     command handlers and registers them against Bukkit once the discovery rules below are satisfied.
 *     The factory expects each handler to provide a dedicated configuration section so that command
 *     metadata and permission prompts remain synchronized across projects.
 * </p>
 *
 * <h3>Discovery rules</h3>
 * <ul>
 *     <li>
 *         Classes must live in a package containing the word {@code command}; this keeps the discovery
 *         filter lightweight while still matching project conventions.
 *     </li>
 *     <li>
 *         Each handler must be annotated with <strong>{@link com.raindropcentral.commands.utility.Command @Command}</strong>
 *         so the factory can distinguish it from supporting types.
 *     </li>
 *     <li>
 *         A matching <strong>CommandNameSection</strong> type must exist in the same package. The factory
 *         derives the section name by appending {@code Section} to the command class name and loading it as
 *         a {@link de.jexcellence.evaluable.section.ACommandSection command section} implementation.
 *     </li>
 *     <li>
 *         The section is mapped through the command configuration stored under {@code commands/<command>.yml};
 *         make sure the YAML file follows the same casing as the section class to keep automatic mapping
 *         stable.
 *     </li>
 * </ul>
 *
 * <h3>Constructor requirements</h3>
 * <p>
 *     Command constructors must expose a two-argument signature so {@code CommandFactory} can supply both the
 *     mapped section and the runtime context. Use the table below to validate new handlers before enabling
 *     auto-registration.
 * </p>
 *
 * <table>
 *     <thead>
 *         <tr>
 *             <th>Parameter position</th>
 *             <th>Type requirement</th>
 *             <th>Notes</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>First</td>
 *             <td>{@link de.jexcellence.evaluable.section.ACommandSection} implementation</td>
 *             <td>
 *                 Provide the {@code CommandNameSection} that mirrors the handler; for example,
 *                 {@link com.raindropcentral.rdq.command.player.rq.PRQSection} pairs with
 *                 {@code PRQ}.
 *             </td>
 *         </tr>
 *         <tr>
 *             <td>Second</td>
 *             <td>{@link org.bukkit.plugin.java.JavaPlugin} or plugin-specific context</td>
 *             <td>
 *                 {@link CommandFactory} first attempts to pass the custom context object (e.g., an RDQ instance)
 *                 and falls back to the active plugin if no better match exists.
 *             </td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <p>
 *     Following these requirements keeps command registration deterministic, ensures IDE navigation exposes the
 *     related {@code CommandNameSection} types, and allows contributors to audit new handlers quickly.
 * </p>
 */
package com.raindropcentral.commands;
