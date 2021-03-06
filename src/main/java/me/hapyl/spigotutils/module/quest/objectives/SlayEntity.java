package me.hapyl.spigotutils.module.quest.objectives;

import me.hapyl.spigotutils.module.chat.Chat;
import me.hapyl.spigotutils.module.quest.QuestObjective;
import me.hapyl.spigotutils.module.quest.QuestObjectiveType;
import me.hapyl.spigotutils.module.util.Validate;
import org.bukkit.entity.EntityType;

public class SlayEntity extends QuestObjective {

    private final EntityType entityType;

    public SlayEntity(EntityType type, long goalTotal) {
        super(
                QuestObjectiveType.SLAY_ENTITY,
                goalTotal,
                Chat.capitalize(type) + " Slayer",
                String.format("Slay %s %s.", goalTotal, Chat.capitalize(type))
        );
        Validate.isTrue(type.isAlive(), "entity must be living entity");
        this.entityType = type;
    }

    @Override
    public double testQuestCompletion(Object... objects) {
        return super.validateArgument(0, this.entityType, objects);
    }
}
