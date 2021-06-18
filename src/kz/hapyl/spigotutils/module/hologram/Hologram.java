package kz.hapyl.spigotutils.module.hologram;

import kz.hapyl.spigotutils.SpigotUtilsPlugin;
import kz.hapyl.spigotutils.module.chat.Chat;
import kz.hapyl.spigotutils.module.reflect.Reflect;
import kz.hapyl.spigotutils.module.reflect.ReflectPacket;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Hologram {

	public static final Collection<Hologram> holograms = new ArrayList<>();

	/**
	 * Vertical offset of the holograms.
	 */
	public static final double HOLOGRAM_OFFSET = 0.3d;

	private BukkitTask task;
	private List<String> lines;
	private final HashMap<EntityArmorStand, ReflectPacket> packets;
	private final Map<Player, Boolean> showingTo;
	private int removeWhenFarAway = 25;
	private boolean persistent;

	private Location location;

	public Hologram(int size) {
		this.lines = new ArrayList<>(size);
		this.packets = new HashMap<>();
		this.showingTo = new ConcurrentHashMap<>();
		holograms.add(this);
	}

	public Hologram() {
		this(1);
	}

	public Hologram addLine(String line) {
		this.lines.add(line);
		return this;
	}

	public Hologram removeLine(int index) throws IndexOutOfBoundsException {
		if (this.lines.size() - 1 < index) {
			throw new IndexOutOfBoundsException(String.format("There is only %s lines, not %s.", this.lines.size(), index));
		}
		else {
			this.lines.remove(index);
		}
		return this;
	}

	public Hologram setLine(int index, String line) throws IndexOutOfBoundsException {
		if (this.lines.size() - 1 < index) {
			for (int i = 0; i < index; i++) {
				addLine("");
			}
		}
		else {
			this.lines.set(index, line);
		}
		return this;
	}

	public Hologram fillLines(String values) {
		final List<String> newList = new ArrayList<>();
		for (int i = 0; i < this.lines.size(); i++) {
			newList.add(values);
		}
		this.lines = newList;
		this.updateLines();
		return this;
	}

	public static void removeAll() {
		if (!holograms.isEmpty()) {
			for (Hologram hologram : holograms) {
				hologram.removeStands();
			}
		}
		holograms.clear();
	}

	public Hologram setPersistent(boolean persistent) {
		this.persistent = persistent;
		return this;
	}

	public boolean isPersistent() {
		return persistent;
	}

	public Hologram setRemoveWhenFarAway(int removeWhenFarAway) {
		this.removeWhenFarAway = removeWhenFarAway;
		return this;
	}

	public int getRemoveWhenFarAway() {
		return removeWhenFarAway;
	}

	public Map<Player, Boolean> getShowingTo() {
		return showingTo;
	}

	public boolean isShowingTo(Player player) {
		return this.showingTo.containsKey(player) && this.showingTo.get(player);
	}

	public Location getLocation() {
		return location;
	}

	public void destroy() {

		this.removeStands();

		if (this.task != null) {
			this.task.cancel();
		}

		holograms.remove(this);
	}

	private void removeStands() {
		this.showingTo.forEach((player, status) -> this.hide(player));
		this.packets.clear();
	}

	public Hologram showAll() {
		return this.show(Bukkit.getOnlinePlayers().toArray(new Player[]{}));
	}

	public Hologram show(Player... players) {
		if (this.location == null) {
			for (final Player player : players) {
				Chat.sendMessage(player, "&4Could not spawn a hologram for you since is wasn't created yet!");
			}
			return null;
		}

		this.packets.forEach((entity, packet) -> {
			for (final Player player : players) {
				this.showingTo.put(player, true);
				packet.sendPacket(0, player);
				packet.sendPacket(1, player);
			}
		});

		return this;
	}

	/**
	 * @param flag true -> hide, false -> destroy
	 */
	public Hologram hide(boolean flag, Player... players) {
		if (players.length == 0) {
			players = onlinePlayersToArray();
		}

		final Player[] finalPlayers = players;
		this.packets.forEach((entity, packet) -> {
			for (final Player player : finalPlayers) {
				if (flag) {
					this.showingTo.put(player, false);
				}
				else {
					this.showingTo.remove(player);
				}
				packet.sendPacket(2, player);
			}
		});

		return this;
	}

	public Hologram hide(Player... players) {
		return this.hide(false, players);
	}

	public Hologram onTick(HologramAction<?> action, int tick) {
		return onTick(action, tick, Bukkit.getOnlinePlayers().toArray(new Player[]{}));
	}

	public Hologram onTick(HologramAction<?> action, int tick, Player... receivers) {
		if (this.task != null) {
			this.task.cancel();
		}

		this.task = new BukkitRunnable() {
			@Override
			public void run() {
				action.consume(receivers);
			}
		}.runTaskTimer(SpigotUtilsPlugin.getPlugin(), 0, tick);
		return this;
	}

	public Hologram teleport(Location location) {
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			this.move(location, onlinePlayer);
		}
		return this;
	}

	public Hologram updateLines(boolean keepListSorted) {
		Location location = this.location.clone();
		location.add(0.0d, 1.0d, 0.0d); // fix marker
		this.removeStands();

		if (keepListSorted) {
			for (final String line : lines) {
				location.add(0.0d, HOLOGRAM_OFFSET, 0.0d);
				createStand(location, ChatColor.translateAlternateColorCodes('&', line));
			}
		}
		else {
			location.add(0.0d, HOLOGRAM_OFFSET * (double)lines.size(), 0.0d);
			for (final String line : lines) {
				createStand(location, ChatColor.translateAlternateColorCodes('&', line));
				location.subtract(0.0d, HOLOGRAM_OFFSET, 0.0d);
			}
		}

		return this;
	}

	public Hologram updateLines() {
		return this.updateLines(false);
	}

	public Hologram create(Location loc) {
		if (this.location != null) {
			throw new IllegalArgumentException("This hologram was already created!");
		}

		// Saved to use it later when updating stands
		this.location = loc.clone();
		// fix marker location
		this.location.add(0.0d, 1.0d, 0.0);
		// Move current location up so holograms stop at the start position
		this.location.add(0.0d, HOLOGRAM_OFFSET * (double)lines.size(), 0.0d);
		for (String string : lines) {
			createStand(this.location, ChatColor.translateAlternateColorCodes('&', string));
			this.location.subtract(0.0d, HOLOGRAM_OFFSET, 0.0d);
		}
		return this;
	}

	private Player[] onlinePlayersToArray() {
		final Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
		final Player[] array = new Player[onlinePlayers.size()];
		int step = 0;
		for (final Player onlinePlayer : onlinePlayers) {
			array[step] = onlinePlayer;
			++step;
		}
		return array;
	}

	public Hologram move(Location location, Player... players) {
		location.add(0.0d, HOLOGRAM_OFFSET * (double)lines.size(), 0.0d);
		this.packets.forEach((stand, packets) -> {
			teleportAndSync(stand, location.getX(), location.getY(), location.getZ(), players);
			location.subtract(0.0d, HOLOGRAM_OFFSET, 0.0d);
		});
		return this;
	}

	private void teleportAndSync(EntityArmorStand stand, double x, double y, double z, Player... receivers) {
		stand.teleportAndSync(x, y, z);
		new ReflectPacket(new PacketPlayOutEntityTeleport(stand)).sendPackets(receivers);
	}

	private void createStand(Location location, String name) {
		// skip line if empty instead of | thing
		if (name.isEmpty()) {
			return;
		}

		EntityArmorStand armorStand = new EntityArmorStand(EntityTypes.c, Reflect.getMinecraftWorld(location.getWorld()));
		armorStand.setLocation(location.getX(), location.getY(), location.getZ(), 0.0f, 0.0f);
		armorStand.setSmall(true);
		armorStand.setInvisible(true);
		armorStand.setMarker(true);
		armorStand.setCustomName(new ChatComponentText(name));
		armorStand.setCustomNameVisible(true);

		this.packets.put(armorStand, new ReflectPacket(
				new PacketPlayOutSpawnEntityLiving(armorStand),
				new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), true),
				new PacketPlayOutEntityDestroy(armorStand.getId())));

	}

	@Override
	public String toString() {
		return this.lines.toString();
	}
}
