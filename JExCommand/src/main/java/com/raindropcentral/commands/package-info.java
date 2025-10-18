/**
 * <h2>Auto-registration pipeline</h2>
 * <p>
 *     Command handlers and configuration sections advance together through the auto-registration pipeline, keeping
 *     {@code @Command}-annotated classes paired with their corresponding {@code CommandNameSection} implementations.
 * </p>
 * <ul>
 *     <li>Command discovery scans for classes annotated with {@code @Command} and links them to their section peers.</li>
 *     <li>Section bootstrapping prepares the configuration instance before any command constructor receives control.</li>
 *     <li>Lifecycle alignment ensures new commands comply with the workflow documented in this module's agent guidelines.</li>
 * </ul>
 *
 * <h2>CommandFactory highlights</h2>
 * <p>
 *     The reflection-driven {@code CommandFactory} orchestrates instantiation, validating constructor signatures and
 *     sequencing section creation according to the steps outlined in the agent's workflow guidelines.
 * </p>
 * <ul>
 *     <li>Section instances are created first and provided as the primary argument to each command.</li>
 *     <li>Dependencies such as plugin contexts, localization managers, and schedulers follow in the constructor order.</li>
 *     <li>Failed validations surface as actionable errors so maintainers can reconcile implementations with the guidelines.</li>
 * </ul>
 *
 * <h2>Integration with Paper and Spigot</h2>
 * <p>
 *     After commands pass the factory pipeline, they are registered with the Paper and Spigot command APIs, mirroring the
 *     practices mandated by the repository-wide agent workflow. Paper's asynchronous capabilities and Spigot's legacy
 *     compatibility both rely on the factory to deliver initialized handlers that respect the platform contracts.
 * </p>
 * <ul>
 *     <li>Paper integrations map command metadata into the asynchronous command manager without additional boilerplate.</li>
 *     <li>Spigot compatibility is preserved through mirrored registration calls that honor legacy dispatcher rules.</li>
 *     <li>Shared error handling communicates permission and validation failures back through the Bukkit command framework.</li>
 * </ul>
 */
package com.raindropcentral.commands;

