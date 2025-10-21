package org.bukkit.craftbukkit.v1_20_R3;

import be.seeseemelk.mockbukkit.ServerMock;

/**
 * Simple {@link ServerMock} subclass that lives in a CraftBukkit-style
 * versioned package so {@link com.raindropcentral.rplatform.version.ServerEnvironment}
 * can parse the expected {@code v1_20_R3} identifier when invoking
 * {@code Bukkit.getServer().getClass().getPackage().getName()}.
 */
public class VersionedServerMock extends ServerMock {
}
