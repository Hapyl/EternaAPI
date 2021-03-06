package me.hapyl.spigotutils;

import me.hapyl.spigotutils.builtin.command.NoteBlockStudioCommand;
import me.hapyl.spigotutils.builtin.command.QuestCommand;
import me.hapyl.spigotutils.builtin.command.ReloadPlayerConfigCommand;
import me.hapyl.spigotutils.builtin.event.PlayerConfigEvent;
import me.hapyl.spigotutils.module.command.CommandProcessor;
import me.hapyl.spigotutils.module.hologram.HologramRunnable;
import me.hapyl.spigotutils.module.inventory.ChestInventoryListener;
import me.hapyl.spigotutils.module.inventory.ItemBuilderListener;
import me.hapyl.spigotutils.module.inventory.gui.GUIListener;
import me.hapyl.spigotutils.module.inventory.item.CustomItemHolder;
import me.hapyl.spigotutils.module.inventory.item.CustomItemListener;
import me.hapyl.spigotutils.module.locaiton.TriggerManager;
import me.hapyl.spigotutils.module.parkour.ParkourListener;
import me.hapyl.spigotutils.module.parkour.ParkourManager;
import me.hapyl.spigotutils.module.parkour.ParkourRunnable;
import me.hapyl.spigotutils.module.player.song.SongPlayer;
import me.hapyl.spigotutils.module.quest.QuestListener;
import me.hapyl.spigotutils.module.record.ReplayListener;
import me.hapyl.spigotutils.module.reflect.NPCRunnable;
import me.hapyl.spigotutils.module.reflect.glow.GlowingManager;
import me.hapyl.spigotutils.module.reflect.glow.GlowingRunnable;
import me.hapyl.spigotutils.module.reflect.protocol.GlowingListener;
import me.hapyl.spigotutils.module.reflect.protocol.HumanNPCListener;
import me.hapyl.spigotutils.module.reflect.protocol.SignListener;
import me.hapyl.spigotutils.module.util.Runnables;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import test.Test;

import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class EternaPlugin extends JavaPlugin {

    private static EternaPlugin plugin;

    private EternaRegistry registry;

    @Override
    public void onEnable() {
        plugin = this;

        final PluginManager manager = this.getServer().getPluginManager();

        manager.registerEvents(new ItemBuilderListener(), this);
        manager.registerEvents(new CustomItemListener(), this);
        manager.registerEvents(new ChestInventoryListener(), this);
        manager.registerEvents(new GUIListener(), this);
        manager.registerEvents(new TriggerManager(), this);
        manager.registerEvents(new QuestListener(), this);
        manager.registerEvents(new ParkourListener(), this);
        manager.registerEvents(new PlayerConfigEvent(), this);
        manager.registerEvents(new ReplayListener(), this);

        registry = new EternaRegistry(this);

        final BukkitScheduler scheduler = Bukkit.getScheduler();

        // Schedule tasks
        scheduler.runTaskTimer(this, new NPCRunnable(), 0, 2);
        scheduler.runTaskTimer(this, new HologramRunnable(), 0, 2);
        scheduler.runTaskTimer(this, new ParkourRunnable(), 0, 2);
        scheduler.runTaskTimer(this, new GlowingRunnable(), 0, 1);

        // Load ProtocolLib listeners
        new SignListener();
        new GlowingListener();
        new HumanNPCListener();

        // Load built-in commands
        final CommandProcessor commandProcessor = new CommandProcessor(this);
        commandProcessor.registerCommand(new QuestCommand("quest"));
        commandProcessor.registerCommand(new NoteBlockStudioCommand("nbs"));
        commandProcessor.registerCommand(new ReloadPlayerConfigCommand("reloadplayerconfig"));

        // Load configuration file
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        // Create songs folder
        try {
            boolean b = new File(this.getDataFolder() + "/songs").mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load PlayerConfig for online players
        Runnables.runLater(() -> Bukkit.getOnlinePlayers().forEach(player -> registry.configManager.getConfig(player).loadAll()), 10L);

        // Load dependencies
        EternaAPI.loadAll();

        new Test().test();
    }

    public EternaRegistry getRegistry() {
        return registry;
    }

    @Override
    public void onDisable() {
        registry.onDisable();
    }

    public CustomItemHolder getItemHolder() {
        return registry.itemHolder;
    }

    public SongPlayer getSongPlayer() {
        return registry.songPlayer;
    }

    public ParkourManager getParkourManager() {
        return registry.parkourManager;
    }

    public GlowingManager getGlowingManager() {
        return registry.glowingManager;
    }

    public static void runTaskLater(Consumer<BukkitTask> runnable, int later) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, later);
    }

    public static EternaPlugin getPlugin() {
        return plugin;
    }

    public static Logger logger() {
        return getPlugin().getLogger();
    }

}
