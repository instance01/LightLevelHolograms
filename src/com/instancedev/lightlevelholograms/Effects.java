package com.instancedev.lightlevelholograms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Effects {

	Main m;

	public Effects(Main m) {
		this.m = m;
	}

	HashMap<Integer, Integer> effectlocd = new HashMap<Integer, Integer>();
	HashMap<Integer, Integer> effectlocd_taskid = new HashMap<Integer, Integer>();

	public int getClientProtocolVersion(Player p) {
		int ret = 0;
		try {
			if (!m.version.equalsIgnoreCase("v1_8_r1")) {
				Method getHandle = Class.forName("org.bukkit.craftbukkit." + m.version + ".entity.CraftPlayer").getMethod("getHandle");
				Field playerConnection = Class.forName("net.minecraft.server." + m.version + ".EntityPlayer").getField("playerConnection");
				playerConnection.setAccessible(true);
				Object playerConInstance = playerConnection.get(getHandle.invoke(p));
				Field networkManager = playerConInstance.getClass().getField("networkManager");
				networkManager.setAccessible(true);
				Object networkManagerInstance = networkManager.get(playerConInstance);
				Method getVersion = networkManagerInstance.getClass().getMethod("getVersion");
				Object version = getVersion.invoke(networkManagerInstance);
				ret = (Integer) version;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public void setValue(Object instance, String fieldName, Object value) throws Exception {
		Field field = instance.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(instance, value);
	}

	/**
	 * Sends a hologram to a player
	 * 
	 * @param p
	 *            Player to send the hologram to
	 * @param l
	 *            Location where the hologram will spawn (and slowly move down)
	 * @param text
	 *            Hologram text
	 */
	public void playHologram(final Player p, final Location l, String text) {
		if (m.version.equalsIgnoreCase("v1_8_r1")) {
			try {
				final Method getPlayerHandle = Class.forName("org.bukkit.craftbukkit." + m.version + ".entity.CraftPlayer").getMethod("getHandle");
				final Field playerConnection = Class.forName("net.minecraft.server." + m.version + ".EntityPlayer").getField("playerConnection");
				playerConnection.setAccessible(true);
				final Method sendPacket = playerConnection.getType().getMethod("sendPacket", Class.forName("net.minecraft.server." + m.version + ".Packet"));

				Class craftw = Class.forName("org.bukkit.craftbukkit." + m.version + ".CraftWorld");
				Class w = Class.forName("net.minecraft.server." + m.version + ".World");
				Class entity = Class.forName("net.minecraft.server." + m.version + ".Entity");
				Method getWorldHandle = craftw.getDeclaredMethod("getHandle");
				Object worldServer = getWorldHandle.invoke(craftw.cast(l.getWorld()));
				final Constructor packetPlayOutSpawnEntityConstr = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutSpawnEntity").getConstructor(entity, int.class);
				final Constructor packetPlayOutSpawnEntityLivingConstr = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutSpawnEntityLiving").getConstructor(Class.forName("net.minecraft.server." + m.version + ".EntityLiving"));
				final Constructor packetPlayOutAttachEntityConstr = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutAttachEntity").getConstructor(int.class, entity, entity);
				final Constructor packetPlayOutEntityDestroyConstr = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutEntityDestroy").getConstructor(int[].class);
				final Constructor packetPlayOutEntityVelocity = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutEntityVelocity").getConstructor(int.class, double.class, double.class, double.class);

				// EntityArmorStand
				Constructor entityArmorStandConstr = Class.forName("net.minecraft.server." + m.version + ".EntityArmorStand").getConstructor(w);
				final Object entityArmorStand = entityArmorStandConstr.newInstance(worldServer);
				final Method setLoc2 = entityArmorStand.getClass().getSuperclass().getSuperclass().getDeclaredMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
				setLoc2.invoke(entityArmorStand, l.getX(), l.getY() - 1.75D, l.getZ(), 0F, 0F);
				Method setCustomName = entityArmorStand.getClass().getSuperclass().getSuperclass().getDeclaredMethod("setCustomName", String.class);
				setCustomName.invoke(entityArmorStand, text);
				Method setCustomNameVisible = entityArmorStand.getClass().getSuperclass().getSuperclass().getDeclaredMethod("setCustomNameVisible", boolean.class);
				setCustomNameVisible.invoke(entityArmorStand, true);
				Method getArmorStandId = entityArmorStand.getClass().getSuperclass().getSuperclass().getDeclaredMethod("getId");
				final int armorstandId = (Integer) (getArmorStandId.invoke(entityArmorStand));
				Method setInvisble = entityArmorStand.getClass().getSuperclass().getSuperclass().getDeclaredMethod("setInvisible", boolean.class);
				setInvisble.invoke(entityArmorStand, true);

				effectlocd.put(armorstandId, 12); // send move packet 12 times

				// Send EntityArmorStand packet
				Object horsePacket = packetPlayOutSpawnEntityLivingConstr.newInstance(entityArmorStand);
				sendPacket.invoke(playerConnection.get(getPlayerHandle.invoke(p)), horsePacket);

				// Send velocity packets to move the entities slowly down
				effectlocd_taskid.put(armorstandId, Bukkit.getScheduler().runTaskTimer(m, new Runnable() {
					public void run() {
						try {
							int i = effectlocd.get(armorstandId);
							Object packet = packetPlayOutEntityVelocity.newInstance(armorstandId, 0D, -0.05D, 0D);
							sendPacket.invoke(playerConnection.get(getPlayerHandle.invoke(p)), packet);
							if (i < -1) {
								int taskid = effectlocd_taskid.get(armorstandId);
								effectlocd_taskid.remove(armorstandId);
								effectlocd.remove(armorstandId);
								Bukkit.getScheduler().cancelTask(taskid);
								return;
							}
							effectlocd.put(armorstandId, effectlocd.get(armorstandId) - 1);
						} catch (Exception e) {

							e.printStackTrace();

						}
					}
				}, 2L, 2L).getTaskId());

				// Remove both entities (and thus the hologram) after 2 seconds
				Bukkit.getScheduler().runTaskLater(m, new Runnable() {
					public void run() {
						try {
							Object destroyPacket = packetPlayOutEntityDestroyConstr.newInstance((Object) new int[] { armorstandId });
							sendPacket.invoke(playerConnection.get(getPlayerHandle.invoke(p)), destroyPacket);
						} catch (Exception e) {

							e.printStackTrace();

						}
					}
				}, 20L * 2);

			} catch (Exception e) {

				e.printStackTrace();

			}
			return;
		}
		try {
			// If player is on 1.8, we'll have to use armor stands, otherwise just use the old 1.7 technique
			final boolean playerIs1_8 = getClientProtocolVersion(p) > 5;

			final Method getPlayerHandle = Class.forName("org.bukkit.craftbukkit." + m.version + ".entity.CraftPlayer").getMethod("getHandle");
			final Field playerConnection = Class.forName("net.minecraft.server." + m.version + ".EntityPlayer").getField("playerConnection");
			playerConnection.setAccessible(true);
			final Method sendPacket = playerConnection.getType().getMethod("sendPacket", Class.forName("net.minecraft.server." + m.version + ".Packet"));

			Class craftw = Class.forName("org.bukkit.craftbukkit." + m.version + ".CraftWorld");
			Class w = Class.forName("net.minecraft.server." + m.version + ".World");
			Class entity = Class.forName("net.minecraft.server." + m.version + ".Entity");
			Method getWorldHandle = craftw.getDeclaredMethod("getHandle");
			Object worldServer = getWorldHandle.invoke(craftw.cast(l.getWorld()));
			final Constructor packetPlayOutSpawnEntityConstr = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutSpawnEntity").getConstructor(entity, int.class);
			final Constructor packetPlayOutSpawnEntityLivingConstr = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutSpawnEntityLiving").getConstructor(Class.forName("net.minecraft.server." + m.version + ".EntityLiving"));
			final Constructor packetPlayOutAttachEntityConstr = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutAttachEntity").getConstructor(int.class, entity, entity);
			final Constructor packetPlayOutEntityDestroyConstr = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutEntityDestroy").getConstructor(int[].class);
			final Constructor packetPlayOutEntityVelocity = Class.forName("net.minecraft.server." + m.version + ".PacketPlayOutEntityVelocity").getConstructor(int.class, double.class, double.class, double.class);

			// WitherSkull
			Constructor witherSkullConstr = Class.forName("net.minecraft.server." + m.version + ".EntityWitherSkull").getConstructor(w);
			final Object witherSkull = witherSkullConstr.newInstance(worldServer);
			final Method setLoc = witherSkull.getClass().getSuperclass().getSuperclass().getDeclaredMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
			setLoc.invoke(witherSkull, l.getX(), l.getY() + 33D, l.getZ(), 0F, 0F);
			Method getWitherSkullId = witherSkull.getClass().getSuperclass().getSuperclass().getDeclaredMethod("getId");
			final int witherSkullId = (Integer) (getWitherSkullId.invoke(witherSkull));

			// EntityHorse
			Constructor entityHorseConstr = Class.forName("net.minecraft.server." + m.version + ".EntityHorse").getConstructor(w);
			final Object entityHorse = entityHorseConstr.newInstance(worldServer);
			final Method setLoc2 = entityHorse.getClass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getDeclaredMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
			setLoc2.invoke(entityHorse, l.getX(), l.getY() + (playerIs1_8 ? -1D : 33D), l.getZ(), 0F, 0F);
			Method setAge = entityHorse.getClass().getSuperclass().getSuperclass().getDeclaredMethod("setAge", int.class);
			setAge.invoke(entityHorse, -1000000);
			Method setCustomName = entityHorse.getClass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getDeclaredMethod("setCustomName", String.class);
			setCustomName.invoke(entityHorse, text);
			Method setCustomNameVisible = entityHorse.getClass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getDeclaredMethod("setCustomNameVisible", boolean.class);
			setCustomNameVisible.invoke(entityHorse, true);
			Method getHorseId = entityHorse.getClass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getDeclaredMethod("getId");
			final int horseId = (Integer) (getHorseId.invoke(entityHorse));

			if (playerIs1_8) {
				// Set horse (later armor stand) invisible
				Method setInvisble = entityHorse.getClass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getDeclaredMethod("setInvisible", boolean.class);
				setInvisble.invoke(entityHorse, true);
			}

			effectlocd.put(horseId, 12); // send move packet 12 times

			// Send Witherskull+EntityHorse packet
			Object horsePacket = packetPlayOutSpawnEntityLivingConstr.newInstance(entityHorse);
			if (playerIs1_8) {
				// Set entity id to 30 (armor stand):
				setValue(horsePacket, "b", 30);
				// Fix datawatcher values to prevent crashes (ofc armor stands expect other data than horses):
				Field datawatcher = entityHorse.getClass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getDeclaredField("datawatcher");
				datawatcher.setAccessible(true);
				Object datawatcherInstance = datawatcher.get(entityHorse);
				Field d = datawatcherInstance.getClass().getDeclaredField("d");
				d.setAccessible(true);
				Map dmap = (Map) d.get(datawatcherInstance);
				dmap.remove(10);
				// These are the Rotation ones
				dmap.remove(11);
				dmap.remove(12);
				dmap.remove(13);
				dmap.remove(14);
				dmap.remove(15);
				dmap.remove(16);
				Method a = datawatcherInstance.getClass().getDeclaredMethod("a", int.class, Object.class);
				a.invoke(datawatcherInstance, 10, (byte) 0);
			}
			sendPacket.invoke(playerConnection.get(getPlayerHandle.invoke(p)), horsePacket);
			if (!playerIs1_8) {
				Object witherPacket = packetPlayOutSpawnEntityConstr.newInstance(witherSkull, 64);
				sendPacket.invoke(playerConnection.get(getPlayerHandle.invoke(p)), witherPacket);
			}

			// Send attach packet
			if (!playerIs1_8) {
				Object attachPacket = packetPlayOutAttachEntityConstr.newInstance(0, entityHorse, witherSkull);
				sendPacket.invoke(playerConnection.get(getPlayerHandle.invoke(p)), attachPacket);
			}

			// Send velocity packets to move the entities slowly down
			effectlocd_taskid.put(horseId, Bukkit.getScheduler().runTaskTimer(m, new Runnable() {
				public void run() {
					try {
						int i = effectlocd.get(horseId);
						Object packet = packetPlayOutEntityVelocity.newInstance(horseId, 0D, -0.05D, 0D);
						sendPacket.invoke(playerConnection.get(getPlayerHandle.invoke(p)), packet);
						if (!playerIs1_8) {
							Object packet2 = packetPlayOutEntityVelocity.newInstance(witherSkullId, 0D, -0.05D, 0D);
							sendPacket.invoke(playerConnection.get(getPlayerHandle.invoke(p)), packet2);
						}
						if (i < -1) {
							int taskid = effectlocd_taskid.get(horseId);
							effectlocd_taskid.remove(horseId);
							effectlocd.remove(horseId);
							Bukkit.getScheduler().cancelTask(taskid);
							return;
						}
						effectlocd.put(horseId, effectlocd.get(horseId) - 1);
					} catch (Exception e) {

						e.printStackTrace();

					}
				}
			}, 2L, 2L).getTaskId());

			// Remove both entities (and thus the hologram) after 5 seconds
			Bukkit.getScheduler().runTaskLater(m, new Runnable() {
				public void run() {
					try {
						Object destroyPacket = packetPlayOutEntityDestroyConstr.newInstance((Object) new int[] { witherSkullId, horseId });
						sendPacket.invoke(playerConnection.get(getPlayerHandle.invoke(p)), destroyPacket);
					} catch (Exception e) {

						e.printStackTrace();

					}
				}
			}, 20L * 5);

		} catch (Exception e) {

			e.printStackTrace();

		}
	}

}
