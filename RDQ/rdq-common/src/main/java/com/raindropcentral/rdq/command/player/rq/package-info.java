/**
 * RaindropQuest player command implementation and supporting enums.
 * <p>
 * The classes in this package model the <em>prq</em> command tree, including the
 * {@link com.raindropcentral.rdq.command.player.rq.PRQ} executor,
 * {@link com.raindropcentral.rdq.command.player.rq.PRQSection} configuration,
 * and associated enums for tab-completion and permission checks. They are
 * constructed through the shared
 * {@link com.raindropcentral.commands.CommandFactory}, which injects the active
 * {@link com.raindropcentral.rdq.RDQ} instance so command handlers can route
 * players to edition-aware views such as
 * {@link com.raindropcentral.rdq.view.bounty.BountyMainView} or consult the
 * manager layer.
 * </p>
 */
package com.raindropcentral.rdq.command.player.rq;
