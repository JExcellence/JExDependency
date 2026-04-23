/**
 * Custom JExCommand 2.0 argument types for JExQuests identifiers —
 * quest, perk, machine-type, rank-tree. Each validates the token
 * against its owning service / registry and provides TTL-cached tab
 * completion. Registered with the shared
 * {@code ArgumentTypeRegistry} in the orchestrator's command-setup
 * pipeline.
 */
package de.jexcellence.quests.command.argument;
