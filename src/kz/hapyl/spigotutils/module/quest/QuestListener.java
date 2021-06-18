package kz.hapyl.spigotutils.module.quest;

import kz.hapyl.spigotutils.SpigotUtilsPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.TimeUnit;

public class QuestListener implements Listener {

	private final QuestManager manager = QuestManager.current();


	@EventHandler(priority = EventPriority.HIGH)
	public void handleAsyncChatEvent(AsyncPlayerChatEvent ev) {
		final Player player = ev.getPlayer();
		final String message = ev.getMessage();

		/**
		 * Have to manual check because of event cancelling
		 */

		if (!manager.hasQuestsOfType(player, QuestObjectiveType.SAY_IN_CHAT)) {
			return;
		}

		for (final PlayerQuestObjective obj : manager.getActiveObjectivesOfType(player, QuestObjectiveType.SAY_IN_CHAT)) {
			if (obj.testQuestCompletion(message) == 1.0d) {
				ev.setCancelled(true);
				new BukkitRunnable() {
					@Override
					public void run() {
						obj.incrementGoal(true);
					}
				}.runTask(SpigotUtilsPlugin.getPlugin());
			}
		}

	}

	@EventHandler()
	public void handleEntityBreed(EntityBreedEvent ev) {
		final LivingEntity breeder = ev.getBreeder();
		if (!(breeder instanceof Player)) {
			return;
		}

		final Player player = ((Player) breeder);
		manager.checkActiveQuests(player, QuestObjectiveType.BREED_ANIMALS, ev.getFather());

	}

	@EventHandler(priority = EventPriority.HIGH)
	public void handleStatistic(PlayerStatisticIncrementEvent ev) {
		final Statistic stat = ev.getStatistic();
		final int increment = ev.getNewValue() - ev.getPreviousValue();

		if (increment > 0) {

			switch (stat) {

				case JUMP: {
					manager.checkActiveQuests(ev.getPlayer(), QuestObjectiveType.JUMP);
					return;
				}

				default: {
					return;
				}

			}

		}

	}

	@EventHandler(priority = EventPriority.HIGH)
	public void handlePlayerInteract(PlayerInteractEvent ev) {
		if (ev.getHand() == EquipmentSlot.OFF_HAND
				|| ev.getAction() == Action.PHYSICAL
				|| ev.getClickedBlock() == null) {
			return;
		}

		// prevent creative block break
		if (ev.getPlayer().getGameMode() == GameMode.CREATIVE
				&& (ev.getAction() == Action.LEFT_CLICK_AIR || ev.getAction() == Action.LEFT_CLICK_BLOCK)) {
			return;
		}

		final Block block = ev.getClickedBlock();
		if (block.getType() != Material.NOTE_BLOCK) {
			return;
		}

		if (block.getBlockData() instanceof NoteBlock) {
			final Instrument instrument = ((NoteBlock) block.getBlockData()).getInstrument();
			final Note note = ((NoteBlock) block.getBlockData()).getNote();
			manager.checkActiveQuests(ev.getPlayer(), QuestObjectiveType.PLAY_NOTE, instrument, note);
		}

	}

	@EventHandler(priority = EventPriority.HIGH)
	public void handleQuestListener(PlayerMoveEvent ev) {

		final Player player = ev.getPlayer();
		final Location from = ev.getFrom();
		final Location to = ev.getTo();

		if (to == null) {
			return;
		}

		// travel distance
		final double distance = to.distance(from);
		if (distance > 0.0d) {
			TravelType type = TravelType.FOOT;

			// ELYTRA
			if (player.isGliding()) {
				type = TravelType.ELYTRA;
			}
			// MOUNT OR MINECART
			else if (player.isInsideVehicle()) {
				final Entity vehicle = player.getVehicle();
				if (vehicle != null) {
					type = TravelType.MOUNT;
				}
			}

			manager.checkActiveQuests(player, QuestObjectiveType.TRAVEL_DISTANCE, type, distance);

		}

		manager.checkActiveQuests(player, QuestObjectiveType.TRAVEL_TO, to);

	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void handleBlockBreak(BlockBreakEvent ev) {
		final Player player = ev.getPlayer();
		final Block block = ev.getBlock();

		manager.checkActiveQuests(player, QuestObjectiveType.BREAK_BLOCKS, block.getType());

	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void handleBlockPlace(BlockPlaceEvent ev) {
		final Player player = ev.getPlayer();
		final Block block = ev.getBlock();

		manager.checkActiveQuests(player, QuestObjectiveType.PLACE_BLOCKS, block.getType());

	}

	@EventHandler(priority = EventPriority.HIGH)
	public void handleEntityDeath(EntityDeathEvent ev) {

		final LivingEntity entity = ev.getEntity();
		final Player killer = entity.getKiller();

		if (killer == null) {
			return;
		}

		manager.checkActiveQuests(killer, QuestObjectiveType.SLAY_ENTITY, entity.getType());

	}

	@EventHandler(priority = EventPriority.HIGH)
	public void handleEntityDamage(EntityDamageByEntityEvent ev) {
		final Entity entity = ev.getEntity();

		if (!(ev.getDamager() instanceof Player)) {
			return;
		}

		final Player player = (Player) ev.getDamager();
		final double damage = ev.getFinalDamage();

		if (damage <= 0.0d) {
			return;
		}

		manager.checkActiveQuests(player, QuestObjectiveType.DEAL_DAMAGE, damage);
		manager.checkActiveQuests(player, QuestObjectiveType.DEAL_DAMAGE_TO, damage, entity);

	}

	@EventHandler(priority = EventPriority.HIGH)
	public void handleEntityDamageEvent(EntityDamageEvent ev) {
		if (!(ev.getEntity() instanceof Player)) {
			return;
		}
		final Player player = (Player) ev.getEntity();
		final double damage = ev.getFinalDamage();

		if (damage <= 0.0d) {
			return;
		}

		manager.checkActiveQuests(player, QuestObjectiveType.TAKE_DAMAGE, damage);

		if (ev instanceof EntityDamageByEntityEvent) {
			final EntityDamageByEntityEvent evDamageByEntity = (EntityDamageByEntityEvent) ev;
			manager.checkActiveQuests(player, QuestObjectiveType.TAKE_DAMAGE_FROM, damage, evDamageByEntity.getDamager());
		}

	}

	@EventHandler(priority = EventPriority.HIGH)
	public void handleItemBreakEvent(PlayerItemBreakEvent ev) {
		final Player player = ev.getPlayer();
		final ItemStack brokenItem = ev.getBrokenItem();

		manager.checkActiveQuests(player, QuestObjectiveType.BREAK_ITEM, brokenItem.getType());
	}

	@EventHandler()
	public void handleItemCraftEvent(CraftItemEvent ev) {

		if (!(ev.getWhoClicked() instanceof Player)) {
			return;
		}

		final Player player = (Player) ev.getWhoClicked();
		final ItemStack item = ev.getCurrentItem();

		if (item == null) {
			return;
		}

		manager.checkActiveQuests(player, QuestObjectiveType.CRAFT_ITEM, item.getType());

	}

	@EventHandler()
	public void handlePlayerNoteEvent(NotePlayEvent ev) {
		final Instrument instrument = ev.getInstrument();

	}


}