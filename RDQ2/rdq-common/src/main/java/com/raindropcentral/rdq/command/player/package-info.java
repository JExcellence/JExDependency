/**
 * Player-facing command surfaces backed by the shared RDQ command factory.
 * <p>
 * Implementations here are instantiated by the
 * {@link com.raindropcentral.commands.CommandFactory} created during the
 * component setup stage. Provide constructors that accept both the generated
 * section (extending {@code ACommandSection}) and the active
 * {@link com.raindropcentral.rdq.RDQ} context so the factory can inject edition
 * specific state when running in free or premium distributions.
 * </p>
 */
package com.raindropcentral.rdq.command.player;
