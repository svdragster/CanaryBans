package de.svdragster.canarybans;

import net.canarymod.Canary;
import net.canarymod.chat.ChatFormat;
import net.canarymod.commandsys.CommandDependencyException;
import net.canarymod.plugin.Plugin;

public class CanaryBans extends Plugin {

	CanaryBansListener listener = new CanaryBansListener();
	
	public void disable() {
		listener.saveProperties();
	}

	public boolean enable() {
		Canary.hooks().registerListener(listener, this);
		listener.loadProperties();
		try {
			Canary.commands().registerCommands(new CanaryBansCommands(), this, false);
		} catch (CommandDependencyException e) {
			e.printStackTrace();
			Canary.getServer().broadcastMessageToAdmins(ChatFormat.GOLD + "[CanaryBans] " + ChatFormat.YELLOW + "Could not register commands: " + e.getMessage());
			return false;
		}
		return true;
	}

}
