package kz.hapyl.spigotutils.module.entity;

import kz.hapyl.spigotutils.module.util.BukkitUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.entity.Bat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Rope {

	private static final Map<Integer, Rope> byId = new HashMap<>();

	private static int freeId = 0;

	private boolean removeAtServerRestart;
	private int id;

	private final Bat[] bats = new Bat[2];
	private final Location startPoint;
	private final Location endPoint;

	public Rope(Location startPoint, Location endPoint) {
		this.startPoint = startPoint;
		this.endPoint = endPoint;
	}

	public Rope(World world, double x, double y, double z, double x1, double y1, double z1) {
		this(new Location(world, x, y, z), new Location(world, x1, y1, z1));
	}

	public Rope(double x, double y, double z, double x1, double y1, double z1) {
		this(new Location(Bukkit.getWorlds().get(0), x, y, z), new Location(Bukkit.getWorlds().get(0), x1, y1, z1));
	}

	public Rope setRemoveAtServerRestart(boolean flag) {
		this.removeAtServerRestart = flag;
		return this;
	}

	public void remove() {
		for (final Bat bat : this.bats) {
			if (bat != null) {
				bat.remove();
				bat.setLeashHolder(null);
			}
		}
		byId.remove(this.id);
	}

	public void spawn() {
		this.validateLocation();
		this.bats[0] = this.spawnBat(BukkitUtils.centerLocation(this.startPoint));
		this.bats[1] = this.spawnBat(BukkitUtils.centerLocation(this.endPoint));
		this.bats[0].setLeashHolder(this.bats[1]);
		this.id = freeId++;
		byId.put(this.id, this);
	}

	public Location getStartPoint() {
		return this.startPoint;
	}

	public Location getEndPoint() {
		return this.endPoint;
	}

	public int getId() {
		return this.id;
	}

	// static members
	@Nullable
	public static Rope getById(int id) {
		return byId.getOrDefault(id, null);
	}

	public static Map<Integer, Rope> getRopes() {
		return byId;
	}

	public static void callThisOnDisable() {
		if (!byId.isEmpty()) {
			byId.forEach((id, rope) -> {
				if (rope.removeAtServerRestart) {
					rope.remove();
				}
			});
		}
	}

	private Bat spawnBat(Location location) {
		return location.getWorld().spawn(location, Bat.class, me -> {
			me.setAwake(false);
			me.setAI(false);
			me.setGravity(false);
			me.setInvulnerable(true);
			me.setInvisible(true);
			me.setCollidable(false);
			me.setPersistent(true);
			me.setSilent(true);
			me.setPersistent(true);
			me.addScoreboardTag("RopeEntity");
		});
	}

	private void validateLocation() {
		if (!Objects.equals(startPoint.getWorld(), endPoint.getWorld())) {
			throw new IllegalArgumentException("Both locations must be in the same world!");
		}
	}

}
