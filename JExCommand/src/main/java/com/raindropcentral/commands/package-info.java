/**
 * ## Command section and YAML pairing
 *
 * The command framework creates a **CommandNameSection** class for every **@Command**-annotated handler and
 * expects a matching `CommandName.yml` resource. The command updater uses the pairing to detect new
 * commands, hydrate their default configuration, and feed the section instance into **CommandFactory**
 * during registration.
 *
 * ## Checklist
 * - Name section classes with the `CommandNameSection` suffix so they mirror the associated command handler.
 * - Place the default YAML under `CommandName.yml` inside the resources tree so the updater can find it.
 * - Confirm the command is registered through the updater rather than manual plugin hooks to preserve auto-sync.
 *
 * ## Pitfalls
 * - Mismatched section or YAML names break auto-registration because the updater cannot resolve the pair.
 * - Placing YAML files in custom folders prevents the updater from packaging defaults alongside the section.
 * - Skipping the updater in favor of manual registration results in untracked configuration changes.
 */
package com.raindropcentral.commands;
