/**
 * Utility abstractions that connect annotated command implementations to the
 * reflection-driven registration pipeline provided by the {@link com.raindropcentral.commands.CommandFactory}.
 *
 * <p>The {@link com.raindropcentral.commands.utility.Command Command} annotation marks concrete
 * command handlers so they can be discovered, paired with their {@link de.jexcellence.evaluable.section.ACommandSection
 * configuration sections}, and registered automatically. Implementations are expected to extend
 * {@link com.raindropcentral.commands.BukkitCommand} or one of its specialisations so that the base command
 * contract can wire the mapped section into the command metadata (name, aliases, usage), feed localized
 * messages into command responses, and expose helper hooks for permission and argument checks.</p>
 *
 * <p>Through that base contract the framework:</p>
 * <ul>
 *     <li>Creates the associated configuration section before invoking the command constructor, ensuring
 *     translated descriptions, usage strings, and permission prompts are injected from the section model.</li>
 *     <li>Executes invocations inside the shared error handling routine, translating {@link de.jexcellence.evaluable.error.CommandError}
 *     instances into sender-facing feedback supplied by the section while logging unexpected failures.</li>
 *     <li>Relies on the section&apos;s localization utilities to obtain MiniMessage-based responses (for example,
 *     internal error, malformed argument, or permission messages) so each command stays consistent with the
 *     project&apos;s internationalisation strategy.</li>
 * </ul>
 *
 * <p>See the {@link com.raindropcentral.commands package documentation} for how these utilities relate to the
 * concrete command classes that the factory discovers and registers.</p>
 */
package com.raindropcentral.commands.utility;

