/**
 * Consolidated {@code /economy} admin command.
 *
 * <p>Exposes one top-level command with {@code give}, {@code take}, {@code set},
 * {@code reset}, {@code migrate}, {@code reload}, and {@code help} subcommands —
 * replacing the legacy {@code /cdeposit}, {@code /cwithdraw}, and
 * {@code /cmigrate} console-only commands.
 *
 * <p>Usable from the console (no permission checks) and from players holding the
 * matching {@code economy.command.*} permission, so operators can delegate
 * specific mutations without handing over full admin access.
 *
 * @author JExcellence
 * @since 3.0.0
 */
package de.jexcellence.economy.command.admin.economy;
