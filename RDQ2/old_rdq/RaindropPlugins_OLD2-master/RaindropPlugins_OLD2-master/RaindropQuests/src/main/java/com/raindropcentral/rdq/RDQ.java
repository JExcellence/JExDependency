package com.raindropcentral.rdq;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class RDQ extends JavaPlugin {
	
	private RDQImpl rdqImpl;
	
	@Override
	public void onLoad() {
		try {
			JEDependency.initialize(this, RDQ.class, null);
			this.rdqImpl = (RDQImpl) Class.forName("com.raindropcentral.rdq.RDQImpl").getDeclaredConstructor(RDQ.class).newInstance(this);
			this.rdqImpl.onLoad();
		} catch (
			  final Exception exception
		) {
			this.getLogger().log(Level.SEVERE, "[RDQ] Failed to load RDQ", exception);
			this.rdqImpl = null;
		}
	}
	
	@Override
	public void onEnable() {
		if (this.rdqImpl != null) {
			this.rdqImpl.onEnable();
		}
	}
	
	@Override
	public void onDisable() {
		if (this.rdqImpl != null) {
			this.rdqImpl.onDisable();
		}
	}
	
	public RDQImpl getImpl() {
		return this.rdqImpl;
	}
}
