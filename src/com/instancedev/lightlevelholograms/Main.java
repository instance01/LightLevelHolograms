package com.instancedev.lightlevelholograms;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

	String version;
	int radius = 10;
	Effects e;

	ArrayList<String> pcooldown = new ArrayList<String>();

	public void onEnable() {
		this.version = Bukkit.getServer().getClass().getPackage().getName().substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf(".") + 1);

		Bukkit.getPluginManager().registerEvents(this, this);
		e = new Effects(this);

		this.getConfig().addDefault("config.cooldown_enabled", true);
		this.getConfig().addDefault("config.cooldown_in_seconds", 4);

		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
	}

	@EventHandler
	public void onInteract(final PlayerInteractEvent event) {
		if (event.hasBlock()) {
			if (event.hasItem()) {
				if (event.getItem().getType() == Material.TORCH) {
					if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
						if (!pcooldown.contains(event.getPlayer().getName())) {
							for (int x = -4; x < 5; x++) {
								for (int z = -4; z < 5; z++) {
									Location l = new Location(event.getClickedBlock().getWorld(), event.getClickedBlock().getLocation().getBlockX() + x, event.getClickedBlock().getLocation().getBlockY(), event.getClickedBlock().getLocation().getBlockZ() + z);
									e.playHologram(event.getPlayer(), l, getColor(l.getBlock().getLightLevel()) + "" + Byte.toString(l.getBlock().getLightLevel()));
								}
							}
							if (this.getConfig().getBoolean("config.cooldown_enabled")) {
								pcooldown.add(event.getPlayer().getName());
								Bukkit.getScheduler().runTaskLater(this, new Runnable() {
									public void run() {
										pcooldown.remove(event.getPlayer().getName());
									}
								}, 20L * this.getConfig().getInt("config.cooldown_in_seconds"));
							}
						} else {
							event.getPlayer().sendMessage(ChatColor.RED + "Sorry, you have to wait a bit to check the lightlevels.");
						}
					}
				}
			}
		}
	}

	public ChatColor getColor(Byte lightlevel) {
		ChatColor ret = ChatColor.BLUE;
		if (lightlevel > 7) {
			ret = ChatColor.YELLOW;
		} else {
			ret = ChatColor.RED;
		}
		if (lightlevel > 14) {
			ret = ChatColor.GREEN;
		}
		return ret;
	}

}
