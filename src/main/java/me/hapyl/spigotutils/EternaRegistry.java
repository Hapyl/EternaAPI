package me.hapyl.spigotutils;

import me.hapyl.spigotutils.config.PlayerConfigManager;
import me.hapyl.spigotutils.module.entity.RopeRegistry;
import me.hapyl.spigotutils.module.hologram.HologramRegistry;
import me.hapyl.spigotutils.module.inventory.item.CustomItemHolder;
import me.hapyl.spigotutils.module.parkour.ParkourManager;
import me.hapyl.spigotutils.module.player.song.SongPlayer;
import me.hapyl.spigotutils.module.reflect.glow.GlowingManager;
import me.hapyl.spigotutils.module.reflect.npc.HumanNPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EternaRegistry {

    private static boolean init;

    private final EternaPlugin plugin;

    // registries (should probably refactor for consistency)
    public final SongPlayer songPlayer;
    public final CustomItemHolder itemHolder;
    public final GlowingManager glowingManager;
    public final ParkourManager parkourManager;
    public final PlayerConfigManager configManager;
    public final HumanNPCRegistry npcRegistry;
    public final HologramRegistry hologramRegistry;
    public final RopeRegistry ropeRegistry;

    public EternaRegistry(EternaPlugin plugin) {
        if (init) {
            throw new IllegalStateException("registry already created!");
        }

        this.plugin = plugin;

        // register registries
        songPlayer = new SongPlayer(plugin);
        glowingManager = new GlowingManager(plugin);
        itemHolder = new CustomItemHolder(plugin);
        parkourManager = new ParkourManager(plugin);
        configManager = new PlayerConfigManager(plugin);
        npcRegistry = new HumanNPCRegistry(plugin);
        hologramRegistry = new HologramRegistry(plugin);
        ropeRegistry = new RopeRegistry(plugin);

        init = true;
    }

    public static RopeRegistry getRopeRegistry() {
        return current().ropeRegistry;
    }

    public static HologramRegistry getHologramRegistry() {
        return current().hologramRegistry;
    }

    public static HumanNPCRegistry getNpcRegistry() {
        return current().npcRegistry;
    }

    public static PlayerConfigManager getConfigManager() {
        return current().configManager;
    }

    public static SongPlayer getSongPlayer() {
        return current().songPlayer;
    }

    public static SongPlayer getNewSongPlayer(JavaPlugin plugin) {
        return new SongPlayer(plugin);
    }

    public static CustomItemHolder getItemHolder() {
        return current().itemHolder;
    }

    public static GlowingManager getGlowingManager() {
        return current().glowingManager;
    }

    public static ParkourManager getParkourManager() {
        return current().parkourManager;
    }

    //
    public static EternaRegistry current() {
        return EternaPlugin.getPlugin().getRegistry();
    }

    public EternaPlugin getPlugin() {
        return plugin;
    }

    public void onDisable() {
        runSafe(configManager::saveAllData, "config save");
        runSafe(parkourManager::restoreAllData, "parkour save");
        runSafe(npcRegistry::removeAll, "npc removal");
        runSafe(hologramRegistry::removeAll, "hologram removal");
        runSafe(ropeRegistry::removeAll, "rope removal");

        init = false;
    }

    private void register(Registry<?> registry, Registry<?> registry1) {

    }

    private void runSafe(Runnable runnable, String name) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            Bukkit.getLogger().severe("Unable to run '%s'! Did you /reload your server? {%s}".formatted(name, throwable.getMessage()));
        }
    }
}
