/**
 * Shared command entry points for RDQ across all editions.
 * <p>
 * Classes in this package are discovered during the component stage of the RDQ
 * enable pipeline. {@link com.raindropcentral.rdq.RDQ#onEnable()} constructs a
 * {@link com.raindropcentral.commands.CommandFactory} inside
 * {@link com.raindropcentral.rdq.RDQ#initializeComponents()} before views or
 * repositories are wired, allowing commands to rely on freshly initialised
 * managers such as the bounty and rank systems.
 * </p>
 * <p>
 * The {@code CommandFactory} instance is shared between the free and premium
 * editions. It selects constructors that accept the active
 * {@link com.raindropcentral.rdq.RDQ} context so implementations can branch on
 * {@link com.raindropcentral.rdq.RDQ#getEdition()} or the bound manager to keep
 * edition-specific behaviours aligned. New commands must therefore expose a
 * constructor compatible with the shared factory instead of attempting manual
 * Bukkit registrations.
 * </p>
 */
package com.raindropcentral.rdq.command;
