package kz.hapyl.spigotutils.module.reflect.packet;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import kz.hapyl.spigotutils.module.annotate.TestedNMS;
import kz.hapyl.spigotutils.module.reflect.npc.ItemSlot;
import kz.hapyl.spigotutils.module.reflect.npc.NPCAnimation;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;

import java.util.List;

import static kz.hapyl.spigotutils.module.reflect.Reflect.*;

/**
 * This class is a shortcut to packet creating and sending.
 */
@TestedNMS(version = "1.18.2")
public final class Packets {

    private Packets() {
        throw new IllegalStateException();
    }

    /**
     * Represents all server packets, or 'Out'.
     */
    public final static class Server {

        public static void spawnEntityLiving(EntityLiving entity, Player... players) {
            sendPacket(new PacketPlayOutSpawnEntityLiving(entity), players);
        }

        public static void entityDestroy(Entity entity, Player... players) {
            destroyEntity(entity, players);
        }

        public static void entityTeleport(Entity entity, Player... players) {
            sendPacket(new PacketPlayOutEntityTeleport(entity), players);
        }

        public static void entityMetadata(Entity entity, DataWatcher dataWatcher, Player... players) {
            sendPacket(new PacketPlayOutEntityMetadata(getEntityId(entity), dataWatcher, true), players);
        }

        public static void animation(Entity entity, NPCAnimation type, Player... players) {
            sendPacket(new PacketPlayOutAnimation(entity, type.getPos()), players);
        }

        public static void entityEquipment(Entity entity, EntityEquipment equipment, Player... players) {
            final List<Pair<EnumItemSlot, ItemStack>> list = Lists.newArrayList();

            list.add(new Pair<>(ItemSlot.HEAD.getSlot(), bukkitItemToNMS(equipment.getHelmet())));
            list.add(new Pair<>(ItemSlot.CHEST.getSlot(), bukkitItemToNMS(equipment.getChestplate())));
            list.add(new Pair<>(ItemSlot.LEGS.getSlot(), bukkitItemToNMS(equipment.getLeggings())));
            list.add(new Pair<>(ItemSlot.FEET.getSlot(), bukkitItemToNMS(equipment.getBoots())));
            list.add(new Pair<>(ItemSlot.MAINHAND.getSlot(), bukkitItemToNMS(equipment.getItemInMainHand())));
            list.add(new Pair<>(ItemSlot.OFFHAND.getSlot(), bukkitItemToNMS(equipment.getItemInOffHand())));

            sendPacket(new PacketPlayOutEntityEquipment(getEntityId(entity), list), players);
        }

    }

    /**
     * Represents all client packets, or 'In'.
     */
    public static class Client {

        public static void clientPacketsNotYetImplemented() {
            throw new NotImplementedException("client packets not yet implemented");
        }

        static {
        }
    }


}
