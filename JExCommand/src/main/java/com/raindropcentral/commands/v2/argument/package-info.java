/**
 * Typed argument support for JExCommand 2.0 command trees.
 *
 * <p>An {@link com.raindropcentral.commands.v2.argument.ArgumentType ArgumentType}
 * converts raw string tokens into typed Java values while reporting failures as
 * i18n keys (never throwing). Built-in types cover strings, numeric primitives,
 * players, UUIDs, and enums; plugins register custom types against a
 * {@link com.raindropcentral.commands.v2.argument.ArgumentTypeRegistry ArgumentTypeRegistry}.
 *
 * @author JExcellence
 * @since 2.0.0
 */
package com.raindropcentral.commands.v2.argument;
