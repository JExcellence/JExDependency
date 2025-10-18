/**
 * Core command package for Raindrop Central plugins.
 *
 * <h2>Command onboarding checklist</h2>
 * <ul>
 *     <li><b>Annotate the command class</b> with {@link com.raindropcentral.commands.utility.Command Command}
 *     so the {@link com.raindropcentral.commands.CommandFactory} and {@link de.jexcellence.evaluable.CommandUpdater}
 *     discovery routines pick it up during scans.</li>
 *     <li><b>Create the section companion</b> by implementing a {@link de.jexcellence.evaluable.section.ACommandSection}
 *     derivative (for example {@code MyCommandSection}) so {@link de.jexcellence.configmapper.ConfigMapper}
 *     can hydrate defaults and expose metadata.</li>
 *     <li><b>Define permissions</b> inside the section using {@link de.jexcellence.evaluable.section.PermissionsSection}
 *     helpers to keep {@link com.raindropcentral.commands.PlayerCommand} and related guards aligned with reviewer expectations.</li>
 *     <li><b>Supply the YAML resource</b> (named <code>CommandName.yml</code>) so {@link de.jexcellence.evaluable.ConfigManager}
 *     and {@link de.jexcellence.evaluable.ConfigKeeper} can sync runtime updates with shipped defaults.</li>
 * </ul>
 */
package com.raindropcentral.commands;
