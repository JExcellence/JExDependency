package org.bukkit.craftbukkit.v1_12_R1;

import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Legacy server mock packaged to emulate older CraftBukkit implementations so
 * {@link com.raindropcentral.rplatform.version.ServerEnvironment} marks the
 * instance as non-modern when parsing the package identifier.
 */
public class LegacyServerMock extends ServerMock {
}
